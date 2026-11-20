package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AlertMode
import com.example.data.model.PlannerItemEntity
import com.example.data.model.PlannerItemType
import com.example.data.model.PriorityLevel
import com.example.ui.theme.AlabasterCanvas
import com.example.ui.theme.DeepInk
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IndigoBridge
import com.example.ui.theme.PureIvory
import com.example.ui.theme.SlateSecondary
import com.example.ui.theme.SoftLinen
import com.example.ui.theme.UrgentCrimson
import com.example.ui.theme.VermilionAccent
import com.example.util.DateTimeUtils

private data class TaskDraft(
    val key: Int,
    val title: String = "",
    val description: String = "",
    val hasCustomTime: Boolean = true,
    val startMinutes: Int = 9 * 60,
    val hasEndTime: Boolean = false,
    val endMinutes: Int = 9 * 60 + 45,
    val alertMode: AlertMode = AlertMode.SNOOZE,
    val priority: PriorityLevel = PriorityLevel.MEDIUM,
    val repeatMask: Int = 0,
    val isHabit: Boolean = false,
    val ringtoneUri: String? = null,
    val ringtoneTitle: String = "Default Alarm",
    val snoozeMinutes: Int = 10,
    val notifyLaptop: Boolean = true,
    val showAdvanced: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemComposerSheet(
    initialItem: PlannerItemEntity?,
    initialType: PlannerItemType,
    selectedEpochDay: Long,
    defaultSnoozeMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (List<PlannerItemEntity>, Boolean) -> Unit,
    onDelete: ((PlannerItemEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val defaultNextHour = ((DateTimeUtils.currentMinutesOfDay() / 60 + 1) * 60).coerceAtMost(23 * 60)
    var nextKey by remember { mutableIntStateOf(2) }

    val drafts = remember(initialItem, initialType) {
        mutableStateListOf(
            if (initialItem != null) {
                TaskDraft(
                    key = 1,
                    title = initialItem.title,
                    description = initialItem.description,
                    hasCustomTime = initialItem.startMinutesOfDay != null,
                    startMinutes = initialItem.startMinutesOfDay ?: defaultNextHour,
                    hasEndTime = initialItem.endMinutesOfDay != null,
                    endMinutes = initialItem.endMinutesOfDay ?: ((initialItem.startMinutesOfDay ?: defaultNextHour) + 45).coerceAtMost(1439),
                    alertMode = initialItem.alertMode,
                    priority = initialItem.priority,
                    repeatMask = initialItem.repeatMask,
                    isHabit = initialItem.itemType == PlannerItemType.HABIT,
                    ringtoneUri = initialItem.ringtoneUri,
                    ringtoneTitle = initialItem.ringtoneTitle,
                    snoozeMinutes = initialItem.snoozeMinutes,
                    notifyLaptop = initialItem.notifyLaptop,
                    showAdvanced = initialItem.description.isNotBlank() ||
                        initialItem.repeatMask != 0 ||
                        initialItem.endMinutesOfDay != null ||
                        initialItem.alertMode == AlertMode.ALARM
                )
            } else {
                TaskDraft(
                    key = 1,
                    hasCustomTime = true,
                    startMinutes = defaultNextHour,
                    endMinutes = (defaultNextHour + 45).coerceAtMost(1439),
                    alertMode = if (initialType == PlannerItemType.ALARM) AlertMode.ALARM else AlertMode.SNOOZE,
                    repeatMask = if (initialType == PlannerItemType.HABIT) DateTimeUtils.MASK_EVERY_DAY else 0,
                    isHabit = initialType == PlannerItemType.HABIT,
                    snoozeMinutes = defaultSnoozeMinutes
                )
            }
        )
    }

    var alsoPushImmediately by remember { mutableStateOf(false) }
    var showValidationError by remember { mutableStateOf(false) }
    var activeTimePickerTarget by remember { mutableStateOf<Pair<Int, Boolean>?>(null) } // draftIndex to isEndTime
    var activeRingtoneTargetIndex by remember { mutableIntStateOf(-1) }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && activeRingtoneTargetIndex in drafts.indices) {
            val data = result.data
            val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            val current = drafts[activeRingtoneTargetIndex]
            if (pickedUri != null) {
                val title = runCatching {
                    RingtoneManager.getRingtone(context, pickedUri)?.getTitle(context)
                }.getOrNull() ?: "Custom Ringtone"
                drafts[activeRingtoneTargetIndex] = current.copy(
                    ringtoneUri = pickedUri.toString(),
                    ringtoneTitle = title
                )
            } else {
                drafts[activeRingtoneTargetIndex] = current.copy(
                    ringtoneUri = null,
                    ringtoneTitle = "Default Alarm"
                )
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AlabasterCanvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (initialItem == null) "Add Tasks" else "Edit Task",
                        style = MaterialTheme.typography.headlineSmall,
                        color = DeepInk
                    )
                    if (initialItem == null) {
                        Text(
                            text = "Add one or more tasks and set your custom time under each.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSecondary
                        )
                    }
                }
                if (initialItem != null && onDelete != null) {
                    IconButton(
                        onClick = { onDelete(initialItem) },
                        modifier = Modifier.testTag("delete_item_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete task",
                            tint = UrgentCrimson
                        )
                    }
                }
            }

            drafts.forEachIndexed { index, draft ->
                Surface(
                    color = PureIvory,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, HairlineBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (drafts.size > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Task ${index + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = VermilionAccent
                                )
                                IconButton(
                                    onClick = { drafts.removeAt(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove task",
                                        tint = SlateSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = draft.title,
                            onValueChange = {
                                drafts[index] = draft.copy(title = it)
                                showValidationError = false
                            },
                            label = { Text("Task title") },
                            placeholder = { Text("e.g., Study session or project review") },
                            isError = showValidationError && draft.title.isBlank() && index == 0,
                            supportingText = if (showValidationError && draft.title.isBlank() && index == 0) {
                                { Text("Enter at least one task") }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("composer_title_input_$index")
                        )

                        // Custom Time directly under the task
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Custom Time",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DeepInk
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (draft.hasCustomTime) "Scheduled" else "Anytime",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SlateSecondary
                                    )
                                    Switch(
                                        checked = draft.hasCustomTime,
                                        onCheckedChange = {
                                            drafts[index] = draft.copy(hasCustomTime = it)
                                        }
                                    )
                                }
                            }

                            if (draft.hasCustomTime) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { activeTimePickerTarget = index to false },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("pick_start_time_button_$index"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AccessTime,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Text(DateTimeUtils.formatMinutes(draft.startMinutes))
                                    }

                                    if (draft.hasEndTime) {
                                        OutlinedButton(
                                            onClick = { activeTimePickerTarget = index to true },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("To ${DateTimeUtils.formatMinutes(draft.endMinutes)}")
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val nowMins = DateTimeUtils.currentMinutesOfDay()
                                    listOf(
                                        "In 15m" to ((nowMins + 15) % 1440),
                                        "In 1h" to ((nowMins + 60) % 1440),
                                        "Morning 9 AM" to (9 * 60),
                                        "Afternoon 3 PM" to (15 * 60),
                                        "Evening 7 PM" to (19 * 60)
                                    ).forEach { (label, mins) ->
                                        Surface(
                                            color = SoftLinen,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.clickable {
                                                drafts[index] = draft.copy(
                                                    startMinutes = mins,
                                                    endMinutes = (mins + 45).coerceAtMost(1439)
                                                )
                                            }
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DeepInk,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Notification / Alert Style selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Phone & Laptop Alert Style",
                                style = MaterialTheme.typography.labelLarge,
                                color = DeepInk
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AlertMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = draft.alertMode == mode,
                                        onClick = {
                                            drafts[index] = draft.copy(
                                                alertMode = mode,
                                                hasCustomTime = true,
                                                showAdvanced = if (mode == AlertMode.ALARM) true else draft.showAdvanced
                                            )
                                        },
                                        label = { Text(mode.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = VermilionAccent,
                                            selectedLabelColor = PureIvory
                                        ),
                                        modifier = Modifier.testTag("alert_mode_${mode.name}_$index")
                                    )
                                }
                            }
                            Text(
                                text = draft.alertMode.shortDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateSecondary
                            )
                        }

                        // Priority row
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Priority",
                                style = MaterialTheme.typography.labelLarge,
                                color = DeepInk
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PriorityLevel.entries.forEach { level ->
                                    FilterChip(
                                        selected = draft.priority == level,
                                        onClick = { drafts[index] = draft.copy(priority = level) },
                                        label = { Text(level.label) },
                                        modifier = Modifier.testTag("priority_${level.name}_$index")
                                    )
                                }
                            }
                        }

                        // Expandable More Options (End Time, Repeat/Habit, Tone, Notes, Laptop Toggle)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { drafts[index] = draft.copy(showAdvanced = !draft.showAdvanced) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "More options (duration, repeat, ringtone, notes)",
                                style = MaterialTheme.typography.labelLarge,
                                color = IndigoBridge
                            )
                            Icon(
                                imageVector = if (draft.showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = IndigoBridge
                            )
                        }

                        AnimatedVisibility(visible = draft.showAdvanced) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = draft.description,
                                    onValueChange = { drafts[index] = draft.copy(description = it) },
                                    label = { Text("Task notes or link (optional)") },
                                    minLines = 2,
                                    maxLines = 3,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Include end time (timed event)", style = MaterialTheme.typography.bodyMedium)
                                    Switch(
                                        checked = draft.hasEndTime,
                                        onCheckedChange = {
                                            drafts[index] = draft.copy(
                                                hasEndTime = it,
                                                endMinutes = (draft.startMinutes + 45).coerceAtMost(1439)
                                            )
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Track as daily habit streak", style = MaterialTheme.typography.bodyMedium)
                                    Switch(
                                        checked = draft.isHabit,
                                        onCheckedChange = {
                                            drafts[index] = draft.copy(
                                                isHabit = it,
                                                repeatMask = if (it && draft.repeatMask == 0) DateTimeUtils.MASK_EVERY_DAY else draft.repeatMask
                                            )
                                        }
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Repeat Days", style = MaterialTheme.typography.labelLarge, color = SlateSecondary)
                                        Text(
                                            text = DateTimeUtils.formatRepeatLabel(draft.repeatMask),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VermilionAccent
                                        )
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { idx, lbl ->
                                            FilterChip(
                                                selected = (draft.repeatMask and (1 shl idx)) != 0,
                                                onClick = {
                                                    drafts[index] = draft.copy(repeatMask = draft.repeatMask xor (1 shl idx))
                                                },
                                                label = { Text(lbl) }
                                            )
                                        }
                                        FilterChip(
                                            selected = draft.repeatMask == DateTimeUtils.MASK_EVERY_DAY,
                                            onClick = {
                                                val next = if (draft.repeatMask == DateTimeUtils.MASK_EVERY_DAY) 0 else DateTimeUtils.MASK_EVERY_DAY
                                                drafts[index] = draft.copy(repeatMask = next)
                                            },
                                            label = { Text("Daily") }
                                        )
                                    }
                                }

                                if (draft.alertMode == AlertMode.ALARM) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Alarm Ringtone", style = MaterialTheme.typography.titleSmall)
                                            Text(draft.ringtoneTitle, style = MaterialTheme.typography.bodySmall, color = SlateSecondary)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                activeRingtoneTargetIndex = index
                                                val pickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                                    draft.ringtoneUri?.let {
                                                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                                                    }
                                                }
                                                ringtoneLauncher.launch(pickerIntent)
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.size(4.dp))
                                            Text("Tone")
                                        }
                                    }
                                }

                                if (draft.alertMode == AlertMode.SNOOZE || draft.alertMode == AlertMode.LOUD_NOTIFICATION || draft.alertMode == AlertMode.ALARM) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Snooze:", style = MaterialTheme.typography.labelLarge, color = SlateSecondary)
                                        listOf(5, 10, 15).forEach { mins ->
                                            FilterChip(
                                                selected = draft.snoozeMinutes == mins,
                                                onClick = { drafts[index] = draft.copy(snoozeMinutes = mins) },
                                                label = { Text("${mins}m") }
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Filled.Computer, contentDescription = null, tint = IndigoBridge, modifier = Modifier.size(18.dp))
                                        Text("Also notify my laptop", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Switch(
                                        checked = draft.notifyLaptop,
                                        onCheckedChange = { drafts[index] = draft.copy(notifyLaptop = it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Easy multi-task entry: add 2–3 tasks in one go
            if (initialItem == null) {
                OutlinedButton(
                    onClick = {
                        val lastStart = drafts.lastOrNull()?.startMinutes ?: defaultNextHour
                        val nextSuggested = (lastStart + 60) % 1440
                        drafts.add(
                            TaskDraft(
                                key = nextKey++,
                                hasCustomTime = true,
                                startMinutes = nextSuggested,
                                endMinutes = (nextSuggested + 45).coerceAtMost(1439),
                                snoozeMinutes = defaultSnoozeMinutes
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_another_task_draft_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Add another task for today")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Also push to laptop right now on save",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateSecondary
                )
                Switch(
                    checked = alsoPushImmediately,
                    onCheckedChange = { alsoPushImmediately = it }
                )
            }

            val nonEmptyCount = drafts.count { it.title.isNotBlank() }
            Button(
                onClick = {
                    val validDrafts = drafts.filter { it.title.isNotBlank() }
                    if (validDrafts.isEmpty()) {
                        showValidationError = true
                        return@Button
                    }
                    val entities = validDrafts.mapIndexed { idx, d ->
                        val resolvedType = when {
                            d.isHabit -> PlannerItemType.HABIT
                            d.alertMode == AlertMode.ALARM -> PlannerItemType.ALARM
                            d.hasCustomTime && d.hasEndTime -> PlannerItemType.EVENT
                            else -> PlannerItemType.TASK
                        }
                        val finalStart = if (d.hasCustomTime) d.startMinutes else null
                        val finalEnd = if (d.hasCustomTime && d.hasEndTime && finalStart != null) {
                            if (d.endMinutes > finalStart) d.endMinutes else (finalStart + 45).coerceAtMost(1439)
                        } else null

                        PlannerItemEntity(
                            id = if (idx == 0) initialItem?.id ?: 0L else 0L,
                            title = d.title.trim(),
                            description = d.description.trim(),
                            itemType = resolvedType,
                            alertMode = d.alertMode,
                            priority = d.priority,
                            dateEpochDay = if (d.repeatMask == 0) (initialItem?.dateEpochDay ?: selectedEpochDay) else null,
                            startMinutesOfDay = finalStart,
                            endMinutesOfDay = finalEnd,
                            repeatMask = d.repeatMask,
                            ringtoneUri = d.ringtoneUri,
                            ringtoneTitle = d.ringtoneTitle,
                            snoozeMinutes = d.snoozeMinutes,
                            notifyLaptop = d.notifyLaptop,
                            isCompleted = if (idx == 0) initialItem?.isCompleted ?: false else false,
                            isEnabled = if (idx == 0) initialItem?.isEnabled ?: true else true,
                            createdAtMillis = if (idx == 0) initialItem?.createdAtMillis ?: System.currentTimeMillis() else System.currentTimeMillis() + idx
                        )
                    }
                    onSave(entities, alsoPushImmediately)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_item_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VermilionAccent,
                    contentColor = PureIvory
                )
            ) {
                Text(
                    text = when {
                        initialItem != null -> "Update Task"
                        nonEmptyCount > 1 -> "Save $nonEmptyCount Tasks"
                        else -> "Save Task"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    val target = activeTimePickerTarget
    if (target != null && target.first in drafts.indices) {
        val (draftIdx, isEnd) = target
        val draft = drafts[draftIdx]
        val initialMins = if (isEnd) draft.endMinutes else draft.startMinutes
        val state = rememberTimePickerState(
            initialHour = initialMins / 60,
            initialMinute = initialMins % 60,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { activeTimePickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    val picked = state.hour * 60 + state.minute
                    drafts[draftIdx] = if (isEnd) {
                        draft.copy(endMinutes = if (picked > draft.startMinutes) picked else (draft.startMinutes + 30).coerceAtMost(1439))
                    } else {
                        draft.copy(
                            startMinutes = picked,
                            endMinutes = if (draft.endMinutes <= picked) (picked + 45).coerceAtMost(1439) else draft.endMinutes
                        )
                    }
                    activeTimePickerTarget = null
                }) { Text("Set Time") }
            },
            dismissButton = {
                TextButton(onClick = { activeTimePickerTarget = null }) { Text("Cancel") }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = state)
                }
            }
        )
    }
}
