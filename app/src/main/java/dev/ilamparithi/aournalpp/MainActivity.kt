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
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
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
import dev.ilamparithi.aournalpp.ui.LicensesScreen
import dev.ilamparithi.aournalpp.ui.theme.AournalTheme
import dev.ilamparithi.aournalpp.utils.ExternalFileHandler
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var pendingIntentToProcess: Intent? = null

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
                        MainResponsiveAppShell()
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
                    val canvasIntent = Intent(this@MainActivity, CanvasActivity::class.java).apply {
                        putExtra(CanvasActivity.EXTRA_NOTE_PATH, file.absolutePath)
                    }
                    startActivity(canvasIntent)
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
    var selectedTab by remember { mutableIntStateOf(0) }

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
                                imageVector = if (selectedTab == 0) Icons.Filled.Description else Icons.Outlined.Description,
                                contentDescription = "Notes"
                            )
                        },
                        label = { Text("Notes", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )

                    NavigationRailItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )

                    NavigationRailItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Filled.Gavel else Icons.Outlined.Gavel,
                                contentDescription = "Licenses"
                            )
                        },
                        label = { Text("Licenses", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (selectedTab) {
                        0 -> DocumentHubScreen(
                            onNavigateToSettings = { selectedTab = 1 },
                            onNavigateToLicenses = { selectedTab = 2 }
                        )
                        1 -> SettingsScreen(onBack = { selectedTab = 0 })
                        2 -> LicensesScreen(onBack = { selectedTab = 0 })
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
                                    imageVector = if (selectedTab == 0) Icons.Filled.Description else Icons.Outlined.Description,
                                    contentDescription = "Notes"
                                )
                            },
                            label = { Text("Notes") }
                        )

                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 1) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Settings"
                                )
                            },
                            label = { Text("Settings") }
                        )

                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 2) Icons.Filled.Gavel else Icons.Outlined.Gavel,
                                    contentDescription = "Licenses"
                                )
                            },
                            label = { Text("Licenses") }
                        )
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    when (selectedTab) {
                        0 -> DocumentHubScreen(
                            onNavigateToSettings = { selectedTab = 1 },
                            onNavigateToLicenses = { selectedTab = 2 }
                        )
                        1 -> SettingsScreen(onBack = { selectedTab = 0 })
                        2 -> LicensesScreen(onBack = { selectedTab = 0 })
                    }
                }
            }
        }
    }
}
