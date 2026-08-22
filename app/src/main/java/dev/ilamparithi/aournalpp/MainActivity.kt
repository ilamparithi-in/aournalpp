package dev.ilamparithi.aournalpp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.ui.BootstrapScreen
import dev.ilamparithi.aournalpp.ui.BootstrapState
import dev.ilamparithi.aournalpp.ui.BootstrapViewModel
import dev.ilamparithi.aournalpp.ui.DocumentHubScreen
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
                        DocumentHubScreen()
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
