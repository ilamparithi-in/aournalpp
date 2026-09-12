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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderCopy
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
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
import dev.ilamparithi.aournalpp.ui.preview.DragActionTarget
import dev.ilamparithi.aournalpp.ui.preview.FloatingPreviewHost
import dev.ilamparithi.aournalpp.ui.theme.AournalTheme
import dev.ilamparithi.aournalpp.ui.theme.ExpressiveSprings
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
    private val externalFileToOpen = androidx.compose.runtime.mutableStateOf<File?>(null)

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

                // Intercept back presses while bootstrap/update/extraction is active
                BackHandler(enabled = state !is BootstrapState.Ready) {
                    // No-op: Prevent dismissal during checking, update prompt, or extraction
                }

                var hasBootstrapRevealed by rememberSaveable { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (state is BootstrapState.Ready) {
                        LaunchedEffect(state) {
                            pendingIntentToProcess?.let { intentToHandle ->
                                handleExternalIntent(intentToHandle)
                                pendingIntentToProcess = null
                            }
                        }
                        // Provision the runtime tree once the bootstrap is ready, onboarding completed, and reveal animation completes.
                        LaunchedEffect(isOnboardingCompleted) {
                            if (!isOnboardingCompleted) return@LaunchedEffect
                            withContext(Dispatchers.IO) {
                                delay(850.milliseconds)
                                LinuxEnvironment(this@MainActivity).ensureDirectoryTree()
                                val backupPrefs = BackupPreferences(this@MainActivity)
                                if (backupPrefs.isCheckRemoteChangesOnLaunchEnabled) {
                                    val engine = BackupEngine(this@MainActivity)
                                    val remoteChanges = engine.checkAllServicesForRemoteChanges()
                                    if (remoteChanges.isNotEmpty()) {
                                        val serviceNames = remoteChanges.keys.joinToString(", ")
                                        Log.i("MainActivity", "Remote changes detected in cloud service(s): $serviceNames")
                                    }
                                }
                            }
                        }

                        // In-App Periodic Sync while app is actively running
                        LaunchedEffect(isOnboardingCompleted) {
                            if (!isOnboardingCompleted) return@LaunchedEffect
                            val backupPrefs = BackupPreferences(this@MainActivity)
                            while (isActive) {
                                val intervalMins = backupPrefs.periodicSyncIntervalMinutes
                                if (intervalMins > 0) {
                                    delay(intervalMins.minutes)
                                    withContext(Dispatchers.IO) {
                                        try {
                                            val engine = BackupEngine(this@MainActivity)
                                            engine.performMultiServiceBackup()
                                        } catch (e: Exception) {
                                            Log.w("MainActivity", "In-app periodic sync failed", e)
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
                                        NoteOpenManager.openInCanvas(
                                            context = this@MainActivity,
                                            file = note.file,
                                            repository = repo
                                        )
                                    }
                                    DragActionTarget.NONE -> {}
                                }
                            }
                        ) {
                            MainResponsiveAppShell()
                        }

                        val promptFile = externalFileToOpen.value
                        if (promptFile != null && isOnboardingCompleted) {
                            NoteOpenActionDialog(
                                file = promptFile,
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
                                    NoteOpenManager.openInCanvas(
                                        context = this@MainActivity,
                                        file = promptFile,
                                        repository = repo
                                    )
                                }
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
                            onFinish = { viewModel.completeOnboarding() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleExternalIntent(intent)
    }

    private fun handleExternalIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        val action = intent.action

        // 1. Google OAuth2 Redirect Handler
        val isOAuthRedirect = (uri.scheme == "dev.ilamparithi.aournalpp" ||
                uri.scheme?.startsWith("com.googleusercontent.apps.") == true) &&
                (uri.host == "oauth2redirect" || uri.path?.contains("oauth2redirect") == true)
        if (isOAuthRedirect) {
            lifecycleScope.launch {
                try {
                    val result = dev.ilamparithi.aournalpp.backup.security.GoogleOAuthManager.handleRedirectUri(uri)
                    if (result.isSuccess) {
                        val tokenResponse = result.getOrThrow()
                        val vault = dev.ilamparithi.aournalpp.backup.security.CredentialsVault.getInstance(this@MainActivity)
                        val existingGdrive = vault.getAllServices().firstOrNull { it.providerType == dev.ilamparithi.aournalpp.backup.model.StorageProviderType.GOOGLE_DRIVE }
                        if (existingGdrive != null) {
                            val updated = existingGdrive.copy(
                                authToken = tokenResponse.accessToken,
                                refreshToken = tokenResponse.refreshToken ?: existingGdrive.refreshToken,
                                accountIdentifier = tokenResponse.userEmail ?: existingGdrive.accountIdentifier,
                                tokenExpiryEpochMs = System.currentTimeMillis() + (tokenResponse.expiresInSeconds * 1000L),
                                lastSyncStatus = "Connected"
                            )
                            vault.saveService(updated)
                        }
                        Log.i(TAG, "Successfully authenticated Google Drive for ${tokenResponse.userEmail}")
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                this@MainActivity,
                                "Google Drive connected: ${tokenResponse.userEmail ?: "Authorized"}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Log.e(TAG, "Google OAuth token exchange failed: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling Google OAuth redirect", e)
                }
            }
            return
        }

        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_EDIT) return

        lifecycleScope.launch {
            try {
                Log.i(TAG, "Handling external file intent: $uri (action=$action)")
                val env = LinuxEnvironment(this@MainActivity)
                val result = ExternalFileHandler.stageExternalUri(this@MainActivity, uri, env)
                if (result.isSuccess) {
                    val file = result.getOrThrow()
                    val supervisor = ProcessSupervisor(env)
                    val pdfExportManager = PdfExportManager(env, supervisor)
                    val repo = DocumentRepository.getInstance(this@MainActivity)

                    NoteOpenManager.handleFileOpen(
                        context = this@MainActivity,
                        file = file,
                        pdfExportManager = pdfExportManager,
                        scope = lifecycleScope,
                        repository = repo,
                        onShowPrompt = { externalFileToOpen.value = it }
                    )
                } else {
                    Log.e(TAG, "Failed to stage external file URI: $uri", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception handling external intent", e)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        BackupScheduler.triggerOnAppExitSync(this)
    }
}

enum class AppTab(
    @param:StringRes val titleRes: Int,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    HOME(R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home),
    FILES(R.string.tab_files, Icons.Filled.FolderCopy, Icons.Outlined.FolderCopy),
    CLOUD(R.string.tab_cloud, Icons.Filled.Cloud, Icons.Outlined.Cloud),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
    ABOUT(R.string.tab_about, Icons.Filled.Info, Icons.Outlined.Info);

    val id: Int get() = ordinal
}

@Composable
fun MainResponsiveAppShell() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val reduceAnimations = dev.ilamparithi.aournalpp.ui.animation.LocalMotionPreferences.current.reduceAnimations

    var selectedTab by rememberSaveable { mutableIntStateOf(AppTab.HOME.id) }
    val saveableStateHolder = rememberSaveableStateHolder()
    var tabGenerations by rememberSaveable { mutableStateOf(mapOf<Int, Int>()) }

    var lastClickTime by remember { mutableLongStateOf(0L) }
    var lastClickedTab by remember { mutableIntStateOf(-1) }
    var lastTabSwitchTime by remember { mutableLongStateOf(0L) }
    var isRapidSwitch by remember { mutableStateOf(false) }

    val onTabSelect: (Int) -> Unit = { tabId ->
        val currentTime = System.currentTimeMillis()
        val isDoubleTap = (lastClickedTab == tabId) && (currentTime - lastClickTime < 400L)

        if (selectedTab != tabId) {
            val delta = currentTime - lastTabSwitchTime
            isRapidSwitch = lastTabSwitchTime > 0L && delta < 320L
            lastTabSwitchTime = currentTime
            selectedTab = tabId
        }

        if (isDoubleTap) {
            val oldGen = tabGenerations[tabId] ?: 0
            val newGen = oldGen + 1
            tabGenerations = tabGenerations + (tabId to newGen)
            saveableStateHolder.removeState("tab_${tabId}_$oldGen")
            lastClickTime = 0L
        } else {
            lastClickTime = currentTime
            lastClickedTab = tabId
        }
    }

    val isCanvasSessionActive by dev.ilamparithi.aournalpp.runtime.ActiveSessionTracker.activeSessionFlow(context)
        .collectAsStateWithLifecycle(initialValue = null)

    var isClosingSession by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isClosingSession) {
        if (isClosingSession) {
            val activity = context as? android.app.Activity
            while (isClosingSession) {
                if (!dev.ilamparithi.aournalpp.runtime.ActiveSessionTracker.isSessionActive(context)) {
                    isClosingSession = false
                    activity?.finishAffinity() ?: activity?.finish()
                    break
                }
                delay(50.milliseconds)
            }
        }
    }

    androidx.compose.runtime.DisposableEffect(isClosingSession) {
        if (!isClosingSession) return@DisposableEffect onDispose {}
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == "dev.ilamparithi.aournalpp.ACTION_SESSION_CLOSED") {
                    isClosingSession = false
                    val activity = context as? android.app.Activity
                    activity?.finishAffinity() ?: activity?.finish()
                }
            }
        }
        val filter = android.content.IntentFilter("dev.ilamparithi.aournalpp.ACTION_SESSION_CLOSED")
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    BackHandler(enabled = selectedTab != AppTab.HOME.id) {
        onTabSelect(AppTab.HOME.id)
    }

    BackHandler(enabled = selectedTab == AppTab.HOME.id && isCanvasSessionActive?.isRunning == true && !isClosingSession) {
        isClosingSession = true
        val broadcastIntent = Intent(CanvasCommandReceiver.ACTION_REQUEST_BACKGROUND_CLOSE).apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(broadcastIntent)
        CanvasActivity.handleBackgroundCloseRequest()
    }

    val tabTransitionSpec: androidx.compose.animation.AnimatedContentTransitionScope<Int>.() -> androidx.compose.animation.ContentTransform = {
        val isForward = targetState > initialState
        SpringSlideTransition.createSpec<Int>(
            isForward = isForward,
            reduceAnimations = reduceAnimations,
            isRapid = isRapidSwitch
        )(this)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            // Tablet / Landscape: Navigation Rail on Left
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        dev.ilamparithi.aournalpp.ui.AppLogoBadge(
                            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
                            size = 38.dp
                        )
                    }
                ) {
                    AppTab.entries.forEach { tab ->
                        val tabTitle = androidx.compose.ui.res.stringResource(tab.titleRes)
                        NavigationRailItem(
                            selected = selectedTab == tab.id,
                            onClick = { onTabSelect(tab.id) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == tab.id) tab.filledIcon else tab.outlinedIcon,
                                    contentDescription = tabTitle
                                )
                            },
                            label = {
                                Text(
                                    text = tabTitle,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (selectedTab == tab.id) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = tabTransitionSpec,
                        label = "railTabTransition"
                    ) { tabId ->
                        TabHost(
                            tabId = tabId,
                            tabGenerations = tabGenerations,
                            saveableStateHolder = saveableStateHolder,
                            onTabSelect = onTabSelect
                        )
                    }
                }
            }
        } else {
            // Mobile Portrait: Bottom Navigation Bar
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        AppTab.entries.forEach { tab ->
                            val tabTitle = androidx.compose.ui.res.stringResource(tab.titleRes)
                            NavigationBarItem(
                                selected = selectedTab == tab.id,
                                onClick = { onTabSelect(tab.id) },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == tab.id) tab.filledIcon else tab.outlinedIcon,
                                        contentDescription = tabTitle
                                    )
                                },
                                label = {
                                    Text(
                                        text = tabTitle,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (selectedTab == tab.id) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = tabTransitionSpec,
                        label = "bottomTabTransition"
                    ) { tabId ->
                        TabHost(
                            tabId = tabId,
                            tabGenerations = tabGenerations,
                            saveableStateHolder = saveableStateHolder,
                            onTabSelect = onTabSelect
                        )
                    }
                }
            }
        }
    }

    AppAnimatedVisibility(
        visible = isClosingSession,
        enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)),
        exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300))
    ) {
        dev.ilamparithi.aournalpp.ui.SessionClosingScreen(
            documentTitle = isCanvasSessionActive?.documentTitle
        )
    }
}

