package com.example.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.scheduler.AlarmRingingService
import com.example.scheduler.TriggerReceiver
import com.example.ui.theme.AlabasterCanvas
import com.example.ui.theme.DayRelayTheme
import com.example.ui.theme.DeepInk
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IndigoBridge
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.PureIvory
import com.example.ui.theme.SlateSecondary
import com.example.ui.theme.VermilionAccent
import com.example.ui.theme.VermilionContainer

class AlarmAlertActivity : ComponentActivity() {

    private val finishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val itemId = intent.getLongExtra(TriggerReceiver.EXTRA_ITEM_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "DayRelay Alarm"
        val timeLabel = intent.getStringExtra(EXTRA_TIME_LABEL) ?: "Now"
        val snoozeMinutes = intent.getIntExtra(TriggerReceiver.EXTRA_SNOOZE_MINUTES, 10)

        ContextCompat.registerReceiver(
            this,
            finishReceiver,
            IntentFilter(AlarmRingingService.ACTION_ALARM_FINISHED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            DayRelayTheme {
                AlarmAlertScreen(
                    title = title,
                    timeLabel = timeLabel,
                    snoozeMinutes = snoozeMinutes,
                    onSnooze = {
                        val snoozeIntent = Intent(this, AlarmRingingService::class.java).apply {
                            action = AlarmRingingService.ACTION_SNOOZE
                            putExtra(TriggerReceiver.EXTRA_ITEM_ID, itemId)
                            putExtra(TriggerReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                        }
                        startService(snoozeIntent)
                        finish()
                    },
                    onDismiss = {
                        val dismissIntent = Intent(this, AlarmRingingService::class.java).apply {
                            action = AlarmRingingService.ACTION_DISMISS
                        }
                        startService(dismissIntent)
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(finishReceiver) }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_TITLE = "extra_alert_title"
        const val EXTRA_TIME_LABEL = "extra_alert_time_label"
    }
}

@Composable
private fun AlarmAlertScreen(
    title: String,
    timeLabel: String,
    snoozeMinutes: Int,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "alarm_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlabasterCanvas)
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Surface(
                color = IndigoContainer,
                shape = RoundedCornerShape(50),
                modifier = Modifier.border(1.dp, IndigoBridge.copy(alpha = 0.25f), RoundedCornerShape(50))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Computer,
                        contentDescription = null,
                        tint = IndigoBridge,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Also notified on Laptop",
                        style = MaterialTheme.typography.labelLarge,
                        color = IndigoBridge
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(116.dp)
                    .scale(pulseScale)
                    .background(VermilionContainer, CircleShape)
                    .border(2.dp, VermilionAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Alarm,
                    contentDescription = "Ringing alarm",
                    tint = VermilionAccent,
                    modifier = Modifier.size(52.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.displayLarge,
                    color = DeepInk
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = DeepInk,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "DayRelay Loud Alarm",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("alarm_dismiss_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VermilionAccent,
                        contentColor = PureIvory
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Dismiss Alarm",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("alarm_snooze_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, HairlineBorder)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Snooze,
                        contentDescription = null,
                        tint = DeepInk,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Snooze for $snoozeMinutes minutes",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepInk
                    )
                }
            }
        }
    }
}
