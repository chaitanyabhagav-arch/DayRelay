package com.example.data.model

enum class PlannerItemType(val label: String) {
    TASK("Task"),
    EVENT("Timed Event"),
    ALARM("Alarm"),
    HABIT("Habit")
}

enum class AlertMode(val label: String, val shortDesc: String) {
    NORMAL("Normal", "Standard phone & laptop ping"),
    SNOOZE("Snooze", "Heads-up with quick snooze button"),
    LOUD_NOTIFICATION("Loud Alert", "Loud sound & vibration notification"),
    ALARM("Full Alarm", "Full-screen ringing alarm")
}

enum class PriorityLevel(val label: String, val ntfyPriority: Int) {
    LOW("Low", 2),
    MEDIUM("Medium", 3),
    HIGH("High", 4),
    URGENT("Urgent", 5)
}

enum class BridgeProviderType(val label: String) {
    NTFY("ntfy.sh / Self-Hosted"),
    DISCORD("Discord Webhook"),
    SLACK("Slack Webhook"),
    CUSTOM_JSON("Custom JSON Webhook")
}

enum class DeliveryStatus(val label: String) {
    SENT("Delivered"),
    FAILED("Failed"),
    SKIPPED("Skipped")
}
