package dev.ilamparithi.aournalpp.ui.cloud

import android.widget.TimePicker
import androidx.compose.ui.res.stringResource
import dev.ilamparithi.aournalpp.R
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ilamparithi.aournalpp.backup.engine.BackupEngine
import dev.ilamparithi.aournalpp.backup.model.ConflictResolutionPolicy
import dev.ilamparithi.aournalpp.backup.model.CustomFolderMapping
import dev.ilamparithi.aournalpp.backup.model.FileConflictGroup
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import dev.ilamparithi.aournalpp.backup.model.TransferStatus
import dev.ilamparithi.aournalpp.backup.provider.StorageProviderFactory
import dev.ilamparithi.aournalpp.backup.queue.FileTransferQueueManager
import dev.ilamparithi.aournalpp.backup.security.CredentialsVault
import dev.ilamparithi.aournalpp.backup.worker.BackupPreferences
import dev.ilamparithi.aournalpp.backup.worker.BackupScheduler
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.ui.SpeedDialActionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CloudSubpage {
    OVERVIEW,
    TRANSFER_QUEUE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudScreen(
    initialSubpage: CloudSubpage = CloudSubpage.OVERVIEW,
    initialFolderToMapPath: String? = null,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val vault = remember { CredentialsVault(context) }
    val backupPrefs = remember { BackupPreferences(context) }
    val engine = remember { BackupEngine(context) }

    var currentSubpage by remember { mutableStateOf(initialSubpage) }

    var services by remember { mutableStateOf(vault.getAllServices()) }
    var exclusionFilter by remember { mutableStateOf(vault.getExclusionFilter()) }

    var isAutoBackupOnExit by remember { mutableStateOf(backupPrefs.isAutoBackupOnExitEnabled) }
    var isCheckRemoteOnLaunch by remember { mutableStateOf(backupPrefs.isCheckRemoteChangesOnLaunchEnabled) }
    var periodicIntervalMinutes by remember { mutableIntStateOf(backupPrefs.periodicSyncIntervalMinutes) }
    var isDailyScheduledSync by remember { mutableStateOf(backupPrefs.isDailyScheduledSyncEnabled) }
    var dailyHour by remember { mutableIntStateOf(backupPrefs.dailyScheduledHour) }
    var dailyMinute by remember { mutableIntStateOf(backupPrefs.dailyScheduledMinute) }
    var isWifiOnly by remember { mutableStateOf(backupPrefs.isWifiOnlyEnabled) }
    var concurrencyWorkers by remember { mutableIntStateOf(backupPrefs.concurrencyWorkers) }
    var selectedConflictPolicy by remember { mutableStateOf(backupPrefs.defaultConflictPolicy) }

    var showServiceDialog by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<ServiceConfig?>(null) }

    var showMappingDialog by remember { mutableStateOf(initialFolderToMapPath != null) }
    var mappingTargetServiceId by remember { mutableStateOf<String?>(null) }
    var editingMapping by remember { mutableStateOf<CustomFolderMapping?>(null) }
    var initialMappingLocalPath by remember { mutableStateOf(initialFolderToMapPath ?: "") }

    var showExclusionDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var restoreTargetService by remember { mutableStateOf<ServiceConfig?>(null) }

    var isGlobalSyncRunning by remember { mutableStateOf(false) }

    // Multi-service Conflict States
    var detectedConflicts by remember { mutableStateOf<List<FileConflictGroup>>(emptyList()) }
    var showConflictDialog by remember { mutableStateOf(false) }
    var isCheckingConflicts by remember { mutableStateOf(false) }

    val queueItems by FileTransferQueueManager.items.collectAsStateWithLifecycle()
    val activeTransfers = queueItems.filter { it.status == TransferStatus.IN_PROGRESS || it.status == TransferStatus.QUEUED }

    val reduceAnimations = remember {
        context.getSharedPreferences("${context.packageName}_preferences", android.content.Context.MODE_PRIVATE)
            .getBoolean(LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS, false)
    }

    // FAB Speed Dial States
    var isFabExpanded by remember { mutableStateOf(false) }
    val fabRotation by animateFloatAsState(
        targetValue = if (isFabExpanded) 135f else 0f,
        animationSpec = if (reduceAnimations) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "fabRotation"
    )

    fun refreshState() {
        services = vault.getAllServices()
        exclusionFilter = vault.getExclusionFilter()
    }

    if (currentSubpage == CloudSubpage.TRANSFER_QUEUE) {
        TransferQueueSubpage(onNavigateBack = { currentSubpage = CloudSubpage.OVERVIEW })
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.cloud_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        BadgedBox(
                            badge = {
                                if (activeTransfers.isNotEmpty()) {
                                    Badge { Text(activeTransfers.size.toString()) }
                                }
                            }
                        ) {
                            IconButton(onClick = { currentSubpage = CloudSubpage.TRANSFER_QUEUE }) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = stringResource(R.string.cloud_tab_queue)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item {
                OverviewCard(
                    services = services,
                    isSyncRunning = isGlobalSyncRunning,
                    isCheckingConflicts = isCheckingConflicts,
                    onCheckConflicts = {
                        isCheckingConflicts = true
                        coroutineScope.launch {
                            try {
                                val conflicts = engine.detectMultiServiceConflicts()
                                detectedConflicts = conflicts
                                if (conflicts.isNotEmpty()) {
                                    showConflictDialog = true
                                } else {
                                    snackbarHostState.showSnackbar("All cloud files and local notes are up to date with zero conflicts!")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Conflict check failed: ${e.message}")
                            } finally {
                                isCheckingConflicts = false
                            }
                        }
                    },
                    onSyncAll = {
                        isGlobalSyncRunning = true
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Starting synchronization across all active clouds...")
                            try {
                                val results = engine.performMultiServiceBackup(concurrency = concurrencyWorkers)
                                val totalUploaded = results.sumOf { it.filesUploaded }
                                val totalFailed = results.sumOf { it.filesFailed }
                                refreshState()
                                if (totalFailed == 0) {
                                    snackbarHostState.showSnackbar("Backup complete: $totalUploaded files uploaded")
                                } else {
                                    snackbarHostState.showSnackbar("Backup finished with $totalFailed errors")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Sync failed: ${e.message}")
                            } finally {
                                isGlobalSyncRunning = false
                            }
                        }
                    },
                    onOpenQueue = { currentSubpage = CloudSubpage.TRANSFER_QUEUE }
                )
            }

            if (detectedConflicts.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showConflictDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReportProblem,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.cloud_conflicts_detected, detectedConflicts.size),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = stringResource(R.string.cloud_conflicts_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                            Button(
                                onClick = { showConflictDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(stringResource(R.string.cloud_conflicts_button))
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${stringResource(R.string.cloud_services_header)} (${services.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (services.isEmpty()) {
                item {
                    EmptyServicesCard(
                        onAddService = {
                            editingService = null
                            showServiceDialog = true
                        }
                    )
                }
            } else {
                items(services, key = { it.id }) { service ->
                    CloudServiceCard(
                        service = service,
                        onEdit = {
                            editingService = service
                            showServiceDialog = true
                        },
                        onToggleEnabled = { enabled ->
                            val updated = service.copy(isEnabled = enabled)
                            vault.saveService(updated)
                            refreshState()
                        },
                        onDelete = {
                            vault.deleteService(service.id)
                            refreshState()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Removed \"${service.name}\"")
                            }
                        },
                        onSyncNow = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Starting sync for \"${service.name}\"...")
                                try {
                                    val result = engine.performBackup(service, concurrency = concurrencyWorkers)
                                    refreshState()
                                    if (result.isSuccess) {
                                        snackbarHostState.showSnackbar("Synced \"${service.name}\": ${result.filesUploaded} uploaded")
                                    } else {
                                        snackbarHostState.showSnackbar("Sync error on \"${service.name}\": ${result.errors.firstOrNull() ?: "failed"}")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Sync error: ${e.message}")
                                }
                            }
                        },
                        onRestore = {
                            restoreTargetService = service
                            showRestoreConfirmDialog = true
                        },
                        onAddMapping = {
                            mappingTargetServiceId = service.id
                            editingMapping = null
                            initialMappingLocalPath = ""
                            showMappingDialog = true
                        },
                        onEditMapping = { mapping ->
                            mappingTargetServiceId = service.id
                            editingMapping = mapping
                            initialMappingLocalPath = mapping.localFolderPath
                            showMappingDialog = true
                        },
                        onDeleteMapping = { mappingId ->
                            val updatedMappings = service.customMappings.filterNot { it.id == mappingId }
                            val updated = service.copy(customMappings = updatedMappings)
                            vault.saveService(updated)
                            refreshState()
                        }
                    )
                }
            }

            item {
                ConflictPolicyCard(
                    selectedPolicy = selectedConflictPolicy,
                    onPolicySelected = { policy ->
                        selectedConflictPolicy = policy
                        backupPrefs.defaultConflictPolicy = policy
                    }
                )
            }

            item {
                ExclusionFiltersCard(
                    filter = exclusionFilter,
                    onConfigure = { showExclusionDialog = true }
                )
            }

            item {
                AutomationCard(
                    isAutoBackupOnExit = isAutoBackupOnExit,
                    onAutoBackupOnExitChange = { updated ->
                        isAutoBackupOnExit = updated
                        backupPrefs.isAutoBackupOnExitEnabled = updated
                    },
                    isCheckRemoteOnLaunch = isCheckRemoteOnLaunch,
                    onCheckRemoteOnLaunchChange = { updated ->
                        isCheckRemoteOnLaunch = updated
                        backupPrefs.isCheckRemoteChangesOnLaunchEnabled = updated
                    },
                    periodicIntervalMinutes = periodicIntervalMinutes,
                    onPeriodicIntervalChange = { updated ->
                        periodicIntervalMinutes = updated
                        backupPrefs.periodicSyncIntervalMinutes = updated
                        BackupScheduler.updateSchedules(context)
                    },
                    isDailyScheduledSync = isDailyScheduledSync,
                    onDailyScheduledSyncChange = { updated ->
                        isDailyScheduledSync = updated
                        backupPrefs.isDailyScheduledSyncEnabled = updated
                        BackupScheduler.updateSchedules(context)
                    },
                    dailyHour = dailyHour,
                    dailyMinute = dailyMinute,
                    onTimePickerClick = { showTimePickerDialog = true },
                    isWifiOnly = isWifiOnly,
                    onWifiOnlyChange = { updated ->
                        isWifiOnly = updated
                        backupPrefs.isWifiOnlyEnabled = updated
                        BackupScheduler.updateSchedules(context)
                    },
                    concurrency = concurrencyWorkers,
                    onConcurrencyChange = { updated ->
                        concurrencyWorkers = updated
                        backupPrefs.concurrencyWorkers = updated
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    AnimatedVisibility(
        visible = isFabExpanded,
        enter = fadeIn(animationSpec = spring(stiffness = 400f)),
        exit = fadeOut(animationSpec = spring(stiffness = 400f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable { isFabExpanded = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SpeedDialActionItem(
                progress = 1f,
                icon = Icons.Default.CreateNewFolder,
                label = stringResource(R.string.action_add_custom_mapping),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = {
                    isFabExpanded = false
                    editingMapping = null
                    initialMappingLocalPath = ""
                    mappingTargetServiceId = null
                    showMappingDialog = true
                }
            )

            SpeedDialActionItem(
                progress = 1f,
                icon = Icons.Default.Add,
                label = stringResource(R.string.action_add_cloud_service),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = {
                    isFabExpanded = false
                    editingService = null
                    showServiceDialog = true
                }
            )

            val fabInteractionSource = remember { MutableInteractionSource() }
            val isFabPressed by fabInteractionSource.collectIsPressedAsState()
            val fabPressScale by animateFloatAsState(
                targetValue = if (isFabPressed) 0.90f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "fabPressScale"
            )

            FloatingActionButton(
                onClick = { isFabExpanded = !isFabExpanded },
                interactionSource = fabInteractionSource,
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .size(64.dp)
                    .scale(fabPressScale)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Expand Cloud Actions",
                    modifier = Modifier
                        .size(32.dp)
                        .rotate(fabRotation)
                )
            }
        }
    }

    if (showServiceDialog) {
        ServiceConfigDialog(
            initialService = editingService,
            existingServices = services,
            onDismissRequest = {
                showServiceDialog = false
                editingService = null
            },
            onSaveService = { service ->
                vault.saveService(service)
                refreshState()
                BackupScheduler.updateSchedules(context)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Saved service \"${service.name}\"")
                }
            }
        )
    }

    if (showMappingDialog) {
        CustomMappingDialog(
            services = services,
            initialServiceId = mappingTargetServiceId,
            initialMapping = editingMapping,
            initialLocalPath = initialMappingLocalPath,
            onDismissRequest = {
                showMappingDialog = false
                editingMapping = null
                mappingTargetServiceId = null
                initialMappingLocalPath = ""
            },
            onSaveMapping = { targetServiceId, mapping ->
                val srv = services.firstOrNull { it.id == targetServiceId }
                if (srv != null) {
                    val updatedMappings = srv.customMappings.filterNot { it.id == mapping.id } + mapping
                    val updatedSrv = srv.copy(customMappings = updatedMappings)
                    vault.saveService(updatedSrv)
                    refreshState()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Mapped \"${mapping.localFolderPath}\" to \"${mapping.remoteFolderPath}\"")
                    }
                }
            }
        )
    }

    if (showExclusionDialog) {
        ExclusionFilterDialog(
            initialConfig = exclusionFilter,
            onDismissRequest = { showExclusionDialog = false },
            onSaveFilter = { config ->
                vault.saveExclusionFilter(config)
                refreshState()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Updated exclusion filters")
                }
            }
        )
    }

    if (showRestoreConfirmDialog && restoreTargetService != null) {
        val srv = restoreTargetService!!
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                restoreTargetService = null
            },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.cloud_restore_dialog_title, srv.name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.cloud_restore_dialog_desc, srv.name))
                    Text(
                        stringResource(R.string.cloud_restore_dialog_policy, selectedConflictPolicy.displayName),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        val target = srv
                        restoreTargetService = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Restoring from ${target.name}...")
                            try {
                                val result = engine.performRestore(
                                    serviceConfig = target,
                                    conflictPolicy = selectedConflictPolicy,
                                    concurrency = concurrencyWorkers
                                )
                                refreshState()
                                if (result.isSuccess) {
                                    snackbarHostState.showSnackbar("Restore complete: ${result.filesRestored} files restored")
                                } else {
                                    snackbarHostState.showSnackbar("Restore errors: ${result.errors.firstOrNull() ?: "failed"}")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Restore failed: ${e.message}")
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.cloud_restore_now_button))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirmDialog = false
                    restoreTargetService = null
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showConflictDialog && detectedConflicts.isNotEmpty()) {
        MultiServiceConflictDialog(
            conflictGroups = detectedConflicts,
            engine = engine,
            onDismissRequest = { showConflictDialog = false },
            onResolutionComplete = { report ->
                showConflictDialog = false
                detectedConflicts = emptyList()
                refreshState()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        "Resolved conflicts: ${report.filesUpdated} primary updated, ${report.filesSavedAlongside} saved alongside"
                    )
                }
            }
        )
    }
}
}

