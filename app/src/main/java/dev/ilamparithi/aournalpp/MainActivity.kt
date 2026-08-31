package dev.ilamparithi.aournalpp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

                var hasBootstrapRevealed by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (state is BootstrapState.Ready) {
                        LaunchedEffect(state) {
                            pendingIntentToProcess?.let { intentToHandle ->
                                handleExternalIntent(intentToHandle)
                                pendingIntentToProcess = null
                            }
                        }
                        // Provision the runtime tree once the bootstrap is ready.
                        LaunchedEffect(Unit) {
                            withContext(Dispatchers.IO) {
                                LinuxEnvironment(this@MainActivity).ensureDirectoryTree()
                                val backupPrefs = dev.ilamparithi.aournalpp.backup.worker.BackupPreferences(this@MainActivity)
                                if (backupPrefs.isCheckRemoteChangesOnLaunchEnabled) {
                                    val engine = dev.ilamparithi.aournalpp.backup.engine.BackupEngine(this@MainActivity)
                                    val remoteChanges = engine.checkAllServicesForRemoteChanges()
                                    if (remoteChanges.isNotEmpty()) {
                                        val serviceNames = remoteChanges.keys.joinToString(", ")
                                        Log.i("MainActivity", "Remote changes detected in cloud service(s): $serviceNames")
                                    }
                                }
                            }
                        }

                        // Fast In-App Periodic Sync (e.g. 5 min intervals while app is actively running)
                        LaunchedEffect(Unit) {
                            val backupPrefs = dev.ilamparithi.aournalpp.backup.worker.BackupPreferences(this@MainActivity)
                            val intervalMins = backupPrefs.periodicSyncIntervalMinutes
                            if (intervalMins in 1..14) {
                                while (true) {
                                    kotlinx.coroutines.delay(intervalMins * 60 * 1000L)
                                    withContext(Dispatchers.IO) {
                                        try {
                                            val engine = dev.ilamparithi.aournalpp.backup.engine.BackupEngine(this@MainActivity)
                                            engine.performMultiServiceBackup()
                                        } catch (e: Exception) {
                                            Log.w("MainActivity", "In-app periodic sync failed", e)
                                        }
                                    }
                                }
                            }
                        }
                        val env = remember { LinuxEnvironment(this@MainActivity) }
                        val supervisor = remember { ProcessSupervisor(env) }
                        val pdfExportManager = remember { PdfExportManager(env, supervisor) }
                        val repo = remember { DocumentRepository(this@MainActivity) }

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
                                    onUpdate = { viewModel.startInstallOrUpgrade() },
                                    onSkip = { viewModel.skipUpdateForCurrentSession() }
                                )
                            }
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
        if (uri.scheme == "dev.ilamparithi.aournalpp" && uri.host == "oauth2redirect") {
            lifecycleScope.launch {
                try {
                    val result = dev.ilamparithi.aournalpp.backup.security.GoogleOAuthManager.handleRedirectUri(uri)
                    if (result.isSuccess) {
                        val tokenResponse = result.getOrThrow()
                        val vault = dev.ilamparithi.aournalpp.backup.security.CredentialsVault(this@MainActivity)
                        val existingGdrive = vault.getAllServices().firstOrNull { it.providerType == dev.ilamparithi.aournalpp.backup.model.StorageProviderType.GOOGLE_DRIVE }
                        val serviceToSave = (existingGdrive ?: dev.ilamparithi.aournalpp.backup.model.ServiceConfig(
                            id = java.util.UUID.randomUUID().toString(),
                            name = "Google Drive",
                            providerType = dev.ilamparithi.aournalpp.backup.model.StorageProviderType.GOOGLE_DRIVE
                        )).copy(
                            authToken = tokenResponse.accessToken,
                            refreshToken = tokenResponse.refreshToken ?: existingGdrive?.refreshToken ?: "",
                            accountIdentifier = tokenResponse.userEmail ?: existingGdrive?.accountIdentifier ?: "Google Account",
                            isEnabled = true
                        )
                        vault.saveService(serviceToSave)
                        Log.i(TAG, "Successfully authenticated Google Drive for ${tokenResponse.userEmail}")
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
                    val repo = DocumentRepository(this@MainActivity)

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
    @androidx.annotation.StringRes val titleRes: Int,
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
    val aournalPrefs = remember { context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE) }
    val reduceAnimations = remember { aournalPrefs.getBoolean(LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS, false) }

    var selectedTab by rememberSaveable { mutableIntStateOf(AppTab.HOME.id) }
    val saveableStateHolder = rememberSaveableStateHolder()
    var tabGenerations by rememberSaveable { mutableStateOf(mapOf<Int, Int>()) }

    var lastClickTime by remember { mutableLongStateOf(0L) }
    var lastClickedTab by remember { mutableIntStateOf(-1) }

    val onTabSelect: (Int) -> Unit = { tabId ->
        val currentTime = System.currentTimeMillis()
        val isDoubleTap = (lastClickedTab == tabId) && (currentTime - lastClickTime < 400L)

        if (selectedTab != tabId) {
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

    BackHandler(enabled = selectedTab != AppTab.HOME.id) {
        selectedTab = AppTab.HOME.id
    }

    @Composable
    fun RenderTabContent(tab: Int) {
        when (tab) {
            AppTab.HOME.id -> HomeScreen(
                onNavigateToFiles = { selectedTab = AppTab.FILES.id },
                onNavigateToSettings = { selectedTab = AppTab.SETTINGS.id },
                onNavigateToAbout = { selectedTab = AppTab.ABOUT.id }
            )
            AppTab.FILES.id -> DocumentHubScreen(
                onNavigateToSettings = { selectedTab = AppTab.SETTINGS.id },
                onNavigateToLicenses = { selectedTab = AppTab.ABOUT.id }
            )
            AppTab.CLOUD.id -> CloudScreen(
                onNavigateToSettings = { selectedTab = AppTab.SETTINGS.id }
            )
            AppTab.SETTINGS.id -> SettingsScreen(onBack = { selectedTab = AppTab.HOME.id })
            AppTab.ABOUT.id -> LicensesScreen(onBack = { selectedTab = AppTab.HOME.id })
            else -> HomeScreen(
                onNavigateToFiles = { selectedTab = AppTab.FILES.id },
                onNavigateToSettings = { selectedTab = AppTab.SETTINGS.id },
                onNavigateToAbout = { selectedTab = AppTab.ABOUT.id }
            )
        }
    }

    @Composable
    fun TabHost(tabId: Int) {
        val gen = tabGenerations[tabId] ?: 0
        val pageKey = "tab_${tabId}_$gen"
        saveableStateHolder.SaveableStateProvider(key = pageKey) {
            key(pageKey) {
                RenderTabContent(tabId)
            }
        }
    }

    val tabTransitionSpec: androidx.compose.animation.AnimatedContentTransitionScope<Int>.() -> androidx.compose.animation.ContentTransform = {
        if (reduceAnimations) {
            fadeIn(animationSpec = androidx.compose.animation.core.tween(120))
                .togetherWith(fadeOut(animationSpec = androidx.compose.animation.core.tween(100)))
        } else {
            val isForward = targetState > initialState
            val enterOffset = if (isForward) 1 else -1
            val exitOffset = if (isForward) -1 else 1

            (slideInHorizontally(
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = 0.82f,
                    stiffness = 380f
                ),
                initialOffsetX = { (it / 3) * enterOffset }
            ) + fadeIn(
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = 0.9f,
                    stiffness = 400f
                )
            ))
                .togetherWith(
                    slideOutHorizontally(
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = 0.82f,
                            stiffness = 380f
                        ),
                        targetOffsetX = { -(it / 3) * exitOffset }
                    ) + fadeOut(
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = 0.9f,
                            stiffness = 400f
                        )
                    )
                )
        }
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
                        Text(
                            text = "A",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 16.dp)
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
                        TabHost(tabId)
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
                        TabHost(tabId)
                    }
                }
            }
        }
    }
}
