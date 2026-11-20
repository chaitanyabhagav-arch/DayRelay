package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planner_items")
data class PlannerItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String = "",
    val itemType: PlannerItemType = PlannerItemType.TASK,
    val alertMode: AlertMode = AlertMode.SNOOZE,
    val priority: PriorityLevel = PriorityLevel.MEDIUM,
    val dateEpochDay: Long? = null,
    val startMinutesOfDay: Int? = null,
    val endMinutesOfDay: Int? = null,
    val repeatMask: Int = 0,
    val ringtoneUri: String? = null,
    val ringtoneTitle: String = "Default Alarm",
    val snoozeMinutes: Int = 10,
    val notifyLaptop: Boolean = true,
    val isCompleted: Boolean = false,
    val isEnabled: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["itemId", "epochDay"]
)
data class HabitCompletionEntity(
    val itemId: Long,
    val epochDay: Long,
    val completedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_notes")
data class DailyNoteEntity(
    @PrimaryKey val epochDay: Long,
    val content: String = "",
    val priorityFocus: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "delivery_logs")
data class DeliveryLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val itemId: Long? = null,
    val title: String,
    val summary: String,
    val provider: BridgeProviderType,
    val status: DeliveryStatus,
    val httpCode: Int? = null,
    val errorDetail: String? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class BridgeConfig(
    val bridgeEnabled: Boolean = true,
    val providerType: BridgeProviderType = BridgeProviderType.NTFY,
    val ntfyBaseUrl: String = "https://ntfy.sh",
    val ntfyTopic: String = "",
    val authToken: String = "",
    val webhookUrl: String = "",
    val defaultSnoozeMinutes: Int = 10,
    val includePriorityBadges: Boolean = true
) {
    val isConfigured: Boolean
        get() = when (providerType) {
            BridgeProviderType.NTFY -> ntfyBaseUrl.isNotBlank() && ntfyTopic.isNotBlank()
            else -> webhookUrl.isNotBlank()
        }

    val ntfySubscriptionUrl: String
        get() {
            val base = ntfyBaseUrl.trim().trimEnd('/')
            val topic = ntfyTopic.trim().trimStart('/')
            return if (base.isBlank() || topic.isBlank()) "" else "$base/$topic"
        }
}