@Composable
fun OverviewCard(
    services: List<ServiceConfig>,
    isSyncRunning: Boolean,
    isCheckingConflicts: Boolean,
    onCheckConflicts: () -> Unit,
    onSyncAll: () -> Unit,
    onOpenQueue: () -> Unit
) {
    val enabledCount = services.count { it.isEnabled }
    val lastSyncEpoch = services.map { it.lastSyncedAtEpochMs }.maxOrNull() ?: 0L
    val lastSyncFormatted = if (lastSyncEpoch > 0) {
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(lastSyncEpoch))
    } else "Never"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cloud_sync_hub_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(R.string.cloud_services_active_summary, enabledCount, services.size, lastSyncFormatted),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onCheckConflicts,
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isCheckingConflicts
                    ) {
                        if (isCheckingConflicts) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        } else {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    FilledTonalButton(
                        onClick = onOpenQueue,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSyncAll,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSyncRunning && enabledCount > 0
            ) {
                if (isSyncRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.cloud_syncing_progress))
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.cloud_sync_all_button))
                }
            }
        }
    }
}

@Composable
fun EmptyServicesCard(onAddService: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudQueue,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(R.string.cloud_empty_services_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.cloud_empty_services_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(onClick = onAddService) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.cloud_add_first_service))
            }
        }
    }
}

