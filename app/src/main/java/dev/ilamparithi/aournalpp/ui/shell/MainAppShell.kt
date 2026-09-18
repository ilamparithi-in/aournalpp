package dev.ilamparithi.aournalpp.ui.shell

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ilamparithi.aournalpp.CanvasActivity
import dev.ilamparithi.aournalpp.CanvasCommandReceiver
import dev.ilamparithi.aournalpp.MainActivity
import dev.ilamparithi.aournalpp.SettingsScreen
import dev.ilamparithi.aournalpp.backup.engine.ConflictPersistenceManager
import dev.ilamparithi.aournalpp.runtime.ActiveSessionTracker
import dev.ilamparithi.aournalpp.ui.AppLogoBadge
import dev.ilamparithi.aournalpp.ui.DocumentHubScreen
import dev.ilamparithi.aournalpp.ui.HomeScreen
import dev.ilamparithi.aournalpp.ui.LicensesScreen
import dev.ilamparithi.aournalpp.ui.SessionClosingScreen
import dev.ilamparithi.aournalpp.ui.animation.AppAnimatedVisibility
import dev.ilamparithi.aournalpp.ui.animation.LocalMotionPreferences
import dev.ilamparithi.aournalpp.ui.animation.SpringSlideTransition
import dev.ilamparithi.aournalpp.ui.cloud.CloudScreen
import dev.ilamparithi.aournalpp.ui.cloud.CloudSubpage
import dev.ilamparithi.aournalpp.ui.workspace.ActiveWorkspacePortalScreen
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Responsive application shell providing adaptive layout:
 * - Wide screens (>= 600dp): NavigationRail on the left
 * - Compact screens (< 600dp): NavigationBar at the bottom
 */
