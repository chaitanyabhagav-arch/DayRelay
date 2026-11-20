package com.example.network

import com.example.data.model.AlertMode
import com.example.data.model.BridgeConfig
import com.example.data.model.BridgeProviderType
import com.example.data.model.DeliveryStatus
import com.example.data.model.PlannerItemEntity
import com.example.data.model.PlannerItemType
import com.example.data.model.PriorityLevel
import com.example.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class PushResult(
    val status: DeliveryStatus,
    val httpCode: Int? = null,
    val detail: String
)

class LaptopPushClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()
) {

    suspend fun sendItemReminder(
        config: BridgeConfig,
        item: PlannerItemEntity,
        isSnoozed: Boolean = false
    ): PushResult {
        val timeText = item.startMinutesOfDay?.let {
            DateTimeUtils.formatDurationRange(it, item.endMinutesOfDay)
        } ?: "Anytime today"

        val typeTag = when (item.itemType) {
            PlannerItemType.ALARM -> "Alarm"
            PlannerItemType.EVENT -> "Event"
            PlannerItemType.HABIT -> "Habit"
            PlannerItemType.TASK -> "Task"
        }

        val prefix = if (isSnoozed) "[Snoozed $typeTag] " else "[$typeTag · $timeText] "
        val title = "$prefix${item.title}"
        val body = buildString {
            append("Scheduled: $timeText")
            append(" · Alert: ${item.alertMode.label}")
            if (config.includePriorityBadges) {
                append(" · Priority: ${item.priority.label}")
            }
            if (item.description.isNotBlank()) {
                append("\n")
                append(item.description.trim())
            }
        }

        val effectivePriority = when (item.alertMode) {
            AlertMode.ALARM, AlertMode.LOUD_NOTIFICATION -> {
                if (item.priority == PriorityLevel.LOW || item.priority == PriorityLevel.MEDIUM) {
                    PriorityLevel.HIGH
                } else {
                    item.priority
                }
            }
            else -> item.priority
        }

        val tags = when {
            item.alertMode == AlertMode.ALARM -> "alarm_clock,rotating_light"
            item.alertMode == AlertMode.LOUD_NOTIFICATION -> "loudspeaker,bell"
            item.itemType == PlannerItemType.EVENT -> "calendar,clock3"
            item.itemType == PlannerItemType.HABIT -> "repeat,white_check_mark"
            else -> "memo,bell"
        }

        return sendMessage(
            config = config,
            title = title,
            message = body,
            priority = effectivePriority,
            ntfyTags = tags,
            itemTypeLabel = item.itemType.label,
            timeLabel = timeText
        )
    }

    suspend fun sendMessage(
        config: BridgeConfig,
        title: String,
        message: String,
        priority: PriorityLevel = PriorityLevel.MEDIUM,
        ntfyTags: String = "bell,computer",
        itemTypeLabel: String = "Reminder",
        timeLabel: String = DateTimeUtils.formatTimestamp(System.currentTimeMillis())
    ): PushResult = withContext(Dispatchers.IO) {
        if (!config.bridgeEnabled) {
            return@withContext PushResult(
                status = DeliveryStatus.SKIPPED,
                detail = "Laptop bridge is paused in settings"
            )
        }
        if (!config.isConfigured) {
            return@withContext PushResult(
                status = DeliveryStatus.FAILED,
                detail = "Laptop topic or webhook URL is not configured"
            )
        }

        try {
            val request = when (config.providerType) {
                BridgeProviderType.NTFY -> buildNtfyRequest(config, title, message, priority, ntfyTags)
                BridgeProviderType.DISCORD -> buildDiscordRequest(config, title, message, priority)
                BridgeProviderType.SLACK -> buildSlackRequest(config, title, message)
                BridgeProviderType.CUSTOM_JSON -> buildCustomJsonRequest(
                    config = config,
                    title = title,
                    message = message,
                    priority = priority,
                    itemTypeLabel = itemTypeLabel,
                    timeLabel = timeLabel
                )
            }

            okHttpClient.newCall(request).execute().use { response ->
                val code = response.code
                if (response.isSuccessful) {
                    PushResult(
                        status = DeliveryStatus.SENT,
                        httpCode = code,
                        detail = "Delivered to ${config.providerType.label} (HTTP $code)"
                    )
                } else {
                    val errBody = response.body?.string()?.take(180).orEmpty()
                    PushResult(
                        status = DeliveryStatus.FAILED,
                        httpCode = code,
                        detail = "HTTP $code${if (errBody.isNotBlank()) ": $errBody" else ""}"
                    )
                }
            }
        } catch (e: Exception) {
            PushResult(
                status = DeliveryStatus.FAILED,
                detail = e.localizedMessage ?: e.javaClass.simpleName
            )
        }
    }

    private fun buildNtfyRequest(
        config: BridgeConfig,
        title: String,
        message: String,
        priority: PriorityLevel,
        tags: String
    ): Request {
        val url = config.ntfySubscriptionUrl
        val builder = Request.Builder()
            .url(url)
            .header("Title", title.replace("\n", " ").take(120))
            .header("Priority", priority.ntfyPriority.toString())
            .header("Tags", tags)
            .post(message.toRequestBody("text/plain; charset=utf-8".toMediaType()))

        if (config.authToken.isNotBlank()) {
            builder.header("Authorization", "Bearer ${config.authToken.trim()}")
        }
        return builder.build()
    }

    private fun buildDiscordRequest(
        config: BridgeConfig,
        title: String,
        message: String,
        priority: PriorityLevel
    ): Request {
        val color = when (priority) {
            PriorityLevel.URGENT -> 0xDC2626
            PriorityLevel.HIGH -> 0xD97706
            PriorityLevel.MEDIUM -> 0x2563EB
            PriorityLevel.LOW -> 0x15803D
        }
        val embed = JSONObject()
            .put("title", title)
            .put("description", message)
            .put("color", color)
        val json = JSONObject()
            .put("username", "DayRelay")
            .put("embeds", JSONArray().put(embed))

        val builder = Request.Builder()
            .url(config.webhookUrl.trim())
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        if (config.authToken.isNotBlank()) {
            builder.header("Authorization", "Bearer ${config.authToken.trim()}")
        }
        return builder.build()
    }

    private fun buildSlackRequest(
        config: BridgeConfig,
        title: String,
        message: String
    ): Request {
        val json = JSONObject().put("text", "*$title*\n$message")
        val builder = Request.Builder()
            .url(config.webhookUrl.trim())
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        if (config.authToken.isNotBlank()) {
            builder.header("Authorization", "Bearer ${config.authToken.trim()}")
        }
        return builder.build()
    }

    private fun buildCustomJsonRequest(
        config: BridgeConfig,
        title: String,
        message: String,
        priority: PriorityLevel,
        itemTypeLabel: String,
        timeLabel: String
    ): Request {
        val json = JSONObject()
            .put("app", "DayRelay")
            .put("title", title)
            .put("message", message)
            .put("type", itemTypeLabel)
            .put("priority", priority.name)
            .put("scheduledTime", timeLabel)
            .put("sentAtMillis", System.currentTimeMillis())

        val builder = Request.Builder()
            .url(config.webhookUrl.trim())
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        if (config.authToken.isNotBlank()) {
            builder.header("Authorization", "Bearer ${config.authToken.trim()}")
        }
        return builder.build()
    }
}
