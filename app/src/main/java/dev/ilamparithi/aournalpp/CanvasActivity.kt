package dev.ilamparithi.aournalpp

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.rememberCoroutineScope
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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                sessionManager.stopSession()
                finish()
            }
        })

        setContent {
            AournalTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    X11Viewport(
                        modifier = Modifier.fillMaxSize(),
                        onLorieViewReady = { lorieView ->
                            sessionManager.startSession(lorieView, targetPath)
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionManager.stopSession()
    }
}