@Composable
fun MainResponsiveAppShell(
    targetTab: Int? = null,
    targetCloudSubpage: CloudSubpage? = null,
    onNavigationHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val reduceAnimations = LocalMotionPreferences.current.reduceAnimations

    var selectedTab by rememberSaveable { mutableIntStateOf(AppTab.HOME.id) }

    LaunchedEffect(targetTab) {
        if (targetTab != null) {
            selectedTab = targetTab
            onNavigationHandled()
        }
    }
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
            val mainActivity = context as? MainActivity
            if (tabId == AppTab.FILES.id && mainActivity?.quarantinedEmergencySave?.value != null) {
                mainActivity.showEmergencyRecoveryDialog.value = true
            }
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

    val isCanvasSessionActive by ActiveSessionTracker.activeSessionFlow(context)
        .collectAsStateWithLifecycle(initialValue = null)
    val isSessionRunning = isCanvasSessionActive?.isRunning == true

    val visibleTabs = remember(isSessionRunning) {
        if (isSessionRunning) {
            listOf(AppTab.WORKSPACE, AppTab.HOME, AppTab.FILES, AppTab.CLOUD, AppTab.SETTINGS, AppTab.ABOUT)
        } else {
            listOf(AppTab.HOME, AppTab.FILES, AppTab.CLOUD, AppTab.SETTINGS, AppTab.ABOUT)
        }
    }

    LaunchedEffect(isSessionRunning) {
        if (!isSessionRunning && selectedTab == AppTab.WORKSPACE.id) {
            selectedTab = AppTab.HOME.id
        }
    }

    var isClosingSession by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isClosingSession) {
        if (isClosingSession) {
            val activity = context as? Activity
            while (isClosingSession) {
                if (!ActiveSessionTracker.isSessionActive(context)) {
                    isClosingSession = false
                    activity?.finishAffinity() ?: activity?.finish()
                    break
                }
                delay(50.milliseconds)
            }
        }
    }

    DisposableEffect(isClosingSession) {
        if (!isClosingSession) return@DisposableEffect onDispose {}
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == "dev.ilamparithi.aournalpp.ACTION_SESSION_CLOSED") {
                    isClosingSession = false
                    val activity = context as? Activity
                    activity?.finishAffinity() ?: activity?.finish()
                }
            }
        }
        val filter = IntentFilter("dev.ilamparithi.aournalpp.ACTION_SESSION_CLOSED")
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
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

    val allTabs = remember {
        listOf(AppTab.WORKSPACE, AppTab.HOME, AppTab.FILES, AppTab.CLOUD, AppTab.SETTINGS, AppTab.ABOUT)
    }

    val unresolvedConflicts by ConflictPersistenceManager.getInstance(context).unresolvedConflicts.collectAsStateWithLifecycle()
    val hasUnresolvedConflicts = unresolvedConflicts.isNotEmpty()

    val tabTransitionSpec: androidx.compose.animation.AnimatedContentTransitionScope<Int>.() -> androidx.compose.animation.ContentTransform = {
        val fromIndex = allTabs.indexOfFirst { it.id == initialState }
        val toIndex = allTabs.indexOfFirst { it.id == targetState }
        val isForward = if (fromIndex != -1 && toIndex != -1) toIndex > fromIndex else targetState > initialState
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
                        AppLogoBadge(
                            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
                            size = 38.dp
                        )
                    }
                ) {
                    visibleTabs.forEach { tab ->
                        val isFiles = tab == AppTab.FILES
                        val isCloud = tab == AppTab.CLOUD
                        val mainActivity = context as? MainActivity
                        val showFilesRedDot = isFiles && mainActivity?.quarantinedEmergencySave?.value != null
                        val showCloudRedDot = isCloud && hasUnresolvedConflicts
                        val windowCount = isCanvasSessionActive?.openWindowCount ?: 1
                        NavigationRailItem(
                            selected = selectedTab == tab.id,
                            onClick = { onTabSelect(tab.id) },
                            icon = {
                                NavigationTabIcon(
                                    tab = tab,
                                    isSelected = selectedTab == tab.id,
                                    isSessionRunning = isSessionRunning,
                                    windowCount = windowCount,
                                    showFilesRedDot = showFilesRedDot,
                                    showCloudRedDot = showCloudRedDot
                                )
                            },
                            label = {
                                NavigationTabLabel(
                                    tab = tab,
                                    isSelected = selectedTab == tab.id,
                                    isSessionRunning = isSessionRunning
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
                            targetCloudSubpage = targetCloudSubpage,
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
                        visibleTabs.forEach { tab ->
                            val isFiles = tab == AppTab.FILES
                            val isCloud = tab == AppTab.CLOUD
                            val mainActivity = context as? MainActivity
                            val showFilesRedDot = isFiles && mainActivity?.quarantinedEmergencySave?.value != null
                            val showCloudRedDot = isCloud && hasUnresolvedConflicts
                            val windowCount = isCanvasSessionActive?.openWindowCount ?: 1
                            NavigationBarItem(
                                selected = selectedTab == tab.id,
                                onClick = { onTabSelect(tab.id) },
                                icon = {
                                    NavigationTabIcon(
                                        tab = tab,
                                        isSelected = selectedTab == tab.id,
                                        isSessionRunning = isSessionRunning,
                                        windowCount = windowCount,
                                        showFilesRedDot = showFilesRedDot,
                                        showCloudRedDot = showCloudRedDot
                                    )
                                },
                                label = {
                                    NavigationTabLabel(
                                        tab = tab,
                                        isSelected = selectedTab == tab.id,
                                        isSessionRunning = isSessionRunning
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
                            targetCloudSubpage = targetCloudSubpage,
                            onTabSelect = onTabSelect
                        )
                    }
                }
            }
        }
    }

    AppAnimatedVisibility(
        visible = isClosingSession,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300))
    ) {
        SessionClosingScreen(
            documentTitle = isCanvasSessionActive?.documentTitle
        )
    }
}

@Composable
private fun TabHost(
    tabId: Int,
    tabGenerations: Map<Int, Int>,
    saveableStateHolder: SaveableStateHolder,
    targetCloudSubpage: CloudSubpage? = null,
    onTabSelect: (Int) -> Unit
) {
    val gen = tabGenerations[tabId] ?: 0
    val pageKey = "tab_${tabId}_$gen"
    saveableStateHolder.SaveableStateProvider(key = pageKey) {
        key(pageKey) {
            RenderTabContent(
                tab = tabId,
                targetCloudSubpage = targetCloudSubpage,
                onTabSelect = onTabSelect
            )
        }
    }
}

@Composable
private fun RenderTabContent(
    tab: Int,
    targetCloudSubpage: CloudSubpage? = null,
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
        AppTab.WORKSPACE.id -> ActiveWorkspacePortalScreen(
            onNavigateHome = { onTabSelect(AppTab.HOME.id) }
        )
        AppTab.CLOUD.id -> CloudScreen(
            initialSubpage = targetCloudSubpage ?: CloudSubpage.OVERVIEW,
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
