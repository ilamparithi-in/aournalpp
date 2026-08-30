package dev.ilamparithi.aournalpp.ui.cloud

import android.widget.TimePicker
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
                            text = "Cloud Backup & Sync",
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
                                    contentDescription = "Transfer Queue"
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
            // 1. Overview & Quick Sync Banner
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

            // Conflict Alert Banner (if any conflicts detected)
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
                                    text = "${detectedConflicts.size} File Conflicts Detected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Tap to review differing versions across cloud endpoints",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                            Button(
                                onClick = { showConflictDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Resolve")
                            }
                        }
                    }
                }
            }

            // 2. Configured Cloud Services Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Configured Services (${services.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 3. Service Cards or Empty State
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

            // 4. Restore Conflict Policy Selector
            item {
                ConflictPolicyCard(
                    selectedPolicy = selectedConflictPolicy,
                    onPolicySelected = { policy ->
                        selectedConflictPolicy = policy
                        backupPrefs.defaultConflictPolicy = policy
                    }
                )
            }

            // 5. Exclusion Filters Card
            item {
                ExclusionFiltersCard(
                    filter = exclusionFilter,
                    onConfigure = { showExclusionDialog = true }
                )
            }

            // 6. Automation & Performance Card
            item {
                AutomationCard(
                    isAutoBackupOnExit = isAutoBackupOnExit,
                    onAutoBackupOnExitChange = { enabled ->
                        isAutoBackupOnExit = enabled
                        backupPrefs.isAutoBackupOnExitEnabled = enabled
                    },
                    isCheckRemoteOnLaunch = isCheckRemoteOnLaunch,
                    onCheckRemoteOnLaunchChange = { enabled ->
                        isCheckRemoteOnLaunch = enabled
                        backupPrefs.isCheckRemoteChangesOnLaunchEnabled = enabled
                    },
                    periodicIntervalMinutes = periodicIntervalMinutes,
                    onPeriodicIntervalChange = { interval ->
                        periodicIntervalMinutes = interval
                        backupPrefs.periodicSyncIntervalMinutes = interval
                        BackupScheduler.updateSchedules(context)
                    },
                    isDailyScheduledSync = isDailyScheduledSync,
                    onDailyScheduledSyncChange = { enabled ->
                        isDailyScheduledSync = enabled
                        backupPrefs.isDailyScheduledSyncEnabled = enabled
                        BackupScheduler.updateSchedules(context)
                    },
                    dailyHour = dailyHour,
                    dailyMinute = dailyMinute,
                    onTimePickerClick = { showTimePickerDialog = true },
                    isWifiOnly = isWifiOnly,
                    onWifiOnlyChange = { enabled ->
                        isWifiOnly = enabled
                        backupPrefs.isWifiOnlyEnabled = enabled
                        BackupScheduler.updateSchedules(context)
                    },
                    concurrency = concurrencyWorkers,
                    onConcurrencyChange = { count ->
                        concurrencyWorkers = count
                        backupPrefs.concurrencyWorkers = count
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Floating Dim Background Scrim when FAB expanded (identical to Home and Files screens)
    AnimatedVisibility(
        visible = isFabExpanded,
        enter = fadeIn(animationSpec = spring(stiffness = 400f)),
        exit = fadeOut(animationSpec = spring(stiffness = 400f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { isFabExpanded = false }
        )
    }

    // Speed Dial Action Items + Main FAB
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(24.dp)
    ) {
        val mappingItemSpring by animateFloatAsState(
            targetValue = if (isFabExpanded) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 320f),
            label = "mappingItemSpring"
        )
        val serviceItemSpring by animateFloatAsState(
            targetValue = if (isFabExpanded) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 340f),
            label = "serviceItemSpring"
        )

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Staggered Spring Action Items
            SpeedDialActionItem(
                progress = mappingItemSpring,
                icon = Icons.Default.CreateNewFolder,
                label = "Add Custom Folder Mapping",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = {
                    isFabExpanded = false
                    editingMapping = null
                    mappingTargetServiceId = null
                    initialMappingLocalPath = ""
                    showMappingDialog = true
                }
            )

            SpeedDialActionItem(
                progress = serviceItemSpring,
                icon = Icons.Default.CloudQueue,
                label = "Add Cloud Service",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = {
                    isFabExpanded = false
                    editingService = null
                    showServiceDialog = true
                }
            )

            // Main Speed Dial FAB with spring physics on press & rotate
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
}

    // Dialogs
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
            title = { Text("Restore from ${srv.name}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This will download files from ${srv.name} to your local storage.")
                    Text(
                        "Conflict Policy: ${selectedConflictPolicy.displayName}",
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
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirmDialog = false
                    restoreTargetService = null
                }) {
                    Text("Cancel")
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

@Composable
private fun OverviewCard(
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
                        text = "Cloud Sync Hub",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$enabledCount of ${services.size} services active • Last sync: $lastSyncFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onCheckConflicts,
                        enabled = !isCheckingConflicts && enabledCount > 0,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isCheckingConflicts) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Conflicts")
                        }
                    }

                    FilledTonalButton(
                        onClick = onOpenQueue,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Queue")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onSyncAll,
                enabled = !isSyncRunning && enabledCount > 0,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSyncRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Synchronizing Across Active Clouds...")
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync All Active Cloud Services")
                }
            }
        }
    }
}