@Composable
fun CloudServiceCard(
    service: ServiceConfig,
    onEdit: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onSyncNow: () -> Unit,
    onRestore: () -> Unit,
    onAddMapping: () -> Unit,
    onEditMapping: (CustomFolderMapping) -> Unit,
    onDeleteMapping: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name, Provider Icon, Status Switch & Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (service.providerType) {
                        StorageProviderType.GOOGLE_DRIVE -> Icons.Default.Cloud
                        StorageProviderType.NEXTCLOUD -> Icons.Default.Storage
                        StorageProviderType.WEBDAV -> Icons.Default.CloudSync
                        StorageProviderType.SFTP -> Icons.Default.Storage
                        StorageProviderType.SMB3 -> Icons.Default.Folder
                        StorageProviderType.FTP -> Icons.Default.CloudUpload
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${service.providerType.displayName} • ${if (service.serverUrl.isNotBlank()) service.serverUrl else service.host}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = service.isEnabled,
                    onCheckedChange = onToggleEnabled
                )

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_details))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.cloud_menu_edit_config)) },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.cloud_menu_add_folder_mapping)) },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) },
                            onClick = {
                                showMenu = false
                                onAddMapping()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.cloud_menu_delete_service), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sync Scope Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (service.isCompleteBackupEnabled) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.cloud_complete_mirror_chip), style = MaterialTheme.typography.labelSmall) }
                    )
                }
                if (service.customMappings.isNotEmpty()) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.cloud_custom_mappings_chip, service.customMappings.size), style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Custom Mappings Sub-List
            if (service.customMappings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    service.customMappings.forEach { mapping ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditMapping(mapping) }
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mapping.localFolderPath, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                Text("→ ${mapping.remoteFolderPath}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onDeleteMapping(mapping.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onSyncNow,
                    enabled = service.isEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.cloud_sync_now))
                }

                OutlinedButton(
                    onClick = onRestore,
                    enabled = service.isEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.cloud_restore_button))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictPolicyCard(
    selectedPolicy: ConflictResolutionPolicy,
    onPolicySelected: (ConflictResolutionPolicy) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.cloud_policy_card_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.cloud_policy_card_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedPolicy.displayName,
                    onValueChange = {},
                    readOnly = true,
                    supportingText = { Text(selectedPolicy.description) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ConflictResolutionPolicy.entries.forEach { policy ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(policy.displayName, fontWeight = FontWeight.SemiBold)
                                    Text(policy.description, style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            onClick = {
                                onPolicySelected(policy)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExclusionFiltersCard(
    filter: dev.ilamparithi.aournalpp.backup.model.ExclusionFilterConfig,
    onConfigure: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.cloud_exclusion_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onConfigure) {
                    Text(stringResource(R.string.cloud_customize_button))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            val transientPrefix = if (filter.skipDefaultTransient) "Transient & lock files ignored • " else ""
            Text(
                text = stringResource(R.string.cloud_exclusion_card_summary, transientPrefix, filter.regexPatterns.size, filter.excludedExtensions.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationCard(
    isAutoBackupOnExit: Boolean,
    onAutoBackupOnExitChange: (Boolean) -> Unit,
    isCheckRemoteOnLaunch: Boolean,
    onCheckRemoteOnLaunchChange: (Boolean) -> Unit,
    periodicIntervalMinutes: Int,
    onPeriodicIntervalChange: (Int) -> Unit,
    isDailyScheduledSync: Boolean,
    onDailyScheduledSyncChange: (Boolean) -> Unit,
    dailyHour: Int,
    dailyMinute: Int,
    onTimePickerClick: () -> Unit,
    isWifiOnly: Boolean,
    onWifiOnlyChange: (Boolean) -> Unit,
    concurrency: Int,
    onConcurrencyChange: (Int) -> Unit
) {
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", dailyHour, dailyMinute)
    var isFrequencyDropdownExpanded by remember { mutableStateOf(false) }

    val intervalOptions = listOf(
        0 to stringResource(R.string.pref_sync_freq_manual),
        5 to stringResource(R.string.pref_sync_freq_5m),
        15 to stringResource(R.string.pref_sync_freq_15m),
        30 to stringResource(R.string.pref_sync_freq_30m),
        60 to stringResource(R.string.pref_sync_freq_1h),
        1440 to stringResource(R.string.pref_sync_freq_daily)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.cloud_automation_card_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Check for Remote Changes on Launch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cloud_check_launch_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.cloud_check_launch_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isCheckRemoteOnLaunch,
                    onCheckedChange = onCheckRemoteOnLaunchChange
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Periodic Sync Interval (OneNote style)
            Column {
                Text(stringResource(R.string.cloud_sync_freq_title), fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.cloud_sync_freq_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = isFrequencyDropdownExpanded,
                    onExpandedChange = { isFrequencyDropdownExpanded = !isFrequencyDropdownExpanded }
                ) {
                    val currentLabel = intervalOptions.firstOrNull { it.first == periodicIntervalMinutes }?.second ?: stringResource(R.string.pref_sync_freq_15m)
                    OutlinedTextField(
                        value = currentLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFrequencyDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isFrequencyDropdownExpanded,
                        onDismissRequest = { isFrequencyDropdownExpanded = false }
                    ) {
                        intervalOptions.forEach { (mins, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onPeriodicIntervalChange(mins)
                                    isFrequencyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Auto-backup on exit
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cloud_auto_backup_exit_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.cloud_auto_backup_exit_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isAutoBackupOnExit,
                    onCheckedChange = onAutoBackupOnExitChange
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Daily Scheduled Sync
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cloud_daily_sync_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.cloud_daily_sync_desc, timeFormatted),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDailyScheduledSync) {
                        IconButton(onClick = onTimePickerClick) {
                            Icon(Icons.Default.Schedule, contentDescription = stringResource(R.string.action_edit))
                        }
                    }
                    Switch(
                        checked = isDailyScheduledSync,
                        onDailyScheduledSyncChange
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Wi-Fi Only
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cloud_wifi_only_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.cloud_wifi_only_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isWifiOnly,
                    onCheckedChange = onWifiOnlyChange
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Concurrency Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.cloud_concurrency_title), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.cloud_concurrency_workers_label, concurrency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    stringResource(R.string.cloud_concurrency_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = concurrency.toFloat(),
                    onValueChange = { onConcurrencyChange(it.toInt()) },
                    valueRange = 1f..4f,
                    steps = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
