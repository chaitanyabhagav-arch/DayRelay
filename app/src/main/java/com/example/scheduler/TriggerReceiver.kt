package com.example.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.DayRelayApplication
import com.example.data.model.AlertMode
import com.example.data.model.DeliveryLogEntity
import com.example.data.model.HabitCompletionEntity
import com.example.data.model.PlannerItemType
import com.example.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()
        val app = context.applicationContext as? DayRelayApplication
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                if (app == null) return@launch
                val dao = app.database.plannerDao()
                val scheduler = app.scheduler

                when (action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED -> {
                        val enabled = dao.getAllEnabledItemsOnce()
                        enabled.forEach { scheduler.scheduleItem(it) }
                    }

                    ACTION_TRIGGER_ITEM -> {
                        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
                        val isSnoozed = intent.getBooleanExtra(EXTRA_IS_SNOOZED, false)
                        val item = dao.getItemById(itemId) ?: return@launch
                        if (!item.isEnabled) return@launch

                        val timeLabel = item.startMinutesOfDay?.let {
                            DateTimeUtils.formatMinutes(it)
                        } ?: DateTimeUtils.formatTimestamp(System.currentTimeMillis())

                        val shouldRingAlarm = item.itemType == PlannerItemType.ALARM ||
                            item.alertMode == AlertMode.ALARM

                        if (shouldRingAlarm) {
                            val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                                putExtra(EXTRA_ITEM_ID, item.id)
                                putExtra(AlarmRingingService.EXTRA_TITLE, item.title)
                                putExtra(AlarmRingingService.EXTRA_TIME_LABEL, timeLabel)
                                putExtra(AlarmRingingService.EXTRA_RINGTONE_URI, item.ringtoneUri)
                                putExtra(EXTRA_SNOOZE_MINUTES, item.snoozeMinutes)
                            }
                            ContextCompat.startForegroundService(context, serviceIntent)
                        } else {
                            NotificationHelper.showReminderNotification(context, item, isSnoozed)
                        }

                        if (!isSnoozed && item.repeatMask != 0) {
                            scheduler.scheduleItem(item)
                        }

                        if (item.notifyLaptop) {
                            val config = app.bridgePrefs.getConfigOnce()
                            val result = app.pushClient.sendItemReminder(
                                config = config,
                                item = item,
                                isSnoozed = isSnoozed
                            )
                            dao.insertDeliveryLog(
                                DeliveryLogEntity(
                                    itemId = item.id,
                                    title = item.title,
                                    summary = result.detail,
                                    provider = config.providerType,
                                    status = result.status,
                                    httpCode = result.httpCode,
                                    errorDetail = if (result.status.name == "FAILED") result.detail else null
                                )
                            )
                        }
                    }

                    ACTION_MARK_DONE -> {
                        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
                        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                        if (notifId > 0) NotificationHelper.cancelNotification(context, notifId)
                        val item = dao.getItemById(itemId) ?: return@launch
                        if (item.itemType == PlannerItemType.HABIT) {
                            dao.insertHabitCompletion(
                                HabitCompletionEntity(
                                    itemId = item.id,
                                    epochDay = DateTimeUtils.todayEpochDay()
                                )
                            )
                        } else {
                            dao.setItemCompleted(item.id, true)
                            scheduler.cancelItem(item.id)
                        }
                    }

                    ACTION_SNOOZE -> {
                        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
                        val snoozeMins = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
                        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                        if (notifId > 0) NotificationHelper.cancelNotification(context, notifId)
                        val item = dao.getItemById(itemId) ?: return@launch
                        scheduler.scheduleSnooze(
                            itemId = item.id,
                            snoozeMinutes = snoozeMins,
                            isLoudAlarm = item.itemType == PlannerItemType.ALARM || item.alertMode == AlertMode.ALARM
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_ITEM = "com.example.action.TRIGGER_ITEM"
        const val ACTION_MARK_DONE = "com.example.action.MARK_DONE"
        const val ACTION_SNOOZE = "com.example.action.SNOOZE"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_IS_SNOOZED = "extra_is_snoozed"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
