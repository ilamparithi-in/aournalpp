package dev.ilamparithi.aournalpp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.ilamparithi.aournalpp.backup.security.CredentialsVault
import dev.ilamparithi.aournalpp.ui.animation.AppAnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.ui.res.stringResource
import dev.ilamparithi.aournalpp.ui.promptWidth
import dev.ilamparithi.aournalpp.ui.cloud.CloudSubpage
import dev.ilamparithi.aournalpp.ui.onboarding.checkStoragePermissionGranted
import dev.ilamparithi.aournalpp.ui.onboarding.launchStoragePermissionSettings
import dev.ilamparithi.aournalpp.ui.workspace.ActiveWorkspacePortalScreen
import dev.ilamparithi.aournalpp.backup.engine.ConflictPersistenceManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import dev.ilamparithi.aournalpp.ui.shell.MainResponsiveAppShell
import dev.ilamparithi.aournalpp.ui.shell.NavigationTabIcon
import dev.ilamparithi.aournalpp.ui.shell.NavigationTabLabel
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.utils.FileTypeDetector
import dev.ilamparithi.aournalpp.ui.BootstrapScreen
import dev.ilamparithi.aournalpp.ui.BootstrapState
import dev.ilamparithi.aournalpp.ui.BootstrapViewModel
import dev.ilamparithi.aournalpp.ui.DocumentHubScreen
import dev.ilamparithi.aournalpp.ui.EnvironmentUpdateDialog
import dev.ilamparithi.aournalpp.ui.HomeScreen
import dev.ilamparithi.aournalpp.ui.LicensesScreen
import dev.ilamparithi.aournalpp.ui.OnboardingScreen
import dev.ilamparithi.aournalpp.ui.StoragePromptDialog
import dev.ilamparithi.aournalpp.ui.cloud.CloudScreen
import dev.ilamparithi.aournalpp.backup.worker.BackupScheduler
import dev.ilamparithi.aournalpp.SettingsScreen
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.utils.ExternalFileHandler
import dev.ilamparithi.aournalpp.utils.NoteOpenAction
import dev.ilamparithi.aournalpp.utils.NoteOpenManager
import dev.ilamparithi.aournalpp.ui.NoteOpenActionDialog
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.runtime.ActiveNotesTracker
import dev.ilamparithi.aournalpp.ui.ActiveNoteOpenPromptDialog
import dev.ilamparithi.aournalpp.ui.preview.DragActionTarget
import dev.ilamparithi.aournalpp.ui.preview.FloatingPreviewHost
import dev.ilamparithi.aournalpp.ui.theme.AournalTheme
import dev.ilamparithi.aournalpp.ui.theme.ExpressiveSprings
import dev.ilamparithi.aournalpp.ui.hub.dialog.EmergencyRecoveryDialog
import dev.ilamparithi.aournalpp.ui.dialog.EmergencySaveNameDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import dev.ilamparithi.aournalpp.ui.animation.SpringSlideTransition
import androidx.annotation.StringRes
import dev.ilamparithi.aournalpp.backup.engine.BackupEngine
import dev.ilamparithi.aournalpp.backup.worker.BackupPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private var pendingIntentToProcess: Intent? = null
    private val externalFileToOpen = androidx.compose.runtime.mutableStateOf<Pair<File, Boolean>?>(null)
    private val externalActiveNoteToPrompt = androidx.compose.runtime.mutableStateOf<Pair<File, ActiveNotesTracker.ActiveNoteMatch>?>(null)
    private val pendingTabNavigation = androidx.compose.runtime.mutableStateOf<Int?>(null)
    private val pendingCloudSubpageNavigation = androidx.compose.runtime.mutableStateOf<CloudSubpage?>(null)

    val emergencyManager: MainActivityEmergencyManager by lazy {
        MainActivityEmergencyManager(
            context = this,
            scope = lifecycleScope,
            onPromptFileOpen = { file, isImport ->
                externalFileToOpen.value = file to isImport
            },
            onReplayExternalIntent = { intent ->
                intentHandler.handleIntent(intent)
            }
        )
    }

    val intentHandler: MainActivityIntentHandler by lazy {
        MainActivityIntentHandler(
            context = this,
            scope = lifecycleScope,
            emergencyManager = emergencyManager,
            onNavigateTab = { tabId, subpage ->
                pendingTabNavigation.value = tabId
                pendingCloudSubpageNavigation.value = subpage
            },
            onShowPrompt = { file, isImport ->
                externalFileToOpen.value = file to isImport
            },
            onShowActiveNotePrompt = { targetFile, match ->
                externalActiveNoteToPrompt.value = targetFile to match
            }
        )
    }

    val quarantinedEmergencySave: androidx.compose.runtime.MutableState<File?> get() = emergencyManager.quarantinedEmergencySave
    val showEmergencyRecoveryDialog: androidx.compose.runtime.MutableState<Boolean> get() = emergencyManager.showEmergencyRecoveryDialog
    val showEmergencySaveNameDialog: androidx.compose.runtime.MutableState<Boolean> get() = emergencyManager.showEmergencySaveNameDialog
    var emergencySaveNameInput: String
        get() = emergencyManager.emergencySaveNameInput
        set(value) { emergencyManager.emergencySaveNameInput = value }
    var emergencySaveTargetFolder: File?
        get() = emergencyManager.emergencySaveTargetFolder
        set(value) { emergencyManager.emergencySaveTargetFolder = value }

    fun checkEmergencySave() = emergencyManager.checkEmergencySave()
    fun hasPendingEmergencySave(): Boolean = emergencyManager.hasPendingEmergencySave()
    fun deferNoteOpenForEmergencySave(file: File, isImport: Boolean = false) = emergencyManager.deferNoteOpenForEmergencySave(file, isImport)
    fun onEmergencySaveResolved() = emergencyManager.onEmergencySaveResolved()
    fun replayDeferredNoteOpen() = emergencyManager.replayDeferredNoteOpen()

    override fun onResume() {
        super.onResume()
        emergencyManager.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        BackupScheduler.updateSchedules(this)

        pendingIntentToProcess = intent

        setContent {
            AournalTheme {
                val viewModel: BootstrapViewModel = viewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
                val targetTab by pendingTabNavigation
                val targetCloudSubpage by pendingCloudSubpageNavigation

                var isStorageRevokedPostOnboarding by remember { mutableStateOf(false) }
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner, isOnboardingCompleted) {
                    if (!isOnboardingCompleted) return@DisposableEffect onDispose {}
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            isStorageRevokedPostOnboarding = !checkStoragePermissionGranted(this@MainActivity)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    isStorageRevokedPostOnboarding = !checkStoragePermissionGranted(this@MainActivity)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // Intercept back presses while bootstrap/update/extraction is active
                BackHandler(enabled = state !is BootstrapState.Ready) {
                    // No-op: Prevent dismissal during checking, update prompt, or extraction
                }

                var hasBootstrapRevealed by rememberSaveable { mutableStateOf(false) }
                var isRevealingOnboarding by rememberSaveable { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (state is BootstrapState.Ready && (isOnboardingCompleted || isRevealingOnboarding)) {
                        LaunchedEffect(state) {
                            pendingIntentToProcess?.let { intentToHandle ->
                                intentHandler.handleIntent(intentToHandle)
                                pendingIntentToProcess = null
                            }
                        }
                        // Provision the runtime tree once the bootstrap is ready, onboarding completed, and reveal animation completes.
                        LaunchedEffect(isOnboardingCompleted) {
                            if (!isOnboardingCompleted) return@LaunchedEffect
                            lifecycleScope.launch(Dispatchers.IO) {
                                delay(850.milliseconds)
                                LinuxEnvironment(this@MainActivity).ensureDirectoryTree()
                                val backupPrefs = BackupPreferences(this@MainActivity)
                                if (backupPrefs.isCheckRemoteChangesOnLaunchEnabled &&
                                    dev.ilamparithi.aournalpp.utils.NetworkUtils.isOnline(this@MainActivity)) {
                                    val engine = BackupEngine(this@MainActivity)
                                    val remoteChanges = engine.checkAllServicesForRemoteChanges()
                                    if (remoteChanges.isNotEmpty()) {
                                        val serviceNames = remoteChanges.keys.joinToString(", ")
                                        Log.i("MainActivity", "Remote changes detected in cloud service(s): $serviceNames")
                                        val conflicts = engine.detectMultiServiceConflicts()
                                        if (conflicts.isNotEmpty()) {
                                            ConflictPersistenceManager.getInstance(this@MainActivity).addConflicts(conflicts)
                                        }
                                    }
                                }
                            }
                        }

                        // In-App Periodic Sync while app is actively running
                        LaunchedEffect(isOnboardingCompleted) {
                            if (!isOnboardingCompleted) return@LaunchedEffect
                            val backupPrefs = BackupPreferences(this@MainActivity)
                            val engine = BackupEngine(this@MainActivity)
                            while (isActive) {
                                val intervalMins = backupPrefs.periodicSyncIntervalMinutes
                                if (intervalMins > 0) {
                                    delay(intervalMins.minutes)
                                    if (dev.ilamparithi.aournalpp.utils.NetworkUtils.isOnline(this@MainActivity)) {
                                        withContext(Dispatchers.IO) {
                                            try {
                                                val results = engine.performMultiServiceBackup()
                                                val allConflicts = results.flatMap { it.detectedConflicts }
                                                if (allConflicts.isNotEmpty()) {
                                                    ConflictPersistenceManager.getInstance(this@MainActivity).addConflicts(allConflicts)
                                                }
                                                val allErrors = results.flatMap { it.errors }
                                                if (allErrors.isNotEmpty()) {
                                                    Log.w("MainActivity", "In-app periodic sync had errors: ${allErrors.joinToString("; ")}")
                                                }
                                            } catch (e: Exception) {
                                                Log.w("MainActivity", "In-app periodic sync failed", e)
                                            }
                                        }
                                    }
                                } else {
                                    delay(60.seconds)
                                }
                            }
                        }
                        val env = remember { LinuxEnvironment(this@MainActivity) }
                        val supervisor = remember { ProcessSupervisor(env) }
                        val pdfExportManager = remember { PdfExportManager(env, supervisor) }
                        val repo = remember { DocumentRepository.getInstance(this@MainActivity) }

                        DisposableEffect(Unit) {
                            val receiver = object : android.content.BroadcastReceiver() {
                                override fun onReceive(c: Context?, intent: android.content.Intent?) {
                                    if (intent?.action == "dev.ilamparithi.aournalpp.ACTION_SESSION_CLOSED") {
                                        if (emergencyManager.isRecoveredSessionRunning) {
                                            emergencyManager.isRecoveredSessionRunning = false
                                            emergencyManager.replayDeferredNoteOpen()
                                        }
                                    }
                                }
                            }
                            val filter = android.content.IntentFilter("dev.ilamparithi.aournalpp.ACTION_SESSION_CLOSED")
                            androidx.core.content.ContextCompat.registerReceiver(
                                this@MainActivity,
                                receiver,
                                filter,
                                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
                            )
                            onDispose {
                                try {
                                    unregisterReceiver(receiver)
                                } catch (_: Exception) {}
                            }
                        }

                        FloatingPreviewHost(
                            onTriggerAction = { note, action ->
                                when (action) {
                                    DragActionTarget.VIEW_PDF -> {
                                        NoteOpenManager.openAsPdf(
                                            context = this@MainActivity,
                                            file = note.file,
                                            pdfExportManager = pdfExportManager,
                                            scope = lifecycleScope,
                                            repository = repo
                                        )
                                    }
                                    DragActionTarget.EDIT_CANVAS -> {
                                        NoteOpenManager.openInCanvasWithActiveCheck(
                                            context = this@MainActivity,
                                            file = note.file,
                                            repository = repo,
                                            onActiveSessionFound = { targetFile, match ->
                                                externalActiveNoteToPrompt.value = targetFile to match
                                            }
                                        )
                                    }
                                    DragActionTarget.NONE -> {}
                                }
                            }
                        ) {
                            MainResponsiveAppShell(
                                targetTab = targetTab,
                                targetCloudSubpage = targetCloudSubpage,
                                onNavigationHandled = {
                                    pendingTabNavigation.value = null
                                    pendingCloudSubpageNavigation.value = null
                                }
                            )
                        }

                        if (isOnboardingCompleted && isStorageRevokedPostOnboarding) {
                            AlertDialog(
                                onDismissRequest = { /* Non-dismissible */ },
                                properties = androidx.compose.ui.window.DialogProperties(
                                    dismissOnBackPress = false,
                                    dismissOnClickOutside = false
                                ),
                                modifier = Modifier.promptWidth(),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                title = {
                                    Text(stringResource(R.string.permission_storage_revoked_title))
                                },
                                text = {
                                    Text(stringResource(R.string.permission_storage_revoked_desc))
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            launchStoragePermissionSettings(this@MainActivity)
                                        }
                                    ) {
                                        Text(stringResource(R.string.action_open_settings))
                                    }
                                }
                            )
                        }

                        val promptInfo = externalFileToOpen.value
                        if (promptInfo != null && isOnboardingCompleted) {
                            val (promptFile, isImport) = promptInfo
                            NoteOpenActionDialog(
                                file = promptFile,
                                isImport = isImport,
                                onDismiss = { externalFileToOpen.value = null },
                                onViewAsPdf = {
                                    externalFileToOpen.value = null
                                    NoteOpenManager.openAsPdf(
                                        context = this@MainActivity,
                                        file = promptFile,
                                        pdfExportManager = pdfExportManager,
                                        scope = lifecycleScope,
                                        repository = repo
                                    )
                                },
                                onEditInCanvas = {
                                    externalFileToOpen.value = null
                                    NoteOpenManager.openInCanvasWithActiveCheck(
                                        context = this@MainActivity,
                                        file = promptFile,
                                        repository = repo,
                                        onActiveSessionFound = { targetFile, match ->
                                            externalActiveNoteToPrompt.value = targetFile to match
                                        }
                                    )
                                }
                            )
                        }

                        val activePrompt = externalActiveNoteToPrompt.value
                        if (activePrompt != null && isOnboardingCompleted) {
                            ActiveNoteOpenPromptDialog(
                                file = activePrompt.first,
                                activeMatch = activePrompt.second,
                                onDismiss = { externalActiveNoteToPrompt.value = null },
                                onViewExistingWindow = {
                                    val match = activePrompt.second
                                    externalActiveNoteToPrompt.value = null
                                    NoteOpenManager.viewExistingWindow(this@MainActivity, match.windowId)
                                },
                                onOpenInNewWindow = {
                                    val targetFile = activePrompt.first
                                    externalActiveNoteToPrompt.value = null
                                    NoteOpenManager.openInCanvas(
                                        context = this@MainActivity,
                                        file = targetFile,
                                        repository = repo
                                    )
                                }
                            )
                        }

                        val emergencyFile = quarantinedEmergencySave.value
                        if (showEmergencyRecoveryDialog.value && emergencyFile != null && isOnboardingCompleted) {
                            EmergencyRecoveryDialog(
                                emergencyFile = emergencyFile,
                                onDismiss = {
                                    showEmergencyRecoveryDialog.value = false
                                },
                                onOpenNow = {
                                    showEmergencyRecoveryDialog.value = false
                                    val staged = repo.openEmergencyRecoverySession(emergencyFile)
                                    emergencyManager.quarantinedEmergencySave.value = null
                                    emergencyManager.isRecoveredSessionRunning = true
                                    NoteOpenManager.openInCanvas(this@MainActivity, staged, repo)
                                },
                                onSaveAsNote = {
                                    showEmergencyRecoveryDialog.value = false
                                    val defaultName = "Recovered_Note_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(emergencyFile.lastModified()))
                                    emergencySaveNameInput = defaultName
                                    emergencySaveTargetFolder = repo.getRootNotesDirectory()
                                    showEmergencySaveNameDialog.value = true
                                },
                                onDiscard = {
                                    repo.discardEmergencyRecovery()
                                    onEmergencySaveResolved()
                                }
                            )
                        }

                        if (showEmergencySaveNameDialog.value && emergencyFile != null && isOnboardingCompleted) {
                            EmergencySaveNameDialog(
                                file = emergencyFile,
                                initialName = emergencySaveNameInput,
                                initialFolder = emergencySaveTargetFolder ?: repo.getRootNotesDirectory(),
                                repository = repo,
                                onDismiss = { showEmergencySaveNameDialog.value = false },
                                onSaveSuccess = { savedFile ->
                                    onEmergencySaveResolved()
                                    android.widget.Toast.makeText(this@MainActivity, "Saved recovered note as \"${savedFile.name}\"", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onFolderCreated = {}
                            )
                        }
                    }

                    when (state) {
                        is BootstrapState.UpdatePrompt -> {
                            val updateState = state as BootstrapState.UpdatePrompt
                            BootstrapScreen(
                                state = BootstrapState.Checking,
                                onRetry = { viewModel.retry() }
                            )
                            if (isOnboardingCompleted) {
                                EnvironmentUpdateDialog(
                                    installedVersion = updateState.installedVersion,
                                    newVersion = updateState.newVersion,
                                    countdownSeconds = updateState.countdownSeconds,
                                    diff = updateState.diff,
                                    onExpandDetails = { viewModel.cancelUpdateCountdown() },
                                    onUpdate = { viewModel.startInstallOrUpgrade() },
                                    onSkip = { viewModel.skipUpdateForCurrentSession() }
                                )
                            }
                        }
                        is BootstrapState.StorageWarning -> {
                            val warningState = state as BootstrapState.StorageWarning
                            BootstrapScreen(
                                state = BootstrapState.Checking,
                                onRetry = { viewModel.retry() }
                            )
                            StoragePromptDialog(
                                requiredBytes = warningState.requiredBytes,
                                availableBytes = warningState.availableBytes,
                                isInsufficient = warningState.isInsufficient,
                                missingBytes = warningState.missingBytes,
                                onContinue = { viewModel.proceedAfterStorageWarning() },
                                onClose = { finishAffinity() }
                            )
                        }
                        is BootstrapState.Ready -> {
                            if (isOnboardingCompleted && !hasBootstrapRevealed) {
                                BootstrapScreen(
                                    state = state,
                                    onRetry = { viewModel.retry() },
                                    isReady = true,
                                    onRevealFinished = { hasBootstrapRevealed = true }
                                )
                            }
                        }
                        else -> {
                            BootstrapScreen(
                                state = state,
                                onRetry = { viewModel.retry() }
                            )
                        }
                    }

                    if (!isOnboardingCompleted) {
                        OnboardingScreen(
                            bootstrapState = state,
                            onStartReveal = { isRevealingOnboarding = true },
                            onFinish = {
                                hasBootstrapRevealed = true
                                viewModel.completeOnboarding()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentHandler.handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        BackupScheduler.triggerOnAppExitSync(this)
    }
}

