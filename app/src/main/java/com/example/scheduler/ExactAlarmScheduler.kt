package com.example.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.data.model.AlertMode
import com.example.data.model.PlannerItemEntity
import com.example.data.model.PlannerItemType
import com.example.util.DateTimeUtils

class ExactAlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleItem(item: PlannerItemEntity) {
        cancelItem(item.id)
        if (!item.isEnabled || item.isCompleted) return
        val startMinutes = item.startMinutesOfDay ?: return

        val triggerMillis = DateTimeUtils.nextOccurrenceMillis(
            dateEpochDay = item.dateEpochDay,
            minutesOfDay = startMinutes,
            repeatMask = item.repeatMask
        ) ?: return

        scheduleAtMillis(
            itemId = item.id,
            triggerMillis = triggerMillis,
            isLoudAlarm = item.itemType == PlannerItemType.ALARM || item.alertMode == AlertMode.ALARM,
            isSnoozed = false
        )
    }

    fun scheduleSnooze(itemId: Long, snoozeMinutes: Int, isLoudAlarm: Boolean) {
        val triggerMillis = System.currentTimeMillis() + (snoozeMinutes.coerceAtLeast(1) * 60_000L)
        scheduleAtMillis(
            itemId = itemId,
            triggerMillis = triggerMillis,
            isLoudAlarm = isLoudAlarm,
            isSnoozed = true
        )
    }

    private fun scheduleAtMillis(
        itemId: Long,
        triggerMillis: Long,
        isLoudAlarm: Boolean,
        isSnoozed: Boolean
    ) {
        val manager = alarmManager ?: return
        val pendingIntent = buildTriggerPendingIntent(itemId, isSnoozed)

        if (isLoudAlarm) {
            val showIntent = PendingIntent.getActivity(
                context,
                itemId.toInt() + 50_000,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            manager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerMillis, showIntent),
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } else {
            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    fun cancelItem(itemId: Long) {
        val manager = alarmManager ?: return
        manager.cancel(buildTriggerPendingIntent(itemId, isSnoozed = false))
        manager.cancel(buildTriggerPendingIntent(itemId, isSnoozed = true))
    }

    private fun buildTriggerPendingIntent(itemId: Long, isSnoozed: Boolean): PendingIntent {
        val requestCode = (itemId.toInt() * 2) + if (isSnoozed) 1 else 0
        val intent = Intent(context, TriggerReceiver::class.java).apply {
            action = TriggerReceiver.ACTION_TRIGGER_ITEM
            putExtra(TriggerReceiver.EXTRA_ITEM_ID, itemId)
            putExtra(TriggerReceiver.EXTRA_IS_SNOOZED, isSnoozed)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
