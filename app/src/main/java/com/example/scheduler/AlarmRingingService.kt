package com.example.scheduler

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ServiceCompat
import com.example.ui.AlarmAlertActivity

class AlarmRingingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var activeItemId: Long = -1L
    private var activeSnoozeMinutes: Int = 10

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                stopAlarmAndSelf()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                val itemId = intent.getLongExtra(TriggerReceiver.EXTRA_ITEM_ID, activeItemId)
                val snoozeMins = intent.getIntExtra(TriggerReceiver.EXTRA_SNOOZE_MINUTES, activeSnoozeMinutes)
                if (itemId > 0L) {
                    ExactAlarmScheduler(this).scheduleSnooze(
                        itemId = itemId,
                        snoozeMinutes = snoozeMins,
                        isLoudAlarm = true
                    )
                }
                stopAlarmAndSelf()
                return START_NOT_STICKY
            }
        }

        val itemId = intent?.getLongExtra(TriggerReceiver.EXTRA_ITEM_ID, -1L) ?: -1L
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "DayRelay Alarm"
        val timeLabel = intent?.getStringExtra(EXTRA_TIME_LABEL) ?: "Now"
        val ringtoneUriStr = intent?.getStringExtra(EXTRA_RINGTONE_URI)
        val snoozeMins = intent?.getIntExtra(TriggerReceiver.EXTRA_SNOOZE_MINUTES, 10) ?: 10

        activeItemId = itemId
        activeSnoozeMinutes = snoozeMins

        val notification = NotificationHelper.buildAlarmForegroundNotification(
            context = this,
            itemId = itemId,
            title = title,
            snoozeMinutes = snoozeMins,
            timeLabel = timeLabel
        )

        val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(this, ALARM_NOTIFICATION_ID, notification, fgType)

        startRinging(ringtoneUriStr)
        startVibration()

        val alertIntent = Intent(this, AlarmAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(TriggerReceiver.EXTRA_ITEM_ID, itemId)
            putExtra(AlarmAlertActivity.EXTRA_TITLE, title)
            putExtra(AlarmAlertActivity.EXTRA_TIME_LABEL, timeLabel)
            putExtra(TriggerReceiver.EXTRA_SNOOZE_MINUTES, snoozeMins)
        }
        runCatching { startActivity(alertIntent) }

        return START_NOT_STICKY
    }

    private fun startRinging(ringtoneUriStr: String?) {
        stopMedia()
        val targetUri = ringtoneUriStr
            ?.takeIf { it.isNotBlank() }
            ?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (targetUri == null) return

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(applicationContext, targetUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
            // Fallback to default alarm URI if custom tone became unavailable
            val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: return
            runCatching {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(attrs)
                    setDataSource(applicationContext, fallback)
                    isLooping = true
                    prepare()
                    start()
                }
            }
        }
    }

    private fun startVibration() {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        vibrator = vib
        val pattern = longArrayOf(0, 500, 400, 500, 600)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(pattern, 0)
        }
    }

    private fun stopMedia() {
        runCatching {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
        mediaPlayer = null
    }

    private fun stopAlarmAndSelf() {
        stopMedia()
        runCatching { vibrator?.cancel() }
        vibrator = null
        sendBroadcast(Intent(ACTION_ALARM_FINISHED).setPackage(packageName))
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopMedia()
        runCatching { vibrator?.cancel() }
        super.onDestroy()
    }

    companion object {
        const val ALARM_NOTIFICATION_ID = 9001
        const val ACTION_DISMISS = "com.example.action.ALARM_DISMISS"
        const val ACTION_SNOOZE = "com.example.action.ALARM_SNOOZE"
        const val ACTION_ALARM_FINISHED = "com.example.action.ALARM_FINISHED"
        const val EXTRA_TITLE = "extra_alarm_title"
        const val EXTRA_TIME_LABEL = "extra_alarm_time_label"
        const val EXTRA_RINGTONE_URI = "extra_alarm_ringtone_uri"
    }
}
