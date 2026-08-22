package dev.ilamparithi.aournalpp

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.termux.x11.LorieView
import com.termux.x11.input.TouchInputHandler
import dev.ilamparithi.aournalpp.runtime.CanvasSessionManager
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.ui.theme.AournalTheme
import dev.ilamparithi.aournalpp.x11.X11Viewport
import java.io.File

class CanvasActivity : ComponentActivity() {

    companion object {
        const val EXTRA_NOTE_PATH = "dev.ilamparithi.aournalpp.extra.NOTE_PATH"
        const val EXTRA_OPEN_PREFERENCES = "dev.ilamparithi.aournalpp.extra.OPEN_PREFERENCES"
        const val EXTRA_OPEN_PREFS_ALIAS = "EXTRA_OPEN_PREFERENCES"
    }

    private lateinit var env: LinuxEnvironment
    private lateinit var supervisor: ProcessSupervisor
    private lateinit var sessionManager: CanvasSessionManager
    private var inputHandler: TouchInputHandler? = null
    private var activeLorieView: LorieView? = null

    private val showEmergencyForceCloseDialogState = mutableStateOf(false)
    private val backPressTimestamps = mutableListOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleSmartBackPress()
            }
        })

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

        // Automatically finish activity when Xournal++ terminates
        sessionManager.setOnProcessExitListener {
            runOnUiThread {
                if (!isFinishing) {
                    finish()
                }
            }
        }

        val openPreferences = intent.getBooleanExtra(EXTRA_OPEN_PREFERENCES, false)
            || intent.getBooleanExtra(EXTRA_OPEN_PREFS_ALIAS, false)
            || intent.getBooleanExtra("EXTRA_OPEN_PREFS", false)

        val targetPath = intent.getStringExtra(EXTRA_NOTE_PATH)
        val initialTitle = when {
            targetPath != null -> File(targetPath).name
            openPreferences -> "Preferences"
            else -> "New Note"
        }

        setContent {
            AournalTheme {
                val showEmergencyForceCloseDialog by remember { showEmergencyForceCloseDialogState }
                var isHeaderExpanded by remember { mutableStateOf(true) }
                val liveTitle by sessionManager.documentTitle.collectAsState()
                val displayTitle = liveTitle ?: initialTitle

                BackHandler(enabled = true) {
                    handleSmartBackPress()
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        X11Viewport(
                            modifier = Modifier.fillMaxSize(),
                            onLorieViewReady = { lorieView ->
                                activeLorieView = lorieView
                                sessionManager.startSession(lorieView, targetPath, openPreferences)
                            },
                            onInputHandlerReady = { handler ->
                                inputHandler = handler
                            }
                        )

                        // Floating Top Header Bar
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 8.dp)
                        ) {
                            AnimatedVisibility(
                                visible = isHeaderExpanded,
                                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp)),
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                    tonalElevation = 6.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { handleSmartBackPress() },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Exit Note",
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )

                                        Spacer(modifier = Modifier.width(2.dp))

                                        Text(
                                            text = displayTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )

                                        // Keyboard Toggle Action
                                        IconButton(
                                            onClick = {
                                                activeLorieView?.let { view ->
                                                    view.requestFocus()
                                                    view.toggleKeyboardVisible()
                                                } ?: run {
                                                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                                    imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Keyboard,
                                                contentDescription = "Toggle Keyboard",
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        // Collapse Pill Action
                                        IconButton(
                                            onClick = { isHeaderExpanded = false },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ExpandLess,
                                                contentDescription = "Collapse Header",
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Minimalist Floating Collapse Indicator
                            if (!isHeaderExpanded) {
                                Surface(
                                    modifier = Modifier
                                        .shadow(elevation = 6.dp, shape = CircleShape)
                                        .clip(CircleShape)
                                        .clickable { isHeaderExpanded = true },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                                    tonalElevation = 4.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = displayTitle,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ExpandMore,
                                            contentDescription = "Expand Header",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Emergency Force Close Dialog (Non-dismissible by 4th+ back presses)
                        if (showEmergencyForceCloseDialog) {
                            AlertDialog(
                                onDismissRequest = { /* Non-dismissible on tap outside or back press */ },
                                properties = DialogProperties(
                                    dismissOnBackPress = false,
                                    dismissOnClickOutside = false
                                ),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                title = {
                                    Text(
                                        text = "Force Close Session?",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Text(
                                        text = "Xournal++ is not responding to normal close requests. Force terminating will immediately exit the canvas.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        onClick = {
                                            showEmergencyForceCloseDialogState.value = false
                                            sessionManager.stopSession()
                                            finish()
                                        }
                                    ) {
                                        Text("Force Close")
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = {
                                            showEmergencyForceCloseDialogState.value = false
                                        }
                                    ) {
                                        Text("Wait / Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleSmartBackPress() {
        // If force close dialog is already active, ignore back press to avoid accidental dismissal
        if (showEmergencyForceCloseDialogState.value) {
            return
        }

        val now = System.currentTimeMillis()
        backPressTimestamps.add(now)
        backPressTimestamps.removeAll { now - it > 2000 }

        val prefs = getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        val tripleBackEnabled = prefs.getBoolean("pref_triple_back_force_close", true)

        if (tripleBackEnabled && backPressTimestamps.size >= 3) {
            backPressTimestamps.clear()
            showEmergencyForceCloseDialogState.value = true
        } else {
            // Send Ctrl+Q shortcut to Xournal++ to trigger native close / GTK save confirmation dialog
            sessionManager.requestCloseSession()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            return super.dispatchKeyEvent(event)
        }
        val handler = inputHandler
        if (handler != null && handler.sendKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
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
