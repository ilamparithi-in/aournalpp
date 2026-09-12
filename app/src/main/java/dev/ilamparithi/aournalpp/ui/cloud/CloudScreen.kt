package dev.ilamparithi.aournalpp.ui.cloud

import android.widget.TimePicker
import androidx.compose.ui.res.stringResource
import dev.ilamparithi.aournalpp.R
import androidx.compose.animation.AnimatedContent
import dev.ilamparithi.aournalpp.ui.animation.AppAnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import dev.ilamparithi.aournalpp.ui.common.AppIconButton
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import dev.ilamparithi.aournalpp.ui.AppDialogDefaults
import dev.ilamparithi.aournalpp.ui.promptWidth
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import dev.ilamparithi.aournalpp.utils.NetworkUtils
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
import androidx.compose.runtime.key
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
import dev.ilamparithi.aournalpp.ui.InteractiveMarqueeText
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

    val vault = remember { CredentialsVault.getInstance(context) }
    val backupPrefs = remember { BackupPreferences(context) }
    val engine = remember { BackupEngine(context, vault = vault) }
    val mappingRepo = remember { CustomMappingRepository(context) }

    var currentSubpage by remember { mutableStateOf(initialSubpage) }
    var selectedDetailServiceId by remember { mutableStateOf<String?>(null) }
    var showMappingSetsDialog by remember { mutableStateOf(false) }

    val services by vault.servicesFlow.collectAsStateWithLifecycle()
    val pendingDeletedServiceIds by vault.pendingDeletionsFlow.collectAsStateWithLifecycle()
    var exclusionFilter by remember { mutableStateOf(vault.getExclusionFilter()) }

    var isAutoBackupOnExit by remember { mutableStateOf(backupPrefs.isAutoBackupOnExitEnabled) }
    var isCheckRemoteOnLaunch by remember { mutableStateOf(backupPrefs.isCheckRemoteChangesOnLaunchEnabled) }
    var periodicIntervalMinutes by remember { mutableIntStateOf(backupPrefs.periodicSyncIntervalMinutes) }
    var isDailyScheduledSync by remember { mutableStateOf(backupPrefs.isDailyScheduledSyncEnabled) }
    var dailyHour by remember { mutableIntStateOf(backupPrefs.dailyScheduledHour) }
    var dailyMinute by remember { mutableIntStateOf(backupPrefs.dailyScheduledMinute) }
    var isWifiOnly by remember { mutableStateOf(backupPrefs.isWifiOnlyEnabled) }
    var concurrencyWorkers by remember { mutableIntStateOf(backupPrefs.concurrencyWorkers) }
    val activeConcurrency by FileTransferQueueManager.concurrencyWorkers.collectAsStateWithLifecycle()
    LaunchedEffect(activeConcurrency) {
        if (concurrencyWorkers != activeConcurrency) {
            concurrencyWorkers = activeConcurrency
        }
    }
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

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var servicePendingDeletion by remember { mutableStateOf<ServiceConfig?>(null) }

    val isSyncRunningByManager by FileTransferQueueManager.isSyncRunning.collectAsStateWithLifecycle()
    var isLocalSyncRunning by remember { mutableStateOf(false) }
    val isGlobalSyncRunning = isSyncRunningByManager || isLocalSyncRunning

    // Multi-service Conflict States
    var detectedConflicts by remember { mutableStateOf<List<FileConflictGroup>>(emptyList()) }
    var showConflictDialog by remember { mutableStateOf(false) }
    var isCheckingConflicts by remember { mutableStateOf(false) }

    val queueItems by FileTransferQueueManager.items.collectAsStateWithLifecycle()
    val activeTransfers = queueItems.filter { it.status == TransferStatus.IN_PROGRESS || it.status == TransferStatus.QUEUED }

    val reduceAnimations = dev.ilamparithi.aournalpp.ui.animation.LocalMotionPreferences.current.reduceAnimations

    // FAB Speed Dial States
    var isFabExpanded by remember { mutableStateOf(false) }
    val fabRotation by dev.ilamparithi.aournalpp.ui.animation.rememberFabRotation(isFabExpanded, reduceAnimations)

    fun refreshState() {
        exclusionFilter = vault.getExclusionFilter()
    }

    LaunchedEffect(isSyncRunningByManager) {
        if (!isSyncRunningByManager) {
            refreshState()
        }
    }

    BackHandler(enabled = currentSubpage != CloudSubpage.OVERVIEW || selectedDetailServiceId != null) {
        if (currentSubpage != CloudSubpage.OVERVIEW) {
            currentSubpage = CloudSubpage.OVERVIEW
        } else if (selectedDetailServiceId != null) {
            selectedDetailServiceId = null
        }
    }

    AnimatedContent(
        targetState = currentSubpage,
        transitionSpec = {
            if (reduceAnimations) {
                fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
            } else if (targetState.ordinal > initialState.ordinal) {
                slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
            } else {
                slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
            }
        },
        label = "CloudSubpageTransition"
    ) { subpage ->
        when (subpage) {
            CloudSubpage.MAPPING_SETS -> {
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
                            snackbarHostState.showSnackbar(
                                context.resources.getQuantityString(
                                    R.plurals.msg_mapping_set_applied,
                                    set.items.size,
                                    set.name,
                                    targetService.name,
                                    set.items.size
                                )
                            )
                        }
                    },
                    onShowSnackbar = { msg ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                )
            }
            CloudSubpage.TRANSFER_QUEUE -> {
                TransferQueueSubpage(
                    engine = engine,
                    onNavigateBack = { currentSubpage = CloudSubpage.OVERVIEW }
                )
            }
            CloudSubpage.OVERVIEW -> {
                AnimatedContent(
                    targetState = selectedDetailServiceId,
                    transitionSpec = {
                        if (reduceAnimations) {
                            fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                        } else if (targetState != null && initialState == null) {
                            slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                        } else if (targetState == null && initialState != null) {
                            slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
                        } else {
                            fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "ServiceDetailTransition"
                ) { detailId ->
                    if (detailId != null) {
                        val detailService = services.firstOrNull { it.id == detailId }
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
                                    servicePendingDeletion = detailService
                                    showDeleteConfirmDialog = true
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
                    } else {
                        Scaffold(
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.cloud_title),
                                fontWeight = FontWeight.Bold
                            )
                            val nonPendingServices = services.filter { it.id !in pendingDeletedServiceIds }
                            val enabledCount = nonPendingServices.count { it.isEnabled }
                            val lastSyncEpoch = nonPendingServices.maxOfOrNull { it.lastSyncedAtEpochMs } ?: 0L
                            val neverSyncedText = stringResource(R.string.cloud_never_synced)
                            val lastSyncFormatted = if (lastSyncEpoch > 0) {
                                FormatUtils.formatDateTimeMedium(lastSyncEpoch)
                            } else neverSyncedText

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = stringResource(R.string.cloud_services_active_summary, enabledCount, nonPendingServices.size, lastSyncFormatted),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        val nonPendingServices = services.filter { it.id !in pendingDeletedServiceIds }
                        val enabledCount = nonPendingServices.count { it.isEnabled }

                        // 1. Sync All Active Cloud Services
                        AppIconButton(
                            onClick = {
                                val netCheck = NetworkUtils.checkSyncNetworkPreconditions(context, wifiOnly = isWifiOnly)
                                if (!netCheck.canSync) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = netCheck.errorMessage ?: "Network not available",
                                            duration = androidx.compose.material3.SnackbarDuration.Short
                                        )
                                    }
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Synchronization queued in background...")
                                    }
                                    BackupScheduler.triggerImmediateSync(context, wifiOnly = isWifiOnly)
                                }
                            },
                            tooltip = stringResource(R.string.cloud_sync_all_button),
                            enabled = !isGlobalSyncRunning && enabledCount > 0
                        ) {
                            if (isGlobalSyncRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.cloud_sync_all_button)
                                )
                            }
                        }

                        // 2. Check Conflicts
                        AppIconButton(
                            onClick = {
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
                            tooltip = stringResource(R.string.cd_cloud_check_conflicts),
                            enabled = !isCheckingConflicts
                        ) {
                            if (isCheckingConflicts) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = stringResource(R.string.cd_cloud_check_conflicts)
                                )
                            }
                        }

                        // 3. Mapping Sets
                        AppIconButton(
                            onClick = { currentSubpage = CloudSubpage.MAPPING_SETS },
                            tooltip = stringResource(R.string.title_mapping_sets)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = stringResource(R.string.title_mapping_sets)
                            )
                        }

                        // 4. Transfer Queue
                        AppIconButton(
                            onClick = { currentSubpage = CloudSubpage.TRANSFER_QUEUE },
                            tooltip = stringResource(R.string.cloud_tab_queue)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (activeTransfers.isNotEmpty()) {
                                        Badge { Text(activeTransfers.size.toString()) }
                                    }
                                }
                            ) {
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
                                    text = pluralStringResource(R.plurals.cloud_conflicts_detected, detectedConflicts.size, detectedConflicts.size),
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
                ConfiguredServicesCarousel(
                    services = services,
                    pendingDeletedServiceIds = pendingDeletedServiceIds,
                    onSelectService = { service ->
                        showServiceDialog = false
                        editingService = null
                        selectedDetailServiceId = service.id
                    },
                    onEditService = { service ->
                        editingService = service
                        showServiceDialog = true
                    },
                    onToggleEnabled = { service, enabled ->
                        val updated = service.copy(isEnabled = enabled)
                        vault.saveService(updated)
                        refreshState()
                    },
                    onRestoreService = { service ->
                        vault.restorePendingDeletedService(service.id)
                        refreshState()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("${context.getString(R.string.action_restore_cloud)}: \"${service.name}\"")
                        }
                    },
                    onAddService = {
                        editingService = null
                        showServiceDialog = true
                    }
                )
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
                    onFilterUpdated = { updated ->
                        vault.saveExclusionFilter(updated)
                        backupPrefs.isSyncTrashEnabled = updated.syncTrash
                        refreshState()
                    },
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
                        FileTransferQueueManager.setConcurrencyWorkers(updated)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    AppAnimatedVisibility(
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
}
}

    if (showServiceDialog) {
        key(editingService?.id ?: "new_service") {
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
                backupPrefs.isSyncTrashEnabled = config.syncTrash
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
                        restoreTargetService = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Restoring from ${srv.name}...")
                            try {
                                val result = engine.performRestore(
                                    serviceConfig = srv,
                                    conflictPolicy = selectedConflictPolicy,
                                    concurrency = concurrencyWorkers
                                )
                                refreshState()
                                if (result.isSuccess) {
                                    snackbarHostState.showSnackbar(
                                        context.resources.getQuantityString(
                                            R.plurals.msg_restore_files_complete,
                                            result.filesRestored,
                                            result.filesRestored
                                        )
                                    )
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

    if (showDeleteConfirmDialog && servicePendingDeletion != null) {
        val target = servicePendingDeletion!!
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                servicePendingDeletion = null
            },
            properties = AppDialogDefaults.Properties,
            modifier = Modifier.promptWidth(),
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.dialog_delete_cloud_title, target.name), fontWeight = FontWeight.Bold) },
            text = {
                Text(stringResource(R.string.dialog_delete_cloud_message, target.name))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        servicePendingDeletion = null
                        vault.markServicePendingDeletion(target.id)
                        selectedDetailServiceId = null
                        refreshState()
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = context.getString(R.string.label_cloud_pending_deletion),
                                actionLabel = context.getString(R.string.action_restore_cloud),
                                duration = SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                vault.restorePendingDeletedService(target.id)
                                refreshState()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.action_delete_cloud_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        servicePendingDeletion = null
                    }
                ) {
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
