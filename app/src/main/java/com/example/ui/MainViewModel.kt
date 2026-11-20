package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.DayRelayApplication
import com.example.data.model.BridgeConfig
import com.example.data.model.DailyNoteEntity
import com.example.data.model.DeliveryLogEntity
import com.example.data.model.DeliveryStatus
import com.example.data.model.HabitCompletionEntity
import com.example.data.model.PlannerItemEntity
import com.example.data.model.PlannerItemType
import com.example.data.repository.PlannerRepository
import com.example.scheduler.NotificationHelper
import com.example.util.DateTimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitWithStats(
    val item: PlannerItemEntity,
    val completedOnSelectedDay: Boolean,
    val streakDays: Int,
    val lastSevenDays: List<Pair<Long, Boolean>>
)

data class FocusTimerState(
    val taskTitle: String = "",
    val totalMinutes: Int = 25,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false
) {
    val isActive: Boolean get() = remainingSeconds > 0 || isRunning
    val formattedRemaining: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return String.format(java.util.Locale.getDefault(), "%02d:%02d", mins, secs)
        }
}

data class PlannerUiState(
    val selectedEpochDay: Long = DateTimeUtils.todayEpochDay(),
    val todayEpochDay: Long = DateTimeUtils.todayEpochDay(),
    val timedItemsForDay: List<PlannerItemEntity> = emptyList(),
    val anytimeTasksForDay: List<PlannerItemEntity> = emptyList(),
    val allDayActiveItems: List<PlannerItemEntity> = emptyList(),
    val alarms: List<PlannerItemEntity> = emptyList(),
    val habits: List<HabitWithStats> = emptyList(),
    val noteForSelectedDay: DailyNoteEntity? = null,
    val recentNotes: List<DailyNoteEntity> = emptyList(),
    val bridgeConfig: BridgeConfig = BridgeConfig(),
    val deliveryLogs: List<DeliveryLogEntity> = emptyList(),
    val isPushingToLaptop: Boolean = false,
    val focusTimer: FocusTimerState = FocusTimerState()
) {
    val completedCountForDay: Int
        get() {
            val nonHabitDone = allDayActiveItems
                .filter { it.itemType != PlannerItemType.HABIT && it.isCompleted }
                .size
            val habitDone = habits.count { h ->
                DateTimeUtils.matchesRepeatMask(
                    selectedEpochDay,
                    h.item.repeatMask.takeIf { it != 0 } ?: DateTimeUtils.MASK_EVERY_DAY
                ) && h.completedOnSelectedDay
            }
            return nonHabitDone + habitDone
        }

    val totalCountForDay: Int
        get() {
            val nonHabit = allDayActiveItems.count { it.itemType != PlannerItemType.HABIT }
            val dayHabits = habits.count { h ->
                DateTimeUtils.matchesRepeatMask(
                    selectedEpochDay,
                    h.item.repeatMask.takeIf { it != 0 } ?: DateTimeUtils.MASK_EVERY_DAY
                )
            }
            return nonHabit + dayHabits
        }
}

