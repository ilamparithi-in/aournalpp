package dev.ilamparithi.aournalpp

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.ui.BootstrapScreen
import dev.ilamparithi.aournalpp.ui.BootstrapState
import dev.ilamparithi.aournalpp.ui.BootstrapViewModel
import dev.ilamparithi.aournalpp.ui.DocumentHubScreen
import dev.ilamparithi.aournalpp.ui.HomeScreen
import dev.ilamparithi.aournalpp.ui.LicensesScreen
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

        pendingIntentToProcess = intent

        setContent {
            AournalTheme {
                val viewModel: BootstrapViewModel = viewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                when (state) {
                    is BootstrapState.Ready -> {
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
                        if (promptFile != null) {
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
                    else -> {
                        BootstrapScreen(
                            state = state,
                            onRetry = { viewModel.retry() }
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
}

@Composable
fun MainResponsiveAppShell() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
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
                    NavigationRailItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )

                    NavigationRailItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.FolderCopy else Icons.Outlined.FolderCopy,
                                contentDescription = "Files"
                            )
                        },
                        label = { Text("Files", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )

                    NavigationRailItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                    )

                    NavigationRailItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 3) Icons.Filled.Info else Icons.Outlined.Info,
                                contentDescription = "About"
                            )
                        },
                        label = { Text("About", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally(
                                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
                                    initialOffsetX = { it / 3 }
                                ) + fadeIn(animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f)))
                                    .togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
                                            targetOffsetX = { -it / 3 }
                                        ) + fadeOut(animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f))
                                    )
                            } else {
                                (slideInHorizontally(
                                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
                                    initialOffsetX = { -it / 3 }
                                ) + fadeIn(animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f)))
                                    .togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
                                            targetOffsetX = { it / 3 }
                                        ) + fadeOut(animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f))
                                    )
                            }
                        },
                        label = "railTabTransition"
                    ) { tab ->
                        when (tab) {
                            0 -> HomeScreen(
                                onNavigateToFiles = { selectedTab = 1 },
                                onNavigateToSettings = { selectedTab = 2 },
                                onNavigateToAbout = { selectedTab = 3 }
                            )
                            1 -> DocumentHubScreen(
                                onNavigateToSettings = { selectedTab = 2 },
                                onNavigateToLicenses = { selectedTab = 3 }
                            )
                            2 -> SettingsScreen(onBack = { selectedTab = 0 })
                            3 -> LicensesScreen(onBack = { selectedTab = 0 })
                        }
                    }
                }
            }
        } else {
            // Mobile Portrait: Bottom Navigation Bar
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Home"
                                )
                            },
                            label = { Text("Home") }
                        )

                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 1) Icons.Filled.FolderCopy else Icons.Outlined.FolderCopy,
                                    contentDescription = "Files"
                                )
                            },
                            label = { Text("Files") }
                        )

                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Settings"
                                )
                            },
                            label = { Text("Settings") }
                        )

                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 3) Icons.Filled.Info else Icons.Outlined.Info,
                                    contentDescription = "About"
                                )
                            },
                            label = { Text("About") }
                        )
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally(
                                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
                                    initialOffsetX = { it / 3 }
                                ) + fadeIn(animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f)))
                                    .togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
                                            targetOffsetX = { -it / 3 }
                                        ) + fadeOut(animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f))
                                    )
                            } else {
                                (slideInHorizontally(
                                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
                                    initialOffsetX = { -it / 3 }
                                ) + fadeIn(animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f)))
                                    .togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
                                            targetOffsetX = { it / 3 }
                                        ) + fadeOut(animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f))
                                    )
                            }
                        },
                        label = "bottomTabTransition"
                    ) { tab ->
                        when (tab) {
                            0 -> HomeScreen(
                                onNavigateToFiles = { selectedTab = 1 },
                                onNavigateToSettings = { selectedTab = 2 },
                                onNavigateToAbout = { selectedTab = 3 }
                            )
                            1 -> DocumentHubScreen(
                                onNavigateToSettings = { selectedTab = 2 },
                                onNavigateToLicenses = { selectedTab = 3 }
                            )
                            2 -> SettingsScreen(onBack = { selectedTab = 0 })
                            3 -> LicensesScreen(onBack = { selectedTab = 0 })
                        }
                    }
                }
            }
        }
    }
}
