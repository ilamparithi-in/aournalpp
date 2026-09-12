package dev.ilamparithi.aournalpp.ui.cloud

import android.content.Context
import androidx.activity.compose.BackHandler
import dev.ilamparithi.aournalpp.ui.animation.AppAnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.backup.engine.BackupEngine
import dev.ilamparithi.aournalpp.backup.model.CustomFolderMapping
import dev.ilamparithi.aournalpp.backup.model.FolderValidationResult
import dev.ilamparithi.aournalpp.backup.model.MappingSet
import dev.ilamparithi.aournalpp.backup.model.MappingTemplateItem
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import dev.ilamparithi.aournalpp.backup.security.CustomMappingRepository
import dev.ilamparithi.aournalpp.backup.model.TransferStatus
import dev.ilamparithi.aournalpp.backup.queue.FileTransferQueueManager
import dev.ilamparithi.aournalpp.backup.worker.BackupPreferences
import dev.ilamparithi.aournalpp.backup.worker.BackupScheduler
import dev.ilamparithi.aournalpp.utils.NetworkUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ilamparithi.aournalpp.ui.InteractiveMarqueeText
import dev.ilamparithi.aournalpp.ui.common.AppIconButton
import dev.ilamparithi.aournalpp.ui.promptWidth
import dev.ilamparithi.aournalpp.utils.FormatUtils
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

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
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 5

    var isBatchMode by remember { mutableStateOf(false) }
    val selectedMappingIds = remember { mutableStateListOf<String>() }

    val prefs = remember { BackupPreferences(context) }
    val queueItems by FileTransferQueueManager.items.collectAsStateWithLifecycle()
    val isSyncRunningByManager by FileTransferQueueManager.isSyncRunning.collectAsStateWithLifecycle()
    val isServiceSyncing = queueItems.any {
        it.serviceId == service.id && (it.status == TransferStatus.IN_PROGRESS || it.status == TransferStatus.QUEUED)
    } || isSyncRunningByManager

    var isCheckingFolders by remember { mutableStateOf(false) }
    var folderValidationResults by remember { mutableStateOf<Map<String, FolderValidationResult>>(emptyMap()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showBatchSaveSetDialog by remember { mutableStateOf(false) }

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CloudProviderIcon(
                                providerType = service.providerType,
                                size = 32.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = service.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${service.providerType.displayName} • ${service.serverUrl.ifBlank { service.host }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                                val netCheck = NetworkUtils.checkSyncNetworkPreconditions(
                                    context,
                                    wifiOnly = prefs.isWifiOnlyEnabled
                                )
                                if (!netCheck.canSync) {
                                    onShowSnackbar(netCheck.errorMessage ?: "Network not available")
                                    return@IconButton
                                }

                                BackupScheduler.triggerImmediateSync(
                                    context,
                                    wifiOnly = prefs.isWifiOnlyEnabled,
                                    targetServiceId = service.id
                                )
                                onShowSnackbar("Starting background sync for \"${service.name}\"…")
                            },
                            enabled = service.isEnabled && !isServiceSyncing
                        ) {
                            if (isServiceSyncing) {
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
            AppAnimatedVisibility(
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
                                onShowSnackbar(
                                    context.resources.getQuantityString(
                                        R.plurals.msg_mappings_disabled,
                                        count,
                                        count
                                    )
                                )
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
                                onShowSnackbar(
                                    context.resources.getQuantityString(
                                        R.plurals.msg_mappings_enabled,
                                        count,
                                        count
                                    )
                                )
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
                    AppAnimatedVisibility(visible = isCheckingFolders) {
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
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.dialog_delete_mappings_confirm,
                        selectedMappingIds.size,
                        selectedMappingIds.size
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = selectedMappingIds.size
                        val updated = service.customMappings.filterNot { selectedMappingIds.contains(it.id) }
                        onMappingsUpdated(updated)
                        selectedMappingIds.clear()
                        isBatchMode = false
                        showBatchDeleteConfirm = false
                        onShowSnackbar(
                            context.resources.getQuantityString(
                                R.plurals.msg_mappings_deleted,
                                count,
                                count
                            )
                        )
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
                            text = pluralStringResource(
                                R.plurals.dialog_create_mapping_set_with_count,
                                selectedMappingIds.size,
                                selectedMappingIds.size
                            ),
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
                            text = pluralStringResource(
                                R.plurals.dialog_check_sets_include_count,
                                selectedMappingIds.size,
                                selectedMappingIds.size
                            ),
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
                                            val itemsLabel = pluralStringResource(R.plurals.queue_items_count, totalItems, totalItems)
                                            val subtitle = when {
                                                matchCount == selectedMappings.size -> "$itemsLabel • Contains all selected"
                                                matchCount > 0 -> "$itemsLabel • Contains $matchCount of ${selectedMappings.size} selected"
                                                else -> itemsLabel
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
                                addedCount > 0 -> context.resources.getQuantityString(R.plurals.msg_mappings_added_to_sets, addedCount, addedCount)
                                removedCount > 0 -> context.resources.getQuantityString(R.plurals.msg_mappings_removed_from_sets, removedCount, removedCount)
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