class MainViewModel(
    application: Application,
    private val repository: PlannerRepository
) : AndroidViewModel(application) {

    private val selectedEpochDay = MutableStateFlow(DateTimeUtils.todayEpochDay())
    private val isPushing = MutableStateFlow(false)
    private val focusTimer = MutableStateFlow(FocusTimerState())
    private var focusTickerJob: Job? = null

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultBridgeTopic()
        }
    }

    val uiState: StateFlow<PlannerUiState> = combine(
        selectedEpochDay,
        repository.allItems,
        repository.allHabitCompletions,
        repository.allNotes,
        repository.bridgeConfig,
        repository.deliveryLogs,
        isPushing,
        focusTimer
    ) { values ->
        val day = values[0] as Long
        @Suppress("UNCHECKED_CAST")
        val allItems = values[1] as List<PlannerItemEntity>
        @Suppress("UNCHECKED_CAST")
        val habitCompletions = values[2] as List<HabitCompletionEntity>
        @Suppress("UNCHECKED_CAST")
        val allNotes = values[3] as List<DailyNoteEntity>
        val config = values[4] as BridgeConfig
        @Suppress("UNCHECKED_CAST")
        val logs = values[5] as List<DeliveryLogEntity>
        val pushing = values[6] as Boolean
        val timer = values[7] as FocusTimerState

        val today = DateTimeUtils.todayEpochDay()
        val completionMap = habitCompletions.groupBy { it.itemId }
            .mapValues { entry -> entry.value.map { it.epochDay }.toSet() }

        val itemsForDay = allItems.filter { item ->
            when {
                item.itemType == PlannerItemType.HABIT -> {
                    val mask = item.repeatMask.takeIf { it != 0 } ?: DateTimeUtils.MASK_EVERY_DAY
                    DateTimeUtils.matchesRepeatMask(day, mask)
                }
                item.repeatMask != 0 -> DateTimeUtils.matchesRepeatMask(day, item.repeatMask)
                else -> item.dateEpochDay == day
            }
        }

        val timed = itemsForDay
            .filter { it.startMinutesOfDay != null }
            .sortedWith(compareBy<PlannerItemEntity> { it.startMinutesOfDay ?: 0 }.thenBy { it.createdAtMillis })

        val anytime = itemsForDay
            .filter { it.startMinutesOfDay == null && it.itemType != PlannerItemType.HABIT }

        val alarms = allItems
            .filter { it.itemType == PlannerItemType.ALARM }
            .sortedBy { it.startMinutesOfDay ?: 0 }

        val habits = allItems
            .filter { it.itemType == PlannerItemType.HABIT }
            .map { habit ->
                val completedDays = completionMap[habit.id].orEmpty()
                val mask = habit.repeatMask.takeIf { it != 0 } ?: DateTimeUtils.MASK_EVERY_DAY
                val streak = computeHabitStreak(today, mask, completedDays)
                val lastSeven = (6 downTo 0).map { offset ->
                    val d = day - offset
                    d to completedDays.contains(d)
                }
                HabitWithStats(
                    item = habit,
                    completedOnSelectedDay = completedDays.contains(day),
                    streakDays = streak,
                    lastSevenDays = lastSeven
                )
            }

        PlannerUiState(
            selectedEpochDay = day,
            todayEpochDay = today,
            timedItemsForDay = timed,
            anytimeTasksForDay = anytime,
            allDayActiveItems = itemsForDay,
            alarms = alarms,
            habits = habits,
            noteForSelectedDay = allNotes.firstOrNull { it.epochDay == day },
            recentNotes = allNotes.filter { it.content.isNotBlank() || it.priorityFocus.isNotBlank() }.take(14),
            bridgeConfig = config,
            deliveryLogs = logs,
            isPushingToLaptop = pushing,
            focusTimer = timer
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlannerUiState()
    )

    private fun computeHabitStreak(
        today: Long,
        repeatMask: Int,
        completedDays: Set<Long>
    ): Int {
        var streak = 0
        var cursor = if (!completedDays.contains(today)) today - 1 else today
        for (i in 0..365) {
            val day = cursor - i
            if (DateTimeUtils.matchesRepeatMask(day, repeatMask)) {
                if (completedDays.contains(day)) streak++ else break
            }
        }
        return streak
    }

    fun selectDay(epochDay: Long) {
        selectedEpochDay.value = epochDay
    }

    fun saveItems(items: List<PlannerItemEntity>, alsoPushNow: Boolean = false) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val savedList = items.map { item ->
                val id = repository.upsertItem(item)
                item.copy(id = id)
            }
            _messages.tryEmit(
                if (savedList.size == 1) "Saved \"${savedList.first().title}\""
                else "Saved ${savedList.size} tasks for the day"
            )
            if (alsoPushNow) {
                isPushing.value = true
                var lastResultMsg: String? = null
                savedList.filter { it.notifyLaptop }.forEach { saved ->
                    val res = repository.pushItemToLaptopNow(saved)
                    lastResultMsg = if (res.status == DeliveryStatus.SENT) {
                        "Sent reminder to laptop"
                    } else {
                        "Laptop push issue: ${res.detail}"
                    }
                }
                isPushing.value = false
                lastResultMsg?.let { _messages.tryEmit(it) }
            }
        }
    }

    fun updateItemStartTime(item: PlannerItemEntity, newStartMinutes: Int) {
        viewModelScope.launch {
            val newEnd = item.endMinutesOfDay?.let { oldEnd ->
                val oldStart = item.startMinutesOfDay ?: newStartMinutes
                val dur = (oldEnd - oldStart).coerceAtLeast(15)
                (newStartMinutes + dur).coerceAtMost(1439)
            }
            val updated = item.copy(
                startMinutesOfDay = newStartMinutes,
                endMinutesOfDay = newEnd,
                isEnabled = true
            )
            repository.upsertItem(updated)
            _messages.tryEmit("Time updated to ${DateTimeUtils.formatMinutes(newStartMinutes)}")
        }
    }

    fun quickSnoozeItem(item: PlannerItemEntity, minutes: Int = 10) {
        viewModelScope.launch {
            repository.snoozeItemMinutes(item, minutes)
            _messages.tryEmit("Moved \"${item.title}\" +${minutes}m")
        }
    }

    fun toggleTaskOrEventCompleted(item: PlannerItemEntity) {
        viewModelScope.launch {
            if (item.itemType == PlannerItemType.HABIT) {
                val state = uiState.value
                val currentlyDone = state.habits.firstOrNull { it.item.id == item.id }?.completedOnSelectedDay ?: false
                repository.toggleHabitForDay(item.id, state.selectedEpochDay, currentlyDone)
            } else {
                repository.toggleTaskCompleted(item)
            }
        }
    }

    fun toggleHabitForSpecificDay(itemId: Long, epochDay: Long, currentlyCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleHabitForDay(itemId, epochDay, currentlyCompleted)
        }
    }

    fun toggleAlarmEnabled(item: PlannerItemEntity) {
        viewModelScope.launch {
            repository.toggleItemEnabled(item)
        }
    }

    fun deleteItem(item: PlannerItemEntity) {
        viewModelScope.launch {
            repository.deleteItem(item.id)
            _messages.tryEmit("Deleted \"${item.title}\"")
        }
    }

    fun pushItemToLaptopNow(item: PlannerItemEntity) {
        viewModelScope.launch {
            isPushing.value = true
            val res = repository.pushItemToLaptopNow(item)
            isPushing.value = false
            _messages.tryEmit(
                if (res.status == DeliveryStatus.SENT) "Sent \"${item.title}\" to laptop"
                else "Failed to send: ${res.detail}"
            )
        }
    }

    fun sendDayBriefToLaptop() {
        viewModelScope.launch {
            val state = uiState.value
            isPushing.value = true
            val res = repository.sendDayBriefToLaptop(
                epochDay = state.selectedEpochDay,
                itemsForDay = state.allDayActiveItems,
                note = state.noteForSelectedDay
            )
            isPushing.value = false
            _messages.tryEmit(
                if (res.status == DeliveryStatus.SENT) "Daily brief delivered to laptop"
                else "Could not send brief: ${res.detail}"
            )
        }
    }

    // Cherry-on-top Focus Timer
    fun startFocusTimer(taskTitle: String, minutes: Int = 25) {
        focusTickerJob?.cancel()
        focusTimer.value = FocusTimerState(
            taskTitle = taskTitle,
            totalMinutes = minutes,
            remainingSeconds = minutes * 60,
            isRunning = true
        )
        runFocusTicker()
        _messages.tryEmit("Focus sprint started ($minutes min)")
    }

    fun toggleFocusTimerPause() {
        val current = focusTimer.value
        if (current.remainingSeconds <= 0) return
        val nextRunning = !current.isRunning
        focusTimer.value = current.copy(isRunning = nextRunning)
        if (nextRunning) {
            runFocusTicker()
        } else {
            focusTickerJob?.cancel()
        }
    }

    fun addFocusMinutes(extraMinutes: Int = 5) {
        val current = focusTimer.value
        if (!current.isActive) return
        focusTimer.value = current.copy(
            totalMinutes = current.totalMinutes + extraMinutes,
            remainingSeconds = current.remainingSeconds + extraMinutes * 60
        )
    }

    fun stopFocusTimer() {
        focusTickerJob?.cancel()
        focusTimer.value = FocusTimerState()
    }

    private fun runFocusTicker() {
        focusTickerJob?.cancel()
        focusTickerJob = viewModelScope.launch {
            while (focusTimer.value.isRunning && focusTimer.value.remainingSeconds > 0) {
                delay(1_000L)
                val current = focusTimer.value
                if (!current.isRunning) break
                val next = (current.remainingSeconds - 1).coerceAtLeast(0)
                if (next == 0) {
                    focusTimer.value = FocusTimerState()
                    val app = getApplication<DayRelayApplication>()
                    NotificationHelper.showFocusCompleteNotification(
                        context = app,
                        taskTitle = current.taskTitle,
                        minutes = current.totalMinutes
                    )
                    repository.pushFocusDoneToLaptop(current.taskTitle, current.totalMinutes)
                    _messages.tryEmit("Focus complete! Notified phone & laptop")
                    break
                } else {
                    focusTimer.value = current.copy(remainingSeconds = next)
                }
            }
        }
    }

    fun saveBridgeConfig(config: BridgeConfig) {
        viewModelScope.launch {
            repository.saveBridgeConfig(config)
            _messages.tryEmit("Laptop bridge settings saved")
        }
    }

    fun generateFreshTopicAndSave(current: BridgeConfig) {
        val fresh = repository.generateFreshTopic()
        saveBridgeConfig(current.copy(ntfyTopic = fresh))
    }

    fun sendTestPingToLaptop() {
        viewModelScope.launch {
            isPushing.value = true
            val res = repository.sendTestPingToLaptop()
            isPushing.value = false
            _messages.tryEmit(
                if (res.status == DeliveryStatus.SENT) "Test ping received by laptop!"
                else "Test ping failed: ${res.detail}"
            )
        }
    }

    fun clearDeliveryLogs() {
        viewModelScope.launch {
            repository.clearDeliveryLogs()
            _messages.tryEmit("Delivery log cleared")
        }
    }

    fun saveDailyNote(epochDay: Long, content: String, priorityFocus: String) {
        viewModelScope.launch {
            repository.saveDailyNote(epochDay, content, priorityFocus)
            _messages.tryEmit("Note saved")
        }
    }

    fun pushNoteToLaptop(epochDay: Long, priorityFocus: String, content: String) {
        viewModelScope.launch {
            repository.saveDailyNote(epochDay, content, priorityFocus)
            isPushing.value = true
            val res = repository.pushNoteToLaptop(epochDay, priorityFocus, content)
            isPushing.value = false
            _messages.tryEmit(
                if (res.status == DeliveryStatus.SENT) "Notes pushed to laptop"
                else "Push failed: ${res.detail}"
            )
        }
    }

    class Factory(
        private val application: Application,
        private val repository: PlannerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(application, repository) as T
        }
    }
}
