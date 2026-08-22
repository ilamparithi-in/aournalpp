package dev.ilamparithi.aournalpp

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    }

    private lateinit var env: LinuxEnvironment
    private lateinit var supervisor: ProcessSupervisor
    private lateinit var sessionManager: CanvasSessionManager
    private var inputHandler: TouchInputHandler? = null
    private var activeLorieView: LorieView? = null

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
        val initialTitle = targetPath?.let { File(it).name } ?: "New Note"

        setContent {
            AournalTheme {
                var showExitDialog by remember { mutableStateOf(false) }
                var isHeaderExpanded by remember { mutableStateOf(true) }
                val liveTitle by sessionManager.documentTitle.collectAsState()
                val displayTitle = liveTitle ?: initialTitle

                BackHandler {
                    showExitDialog = true
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        X11Viewport(
                            modifier = Modifier.fillMaxSize(),
                            onLorieViewReady = { lorieView ->
                                activeLorieView = lorieView
                                sessionManager.startSession(lorieView, targetPath)
                            },
                            onInputHandlerReady = { handler ->
                                inputHandler = handler
                            }
                        )

                        // Floating Top Header Bar with Document Name and Keyboard Toggle
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
                                            onClick = { showExitDialog = true },
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

                            // Minimalist Floating Collapse Indicator when header is collapsed
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
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
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
