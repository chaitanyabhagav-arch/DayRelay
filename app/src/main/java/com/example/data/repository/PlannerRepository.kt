package com.example.data.repository

import com.example.data.local.BridgePreferencesDataStore
import com.example.data.local.PlannerDao
import com.example.data.model.BridgeConfig
import com.example.data.model.DailyNoteEntity
import com.example.data.model.DeliveryLogEntity
import com.example.data.model.HabitCompletionEntity
import com.example.data.model.PlannerItemEntity
import com.example.data.model.PlannerItemType
import com.example.data.model.PriorityLevel
import com.example.network.LaptopPushClient
import com.example.network.PushResult
import com.example.scheduler.ExactAlarmScheduler
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow

class PlannerRepository(
    private val dao: PlannerDao,
    private val bridgePrefs: BridgePreferencesDataStore,
    private val pushClient: LaptopPushClient,
    private val scheduler: ExactAlarmScheduler
) {

    val allItems: Flow<List<PlannerItemEntity>> = dao.observeAllItems()
    val allHabitCompletions: Flow<List<HabitCompletionEntity>> = dao.observeAllHabitCompletions()
    val allNotes: Flow<List<DailyNoteEntity>> = dao.observeAllNotes()
    val deliveryLogs: Flow<List<DeliveryLogEntity>> = dao.observeDeliveryLogs()
    val bridgeConfig: Flow<BridgeConfig> = bridgePrefs.configFlow

    suspend fun ensureDefaultBridgeTopic(): String {
        return bridgePrefs.ensureDefaultTopicIfEmpty()
    }

    fun generateFreshTopic(): String = bridgePrefs.generateFreshTopicName()

    suspend fun saveBridgeConfig(config: BridgeConfig) {
        bridgePrefs.saveConfig(config)
    }

    suspend fun upsertItem(item: PlannerItemEntity): Long {
        val rowId = dao.upsertItem(item)
        val finalId = if (item.id == 0L) rowId else item.id
        val savedItem = item.copy(id = finalId)
        scheduler.scheduleItem(savedItem)
        return finalId
    }

    suspend fun toggleTaskCompleted(item: PlannerItemEntity) {
        val next = !item.isCompleted
        dao.setItemCompleted(item.id, next)
        if (next) {
            scheduler.cancelItem(item.id)
        } else {
            scheduler.scheduleItem(item.copy(isCompleted = false))
        }
    }

    suspend fun toggleItemEnabled(item: PlannerItemEntity) {
        val next = !item.isEnabled
        dao.setItemEnabled(item.id, next)
        if (next) {
            scheduler.scheduleItem(item.copy(isEnabled = true))
        } else {
            scheduler.cancelItem(item.id)
        }
    }

    suspend fun snoozeItemMinutes(item: PlannerItemEntity, minutes: Int) {
        val base = item.startMinutesOfDay ?: DateTimeUtils.currentMinutesOfDay()
        val updated = item.copy(startMinutesOfDay = (base + minutes) % 1440, isCompleted = false)
        dao.upsertItem(updated)
        scheduler.scheduleItem(updated)
    }

    suspend fun toggleHabitForDay(itemId: Long, epochDay: Long, currentlyCompleted: Boolean) {
        if (currentlyCompleted) {
            dao.deleteHabitCompletion(itemId, epochDay)
        } else {
            dao.insertHabitCompletion(HabitCompletionEntity(itemId = itemId, epochDay = epochDay))
        }
    }

    suspend fun deleteItem(id: Long) {
        scheduler.cancelItem(id)
        dao.deleteItemById(id)
    }

    suspend fun saveDailyNote(epochDay: Long, content: String, priorityFocus: String) {
        dao.upsertNote(
            DailyNoteEntity(
                epochDay = epochDay,
                content = content,
                priorityFocus = priorityFocus,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun pushItemToLaptopNow(item: PlannerItemEntity): PushResult {
        val config = bridgePrefs.getConfigOnce()
        val result = pushClient.sendItemReminder(config, item, isSnoozed = false)
        logPushResult(item.id, item.title, config, result)
        return result
    }

    suspend fun pushFocusDoneToLaptop(taskTitle: String, minutes: Int): PushResult {
        val config = bridgePrefs.getConfigOnce()
        val title = "Focus Complete ($minutes min) · $taskTitle"
        val message = "Your $minutes-minute focus block for \"$taskTitle\" just finished."
        val result = pushClient.sendMessage(
            config = config,
            title = title,
            message = message,
            priority = PriorityLevel.HIGH,
            ntfyTags = "hourglass_flowing_sand,white_check_mark",
            itemTypeLabel = "Focus Timer"
        )
        logPushResult(null, title, config, result)
        return result
    }

    suspend fun sendTestPingToLaptop(): PushResult {
        val config = bridgePrefs.getConfigOnce()
        val title = "DayRelay Connected to Laptop"
        val message = "Your Android planner is linked and ready to deliver scheduled task reminders, loud alerts, and alarms here."
        val result = pushClient.sendMessage(
            config = config,
            title = title,
            message = message,
            priority = PriorityLevel.HIGH,
            ntfyTags = "rocket,computer",
            itemTypeLabel = "Connection Test"
        )
        logPushResult(null, title, config, result)
        return result
    }

    suspend fun sendDayBriefToLaptop(
        epochDay: Long,
        itemsForDay: List<PlannerItemEntity>,
        note: DailyNoteEntity?
    ): PushResult {
        val config = bridgePrefs.getConfigOnce()
        val dateLabel = DateTimeUtils.formatFullDate(epochDay)
        val pending = itemsForDay.filter { !it.isCompleted }
        val events = pending.filter { it.itemType == PlannerItemType.EVENT }
        val tasks = pending.filter { it.itemType == PlannerItemType.TASK }
        val alarms = pending.filter { it.itemType == PlannerItemType.ALARM && it.isEnabled }

        val message = buildString {
            if (note != null && note.priorityFocus.isNotBlank()) {
                append("Focus: ${note.priorityFocus.trim()}\n\n")
            }
            if (events.isNotEmpty()) {
                append("Timed Events (${events.size}):\n")
                events.forEach { ev ->
                    val time = ev.startMinutesOfDay?.let {
                        DateTimeUtils.formatDurationRange(it, ev.endMinutesOfDay)
                    } ?: "Timed"
                    append("• $time — ${ev.title}\n")
                }
                append("\n")
            }
            if (tasks.isNotEmpty()) {
                append("Tasks (${tasks.size}):\n")
                tasks.forEach { t ->
                    val time = t.startMinutesOfDay?.let { DateTimeUtils.formatMinutes(it) } ?: "Anytime"
                    append("• [$time · ${t.priority.label} · ${t.alertMode.label}] ${t.title}\n")
                }
                append("\n")
            }
            if (alarms.isNotEmpty()) {
                append("Active Alarms (${alarms.size}): ")
                append(alarms.joinToString(", ") { a ->
                    val time = a.startMinutesOfDay?.let { DateTimeUtils.formatMinutes(it) } ?: ""
                    "$time ${a.title}"
                })
            }
            if (pending.isEmpty()) {
                append("All scheduled items for $dateLabel are complete!")
            }
        }.trim()

        val title = "DayRelay Agenda · $dateLabel"
        val result = pushClient.sendMessage(
            config = config,
            title = title,
            message = message,
            priority = PriorityLevel.MEDIUM,
            ntfyTags = "calendar,spiral_notepad",
            itemTypeLabel = "Daily Brief",
            timeLabel = dateLabel
        )
        logPushResult(null, title, config, result)
        return result
    }

    suspend fun pushNoteToLaptop(epochDay: Long, priorityFocus: String, content: String): PushResult {
        val config = bridgePrefs.getConfigOnce()
        val dateLabel = DateTimeUtils.formatFullDate(epochDay)
        val title = "DayRelay Notes · $dateLabel"
        val message = buildString {
            if (priorityFocus.isNotBlank()) {
                append("Top Focus: ${priorityFocus.trim()}\n\n")
            }
            append(content.trim().ifBlank { "No note content." })
        }
        val result = pushClient.sendMessage(
            config = config,
            title = title,
            message = message,
            priority = PriorityLevel.MEDIUM,
            ntfyTags = "memo,computer",
            itemTypeLabel = "Daily Note",
            timeLabel = dateLabel
        )
        logPushResult(null, title, config, result)
        return result
    }

    suspend fun clearDeliveryLogs() {
        dao.clearDeliveryLogs()
    }

    private suspend fun logPushResult(
        itemId: Long?,
        title: String,
        config: BridgeConfig,
        result: PushResult
    ) {
        dao.insertDeliveryLog(
            DeliveryLogEntity(
                itemId = itemId,
                title = title,
                summary = result.detail,
                provider = config.providerType,
                status = result.status,
                httpCode = result.httpCode,
                errorDetail = if (result.status.name == "FAILED") result.detail else null
            )
        )
    }
}