@Composable
private fun EmptyServicesCard(onAddService: () -> Unit) {
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
                text = "No Cloud Storage Configured",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Connect Google Drive, Nextcloud, WebDAV, SFTP, SMB3, or FTP to synchronize notes and settings securely.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(onClick = onAddService) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add First Cloud Service")
            }
        }
    }
}

@Composable
private fun CloudServiceCard(
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
                        Icon(Icons.Default.MoreVert, contentDescription = "Service Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Configuration") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Folder Mapping") },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) },
                            onClick = {
                                showMenu = false
                                onAddMapping()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete Service", color = MaterialTheme.colorScheme.error) },
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
                        label = { Text("Complete Mirror (~/Notes & ~/.config)", style = MaterialTheme.typography.labelSmall) }
                    )
                }
                if (service.customMappings.isNotEmpty()) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("${service.customMappings.size} Custom Mapping(s)", style = MaterialTheme.typography.labelSmall) }
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
                                Icon(Icons.Default.Delete, contentDescription = "Delete mapping", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
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
                    Text("Sync Now")
                }

                OutlinedButton(
                    onClick = onRestore,
                    enabled = service.isEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore")
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
                text = "Restore Conflict Policy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Controls how incoming cloud files resolve collisions with existing local files.",
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
                        text = "Exclusion Filters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onConfigure) {
                    Text("Customize")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${if (filter.skipDefaultTransient) "Transient & lock files ignored • " else ""}${filter.regexPatterns.size} regex pattern(s) • ${filter.excludedExtensions.size} excluded extension(s)",
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
        0 to "Manual only",
        5 to "Every 5 minutes (Active App)",
        15 to "Every 15 minutes",
        30 to "Every 30 minutes",
        60 to "Every hour",
        1440 to "Daily"
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
                text = "Automation & Performance",
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
                    Text("Check for Cloud Changes on Launch", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Scans remote clouds on app open to notify if notes have upstream updates",
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
                Text("Sync Frequency", fontWeight = FontWeight.SemiBold)
                Text(
                    "Automatically synchronizes notes at periodic intervals",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = isFrequencyDropdownExpanded,
                    onExpandedChange = { isFrequencyDropdownExpanded = !isFrequencyDropdownExpanded }
                ) {
                    val currentLabel = intervalOptions.firstOrNull { it.first == periodicIntervalMinutes }?.second ?: "Every 15 minutes"
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
                    Text("Auto-backup on App Exit", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Automatically synchronizes changed documents when closing the app",
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
                    Text("Daily Scheduled Sync", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Runs background sync daily at $timeFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDailyScheduledSync) {
                        IconButton(onClick = onTimePickerClick) {
                            Icon(Icons.Default.Schedule, contentDescription = "Edit Time")
                        }
                    }
                    Switch(
                        checked = isDailyScheduledSync,
                        onCheckedChange = onDailyScheduledSyncChange
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
                    Text("Transfer on Wi-Fi Only", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Prevent automated transfers over metered cellular connections",
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
                    Text("Concurrent Transfer Streams", fontWeight = FontWeight.SemiBold)
                    Text(
                        "$concurrency parallel worker(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "Accelerates differential uploads and downloads on high-bandwidth connections",
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
