package dev.ilamparithi.aournalpp.ui.settings.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import dev.ilamparithi.aournalpp.ui.settings.components.ConnectedButtonItem
import dev.ilamparithi.aournalpp.ui.settings.components.MultiSelectConnectedButtonGroup
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.logging.CrashReport
import dev.ilamparithi.aournalpp.logging.CrashType
import dev.ilamparithi.aournalpp.logging.LogEntry
import dev.ilamparithi.aournalpp.logging.LogLevel
import dev.ilamparithi.aournalpp.logging.LogShareHelper
import dev.ilamparithi.aournalpp.ui.settings.components.SettingsIconBadge
import dev.ilamparithi.aournalpp.ui.settings.dialogs.CrashDetailsDialog
import dev.ilamparithi.aournalpp.ui.settings.viewmodel.LogManagerUiState
import dev.ilamparithi.aournalpp.ui.settings.viewmodel.LogManagerViewModel
import dev.ilamparithi.aournalpp.ui.settings.viewmodel.LogTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private fun Modifier.minTouchTarget(): Modifier = this.padding(0.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogManagerScreen(
    onBack: () -> Unit,
    viewModel: LogManagerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearConfirmationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (showClearConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmationDialog = false },
            title = { Text(stringResource(R.string.log_manager_clear_dialog_title)) },
            text = { Text(stringResource(R.string.log_manager_clear_dialog_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllCrashReports()
                        showClearConfirmationDialog = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.log_manager_delete_crash))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmationDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    uiState.selectedCrashReport?.let { report ->
        CrashDetailsDialog(
            report = report,
            onDismiss = { viewModel.selectCrashReport(null) },
            onDelete = { reportId ->
                viewModel.deleteCrashReport(reportId)
                viewModel.selectCrashReport(null)
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.pref_log_manager_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minTouchTarget()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (uiState.activeTab == LogTab.CRASHES && uiState.crashReports.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmationDialog = true },
                            modifier = Modifier.minTouchTarget()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.log_manager_clear_all_crashes),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (uiState.activeTab == LogTab.LIVE_LOGS) {
                        IconButton(
                            onClick = { viewModel.toggleAutoRefresh() },
                            modifier = Modifier.minTouchTarget()
                        ) {
                            if (uiState.isAutoRefreshEnabled) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = stringResource(R.string.cd_pause_auto_refresh),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.cd_resume_auto_refresh),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.clearLiveLogs() },
                            modifier = Modifier.minTouchTarget()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.log_manager_clear_logs),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Material 3 Primary Tab Row with Icons & Labels
            PrimaryTabRow(
                selectedTabIndex = uiState.activeTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                val crashCount = uiState.crashReports.size
                val crashesLabel = if (crashCount > 0) {
                    "${stringResource(R.string.log_manager_tab_crashes)} ($crashCount)"
                } else {
                    stringResource(R.string.log_manager_tab_crashes)
                }

                Tab(
                    selected = uiState.activeTab == LogTab.CRASHES,
                    onClick = { viewModel.switchTab(LogTab.CRASHES) },
                    icon = { Icon(Icons.Default.BugReport, contentDescription = null) },
                    text = {
                        Text(
                            text = crashesLabel,
                            fontWeight = if (uiState.activeTab == LogTab.CRASHES) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
                Tab(
                    selected = uiState.activeTab == LogTab.LIVE_LOGS,
                    onClick = { viewModel.switchTab(LogTab.LIVE_LOGS) },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                    text = {
                        Text(
                            text = stringResource(R.string.log_manager_tab_live_logs),
                            fontWeight = if (uiState.activeTab == LogTab.LIVE_LOGS) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (uiState.activeTab) {
                    LogTab.CRASHES -> {
                        CrashReportsTabContent(
                            uiState = uiState,
                            onSelectReport = { viewModel.selectCrashReport(it) },
                            onDeleteReport = { viewModel.deleteCrashReport(it) },
                            onClearAll = { showClearConfirmationDialog = true }
                        )
                    }
                    LogTab.LIVE_LOGS -> {
                        LiveLogsTabContent(
                            uiState = uiState,
                            onLogLevelToggled = { viewModel.toggleLogLevel(it) },
                            onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                            onAutoRefresh = { viewModel.refresh(silent = true) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CrashReportsTabContent(
    uiState: LogManagerUiState,
    onSelectReport: (CrashReport) -> Unit,
    onDeleteReport: (String) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    val copiedToastMsg = stringResource(R.string.log_manager_copied_toast)
    val shareChooserTitle = stringResource(R.string.log_manager_share_report)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    if (uiState.crashReports.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.log_manager_no_crashes_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.log_manager_no_crashes_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(uiState.crashReports, key = { it.id }) { report ->
                    CrashReportCard(
                        report = report,
                        onViewDetails = { onSelectReport(report) },
                        onDelete = { onDeleteReport(report.id) },
                        onCopy = {
                            LogShareHelper.copyToClipboard(
                                context = context,
                                label = "Crash Report",
                                text = report.toMarkdown(),
                                toastMessage = copiedToastMsg
                            )
                        },
                        onShare = {
                            LogShareHelper.shareTextFile(
                                context = context,
                                fileName = "crash_${report.id}.md",
                                content = report.toMarkdown(),
                                subject = "Aournal++ Crash Report - ${report.id}",
                                chooserTitle = shareChooserTitle
                            )
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onClearAll,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minTouchTarget()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.log_manager_clear_all_crashes),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Draggable Scroll Handle with 48dp touch target
            LazyListScrollHandle(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
            )

            // Scroll to bottom floating button
            AnimatedVisibility(
                visible = listState.canScrollForward,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 24.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(uiState.crashReports.lastIndex)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.cd_scroll_to_bottom)
                    )
                }
            }
        }
    }
}

@Composable
private fun CrashReportCard(
    report: CrashReport,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val formattedDate = remember(report.timestamp) {
        val df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
        df.format(Date(report.timestamp))
    }

    val typeLabel = when (report.type) {
        CrashType.JVM_EXCEPTION -> "JVM Exception"
        CrashType.NATIVE_PROCESS -> "Native X11/Xournal++ Crash"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsIconBadge(
                        icon = Icons.Default.BugReport
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = report.processName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = report.errorClass,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (report.errorMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = report.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minTouchTarget()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.log_manager_delete_crash),
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.minTouchTarget()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.log_manager_copy_report),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.minTouchTarget()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.log_manager_share_report),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = onViewDetails,
                        modifier = Modifier.minTouchTarget()
                    ) {
                        Text(stringResource(R.string.log_manager_view_details))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveLogsTabContent(
    uiState: LogManagerUiState,
    onLogLevelToggled: (LogLevel) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onAutoRefresh: () -> Unit
) {
    val context = LocalContext.current
    val logsCopiedToastMsg = stringResource(R.string.log_manager_logs_copied_toast)
    val shareAllTitle = stringResource(R.string.log_manager_share_all)
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-refresh every 5 seconds if enabled and user not actively dragging scroll
    LaunchedEffect(uiState.isAutoRefreshEnabled) {
        if (uiState.isAutoRefreshEnabled) {
            while (isActive) {
                delay(5000L)
                if (!scrollState.isScrollInProgress) {
                    onAutoRefresh()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.log_manager_search_hint)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onSearchQueryChanged("")
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.minTouchTarget()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.cd_clear_search)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Material 3 Expressive Multi-Select Connected Button Group
        val levelItems = remember {
            listOf(
                ConnectedButtonItem(value = LogLevel.DEBUG, label = "DEBUG"),
                ConnectedButtonItem(value = LogLevel.INFO, label = "INFO"),
                ConnectedButtonItem(value = LogLevel.WARN, label = "WARN"),
                ConnectedButtonItem(value = LogLevel.ERROR, label = "ERROR")
            )
        }

        MultiSelectConnectedButtonGroup(
            items = levelItems,
            selectedItems = uiState.selectedLogLevels,
            onItemToggled = onLogLevelToggled,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Info bar + Copy/Share quick actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${uiState.filteredLogs.size} lines",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        val text = uiState.filteredLogs.joinToString("\n") { it.raw }
                        LogShareHelper.copyToClipboard(
                            context = context,
                            label = "Application Logs",
                            text = text,
                            toastMessage = logsCopiedToastMsg
                        )
                    },
                    modifier = Modifier.minTouchTarget()
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.log_manager_copy_all))
                }

                TextButton(
                    onClick = {
                        val text = uiState.filteredLogs.joinToString("\n") { it.raw }
                        LogShareHelper.shareTextFile(
                            context = context,
                            fileName = "aournalpp_logs.txt",
                            content = text,
                            subject = "Aournal++ Application Logs",
                            chooserTitle = shareAllTitle
                        )
                    },
                    modifier = Modifier.minTouchTarget()
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.log_manager_share_all))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Fast, 120 FPS pure Jetpack Compose log viewer with whole-log selection
        if (uiState.filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.log_manager_no_logs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val primaryColor = MaterialTheme.colorScheme.primary
            val errorColor = MaterialTheme.colorScheme.error
            val warnColor = Color(0xFFE65100)
            val infoColor = MaterialTheme.colorScheme.primary
            val debugColor = MaterialTheme.colorScheme.onSurfaceVariant
            val verboseColor = MaterialTheme.colorScheme.outline

            val annotatedString = remember(uiState.filteredLogs) {
                buildAnnotatedString {
                    for (entry in uiState.filteredLogs) {
                        val start = length
                        if (entry.isNew) {
                            withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) {
                                append("▌ ")
                            }
                        } else {
                            append("  ")
                        }

                        val levelColor = when (entry.level) {
                            LogLevel.ERROR, LogLevel.ASSERT -> errorColor
                            LogLevel.WARN -> warnColor
                            LogLevel.INFO -> infoColor
                            LogLevel.DEBUG -> debugColor
                            LogLevel.VERBOSE -> verboseColor
                        }

                        withStyle(SpanStyle(color = levelColor)) {
                            append(entry.raw)
                        }
                        append("\n")

                        if (entry.isNew) {
                            addStyle(
                                SpanStyle(background = primaryColor.copy(alpha = 0.10f)),
                                start,
                                length - 1
                            )
                        }
                    }
                }
            }

            // Auto-follow when at the bottom and not actively scrolling
            LaunchedEffect(annotatedString) {
                if (!scrollState.canScrollForward && !scrollState.isScrollInProgress) {
                    scrollState.scrollTo(scrollState.maxValue)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                SelectionContainer(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = annotatedString,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }

                // Draggable Scrollbar Handle
                DraggableScrollStateBar(
                    scrollState = scrollState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                )

                // Scroll to bottom floating action button
                androidx.compose.animation.AnimatedVisibility(
                    visible = scrollState.canScrollForward,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 24.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.cd_scroll_to_bottom)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Material 3 draggable scroll handle for ScrollState with 48dp touch target.
 */
@Composable
fun DraggableScrollStateBar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val maxScrollPx = scrollState.maxValue.toFloat()
        val showScrollbar by remember {
            derivedStateOf { maxScrollPx > 0 }
        }

        if (showScrollbar) {
            val trackHeightPx = constraints.maxHeight.toFloat()
            val thumbHeightPx = (trackHeightPx * 0.15f).coerceIn(60f, 220f)
            val maxTravelPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)

            val currentScrollVal by remember { derivedStateOf { scrollState.value.toFloat() } }
            val thumbOffsetPx = (currentScrollVal / maxScrollPx.coerceAtLeast(1f)) * maxTravelPx
            val thumbHeightDp = with(density) { thumbHeightPx.toDp() }

            var isDragging by remember { mutableStateOf(false) }

            val currentMaxScroll by rememberUpdatedState(maxScrollPx)
            val currentThumbHeight by rememberUpdatedState(thumbHeightPx)
            val currentMaxTravel by rememberUpdatedState(maxTravelPx)
            val currentOffset by rememberUpdatedState(thumbOffsetPx)

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val targetOffset = (offset.y - currentThumbHeight / 2).coerceIn(0f, currentMaxTravel)
                            val fraction = targetOffset / currentMaxTravel
                            val targetY = (fraction * currentMaxScroll).toInt()
                            coroutineScope.launch {
                                scrollState.scrollTo(targetY)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val newOffset = (currentOffset + dragAmount).coerceIn(0f, currentMaxTravel)
                                val fraction = newOffset / currentMaxTravel
                                val targetY = (fraction * currentMaxScroll).toInt()
                                coroutineScope.launch {
                                    scrollState.scrollTo(targetY)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.TopEnd
            ) {
                Surface(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .offset { IntOffset(0, thumbOffsetPx.roundToInt()) }
                        .height(thumbHeightDp)
                        .width(if (isDragging) 8.dp else 5.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    shape = RoundedCornerShape(4.dp),
                    color = if (isDragging) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    }
                ) {}
            }
        }
    }
}

/**
 * Draggable Material 3 scroll handle overlay for LazyColumn with 48dp touch target.
 */
@Composable
fun LazyListScrollHandle(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val totalItems by remember { derivedStateOf { listState.layoutInfo.totalItemsCount } }
        val visibleItems by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.size } }

        val showScrollbar by remember {
            derivedStateOf { totalItems > visibleItems && totalItems > 0 }
        }

        if (showScrollbar) {
            val trackHeightPx = constraints.maxHeight.toFloat()
            val thumbMetrics by remember(trackHeightPx) {
                derivedStateOf {
                    val total = listState.layoutInfo.totalItemsCount
                    val visible = listState.layoutInfo.visibleItemsInfo.size
                    val thumbHeight = (trackHeightPx * (visible.toFloat() / total)).coerceIn(60f, trackHeightPx)
                    val maxScrollIndex = (total - visible).coerceAtLeast(1)
                    val scrollFraction = (listState.firstVisibleItemIndex.toFloat() / maxScrollIndex).coerceIn(0f, 1f)
                    val maxTravel = (trackHeightPx - thumbHeight).coerceAtLeast(1f)
                    val thumbOffset = maxTravel * scrollFraction
                    Triple(thumbHeight, thumbOffset, maxTravel)
                }
            }

            val thumbHeightPx = thumbMetrics.first
            val thumbOffsetPx = thumbMetrics.second
            val maxTravelPx = thumbMetrics.third
            val thumbHeightDp = with(density) { thumbHeightPx.toDp() }

            var isDragging by remember { mutableStateOf(false) }

            val currentTotalItems by rememberUpdatedState(totalItems)
            val currentThumbHeight by rememberUpdatedState(thumbHeightPx)
            val currentMaxTravel by rememberUpdatedState(maxTravelPx)
            val currentThumbOffset by rememberUpdatedState(thumbOffsetPx)

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val targetOffset = (offset.y - currentThumbHeight / 2).coerceIn(0f, currentMaxTravel)
                            val fraction = targetOffset / currentMaxTravel
                            val targetIndex = (fraction * (currentTotalItems - 1)).toInt().coerceIn(0, currentTotalItems - 1)
                            coroutineScope.launch {
                                listState.scrollToItem(targetIndex)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val newOffset = (currentThumbOffset + dragAmount).coerceIn(0f, currentMaxTravel)
                                val newFraction = if (currentMaxTravel > 0) newOffset / currentMaxTravel else 0f
                                val targetIndex = (newFraction * (currentTotalItems - 1)).toInt().coerceIn(0, currentTotalItems - 1)
                                coroutineScope.launch {
                                    listState.scrollToItem(targetIndex)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.TopEnd
            ) {
                Surface(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .offset { IntOffset(0, thumbOffsetPx.roundToInt()) }
                        .height(thumbHeightDp)
                        .width(if (isDragging) 8.dp else 5.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    shape = RoundedCornerShape(4.dp),
                    color = if (isDragging) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    }
                ) {}
            }
        }
    }
}
