package dev.ilamparithi.aournalpp

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import dev.ilamparithi.aournalpp.runtime.CanvasSessionManager
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.ui.theme.AournalTheme
import dev.ilamparithi.aournalpp.x11.X11Viewport

class CanvasActivity : ComponentActivity() {

    companion object {
        const val EXTRA_NOTE_PATH = "dev.ilamparithi.aournalpp.extra.NOTE_PATH"
    }

    private lateinit var env: LinuxEnvironment
    private lateinit var supervisor: ProcessSupervisor
    private lateinit var sessionManager: CanvasSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure full-screen sticky immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Keep screen on during note-taking
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        com.termux.x11.MainActivity.setPrefs(com.termux.x11.Prefs(this))

        env = LinuxEnvironment(this)
        supervisor = ProcessSupervisor(env)
        sessionManager = CanvasSessionManager(
            context = this,
            env = env,
            supervisor = supervisor,
            scope = lifecycleScope
        )

        val targetPath = intent.getStringExtra(EXTRA_NOTE_PATH)

        setContent {
            AournalTheme {
                var showExitDialog by remember { mutableStateOf(false) }

                BackHandler {
                    showExitDialog = true
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    X11Viewport(
                        modifier = Modifier.fillMaxSize(),
                        onLorieViewReady = { lorieView ->
                            sessionManager.startSession(lorieView, targetPath)
                        }
                    )

                    if (showExitDialog) {
                        AlertDialog(
                            onDismissRequest = { showExitDialog = false },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            title = {
                                Text(
                                    text = "Exit Note Session?",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            text = {
                                Text(
                                    text = "Are you sure you want to close this session? Please ensure your changes in Xournal++ are saved before exiting.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            confirmButton = {
                                androidx.compose.material3.Button(
                                    onClick = { showExitDialog = false }
                                ) {
                                    androidx.compose.material3.Text("Stay")
                                }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        showExitDialog = false
                                        sessionManager.stopSession()
                                        finish()
                                    }
                                ) {
                                    androidx.compose.material3.Text(
                                        text = "Exit Note",
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            sessionManager.stopSession()
            // Terminate isolated :canvas process cleanly so reopening starts fresh
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}
