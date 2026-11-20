package com.example.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.BridgeProviderType
import com.example.data.model.PlannerItemEntity
import com.example.data.model.PlannerItemType
import com.example.scheduler.NotificationHelper
import com.example.ui.components.BridgeStatusBadge
import com.example.ui.components.ItemComposerSheet
import com.example.ui.components.PermissionBannerCard
import com.example.ui.screens.LaptopBridgeScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.AlabasterCanvas
import com.example.ui.theme.DeepInk
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.PureIvory
import com.example.ui.theme.SlateSecondary
import com.example.ui.theme.VermilionAccent
import com.example.ui.theme.VermilionContainer
import kotlinx.coroutines.launch

enum class AppDestination(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    TODAY(R.string.nav_today, Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday, "nav_today"),
    NOTES(R.string.nav_notes, Icons.Filled.EditNote, Icons.Outlined.EditNote, "nav_notes"),
    BRIDGE(R.string.nav_bridge, Icons.Filled.Computer, Icons.Outlined.Computer, "nav_bridge")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayRelayApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentDestination by remember { mutableStateOf(AppDestination.TODAY) }

    var showComposer by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<PlannerItemEntity?>(null) }
    var composerInitialType by remember { mutableStateOf(PlannerItemType.TASK) }

    var hasNotifPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }
    var canScheduleExact by remember {
        mutableStateOf(checkExactAlarmCapability(context))
    }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (!granted) {
            scope.launch {
                snackbarHostState.showSnackbar("Phone notifications denied — laptop pushes will still work")
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifPermission = NotificationHelper.hasNotificationPermission(context)
                canScheduleExact = checkExactAlarmCapability(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (currentDestination != AppDestination.TODAY) {
        BackHandler {
            currentDestination = AppDestination.TODAY
        }
    }

    val openComposer: (PlannerItemType, PlannerItemEntity?) -> Unit = { type, item ->
        composerInitialType = type
        editingItem = item
        showComposer = true
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AlabasterCanvas)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AlabasterCanvas,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall,
                                color = DeepInk
                            )
                        },
                        actions = {
                            val badgeLabel = when (uiState.bridgeConfig.providerType) {
                                BridgeProviderType.NTFY -> uiState.bridgeConfig.ntfyTopic.ifBlank { "Setup ntfy" }
                                else -> uiState.bridgeConfig.providerType.label
                            }
                            BridgeStatusBadge(
                                isConfigured = uiState.bridgeConfig.isConfigured,
                                isEnabled = uiState.bridgeConfig.bridgeEnabled,
                                topicOrProvider = badgeLabel,
                                onClick = { currentDestination = AppDestination.BRIDGE },
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = AlabasterCanvas,
                            titleContentColor = DeepInk
                        )
                    )
                    HorizontalDivider(color = HairlineBorder, thickness = 1.dp)
                }
            },
            floatingActionButton = {
                if (currentDestination == AppDestination.TODAY) {
                    ExtendedFloatingActionButton(
                        onClick = { openComposer(PlannerItemType.TASK, null) },
                        containerColor = VermilionAccent,
                        contentColor = PureIvory,
                        shape = RoundedCornerShape(16.dp),
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(id = R.string.action_add_task),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        modifier = Modifier.testTag("add_task_fab")
                    )
                }
            },
            bottomBar = {
                if (!useRail) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PureIvory)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        HorizontalDivider(color = HairlineBorder, thickness = 1.dp)
                        NavigationBar(
                            containerColor = PureIvory,
                            tonalElevation = 0.dp
                        ) {
                            AppDestination.entries.forEach { destination ->
                                val selected = currentDestination == destination
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentDestination = destination },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                            contentDescription = stringResource(id = destination.labelRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(id = destination.labelRes),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = VermilionAccent,
                                        selectedTextColor = VermilionAccent,
                                        indicatorColor = VermilionContainer,
                                        unselectedIconColor = SlateSecondary,
                                        unselectedTextColor = SlateSecondary
                                    ),
                                    modifier = Modifier.testTag(destination.testTag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = PureIvory,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        AppDestination.entries.forEach { destination ->
                            val selected = currentDestination == destination
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentDestination = destination },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = stringResource(id = destination.labelRes)
                                    )
                                },
                                label = {
                                    Text(stringResource(id = destination.labelRes))
                                },
                                modifier = Modifier.testTag(destination.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 840.dp)
                            .fillMaxSize()
                    ) {
                        if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            PermissionBannerCard(
                                title = stringResource(id = R.string.permission_notifications_title),
                                body = stringResource(id = R.string.permission_notifications_body),
                                buttonText = "Allow",
                                onAction = {
                                    notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        } else if (!canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            PermissionBannerCard(
                                title = stringResource(id = R.string.permission_exact_alarm_title),
                                body = stringResource(id = R.string.permission_exact_alarm_body),
                                buttonText = "Grant",
                                onAction = {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    runCatching { context.startActivity(intent) }
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        when (currentDestination) {
                            AppDestination.TODAY -> TodayScreen(
                                uiState = uiState,
                                onSelectDay = viewModel::selectDay,
                                onToggleComplete = viewModel::toggleTaskOrEventCompleted,
                                onToggleAlarmEnabled = viewModel::toggleAlarmEnabled,
                                onUpdateItemTime = viewModel::updateItemStartTime,
                                onQuickSnoozeItem = viewModel::quickSnoozeItem,
                                onStartFocusTimer = viewModel::startFocusTimer,
                                onToggleFocusPause = viewModel::toggleFocusTimerPause,
                                onAddFocusMinutes = viewModel::addFocusMinutes,
                                onStopFocusTimer = viewModel::stopFocusTimer,
                                onEditItem = { item -> openComposer(item.itemType, item) },
                                onPushItemNow = viewModel::pushItemToLaptopNow,
                                onSendDayBrief = viewModel::sendDayBriefToLaptop,
                                onQuickAddTask = { type -> openComposer(type, null) },
                                modifier = Modifier.weight(1f)
                            )

                            AppDestination.NOTES -> NotesScreen(
                                uiState = uiState,
                                onSelectDay = viewModel::selectDay,
                                onSaveNote = viewModel::saveDailyNote,
                                onPushNoteToLaptop = viewModel::pushNoteToLaptop,
                                modifier = Modifier.weight(1f)
                            )

                            AppDestination.BRIDGE -> LaptopBridgeScreen(
                                uiState = uiState,
                                onSaveConfig = viewModel::saveBridgeConfig,
                                onGenerateFreshTopic = viewModel::generateFreshTopicAndSave,
                                onSendTestPing = viewModel::sendTestPingToLaptop,
                                onClearLogs = viewModel::clearDeliveryLogs,
                                onShowMessage = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showComposer) {
        ItemComposerSheet(
            initialItem = editingItem,
            initialType = composerInitialType,
            selectedEpochDay = uiState.selectedEpochDay,
            defaultSnoozeMinutes = uiState.bridgeConfig.defaultSnoozeMinutes,
            onDismiss = {
                showComposer = false
                editingItem = null
            },
            onSave = { entities, pushNow ->
                viewModel.saveItems(entities, alsoPushNow = pushNow)
                showComposer = false
                editingItem = null
            },
            onDelete = { entity ->
                viewModel.deleteItem(entity)
                showComposer = false
                editingItem = null
            }
        )
    }
}

private fun checkExactAlarmCapability(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        am?.canScheduleExactAlarms() ?: false
    } else {
        true
    }
}