@Composable
private fun TabHost(
    tabId: Int,
    tabGenerations: Map<Int, Int>,
    saveableStateHolder: SaveableStateHolder,
    onTabSelect: (Int) -> Unit
) {
    val gen = tabGenerations[tabId] ?: 0
    val pageKey = "tab_${tabId}_$gen"
    saveableStateHolder.SaveableStateProvider(key = pageKey) {
        key(pageKey) {
            RenderTabContent(tab = tabId, onTabSelect = onTabSelect)
        }
    }
}

@Composable
private fun RenderTabContent(
    tab: Int,
    onTabSelect: (Int) -> Unit
) {
    when (tab) {
        AppTab.HOME.id -> HomeScreen(
            onNavigateToFiles = { onTabSelect(AppTab.FILES.id) },
            onNavigateToSettings = { onTabSelect(AppTab.SETTINGS.id) },
            onNavigateToAbout = { onTabSelect(AppTab.ABOUT.id) }
        )
        AppTab.FILES.id -> DocumentHubScreen(
            onNavigateToSettings = { onTabSelect(AppTab.SETTINGS.id) },
            onNavigateToLicenses = { onTabSelect(AppTab.ABOUT.id) }
        )
        AppTab.CLOUD.id -> CloudScreen(
            onNavigateToSettings = { onTabSelect(AppTab.SETTINGS.id) }
        )
        AppTab.SETTINGS.id -> SettingsScreen(onBack = { onTabSelect(AppTab.HOME.id) })
        AppTab.ABOUT.id -> LicensesScreen(onBack = { onTabSelect(AppTab.HOME.id) })
        else -> HomeScreen(
            onNavigateToFiles = { onTabSelect(AppTab.FILES.id) },
            onNavigateToSettings = { onTabSelect(AppTab.SETTINGS.id) },
            onNavigateToAbout = { onTabSelect(AppTab.ABOUT.id) }
        )
    }
}

