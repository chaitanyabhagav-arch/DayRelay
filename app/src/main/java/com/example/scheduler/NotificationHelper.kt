package com.example.scheduler

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AlertMode
import com.example.data.model.PlannerItemEntity
import com.example.data.model.PlannerItemType
import com.example.ui.AlarmAlertActivity
import com.example.util.DateTimeUtils

object NotificationHelper {

    const val CHANNEL_REMINDERS = "dayrelay_reminders"
    const val CHANNEL_LOUD_ALERTS = "dayrelay_loud_alerts"
    const val CHANNEL_ALARMS = "dayrelay_alarms"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_reminders_desc)
                enableVibration(true)
            }

            val loudSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val loudAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val loudChannel = NotificationChannel(
                CHANNEL_LOUD_ALERTS,
                context.getString(R.string.channel_loud_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_loud_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 200, 350, 200, 500)
                setBypassDnd(true)
                setSound(loudSound, loudAttrs)
            }

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val alarmAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARMS,
                context.getString(R.string.channel_alarms_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_alarms_desc)
                enableVibration(true)
                setBypassDnd(true)
                setSound(alarmSound, alarmAttrs)
            }

            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(loudChannel)
            manager.createNotificationChannel(alarmChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showReminderNotification(
        context: Context,
        item: PlannerItemEntity,
        isSnoozed: Boolean = false
    ) {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return

        val notifId = item.id.toInt() + 10_000
        val openAppIntent = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = PendingIntent.getBroadcast(
            context,
            notifId + 1,
            Intent(context, TriggerReceiver::class.java).apply {
                action = TriggerReceiver.ACTION_MARK_DONE
                putExtra(TriggerReceiver.EXTRA_ITEM_ID, item.id)
                putExtra(TriggerReceiver.EXTRA_NOTIFICATION_ID, notifId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            notifId + 2,
            Intent(context, TriggerReceiver::class.java).apply {
                action = TriggerReceiver.ACTION_SNOOZE
                putExtra(TriggerReceiver.EXTRA_ITEM_ID, item.id)
                putExtra(TriggerReceiver.EXTRA_SNOOZE_MINUTES, item.snoozeMinutes)
                putExtra(TriggerReceiver.EXTRA_NOTIFICATION_ID, notifId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeText = item.startMinutesOfDay?.let {
            DateTimeUtils.formatDurationRange(it, item.endMinutesOfDay)
        } ?: "Today"

        val badge = when (item.itemType) {
            PlannerItemType.EVENT -> "Event"
            PlannerItemType.HABIT -> "Habit"
            PlannerItemType.TASK -> "Task"
            PlannerItemType.ALARM -> "Alarm"
        }
        val titlePrefix = if (isSnoozed) "Snoozed $badge" else "$badge · $timeText"
        val bodyText = buildString {
            append(item.title)
            append(" (${item.priority.label} priority)")
            if (item.description.isNotBlank()) {
                append("\n")
                append(item.description.trim())
            }
            if (item.notifyLaptop) {
                append("\nAlso notified on your laptop")
            }
        }

        val isLoud = item.alertMode == AlertMode.LOUD_NOTIFICATION
        val channel = if (isLoud) CHANNEL_LOUD_ALERTS else CHANNEL_REMINDERS
        val priority = if (isLoud || item.alertMode == AlertMode.SNOOZE) {
            NotificationCompat.PRIORITY_MAX
        } else {
            NotificationCompat.PRIORITY_HIGH
        }

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(titlePrefix)
            .setContentText(item.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .addAction(0, context.getString(R.string.action_mark_done), doneIntent)

        if (isLoud) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (soundUri != null) {
                builder.setSound(soundUri)
            }
            builder.setVibrate(longArrayOf(0, 400, 200, 400, 200, 500))
        }

        if (item.alertMode == AlertMode.SNOOZE || item.alertMode == AlertMode.LOUD_NOTIFICATION) {
            builder.addAction(
                0,
                "${context.getString(R.string.action_snooze)} ${item.snoozeMinutes}m",
                snoozeIntent
            )
        }

        try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        } catch (_: SecurityException) {
            // Ignored if permission was revoked concurrently
        }
    }

    fun showFocusCompleteNotification(context: Context, taskTitle: String, minutes: Int) {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return

        val openAppIntent = PendingIntent.getActivity(
            context,
            88_001,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_LOUD_ALERTS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Focus session complete ($minutes min)")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(88_001, notification)
        } catch (_: SecurityException) {
            // Ignored
        }
    }

    fun buildAlarmForegroundNotification(
        context: Context,
        itemId: Long,
        title: String,
        snoozeMinutes: Int,
        timeLabel: String
    ): Notification {
        ensureChannels(context)

        val fullScreenIntent = Intent(context, AlarmAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(TriggerReceiver.EXTRA_ITEM_ID, itemId)
            putExtra(AlarmAlertActivity.EXTRA_TITLE, title)
            putExtra(AlarmAlertActivity.EXTRA_TIME_LABEL, timeLabel)
            putExtra(TriggerReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            itemId.toInt() + 20_000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getService(
            context,
            itemId.toInt() + 20_100,
            Intent(context, AlarmRingingService::class.java).apply {
                action = AlarmRingingService.ACTION_SNOOZE
                putExtra(TriggerReceiver.EXTRA_ITEM_ID, itemId)
                putExtra(TriggerReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = PendingIntent.getService(
            context,
            itemId.toInt() + 20_200,
            Intent(context, AlarmRingingService::class.java).apply {
                action = AlarmRingingService.ACTION_DISMISS
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ALARMS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("DayRelay Alarm · $timeLabel")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(0, "${context.getString(R.string.action_snooze)} ${snoozeMinutes}m", snoozeIntent)
            .addAction(0, context.getString(R.string.action_dismiss), dismissIntent)
            .build()
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
