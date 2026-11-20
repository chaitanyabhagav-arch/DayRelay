package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BridgeConfig
import com.example.data.model.BridgeProviderType
import com.example.data.model.DeliveryStatus
import com.example.ui.PlannerUiState
import com.example.ui.theme.AlabasterCanvas
import com.example.ui.theme.DeepInk
import com.example.ui.theme.ForestContainer
import com.example.ui.theme.ForestHabit
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IndigoBridge
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.OnForestContainer
import com.example.ui.theme.OnUrgentContainer
import com.example.ui.theme.PureIvory
import com.example.ui.theme.SlateSecondary
import com.example.ui.theme.SoftLinen
import com.example.ui.theme.UrgentContainer
import com.example.ui.theme.VermilionAccent
import com.example.util.DateTimeUtils

@Composable
fun LaptopBridgeScreen(
    uiState: PlannerUiState,
    onSaveConfig: (BridgeConfig) -> Unit,
    onGenerateFreshTopic: (BridgeConfig) -> Unit,
    onSendTestPing: () -> Unit,
    onClearLogs: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val config = uiState.bridgeConfig
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    var bridgeEnabled by remember(config) { mutableStateOf(config.bridgeEnabled) }
    var providerType by remember(config) { mutableStateOf(config.providerType) }
    var ntfyBaseUrl by remember(config) { mutableStateOf(config.ntfyBaseUrl) }
    var ntfyTopic by remember(config) { mutableStateOf(config.ntfyTopic) }
    var webhookUrl by remember(config) { mutableStateOf(config.webhookUrl) }
    var authToken by remember(config) { mutableStateOf(config.authToken) }
    var includePriority by remember(config) { mutableStateOf(config.includePriorityBadges) }

    LaunchedEffect(config) {
        bridgeEnabled = config.bridgeEnabled
        providerType = config.providerType
        ntfyBaseUrl = config.ntfyBaseUrl
        ntfyTopic = config.ntfyTopic
        webhookUrl = config.webhookUrl
        authToken = config.authToken
        includePriority = config.includePriorityBadges
    }

    val currentDraft = BridgeConfig(
        bridgeEnabled = bridgeEnabled,
        providerType = providerType,
        ntfyBaseUrl = ntfyBaseUrl.trim().ifBlank { "https://ntfy.sh" },
        ntfyTopic = ntfyTopic.trim(),
        authToken = authToken.trim(),
        webhookUrl = webhookUrl.trim(),
        defaultSnoozeMinutes = config.defaultSnoozeMinutes,
        includePriorityBadges = includePriority
    )

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
                    text = "PHONE → LAPTOP RELAY",
                    style = MaterialTheme.typography.labelSmall,
                    color = IndigoBridge
                )
                Text(
                    text = "Laptop Notifications",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DeepInk
                )
            }
        }

        // Editorial Banner Illustration
        item {
            Surface(
                color = PureIvory,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, HairlineBorder)
            ) {
                Column {
                    Image(
                        painter = painterResource(id = R.drawable.img_laptop_relay_banner),
                        contentDescription = "DayRelay phone to laptop notification bridge illustration",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp)
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    )
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Never miss a 3:45 PM reminder while working on your laptop",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepInk
                        )
                        Text(
                            text = "Every scheduled task, timed event, habit, and alarm automatically sends a desktop push to your laptop at the exact minute.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSecondary
                        )
                    }
                }
            }
        }

        // Configuration Card
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Always Send to Laptop",
                                style = MaterialTheme.typography.titleSmall,
                                color = DeepInk
                            )
                            Text(
                                text = "Alerts both your phone and laptop simultaneously",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateSecondary
                            )
                        }
                        Switch(
                            checked = bridgeEnabled,
                            onCheckedChange = {
                                bridgeEnabled = it
                                onSaveConfig(currentDraft.copy(bridgeEnabled = it))
                            },
                            modifier = Modifier.testTag("bridge_enabled_switch")
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Delivery Channel",
                            style = MaterialTheme.typography.labelLarge,
                            color = SlateSecondary
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BridgeProviderType.entries.forEach { provider ->
                                FilterChip(
                                    selected = providerType == provider,
                                    onClick = { providerType = provider },
                                    label = { Text(provider.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IndigoBridge,
                                        selectedLabelColor = PureIvory
                                    ),
                                    modifier = Modifier.testTag("provider_${provider.name}")
                                )
                            }
                        }
                    }

                    if (providerType == BridgeProviderType.NTFY) {
                        OutlinedTextField(
                            value = ntfyBaseUrl,
                            onValueChange = { ntfyBaseUrl = it },
                            label = { Text("ntfy Server URL") },
                            placeholder = { Text("https://ntfy.sh") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = ntfyTopic,
                                onValueChange = { ntfyTopic = it },
                                label = { Text("Your Private ntfy Topic") },
                                placeholder = { Text("dayrelay-yourname") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ntfy_topic_input")
                            )
                            IconButton(
                                onClick = {
                                    onGenerateFreshTopic(currentDraft)
                                },
                                modifier = Modifier.testTag("regenerate_topic_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Autorenew,
                                    contentDescription = "Generate random topic",
                                    tint = IndigoBridge
                                )
                            }
                        }

                        if (currentDraft.ntfySubscriptionUrl.isNotBlank()) {
                            Surface(
                                color = IndigoContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Open this URL in your laptop browser (Chrome, Edge, Safari, or ntfy desktop app) and allow desktop notifications:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = IndigoBridge
                                    )
                                    Text(
                                        text = currentDraft.ntfySubscriptionUrl,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DeepInk
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(currentDraft.ntfySubscriptionUrl))
                                                onShowMessage("Copied laptop link")
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.size(4.dp))
                                            Text("Copy Link", style = MaterialTheme.typography.labelLarge)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                runCatching { uriHandler.openUri(currentDraft.ntfySubscriptionUrl) }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.size(4.dp))
                                            Text("Preview", style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = webhookUrl,
                            onValueChange = { webhookUrl = it },
                            label = { Text("${providerType.label} URL") },
                            placeholder = { Text("https://...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("webhook_url_input")
                        )
                    }

                    OutlinedTextField(
                        value = authToken,
                        onValueChange = { authToken = it },
                        label = { Text("Access / Bearer Token (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Include priority level in laptop alerts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepInk
                        )
                        Switch(
                            checked = includePriority,
                            onCheckedChange = { includePriority = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSaveConfig(currentDraft) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_bridge_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Settings")
                        }

                        Button(
                            onClick = {
                                onSaveConfig(currentDraft)
                                onSendTestPing()
                            },
                            enabled = !uiState.isPushingToLaptop,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VermilionAccent,
                                contentColor = PureIvory
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("send_test_ping_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(if (uiState.isPushingToLaptop) "Pinging..." else "Test Laptop")
                        }
                    }
                }
            }
        }

        // Delivery Log History
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DELIVERY HISTORY (${uiState.deliveryLogs.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = SlateSecondary
                )
                if (uiState.deliveryLogs.isNotEmpty()) {
                    TextButton(onClick = onClearLogs) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Clear")
                    }
                }
            }
        }

        if (uiState.deliveryLogs.isEmpty()) {
            item {
                Surface(
                    color = PureIvory,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No laptop pushes logged yet. Tap Test Laptop above or schedule a reminder to see live HTTP delivery confirmations here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(uiState.deliveryLogs, key = { it.id }) { log ->
                val isSuccess = log.status == DeliveryStatus.SENT
                Surface(
                    color = PureIvory,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (isSuccess) ForestContainer else UrgentContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                                contentDescription = log.status.label,
                                tint = if (isSuccess) OnForestContainer else OnUrgentContainer,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = log.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DeepInk,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = DateTimeUtils.formatTimestamp(log.timestampMillis),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateSecondary
                                )
                            }
                            Text(
                                text = log.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSuccess) ForestHabit else OnUrgentContainer
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.size(84.dp)) }
    }
}
