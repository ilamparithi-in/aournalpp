package dev.ilamparithi.aournalpp.ui.cloud

import android.widget.TimePicker
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ilamparithi.aournalpp.backup.engine.BackupEngine
import dev.ilamparithi.aournalpp.backup.model.ConflictResolutionPolicy
import dev.ilamparithi.aournalpp.backup.model.CustomFolderMapping
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import dev.ilamparithi.aournalpp.backup.model.TransferStatus
import dev.ilamparithi.aournalpp.backup.provider.StorageProviderFactory
import dev.ilamparithi.aournalpp.backup.queue.FileTransferQueueManager
import dev.ilamparithi.aournalpp.backup.security.CredentialsVault
import dev.ilamparithi.aournalpp.backup.worker.BackupPreferences
import dev.ilamparithi.aournalpp.backup.worker.BackupScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudScreen(
    onNavigateToSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val vault = remember { CredentialsVault(context) }
    val backupPrefs = remember { BackupPreferences(context) }
    val engine = remember { BackupEngine(context) }

    var services by remember { mutableStateOf(vault.getAllServices()) }
    var exclusionFilter by remember { mutableStateOf(vault.getExclusionFilter()) }

    var isAutoBackupOnExit by remember { mutableStateOf(backupPrefs.isAutoBackupOnExitEnabled) }
    var isDailyScheduledSync by remember { mutableStateOf(backupPrefs.isDailyScheduledSyncEnabled) }
    var dailyHour by remember { mutableIntStateOf(backupPrefs.dailyScheduledHour) }
    var dailyMinute by remember { mutableIntStateOf(backupPrefs.dailyScheduledMinute) }
    var isWifiOnly by remember { mutableStateOf(backupPrefs.isWifiOnlyEnabled) }
    var concurrencyWorkers by remember { mutableIntStateOf(backupPrefs.concurrencyWorkers) }
    var selectedConflictPolicy by remember { mutableStateOf(backupPrefs.defaultConflictPolicy) }

    var showServiceDialog by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<ServiceConfig?>(null) }

    var showMappingDialog by remember { mutableStateOf(false) }
    var mappingTargetServiceId by remember { mutableStateOf<String?>(null) }
    var editingMapping by remember { mutableStateOf<CustomFolderMapping?>(null) }

    var showExclusionDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var restoreTargetService by remember { mutableStateOf<ServiceConfig?>(null) }

    var isGlobalSyncRunning by remember { mutableStateOf(false) }

    val queueItems by FileTransferQueueManager.items.collectAsStateWithLifecycle()
    val activeTransfers = queueItems.filter { it.status == TransferStatus.IN_PROGRESS || it.status == TransferStatus.QUEUED }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isQueueSheetVisible by remember { mutableStateOf(false) }

    fun refreshState() {
        services = vault.getAllServices()
        exclusionFilter = vault.getExclusionFilter()
    }

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
                        IconButton(onClick = { isQueueSheetVisible = true }) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Transfer Queue"
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            editingService = null
                            showServiceDialog = true
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Cloud Service")
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
                    onSyncAll = {
                        isGlobalSyncRunning = true
                        coroutineScope.launch {
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
                    onOpenQueue = { isQueueSheetVisible = true }
                )
            }

            // 2. Configured Cloud Services Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Configured Services",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = {
                            editingService = null
                            showServiceDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Service")
                    }
                }
            }

            // 3. Service Cards
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
                    ServiceItemCard(
                        service = service,
                        onToggleEnabled = { enabled ->
                            val updated = service.copy(isEnabled = enabled)
                            vault.saveService(updated)
                            refreshState()
                        },
                        onEdit = {
                            editingService = service
                            showServiceDialog = true
                        },
                        onDelete = {
                            vault.deleteService(service.id)
                            refreshState()
                        },
                        onSyncNow = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Starting sync for ${service.name}...")
                                val result = engine.performBackup(service, concurrency = concurrencyWorkers)
                                refreshState()
                                if (result.isSuccess) {
                                    snackbarHostState.showSnackbar("${service.name}: Uploaded ${result.filesUploaded} files (${result.filesSkipped} unchanged)")
                                } else {
                                    snackbarHostState.showSnackbar("${service.name}: ${result.filesFailed} files failed")
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
                            showMappingDialog = true
                        },
                        onEditMapping = { mapping ->
                            mappingTargetServiceId = service.id
                            editingMapping = mapping
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

            // 4. Restore & Conflict Resolution Policy
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

            // 6. Automation & Constraints Card
            item {
                AutomationCard(
                    isAutoBackupOnExit = isAutoBackupOnExit,
                    onAutoBackupOnExitChange = {
                        isAutoBackupOnExit = it
                        backupPrefs.isAutoBackupOnExitEnabled = it
                    },
                    isDailyScheduledSync = isDailyScheduledSync,
                    onDailyScheduledSyncChange = {
                        isDailyScheduledSync = it
                        backupPrefs.isDailyScheduledSyncEnabled = it
                        BackupScheduler.updateSchedules(context)
                    },
                    dailyHour = dailyHour,
                    dailyMinute = dailyMinute,
                    onTimePickerClick = { showTimePickerDialog = true },
                    isWifiOnly = isWifiOnly,
                    onWifiOnlyChange = {
                        isWifiOnly = it
                        backupPrefs.isWifiOnlyEnabled = it
                        BackupScheduler.updateSchedules(context)
                    },
                    concurrency = concurrencyWorkers,
                    onConcurrencyChange = {
                        concurrencyWorkers = it
                        backupPrefs.concurrencyWorkers = it
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Dialogs & Sheets
    if (isQueueSheetVisible) {
        FileQueueSheet(
            sheetState = sheetState,
            onDismissRequest = { isQueueSheetVisible = false }
        )
    }

    if (showServiceDialog) {
        ServiceConfigDialog(
            initialService = editingService,
            onDismissRequest = { showServiceDialog = false },
            onSaveService = { savedService ->
                vault.saveService(savedService)
                refreshState()
            }
        )
    }

    if (showMappingDialog && mappingTargetServiceId != null) {
        CustomMappingDialog(
            serviceId = mappingTargetServiceId!!,
            initialMapping = editingMapping,
            onDismissRequest = { showMappingDialog = false },
            onSaveMapping = { mapping ->
                val targetService = services.firstOrNull { it.id == mappingTargetServiceId }
                if (targetService != null) {
                    val currentMappings = targetService.customMappings.toMutableList()
                    val idx = currentMappings.indexOfFirst { it.id == mapping.id }
                    if (idx >= 0) currentMappings[idx] = mapping else currentMappings.add(mapping)
                    vault.saveService(targetService.copy(customMappings = currentMappings))
                    refreshState()
                }
            }
        )
    }

    if (showExclusionDialog) {
        ExclusionFilterDialog(
            initialFilter = exclusionFilter,
            onDismissRequest = { showExclusionDialog = false },
            onSaveFilter = { updated ->
                exclusionFilter = updated
                vault.saveExclusionFilter(updated)
                refreshState()
            }
        )
    }

    if (showRestoreConfirmDialog && restoreTargetService != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("Restore from ${restoreTargetService!!.name}?") },
            text = {
                Text(
                    "This will download remote notes and configuration from ${restoreTargetService!!.name} to your local storage using '${selectedConflictPolicy.displayName}'."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        val target = restoreTargetService!!
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Starting cloud restore from ${target.name}...")
                            val result = engine.performRestore(
                                serviceConfig = target,
                                conflictPolicy = selectedConflictPolicy,
                                concurrency = concurrencyWorkers
                            )
                            if (result.isSuccess) {
                                snackbarHostState.showSnackbar("Restore complete: ${result.filesRestored} files restored (${result.filesSkipped} skipped)")
                            } else {
                                snackbarHostState.showSnackbar("Restore failed: ${result.filesFailed} files failed")
                            }
                        }
                    }
                ) {
                    Text("Start Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = dailyHour,
            initialMinute = dailyMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = { Text("Set Daily Sync Time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                Button(
                    onClick = {
                        dailyHour = timePickerState.hour
                        dailyMinute = timePickerState.minute
                        backupPrefs.dailyScheduledHour = timePickerState.hour
                        backupPrefs.dailyScheduledMinute = timePickerState.minute
                        BackupScheduler.updateSchedules(context)
                        showTimePickerDialog = false
                    }
                ) {
                    Text("Save Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun OverviewCard(
    services: List<ServiceConfig>,
    isSyncRunning: Boolean,
    onSyncAll: () -> Unit,
    onOpenQueue: () -> Unit
) {
    val enabledCount = services.count { it.isEnabled }
    val lastSyncEpoch = services.maxOfOrNull { it.lastSyncedAtEpochMs } ?: 0L
    val lastSyncFormatted = if (lastSyncEpoch > 0) {
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(lastSyncEpoch))
    } else {
        "Never"
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Cloud Status",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (enabledCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "$enabledCount Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (enabledCount > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Last synced: $lastSyncFormatted",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSyncAll,
                    enabled = enabledCount > 0 && !isSyncRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSyncRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Sync All Now")
                }

                OutlinedButton(
                    onClick = onOpenQueue,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Queue")
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Cloud Storage Configured",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Connect Nextcloud, WebDAV, Google Drive, SFTP, SMB3, or FTP to backup and restore notes seamlessly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddService) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Cloud Service")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServiceItemCard(
    service: ServiceConfig,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSyncNow: () -> Unit,
    onRestore: () -> Unit,
    onAddMapping: () -> Unit,
    onEditMapping: (CustomFolderMapping) -> Unit,
    onDeleteMapping: (String) -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = if (service.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
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
                        text = "${service.providerType.displayName} • ${if (service.serverUrl.isNotBlank()) service.serverUrl else "${service.host}:${service.port}"}",
                        style = MaterialTheme.typography.bodySmall,
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
                    IconButton(onClick = { isMenuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Service") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = {
                                isMenuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Folder Mapping") },
                            leadingIcon = { Icon(Icons.Default.Folder, null) },
                            onClick = {
                                isMenuExpanded = false
                                onAddMapping()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Service") },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                isMenuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (service.isCompleteBackupEnabled) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Complete Backup (~/Notes & ~/.config)") },
                        icon = { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                    )
                }
                if (service.customMappings.isNotEmpty()) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("${service.customMappings.size} Custom Mapping(s)") },
                        icon = { Icon(Icons.Default.Folder, null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }

            // Custom Mappings List
            if (service.customMappings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    service.customMappings.forEach { mapping ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${mapping.localFolderPath} ➔ ${mapping.remoteFolderPath}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onDeleteMapping(mapping.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
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

@Composable
fun AutomationCard(
    isAutoBackupOnExit: Boolean,
    onAutoBackupOnExitChange: (Boolean) -> Unit,
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
