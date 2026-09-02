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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.ui.res.pluralStringResource
import dev.ilamparithi.aournalpp.ui.util.AppIconButton
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.ilamparithi.aournalpp.backup.model.FolderValidationResult
import dev.ilamparithi.aournalpp.backup.model.MappingSet
import dev.ilamparithi.aournalpp.backup.model.MappingTemplateItem
import dev.ilamparithi.aournalpp.backup.security.CustomMappingRepository
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
import androidx.compose.material3.AlertDialog
import dev.ilamparithi.aournalpp.ui.AppDialogDefaults
import dev.ilamparithi.aournalpp.ui.promptWidth
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.ui.draw.alpha
import java.io.File
import java.util.UUID
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
import androidx.compose.material3.RadioButton
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
import dev.ilamparithi.aournalpp.utils.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class CloudSubpage {
    OVERVIEW,
    TRANSFER_QUEUE,
    MAPPING_SETS
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
    val mappingRepo = remember { CustomMappingRepository(context) }

    var currentSubpage by remember { mutableStateOf(initialSubpage) }
    var selectedDetailServiceId by remember { mutableStateOf<String?>(null) }
    var showMappingSetsDialog by remember { mutableStateOf(false) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            currentSubpage == CloudSubpage.MAPPING_SETS -> {
                MappingSetsSubpage(
                    services = services,
                    mappingRepo = mappingRepo,
                    onNavigateBack = { currentSubpage = CloudSubpage.OVERVIEW },
                    onApplySetToService = { targetService, set, replace ->
                        val updatedMappings = mappingRepo.applyMappingSetToService(targetService.id, set, replace = replace)
                        val updatedSrv = targetService.copy(customMappings = updatedMappings)
                        vault.saveService(updatedSrv)
                        val env = LinuxEnvironment(context)
                        mappingRepo.syncToNotesHome(env.getNotesDirectory())
                        refreshState()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Applied \"${set.name}\" to ${targetService.name} (${set.items.size} mappings)")
                        }
                    },
                    onShowSnackbar = { msg ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                )
            }
            currentSubpage == CloudSubpage.TRANSFER_QUEUE -> {
                TransferQueueSubpage(onNavigateBack = { currentSubpage = CloudSubpage.OVERVIEW })
            }
            selectedDetailServiceId != null -> {
                val detailService = services.firstOrNull { it.id == selectedDetailServiceId }
                if (detailService != null) {
                    ServiceDetailSubpage(
                        service = detailService,
                        engine = engine,
                        mappingRepo = mappingRepo,
                        onNavigateBack = { selectedDetailServiceId = null },
                        onEditService = {
                            editingService = detailService
                            showServiceDialog = true
                        },
                        onDeleteService = {
                            vault.deleteService(detailService.id)
                            selectedDetailServiceId = null
                            refreshState()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Removed \"${detailService.name}\"")
                            }
                        },
                        onToggleEnabled = { enabled ->
                            val updated = detailService.copy(isEnabled = enabled)
                            vault.saveService(updated)
                            refreshState()
                        },
                        onToggleCompleteBackup = { enabled ->
                            val updated = detailService.copy(isCompleteBackupEnabled = enabled)
                            vault.saveService(updated)
                            refreshState()
                        },
                        onRestore = {
                            restoreTargetService = detailService
                            showRestoreConfirmDialog = true
                        },
                        onAddMapping = {
                            mappingTargetServiceId = detailService.id
                            editingMapping = null
                            initialMappingLocalPath = ""
                            showMappingDialog = true
                        },
                        onEditMapping = { mapping ->
                            mappingTargetServiceId = detailService.id
                            editingMapping = mapping
                            initialMappingLocalPath = mapping.localFolderPath
                            showMappingDialog = true
                        },
                        onOpenMappingSets = {
                            currentSubpage = CloudSubpage.MAPPING_SETS
                        },
                        onShowSnackbar = { msg ->
                            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                        },
                        onMappingsUpdated = { updatedMappings ->
                            val updated = detailService.copy(customMappings = updatedMappings)
                            vault.saveService(updated)
                            mappingRepo.saveMappingsForService(detailService.id, updatedMappings)
                            val env = LinuxEnvironment(context)
                            mappingRepo.syncToNotesHome(env.getNotesDirectory())
                            refreshState()
                        }
                    )
                } else {
                    selectedDetailServiceId = null
                }
            }
            else -> {
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
                        IconButton(onClick = { currentSubpage = CloudSubpage.MAPPING_SETS }) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = stringResource(R.string.title_mapping_sets)
                            )
                        }
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
                    onOpenQueue = { currentSubpage = CloudSubpage.TRANSFER_QUEUE },
                    onOpenMappingSets = { currentSubpage = CloudSubpage.MAPPING_SETS }
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
                    CompactCloudServiceCard(
                        service = service,
                        onClick = { selectedDetailServiceId = service.id },
                        onToggleEnabled = { enabled ->
                            val updated = service.copy(isEnabled = enabled)
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { isFabExpanded = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        val mappingItemSpring by animateFloatAsState(
            targetValue = if (isFabExpanded) 1f else 0f,
            animationSpec = if (reduceAnimations) snap() else spring(dampingRatio = 0.78f, stiffness = 340f),
            label = "mappingItemSpring"
        )
        val serviceItemSpring by animateFloatAsState(
            targetValue = if (isFabExpanded) 1f else 0f,
            animationSpec = if (reduceAnimations) snap() else spring(dampingRatio = 0.78f, stiffness = 360f),
            label = "serviceItemSpring"
        )

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SpeedDialActionItem(
                progress = mappingItemSpring,
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
                progress = serviceItemSpring,
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
                    mappingRepo.saveMappingsForService(srv.id, updatedMappings)
                    val env = LinuxEnvironment(context)
                    mappingRepo.syncToNotesHome(env.getNotesDirectory())
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
            properties = AppDialogDefaults.Properties,
            modifier = Modifier.promptWidth(),
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

@Composable
fun OverviewCard(
    services: List<ServiceConfig>,
    isSyncRunning: Boolean,
    isCheckingConflicts: Boolean,
    onCheckConflicts: () -> Unit,
    onSyncAll: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenMappingSets: () -> Unit
) {
    val enabledCount = services.count { it.isEnabled }
    val lastSyncEpoch = services.map { it.lastSyncedAtEpochMs }.maxOrNull() ?: 0L
    val neverSyncedText = stringResource(R.string.cloud_never_synced)
    val lastSyncFormatted = if (lastSyncEpoch > 0) {
        FormatUtils.formatDateTimeMedium(lastSyncEpoch)
    } else neverSyncedText

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
                        onClick = onOpenMappingSets,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = stringResource(R.string.title_mapping_sets),
                            modifier = Modifier.size(16.dp)
                        )
                    }

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
fun CompactCloudServiceCard(
    service: ServiceConfig,
    onClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit
) {
    val lastSyncFormatted = if (service.lastSyncedAtEpochMs > 0) {
        FormatUtils.formatDateTimeMedium(service.lastSyncedAtEpochMs)
    } else stringResource(R.string.cloud_never_synced)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
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
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${service.providerType.displayName} • ${if (service.serverUrl.isNotBlank()) service.serverUrl else service.host}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (service.isCompleteBackupEnabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text(
                            text = if (service.isCompleteBackupEnabled) "Complete: On" else "Complete: Off",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (service.isCompleteBackupEnabled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (service.customMappings.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = "${service.customMappings.size} mappings",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = service.isEnabled,
                onCheckedChange = onToggleEnabled
            )

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = stringResource(R.string.action_details),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ServiceDetailSubpage(
    service: ServiceConfig,
    engine: BackupEngine,
    mappingRepo: CustomMappingRepository,
    onNavigateBack: () -> Unit,
    onEditService: () -> Unit,
    onDeleteService: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleCompleteBackup: (Boolean) -> Unit,
    onRestore: () -> Unit,
    onAddMapping: () -> Unit,
    onEditMapping: (CustomFolderMapping) -> Unit,
    onOpenMappingSets: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    onMappingsUpdated: (List<CustomFolderMapping>) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    var searchQuery by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 5

    var isBatchMode by remember { mutableStateOf(false) }
    val selectedMappingIds = remember { mutableStateListOf<String>() }

    var isCheckingFolders by remember { mutableStateOf(false) }
    var folderValidationResults by remember { mutableStateOf<Map<String, FolderValidationResult>>(emptyMap()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showBatchSaveSetDialog by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(service.id, service.customMappings) {
        val stored = mappingRepo.getMappingsForService(service.id)
        val activeMappings = if (stored.isEmpty() && service.customMappings.isNotEmpty()) {
            mappingRepo.saveMappingsForService(service.id, service.customMappings)
            service.customMappings
        } else {
            stored
        }
        isCheckingFolders = true
        folderValidationResults = mappingRepo.validateFolders(service, activeMappings, engine)
        isCheckingFolders = false
    }

    val filteredMappings = remember(service.customMappings, searchQuery) {
        if (searchQuery.isBlank()) service.customMappings
        else service.customMappings.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.localFolderPath.contains(searchQuery, ignoreCase = true) ||
            it.remoteFolderPath.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalPages = maxOf(1, (filteredMappings.size + pageSize - 1) / pageSize)
    val safePage = currentPage.coerceIn(0, totalPages - 1)
    val pagedMappings = filteredMappings.drop(safePage * pageSize).take(pageSize)

    BackHandler(enabled = isBatchMode) {
        isBatchMode = false
        selectedMappingIds.clear()
    }

    Scaffold(
        topBar = {
            if (isBatchMode) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.hub_selected_count,
                                    selectedMappingIds.size,
                                    selectedMappingIds.size
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val enabledCount = service.customMappings.count { selectedMappingIds.contains(it.id) && it.isEnabled }
                            val disabledCount = selectedMappingIds.size - enabledCount
                            val summary = listOfNotNull(
                                "$enabledCount enabled".takeIf { enabledCount > 0 },
                                "$disabledCount disabled".takeIf { disabledCount > 0 }
                            ).joinToString(", ")
                            if (summary.isNotBlank()) {
                                Text(
                                    text = summary,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        val cancelLabel = stringResource(R.string.action_cancel)
                        AppIconButton(
                            onClick = {
                                isBatchMode = false
                                selectedMappingIds.clear()
                            },
                            tooltip = cancelLabel
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = cancelLabel
                            )
                        }
                    },
                    actions = {
                        val allSelected = filteredMappings.isNotEmpty() && selectedMappingIds.size == filteredMappings.size
                        val selectAllLabel = if (allSelected) stringResource(R.string.action_deselect_all)
                        else stringResource(R.string.action_select_all)

                        AppIconButton(
                            onClick = {
                                if (allSelected) {
                                    selectedMappingIds.clear()
                                    isBatchMode = false
                                } else {
                                    selectedMappingIds.clear()
                                    selectedMappingIds.addAll(filteredMappings.map { it.id })
                                }
                            },
                            tooltip = selectAllLabel
                        ) {
                            Icon(
                                imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = selectAllLabel
                            )
                        }

                        val invertLabel = stringResource(R.string.action_invert_selection)
                        AppIconButton(
                            onClick = {
                                val allIds = filteredMappings.map { it.id }.toSet()
                                val currentSet = selectedMappingIds.toSet()
                                val inverted = allIds.minus(currentSet)
                                selectedMappingIds.clear()
                                selectedMappingIds.addAll(inverted)
                                if (selectedMappingIds.isEmpty()) {
                                    isBatchMode = false
                                }
                            },
                            tooltip = invertLabel
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = invertLabel
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = service.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${service.providerType.displayName} • ${if (service.serverUrl.isNotBlank()) service.serverUrl else service.host}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                isSyncing = true
                                coroutineScope.launch {
                                    onShowSnackbar("Starting sync for \"${service.name}\"...")
                                    try {
                                        val result = engine.performBackup(service)
                                        if (result.isSuccess) {
                                            onShowSnackbar("Synced \"${service.name}\": ${result.filesUploaded} uploaded")
                                        } else {
                                            onShowSnackbar("Sync error: ${result.errors.firstOrNull() ?: "failed"}")
                                        }
                                    } catch (e: Exception) {
                                        onShowSnackbar("Sync failed: ${e.message}")
                                    } finally {
                                        isSyncing = false
                                    }
                                }
                            },
                            enabled = service.isEnabled && !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.cloud_sync_now))
                            }
                        }

                        IconButton(onClick = onRestore, enabled = service.isEnabled) {
                            Icon(Icons.Default.CloudDownload, contentDescription = stringResource(R.string.cloud_restore_button))
                        }

                        IconButton(onClick = onEditService) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cloud_menu_edit_config))
                        }

                        IconButton(onClick = onDeleteService) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cloud_menu_delete_service), tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isBatchMode && selectedMappingIds.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 10.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Add to Set
                        AppIconButton(
                            onClick = { showBatchSaveSetDialog = true },
                            tooltip = stringResource(R.string.action_add_to_set)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = stringResource(R.string.action_add_to_set),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // 2. Disable Selected
                        AppIconButton(
                            onClick = {
                                val count = selectedMappingIds.size
                                val updated = service.customMappings.map {
                                    if (selectedMappingIds.contains(it.id)) it.copy(isEnabled = false) else it
                                }
                                onMappingsUpdated(updated)
                                onShowSnackbar("Disabled $count mapping(s)")
                            },
                            tooltip = stringResource(R.string.action_batch_disable)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = stringResource(R.string.action_batch_disable),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 3. Enable Selected
                        AppIconButton(
                            onClick = {
                                val count = selectedMappingIds.size
                                val updated = service.customMappings.map {
                                    if (selectedMappingIds.contains(it.id)) it.copy(isEnabled = true) else it
                                }
                                onMappingsUpdated(updated)
                                onShowSnackbar("Enabled $count mapping(s)")
                            },
                            tooltip = stringResource(R.string.action_batch_enable)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = stringResource(R.string.action_batch_enable),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // 4. Delete Selected
                        val deleteLabel = stringResource(R.string.action_delete)
                        AppIconButton(
                            onClick = { showBatchDeleteConfirm = true },
                            tooltip = deleteLabel
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = deleteLabel,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Service Settings Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Service Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if (service.isEnabled) "Synchronization active" else "Service disabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = service.isEnabled, onCheckedChange = onToggleEnabled)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.label_complete_sync_enabled), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "Mirrors Notes and app preferences directly to root cloud backup folder.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = service.isCompleteBackupEnabled, onCheckedChange = onToggleCompleteBackup)
                        }

                        if (service.lastSyncedAtEpochMs > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Last synced: ${FormatUtils.formatDateTimeMedium(service.lastSyncedAtEpochMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // Custom Mappings Section Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Custom Mappings (${service.customMappings.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = onAddMapping,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add")
                            }

                            FilledTonalButton(
                                onClick = onOpenMappingSets,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sets")
                            }
                        }
                    }

                    // Real-time Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            currentPage = 0
                        },
                        placeholder = { Text(stringResource(R.string.hint_search_mappings)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = ""; currentPage = 0 }) {
                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_clear))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Deferred Checking Progress Bar
                    AnimatedVisibility(visible = isCheckingFolders) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.label_checking_folders),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Mappings List (greyed out while checking)
            if (pagedMappings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No mappings match \"$searchQuery\"."
                            else "No custom folder mappings configured. Add a mapping to sync specific folders.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(pagedMappings, key = { it.id }) { mapping ->
                    val isSelected = selectedMappingIds.contains(mapping.id)
                    val valRes = folderValidationResults[mapping.id]
                    val localMissing = valRes != null && !valRes.localExists
                    val remoteMissing = valRes != null && !valRes.remoteExists
                    val isError = localMissing || remoteMissing

                    val cardBgColor = when {
                        isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (isCheckingFolders) 0.6f else 1f)
                            .combinedClickable(
                                onClick = {
                                    if (isBatchMode) {
                                        if (isSelected) {
                                            selectedMappingIds.remove(mapping.id)
                                            if (selectedMappingIds.isEmpty()) isBatchMode = false
                                        } else {
                                            selectedMappingIds.add(mapping.id)
                                        }
                                    } else {
                                        onEditMapping(mapping)
                                    }
                                },
                                onLongClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (!isBatchMode) {
                                        isBatchMode = true
                                        selectedMappingIds.clear()
                                        selectedMappingIds.add(mapping.id)
                                    } else {
                                        if (isSelected) {
                                            selectedMappingIds.remove(mapping.id)
                                            if (selectedMappingIds.isEmpty()) isBatchMode = false
                                        } else {
                                            selectedMappingIds.add(mapping.id)
                                        }
                                    }
                                }
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isBatchMode) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                        modifier = Modifier
                                            .size(26.dp)
                                            .border(
                                                width = if (isSelected) 0.dp else 1.5.dp,
                                                color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                                                shape = CircleShape
                                            )
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mapping.name.ifBlank { File(mapping.localFolderPath).name },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${mapping.localFolderPath} → ${mapping.remoteFolderPath}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Switch(
                                    checked = mapping.isEnabled,
                                    onCheckedChange = { enabled ->
                                        val updated = service.customMappings.map {
                                            if (it.id == mapping.id) it.copy(isEnabled = enabled) else it
                                        }
                                        onMappingsUpdated(updated)
                                    }
                                )
                            }

                            // Error warnings in Red
                            if (localMissing) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.error_local_folder_missing, mapping.localFolderPath),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            if (remoteMissing) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.error_remote_folder_missing, mapping.remoteFolderPath),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Pagination Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { currentPage-- },
                            enabled = safePage > 0
                        ) {
                            Text(stringResource(R.string.action_prev_page))
                        }

                        Text(
                            text = stringResource(R.string.label_page_indicator, safePage + 1, totalPages, filteredMappings.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(
                            onClick = { currentPage++ },
                            enabled = safePage < totalPages - 1
                        ) {
                            Text(stringResource(R.string.action_next_page))
                        }
                    }
                }
            }

            // Bottom spacing when floating batch bar is active
            if (isBatchMode && selectedMappingIds.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(88.dp))
                }
            }
        }
    }

    // Batch delete confirmation dialog
    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text(stringResource(R.string.dialog_delete_multi_title), fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${selectedMappingIds.size} selected folder mapping(s)? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        val count = selectedMappingIds.size
                        val updated = service.customMappings.filterNot { selectedMappingIds.contains(it.id) }
                        onMappingsUpdated(updated)
                        selectedMappingIds.clear()
                        isBatchMode = false
                        showBatchDeleteConfirm = false
                        onShowSnackbar("Deleted $count mapping(s)")
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Batch save as mapping set dialog
    if (showBatchSaveSetDialog) {
        val existingSets = remember { mappingRepo.getAllMappingSets() }
        var isNewSet by remember { mutableStateOf(existingSets.isEmpty()) }
        var setName by remember { mutableStateOf("") }
        var setDesc by remember { mutableStateOf("") }

        val selectedMappings = remember(service.customMappings, selectedMappingIds) {
            service.customMappings.filter { selectedMappingIds.contains(it.id) }
        }
        val selectedLocalPaths = remember(selectedMappings) {
            selectedMappings.map { it.localFolderPath }.toSet()
        }
        val initialCheckedSetIds = remember(existingSets, selectedLocalPaths) {
            existingSets.filter { set ->
                val setPaths = set.items.map { it.localFolderPath }.toSet()
                selectedLocalPaths.isNotEmpty() && selectedLocalPaths.any { setPaths.contains(it) }
            }.map { it.id }
        }
        val checkedSetIds = remember(existingSets, selectedLocalPaths) {
            mutableStateListOf<String>().apply { addAll(initialCheckedSetIds) }
        }

        AlertDialog(
            onDismissRequest = { showBatchSaveSetDialog = false },
            modifier = Modifier.promptWidth(),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.action_add_to_set),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (existingSets.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Row(modifier = Modifier.padding(3.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (isNewSet) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier.clickable { isNewSet = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "New Set",
                                            tint = if (isNewSet) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "New",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isNewSet) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (!isNewSet) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier.clickable { isNewSet = false }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Bookmark,
                                            contentDescription = "Existing Sets",
                                            tint = if (!isNewSet) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Existing",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!isNewSet) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            text = {
                if (isNewSet) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Create a new mapping set with the ${selectedMappingIds.size} selected mapping(s):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = setName,
                            onValueChange = { setName = it },
                            label = { Text("Set Name") },
                            placeholder = { Text("e.g. Work Folders") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = setDesc,
                            onValueChange = { setDesc = it },
                            label = { Text("Description (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Check sets to include these ${selectedMappingIds.size} mapping(s). Unchecking removes them:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(existingSets) { existing ->
                                val isChecked = checkedSetIds.contains(existing.id)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) {
                                                checkedSetIds.remove(existing.id)
                                            } else {
                                                checkedSetIds.add(existing.id)
                                            }
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    checkedSetIds.add(existing.id)
                                                } else {
                                                    checkedSetIds.remove(existing.id)
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = existing.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            val totalItems = existing.items.size
                                            val matchCount = existing.items.count { item -> selectedLocalPaths.contains(item.localFolderPath) }
                                            val subtitle = when {
                                                matchCount == selectedMappings.size -> "$totalItems items • Contains all selected"
                                                matchCount > 0 -> "$totalItems items • Contains $matchCount of ${selectedMappings.size} selected"
                                                else -> "$totalItems items"
                                            }
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val templateItems = selectedMappings.map { m ->
                            MappingTemplateItem(
                                name = m.name,
                                localFolderPath = m.localFolderPath,
                                remoteFolderPath = m.remoteFolderPath,
                                isEnabled = m.isEnabled
                            )
                        }

                        if (isNewSet) {
                            if (setName.isNotBlank()) {
                                val newSet = MappingSet(
                                    id = UUID.randomUUID().toString(),
                                    name = setName.trim(),
                                    description = setDesc.trim(),
                                    createdAtEpochMs = System.currentTimeMillis(),
                                    items = templateItems
                                )
                                mappingRepo.saveMappingSet(newSet)
                                showBatchSaveSetDialog = false
                                onShowSnackbar("Saved mapping set \"${newSet.name}\"")
                            }
                        } else {
                            var addedCount = 0
                            var removedCount = 0
                            existingSets.forEach { set ->
                                val setPaths = set.items.map { it.localFolderPath }.toSet()
                                val shouldContain = checkedSetIds.contains(set.id)
                                if (shouldContain) {
                                    val toAdd = templateItems.filterNot { setPaths.contains(it.localFolderPath) }
                                    if (toAdd.isNotEmpty()) {
                                        val updatedSet = set.copy(items = set.items + toAdd)
                                        mappingRepo.saveMappingSet(updatedSet)
                                        addedCount++
                                    }
                                } else {
                                    val hasAny = set.items.any { selectedLocalPaths.contains(it.localFolderPath) }
                                    if (hasAny) {
                                        val updatedItems = set.items.filterNot { selectedLocalPaths.contains(it.localFolderPath) }
                                        val updatedSet = set.copy(items = updatedItems)
                                        mappingRepo.saveMappingSet(updatedSet)
                                        removedCount++
                                    }
                                }
                            }
                            showBatchSaveSetDialog = false
                            val msg = when {
                                addedCount > 0 && removedCount > 0 -> "Updated sets: added to $addedCount, removed from $removedCount"
                                addedCount > 0 -> "Added mappings to $addedCount set(s)"
                                removedCount > 0 -> "Removed mappings from $removedCount set(s)"
                                else -> "Mapping sets unchanged"
                            }
                            onShowSnackbar(msg)
                        }
                    },
                    enabled = (isNewSet && setName.isNotBlank()) || (!isNewSet && existingSets.isNotEmpty())
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchSaveSetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
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
    val timeFormatted = FormatUtils.formatTime(dailyHour, dailyMinute)
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
                Text(
                    text = stringResource(R.string.cloud_concurrency_title),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.cloud_concurrency_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.cloud_concurrency_workers_label, concurrency),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
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
