package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.PlannerUiState
import com.example.ui.theme.AlabasterCanvas
import com.example.ui.theme.DeepInk
import com.example.ui.theme.ForestHabit
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IndigoBridge
import com.example.ui.theme.PureIvory
import com.example.ui.theme.SlateSecondary
import com.example.ui.theme.SoftLinen
import com.example.ui.theme.VermilionAccent
import com.example.util.DateTimeUtils

@Composable
fun NotesScreen(
    uiState: PlannerUiState,
    onSelectDay: (Long) -> Unit,
    onSaveNote: (Long, String, String) -> Unit,
    onPushNoteToLaptop: (Long, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDay = uiState.selectedEpochDay
    val note = uiState.noteForSelectedDay

    var priorityFocus by remember(selectedDay, note?.updatedAtMillis) {
        mutableStateOf(note?.priorityFocus.orEmpty())
    }
    var content by remember(selectedDay, note?.updatedAtMillis) {
        mutableStateOf(note?.content.orEmpty())
    }

    LaunchedEffect(selectedDay, note) {
        priorityFocus = note?.priorityFocus.orEmpty()
        content = note?.content.orEmpty()
    }

    val checklistLines = remember(content) {
        content.lines()
            .mapIndexedNotNull { idx, line ->
                val trimmed = line.trimStart()
                when {
                    trimmed.startsWith("[ ] ") -> Triple(idx, false, trimmed.removePrefix("[ ] "))
                    trimmed.startsWith("[x] ", ignoreCase = true) -> Triple(idx, true, trimmed.drop(4))
                    else -> null
                }
            }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AlabasterCanvas),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "DAILY NOTES",
                    style = MaterialTheme.typography.labelSmall,
                    color = VermilionAccent
                )
                Text(
                    text = DateTimeUtils.formatFullDate(selectedDay),
                    style = MaterialTheme.typography.headlineMedium,
                    color = DeepInk
                )
            }
        }

        item {
            Surface(
                color = PureIvory,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, HairlineBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = priorityFocus,
                        onValueChange = { priorityFocus = it },
                        label = { Text("Main focus for the day") },
                        placeholder = { Text("e.g., Finish core study topics") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_focus_input")
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "[ ] " to "+ Checklist",
                            "• " to "+ Bullet",
                            "⚑ " to "+ Follow-up"
                        ).forEach { (snippet, label) ->
                            Surface(
                                color = SoftLinen,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    content = if (content.isBlank()) snippet else "$content\n$snippet"
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

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Notes, checklists & links") },
                        placeholder = { Text("Jot down anything you want to remember or send to your laptop...") },
                        minLines = 5,
                        maxLines = 12,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_body_input")
                    )

                    // Interactive checklist preview when user uses [ ] lines
                    if (checklistLines.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "TAP TO CHECK OFF",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateSecondary
                            )
                            checklistLines.forEach { (lineIndex, checked, text) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val lines = content.lines().toMutableList()
                                            if (lineIndex in lines.indices) {
                                                val updatedPrefix = if (checked) "[ ] " else "[x] "
                                                lines[lineIndex] = updatedPrefix + text
                                                content = lines.joinToString("\n")
                                                onSaveNote(selectedDay, content, priorityFocus)
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                        contentDescription = if (checked) "Checked" else "Unchecked",
                                        tint = if (checked) ForestHabit else SlateSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        color = if (checked) SlateSecondary else DeepInk
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSaveNote(selectedDay, content, priorityFocus) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_note_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Save Note")
                        }

                        Button(
                            onClick = { onPushNoteToLaptop(selectedDay, priorityFocus, content) },
                            enabled = !uiState.isPushingToLaptop,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IndigoBridge,
                                contentColor = PureIvory
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("push_note_laptop_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Computer, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Push to Laptop")
                        }
                    }
                }
            }
        }

        if (uiState.recentNotes.isNotEmpty()) {
            item {
                Text(
                    text = "RECENT NOTES",
                    style = MaterialTheme.typography.labelLarge,
                    color = SlateSecondary
                )
            }

            items(uiState.recentNotes, key = { it.epochDay }) { pastNote ->
                Surface(
                    color = PureIvory,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (pastNote.epochDay == selectedDay) VermilionAccent else HairlineBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDay(pastNote.epochDay) }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DateTimeUtils.formatFullDate(pastNote.epochDay),
                                style = MaterialTheme.typography.titleSmall,
                                color = DeepInk
                            )
                            Text(
                                text = DateTimeUtils.formatTimestamp(pastNote.updatedAtMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateSecondary
                            )
                        }
                        if (pastNote.priorityFocus.isNotBlank()) {
                            Text(
                                text = pastNote.priorityFocus,
                                style = MaterialTheme.typography.labelLarge,
                                color = VermilionAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (pastNote.content.isNotBlank()) {
                            Text(
                                text = pastNote.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.size(84.dp)) }
    }
}
