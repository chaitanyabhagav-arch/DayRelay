package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AlertMode
import com.example.data.model.PlannerItemEntity
import com.example.data.model.PlannerItemType
import com.example.data.model.PriorityLevel
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberPriority
import com.example.ui.theme.CobaltContainer
import com.example.ui.theme.CobaltEvent
import com.example.ui.theme.DeepInk
import com.example.ui.theme.ForestContainer
import com.example.ui.theme.ForestHabit
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IndigoBridge
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.MutedStone
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.OnCobaltContainer
import com.example.ui.theme.OnForestContainer
import com.example.ui.theme.OnUrgentContainer
import com.example.ui.theme.OnVermilionContainer
import com.example.ui.theme.PureIvory
import com.example.ui.theme.SlateSecondary
import com.example.ui.theme.SoftLinen
import com.example.ui.theme.UrgentContainer
import com.example.ui.theme.VermilionAccent
import com.example.ui.theme.VermilionContainer
import com.example.util.DateTimeUtils

@Composable
fun itemAccentColors(item: PlannerItemEntity): Triple<Color, Color, Color> {
    return when {
        item.alertMode == AlertMode.ALARM || item.itemType == PlannerItemType.ALARM ->
            Triple(VermilionAccent, VermilionContainer, OnVermilionContainer)
        item.alertMode == AlertMode.LOUD_NOTIFICATION ->
            Triple(VermilionAccent, UrgentContainer, OnUrgentContainer)
        item.itemType == PlannerItemType.EVENT ->
            Triple(CobaltEvent, CobaltContainer, OnCobaltContainer)
        item.itemType == PlannerItemType.HABIT ->
            Triple(ForestHabit, ForestContainer, OnForestContainer)
        else -> Triple(AmberPriority, AmberContainer, OnAmberContainer)
    }
}

@Composable
fun priorityColors(priority: PriorityLevel): Pair<Color, Color> {
    return when (priority) {
        PriorityLevel.URGENT -> UrgentContainer to OnUrgentContainer
        PriorityLevel.HIGH -> AmberContainer to OnAmberContainer
        PriorityLevel.MEDIUM -> CobaltContainer to OnCobaltContainer
        PriorityLevel.LOW -> SoftLinen to SlateSecondary
    }
}

@Composable
fun DayStrip(
    todayEpochDay: Long,
    selectedEpochDay: Long,
    onSelectDay: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = (-2L..10L).map { todayEpochDay + it }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days, key = { it }) { day ->
            val isSelected = day == selectedEpochDay
            val isToday = day == todayEpochDay
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) DeepInk else PureIvory,
                label = "day_pill_bg"
            )
            val textColor = if (isSelected) PureIvory else DeepInk
            val subColor = if (isSelected) PureIvory.copy(alpha = 0.78f) else SlateSecondary

            Column(
                modifier = Modifier
                    .width(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .border(
                        width = if (isToday && !isSelected) 1.5.dp else 1.dp,
                        color = when {
                            isSelected -> DeepInk
                            isToday -> VermilionAccent
                            else -> HairlineBorder
                        },
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelectDay(day) }
                    .padding(vertical = 9.dp)
                    .testTag("day_chip_$day"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = DateTimeUtils.formatShortWeekday(day).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = subColor
                )
                Text(
                    text = DateTimeUtils.dayOfMonth(day).toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
                if (isToday) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(VermilionAccent, CircleShape)
                    )
                } else {
                    Spacer(modifier = Modifier.height(5.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlannerItemCard(
    item: PlannerItemEntity,
    isCompleted: Boolean,
    streakDays: Int = 0,
    onToggleComplete: () -> Unit,
    onToggleAlarmSwitch: (() -> Unit)? = null,
    onPickCustomTime: () -> Unit,
    onStartFocus: () -> Unit,
    onEdit: () -> Unit,
    onPushNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (accent, _, _) = itemAccentColors(item)
    val (prioBg, prioFg) = priorityColors(item.priority)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEdit() }
            .testTag("planner_item_card_${item.id}"),
        color = if (isCompleted) SoftLinen.copy(alpha = 0.72f) else PureIvory,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isCompleted) HairlineBorder else accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(62.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isCompleted) MutedStone else accent)
            )

            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("toggle_item_${item.id}")
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Mark incomplete",
                        tint = ForestHabit,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = "Mark complete",
                        tint = accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isCompleted) SlateSecondary else DeepInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Easy-access chips right under the task title: Custom Time, Alert Mode, Priority
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Tappable custom time pill!
                    Surface(
                        color = if (item.startMinutesOfDay != null) CobaltContainer else SoftLinen,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPickCustomTime() }
                            .testTag("time_pill_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = "Set custom time",
                                tint = if (item.startMinutesOfDay != null) OnCobaltContainer else SlateSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = item.startMinutesOfDay?.let {
                                    DateTimeUtils.formatDurationRange(it, item.endMinutesOfDay)
                                } ?: "Set time",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (item.startMinutesOfDay != null) OnCobaltContainer else SlateSecondary
                            )
                        }
                    }

                    // Alert Mode pill
                    Surface(
                        color = SoftLinen,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = when (item.alertMode) {
                                    AlertMode.ALARM -> Icons.Filled.Alarm
                                    AlertMode.LOUD_NOTIFICATION -> Icons.Filled.VolumeUp
                                    AlertMode.SNOOZE -> Icons.Filled.Snooze
                                    AlertMode.NORMAL -> Icons.Filled.Notifications
                                },
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = item.alertMode.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepInk
                            )
                        }
                    }

                    // Priority pill
                    Surface(
                        color = prioBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = item.priority.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = prioFg,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }

                    if (item.itemType == PlannerItemType.HABIT && streakDays > 0) {
                        Surface(
                            color = ForestContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Repeat,
                                    contentDescription = null,
                                    tint = OnForestContainer,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "${streakDays}d streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnForestContainer
                                )
                            }
                        }
                    }
                }

                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (onToggleAlarmSwitch != null && (item.itemType == PlannerItemType.ALARM || item.alertMode == AlertMode.ALARM)) {
                    Switch(
                        checked = item.isEnabled,
                        onCheckedChange = { onToggleAlarmSwitch() },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isCompleted) {
                            IconButton(
                                onClick = onStartFocus,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("focus_task_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Start focus sprint",
                                    tint = ForestHabit,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (item.notifyLaptop) {
                            IconButton(
                                onClick = onPushNow,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("push_now_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Send,
                                    contentDescription = "Send to laptop now",
                                    tint = IndigoBridge,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionBannerCard(
    title: String,
    body: String,
    buttonText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = VermilionContainer,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, VermilionAccent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PureIvory, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = VermilionAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = OnVermilionContainer
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnVermilionContainer.copy(alpha = 0.85f)
                )
            }
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VermilionAccent,
                    contentColor = PureIvory
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(text = buttonText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun BridgeStatusBadge(
    isConfigured: Boolean,
    isEnabled: Boolean,
    topicOrProvider: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        !isEnabled -> SoftLinen
        isConfigured -> IndigoContainer
        else -> AmberContainer
    }
    val fg = when {
        !isEnabled -> SlateSecondary
        isConfigured -> IndigoBridge
        else -> OnAmberContainer
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
            .testTag("bridge_status_pill"),
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Computer,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = when {
                    !isEnabled -> "Laptop Paused"
                    isConfigured -> topicOrProvider
                    else -> "Setup Laptop"
                },
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
