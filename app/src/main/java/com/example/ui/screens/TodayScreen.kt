package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AlertMode
import com.example.data.model.PlannerItemEntity
import com.example.data.model.PlannerItemType
import com.example.ui.PlannerUiState
import com.example.ui.components.DayStrip
import com.example.ui.components.PlannerItemCard
import com.example.ui.theme.AlabasterCanvas
import com.example.ui.theme.DeepInk
import com.example.ui.theme.ForestContainer
import com.example.ui.theme.ForestHabit
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IndigoBridge
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.OnForestContainer
import com.example.ui.theme.PureIvory
import com.example.ui.theme.SlateSecondary
import com.example.ui.theme.SoftLinen
import com.example.ui.theme.VermilionAccent
import com.example.util.DateTimeUtils

private enum class TodayFilter(val label: String) {
    ALL("All"),
    TIMED("Timed"),
    ALARMS("Alarms & Loud"),
    HABITS("Habits")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    uiState: PlannerUiState,
    onSelectDay: (Long) -> Unit,
    onToggleComplete: (PlannerItemEntity) -> Unit,
    onToggleAlarmEnabled: (PlannerItemEntity) -> Unit,
    onUpdateItemTime: (PlannerItemEntity, Int) -> Unit,
    onQuickSnoozeItem: (PlannerItemEntity, Int) -> Unit,
    onStartFocusTimer: (String, Int) -> Unit,
    onToggleFocusPause: () -> Unit,
    onAddFocusMinutes: (Int) -> Unit,
    onStopFocusTimer: () -> Unit,
    onEditItem: (PlannerItemEntity) -> Unit,
    onPushItemNow: (PlannerItemEntity) -> Unit,
    onSendDayBrief: () -> Unit,
    onQuickAddTask: (PlannerItemType) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf(TodayFilter.ALL) }
    var timeEditingItem by remember { mutableStateOf<PlannerItemEntity?>(null) }

    val habitStatsMap = uiState.habits.associateBy { it.item.id }
    val isViewingToday = uiState.selectedEpochDay == uiState.todayEpochDay
    val currentMinutes = DateTimeUtils.currentMinutesOfDay()

    val nextTimedItem = uiState.timedItemsForDay.firstOrNull { item ->
        val done = if (item.itemType == PlannerItemType.HABIT) {
            habitStatsMap[item.id]?.completedOnSelectedDay ?: false
        } else {
            item.isCompleted
        }
        !done && item.isEnabled && (!isViewingToday || (item.startMinutesOfDay ?: 0) >= currentMinutes)
    }

    val filteredTimed = uiState.timedItemsForDay.filter { item ->
        when (activeFilter) {
            TodayFilter.ALL, TodayFilter.TIMED -> true
            TodayFilter.ALARMS -> item.itemType == PlannerItemType.ALARM ||
                item.alertMode == AlertMode.ALARM ||
                item.alertMode == AlertMode.LOUD_NOTIFICATION
            TodayFilter.HABITS -> item.itemType == PlannerItemType.HABIT
        }
    }

    val filteredAnytime = uiState.anytimeTasksForDay.filter { item ->
        when (activeFilter) {
            TodayFilter.ALL -> true
            TodayFilter.TIMED -> false
            TodayFilter.ALARMS -> item.alertMode == AlertMode.ALARM || item.alertMode == AlertMode.LOUD_NOTIFICATION
            TodayFilter.HABITS -> item.itemType == PlannerItemType.HABIT
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AlabasterCanvas),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date Header & Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = DateTimeUtils.formatMonthYear(uiState.selectedEpochDay).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = VermilionAccent
                        )
                        Text(
                            text = DateTimeUtils.formatFullDate(uiState.selectedEpochDay),
                            style = MaterialTheme.typography.headlineMedium,
                            color = DeepInk
                        )
                    }

                    if (!isViewingToday) {
                        OutlinedButton(
                            onClick = { onSelectDay(uiState.todayEpochDay) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Today", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                DayStrip(
                    todayEpochDay = uiState.todayEpochDay,
                    selectedEpochDay = uiState.selectedEpochDay,
                    onSelectDay = onSelectDay
                )
            }
        }

        // Cherry-on-top: Active Focus Timer Strip
        if (uiState.focusTimer.isActive) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = ForestContainer,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ForestHabit.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FOCUS SPRINT",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnForestContainer
                            )
                            Text(
                                text = uiState.focusTimer.taskTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = DeepInk
                            )
                        }
                        Text(
                            text = uiState.focusTimer.formattedRemaining,
                            style = MaterialTheme.typography.headlineSmall,
                            color = OnForestContainer
                        )
                        IconButton(onClick = onToggleFocusPause) {
                            Icon(
                                imageVector = if (uiState.focusTimer.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (uiState.focusTimer.isRunning) "Pause focus" else "Resume focus",
                                tint = OnForestContainer
                            )
                        }
                        TextButton(onClick = { onAddFocusMinutes(5) }) {
                            Text("+5m", color = OnForestContainer)
                        }
                        IconButton(onClick = onStopFocusTimer) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Stop focus timer",
                                tint = OnForestContainer
                            )
                        }
                    }
                }
            }
        }

        // Clean Day Overview + Next Up Countdown + Send Brief
        item {
            val total = uiState.totalCountForDay
            val done = uiState.completedCountForDay
            val progress = if (total > 0) done.toFloat() / total.toFloat() else 0f

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = PureIvory,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, HairlineBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "TODAY'S TASKS",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateSecondary
                            )
                            Text(
                                text = if (total == 0) "Ready to plan" else "$done of $total completed",
                                style = MaterialTheme.typography.titleLarge,
                                color = DeepInk
                            )
                        }

                        Button(
                            onClick = onSendDayBrief,
                            enabled = !uiState.isPushingToLaptop,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IndigoBridge,
                                contentColor = PureIvory
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("send_day_brief_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Computer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = if (uiState.isPushingToLaptop) "Sending..." else "Send Brief",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50)),
                        color = ForestHabit,
                        trackColor = SoftLinen
                    )

                    if (nextTimedItem != null && nextTimedItem.startMinutesOfDay != null) {
                        val deltaMins = nextTimedItem.startMinutesOfDay - currentMinutes
                        val countdownText = when {
                            !isViewingToday -> DateTimeUtils.formatMinutes(nextTimedItem.startMinutesOfDay)
                            deltaMins <= 0 -> "Due now"
                            deltaMins < 60 -> "In ${deltaMins}m (${DateTimeUtils.formatMinutes(nextTimedItem.startMinutesOfDay)})"
                            else -> "At ${DateTimeUtils.formatMinutes(nextTimedItem.startMinutesOfDay)}"
                        }
                        Surface(
                            color = IndigoContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = IndigoBridge,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "$countdownText · ${nextTimedItem.title}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IndigoBridge,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { onQuickSnoozeItem(nextTimedItem, 10) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("+10m", style = MaterialTheme.typography.labelMedium, color = IndigoBridge)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Clean Filter Row
        if (uiState.allDayActiveItems.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TodayFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = { activeFilter = filter },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepInk,
                                selectedLabelColor = PureIvory
                            )
                        )
                    }
                }
            }
        }

        // Empty State
        if (uiState.timedItemsForDay.isEmpty() && uiState.anytimeTasksForDay.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = PureIvory,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, HairlineBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .background(SoftLinen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = VermilionAccent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Text(
                            text = "Add your tasks for today",
                            style = MaterialTheme.typography.headlineSmall,
                            color = DeepInk
                        )
                        Text(
                            text = "Add 2 to 3 tasks, set your own custom time under each, and choose Normal, Snooze, Loud Alert, or Full Alarm. Reminders arrive on both your phone and laptop.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateSecondary
                        )
                        Button(
                            onClick = { onQuickAddTask(PlannerItemType.TASK) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VermilionAccent,
                                contentColor = PureIvory
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("empty_add_task")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Add Tasks for Today")
                        }
                    }
                }
            }
        }

        // Timed Tasks, Events & Alarms
        if (filteredTimed.isNotEmpty()) {
            item {
                Text(
                    text = "SCHEDULED (${filteredTimed.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = SlateSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            items(filteredTimed, key = { "timed_${it.id}" }) { item ->
                val habitStat = habitStatsMap[item.id]
                val isDone = if (item.itemType == PlannerItemType.HABIT) {
                    habitStat?.completedOnSelectedDay ?: false
                } else {
                    item.isCompleted
                }
                PlannerItemCard(
                    item = item,
                    isCompleted = isDone,
                    streakDays = habitStat?.streakDays ?: 0,
                    onToggleComplete = { onToggleComplete(item) },
                    onToggleAlarmSwitch = if (item.itemType == PlannerItemType.ALARM || item.alertMode == AlertMode.ALARM) {
                        { onToggleAlarmEnabled(item) }
                    } else null,
                    onPickCustomTime = { timeEditingItem = item },
                    onStartFocus = { onStartFocusTimer(item.title, 25) },
                    onEdit = { onEditItem(item) },
                    onPushNow = { onPushItemNow(item) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Anytime Tasks
        if (filteredAnytime.isNotEmpty()) {
            item {
                Text(
                    text = "ANYTIME TODAY (${filteredAnytime.count { !it.isCompleted }})",
                    style = MaterialTheme.typography.labelLarge,
                    color = SlateSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            items(filteredAnytime, key = { "anytime_${it.id}" }) { task ->
                PlannerItemCard(
                    item = task,
                    isCompleted = task.isCompleted,
                    onToggleComplete = { onToggleComplete(task) },
                    onPickCustomTime = { timeEditingItem = task },
                    onStartFocus = { onStartFocusTimer(task.title, 25) },
                    onEdit = { onEditItem(task) },
                    onPushNow = { onPushItemNow(task) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    // 2-tap inline custom time picker when tapping any task's time pill
    val targetItem = timeEditingItem
    if (targetItem != null) {
        val initialMins = targetItem.startMinutesOfDay ?: ((currentMinutes / 60 + 1) * 60).coerceAtMost(23 * 60)
        val pickerState = rememberTimePickerState(
            initialHour = initialMins / 60,
            initialMinute = initialMins % 60,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { timeEditingItem = null },
            title = { Text("Set custom time") },
            confirmButton = {
                TextButton(onClick = {
                    val picked = pickerState.hour * 60 + pickerState.minute
                    onUpdateItemTime(targetItem, picked)
                    timeEditingItem = null
                }) { Text("Save Time") }
            },
            dismissButton = {
                TextButton(onClick = { timeEditingItem = null }) { Text("Cancel") }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = pickerState)
                }
            }
        )
    }
}
