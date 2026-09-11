package dev.ilamparithi.aournalpp.ui.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.data.AppPreferences
import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.ui.AppDialogDefaults
import dev.ilamparithi.aournalpp.ui.BootstrapState
import dev.ilamparithi.aournalpp.ui.ExpressiveHeroSpinner
import dev.ilamparithi.aournalpp.ui.promptWidth
import dev.ilamparithi.aournalpp.ui.theme.CloverShape
import dev.ilamparithi.aournalpp.ui.theme.SunnyShape
import dev.ilamparithi.aournalpp.utils.FormatUtils
import dev.ilamparithi.aournalpp.utils.a11yHeading
import dev.ilamparithi.aournalpp.backup.engine.BackupEngine
import dev.ilamparithi.aournalpp.backup.model.ConfigSyncStatus
import dev.ilamparithi.aournalpp.backup.model.ConflictResolutionPolicy
import dev.ilamparithi.aournalpp.backup.model.FileConflictGroup
import dev.ilamparithi.aournalpp.backup.model.FileVersionItem
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.security.CredentialsVault
import dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager
import dev.ilamparithi.aournalpp.ui.cloud.ConfigDiffActivity
import dev.ilamparithi.aournalpp.ui.cloud.ConflictDialogMode
import dev.ilamparithi.aournalpp.ui.cloud.FolderBrowserDialog
import dev.ilamparithi.aournalpp.ui.cloud.FolderBrowserMode
import dev.ilamparithi.aournalpp.ui.cloud.MultiServiceConflictDialog
import dev.ilamparithi.aournalpp.ui.cloud.ServiceConfigDialog
import dev.ilamparithi.aournalpp.ui.cloud.CloudProviderIcon
import dev.ilamparithi.aournalpp.ui.InteractiveMarqueeText
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingChooseFolderPage(
    env: LinuxEnvironment,
    onContinue: () -> Unit,
    onRestoreLocal: (File) -> Unit,
    onRestoreCloud: (ServiceConfig, String, File, Boolean, ConflictResolutionPolicy) -> Unit
) {
    val context = LocalContext.current
    val vault = remember { CredentialsVault(context) }
    val backupEngine = remember { BackupEngine(context, env, vault) }
    val scope = rememberCoroutineScope()

    var selectedPath by remember { mutableStateOf(env.getNotesDirectory().absolutePath) }
    val isCompatible = remember(selectedPath) {
        NotesHomeConfigManager.isAournalCompatible(File(selectedPath))
    }

    // Cloud Restore Dialog & State variables
    var showServiceConfigDialog by remember { mutableStateOf(false) }
    var showServiceSelectionDialog by remember { mutableStateOf(false) }
    var showNoCompleteSyncDialog by remember { mutableStateOf(false) }
    var showFolderBrowserDialog by remember { mutableStateOf(false) }
    var showConflictDialog by remember { mutableStateOf(false) }
    var isCheckingCloud by remember { mutableStateOf(false) }
    var isResolvingCloudConflict by remember { mutableStateOf(false) }

    var selectedCloudService by remember { mutableStateOf<ServiceConfig?>(null) }
    var currentRemotePath by remember { mutableStateOf(BackupEngine.COMPLETE_BACKUP_REMOTE_ROOT) }
    var configuredServices by remember { mutableStateOf(vault.getAllServices()) }

    var detectedConfigConflicts by remember { mutableStateOf<List<FileConflictGroup>>(emptyList()) }
    var rememberedConfigSelections by remember { mutableStateOf<Map<String, Set<FileVersionItem>>>(emptyMap()) }

    fun checkCloudCompleteSync(service: ServiceConfig, remotePath: String) {
        selectedCloudService = service
        currentRemotePath = remotePath
        isCheckingCloud = true
        scope.launch {
            val result = backupEngine.checkRemoteCompleteSync(service, remotePath)
            isCheckingCloud = false
            if (result.isSuccess && result.getOrNull() == true) {
                // Complete sync found in remote folder! Check for config file conflicts
                val localFolder = File(selectedPath)
                val conflicts = backupEngine.detectConfigConflicts(service, remotePath, localFolder)
                if (conflicts.isNotEmpty()) {
                    detectedConfigConflicts = conflicts
                    showConflictDialog = true
                } else {
                    // No conflicts or 0 diff changes: download and restore from cloud
                    onRestoreCloud(service, remotePath, localFolder, false, ConflictResolutionPolicy.OVERWRITE_LOCAL)
                }
            } else {
                // No complete sync found in this remote folder
                showNoCompleteSyncDialog = true
            }
        }
    }

    fun handleRestoreFromCloudClick() {
        val services = vault.getAllServices()
        configuredServices = services
        if (services.isEmpty()) {
            showServiceConfigDialog = true
        } else if (services.size == 1) {
            val srv = services.first()
            selectedCloudService = srv
            val defaultPath = BackupEngine.getCompleteBackupRemoteRoot(srv)
            checkCloudCompleteSync(srv, defaultPath)
        } else {
            showServiceSelectionDialog = true
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val rawPath = uri.path ?: ""
            val resolved = if (rawPath.contains("primary:")) {
                val rel = rawPath.substringAfter("primary:").trim('/')
                File(Environment.getExternalStorageDirectory(), rel).absolutePath
            } else {
                rawPath
            }
            env.setNotesDirectory(resolved)
            selectedPath = resolved
            Toast.makeText(
                context,
                context.getString(dev.ilamparithi.aournalpp.R.string.msg_notes_folder_set, resolved),
                Toast.LENGTH_SHORT
            ).show()

            if (isResolvingCloudConflict && selectedCloudService != null) {
                isResolvingCloudConflict = false
                checkCloudCompleteSync(selectedCloudService!!, currentRemotePath)
            }
        } else {
            isResolvingCloudConflict = false
        }
    }

    val defaultNotesPath = remember {
        File(Environment.getExternalStorageDirectory(), "Documents/Notes").absolutePath
    }
    val defaultXournalPath = remember {
        File(Environment.getExternalStorageDirectory(), "Documents/Xournal").absolutePath
    }
    val defaultDownloadPath = remember {
        File(Environment.getExternalStorageDirectory(), "Download").absolutePath
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.onboarding_folder_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.a11yHeading()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.onboarding_folder_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Compatible Workspace Detected Card
        if (isCompatible) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) { role = Role.Button }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsBackupRestore,
                        contentDescription = androidx.compose.ui.res.stringResource(R.string.cd_folder_compatible_badge),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.title_onboarding_folder_compatible),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.a11yHeading()
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.desc_onboarding_folder_compatible),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Current Folder Card
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.onboarding_folder_active_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedPath,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Button(
                    onClick = { folderPickerLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_browse_other_folder))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preset Chips
        Text(
            text = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.onboarding_folder_preset_header),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedPath == defaultNotesPath,
                onClick = {
                    env.setNotesDirectory(defaultNotesPath)
                    selectedPath = defaultNotesPath
                },
                label = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.onboarding_folder_preset_default)) }
            )
            FilterChip(
                selected = selectedPath == defaultXournalPath,
                onClick = {
                    env.setNotesDirectory(defaultXournalPath)
                    selectedPath = defaultXournalPath
                },
                label = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.onboarding_folder_preset_xournal)) }
            )
            FilterChip(
                selected = selectedPath == defaultDownloadPath,
                onClick = {
                    env.setNotesDirectory(defaultDownloadPath)
                    selectedPath = defaultDownloadPath
                },
                label = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.onboarding_folder_preset_download)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Restore from Cloud Action Button
        OutlinedButton(
            onClick = { handleRestoreFromCloudClick() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = !isCheckingCloud
        ) {
            if (isCheckingCloud) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.action_restore_from_cloud),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Primary Action: Restore & Get Started (if compatible) OR Continue (if normal)
        Button(
            onClick = {
                if (isCompatible) {
                    onRestoreLocal(File(selectedPath))
                } else {
                    onContinue()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isCompatible) androidx.compose.ui.res.stringResource(R.string.action_restore_and_finish)
                       else androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_continue),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (isCompatible) Icons.Default.SettingsBackupRestore
                              else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    // Dialog 1: Service Selection Dialog (when multiple services exist)
    if (showServiceSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showServiceSelectionDialog = false },
            properties = AppDialogDefaults.Properties,
            title = {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.action_restore_from_cloud),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.a11yHeading()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    configuredServices.forEach { service ->
                        OutlinedCard(
                            onClick = {
                                showServiceSelectionDialog = false
                                selectedCloudService = service
                                val path = BackupEngine.getCompleteBackupRemoteRoot(service)
                                checkCloudCompleteSync(service, path)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = service.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = service.providerType.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showServiceSelectionDialog = false
                        showServiceConfigDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(androidx.compose.ui.res.stringResource(R.string.action_connect_cloud))
                }
            },
            dismissButton = {
                TextButton(onClick = { showServiceSelectionDialog = false }) {
                    Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel))
                }
            }
        )
    }

    // Dialog 2: Service Config Dialog (add new service)
    if (showServiceConfigDialog) {
        ServiceConfigDialog(
            initialService = null,
            existingServices = configuredServices,
            onDismissRequest = { showServiceConfigDialog = false },
            onSaveService = { service ->
                vault.saveService(service)
                configuredServices = vault.getAllServices()
                showServiceConfigDialog = false
                selectedCloudService = service
                val defaultPath = BackupEngine.getCompleteBackupRemoteRoot(service)
                checkCloudCompleteSync(service, defaultPath)
            }
        )
    }

    // Dialog 3: No Complete Sync Found Dialog
    if (showNoCompleteSyncDialog && selectedCloudService != null) {
        AlertDialog(
            onDismissRequest = { showNoCompleteSyncDialog = false },
            properties = AppDialogDefaults.Properties,
            title = {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.title_dialog_no_complete_sync),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.a11yHeading()
                )
            },
            text = {
                Text(
                    androidx.compose.ui.res.stringResource(
                        R.string.dialog_no_complete_sync_desc,
                        currentRemotePath,
                        selectedCloudService!!.name
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNoCompleteSyncDialog = false
                        showFolderBrowserDialog = true
                    }
                ) {
                    Text(androidx.compose.ui.res.stringResource(R.string.action_choose_remote_folder))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showNoCompleteSyncDialog = false
                            // Continue anyway: exits cloud restore loop, returns to local folder selection
                            // Saved cloud services remain in vault!
                        }
                    ) {
                        Text(androidx.compose.ui.res.stringResource(R.string.action_continue_anyway))
                    }
                    TextButton(
                        onClick = {
                            showNoCompleteSyncDialog = false
                            val currentList = vault.getAllServices()
                            configuredServices = currentList
                            if (currentList.size > 1) {
                                showServiceSelectionDialog = true
                            } else {
                                showServiceConfigDialog = true
                            }
                        }
                    ) {
                        Text(androidx.compose.ui.res.stringResource(R.string.action_select_diff_service))
                    }
                }
            }
        )
    }

    // Dialog 4: Remote Folder Browser Dialog
    if (showFolderBrowserDialog && selectedCloudService != null) {
        FolderBrowserDialog(
            mode = FolderBrowserMode.REMOTE,
            title = androidx.compose.ui.res.stringResource(R.string.action_choose_remote_folder),
            serviceConfig = selectedCloudService,
            onFolderSelected = { pickedRemotePath ->
                showFolderBrowserDialog = false
                currentRemotePath = pickedRemotePath
                val updated = selectedCloudService!!.copy(remoteBasePath = pickedRemotePath)
                vault.saveService(updated)
                selectedCloudService = updated
                checkCloudCompleteSync(updated, pickedRemotePath)
            },
            onDismissRequest = { showFolderBrowserDialog = false }
        )
    }

    // Dialog 5: Unified Config Conflict Dialog
    if (showConflictDialog && selectedCloudService != null && detectedConfigConflicts.isNotEmpty()) {
        MultiServiceConflictDialog(
            conflictGroups = detectedConfigConflicts,
            initialSelections = rememberedConfigSelections,
            mode = ConflictDialogMode.CONFIG_CONFLICT,
            onDismissRequest = { showConflictDialog = false },
            onApplyConfigResolutions = { resolutions ->
                showConflictDialog = false
                scope.launch {
                    val localFolder = File(selectedPath)
                    backupEngine.applyConfigResolutions(resolutions, selectedCloudService!!, localFolder)
                    onRestoreCloud(selectedCloudService!!, currentRemotePath, localFolder, false, ConflictResolutionPolicy.KEEP_NEWER)
                }
            },
            onPreviewDiff = { group ->
                context.startActivity(
                    ConfigDiffActivity.createIntent(
                        context = context,
                        fileName = group.fileName,
                        localPath = group.localFilePath ?: "",
                        remotePath = group.remoteFilePath ?: ""
                    )
                )
            },
            secondaryActionButton = {
                TextButton(
                    onClick = {
                        isResolvingCloudConflict = true
                        folderPickerLauncher.launch(null)
                    }
                ) {
                    Text(androidx.compose.ui.res.stringResource(R.string.action_choose_diff_local_folder))
                }
            }
        )
    }
}

