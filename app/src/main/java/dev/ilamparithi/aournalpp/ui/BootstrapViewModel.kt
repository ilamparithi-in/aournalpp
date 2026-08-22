package dev.ilamparithi.aournalpp.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.ilamparithi.aournalpp.runtime.BootstrapInstaller
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.InstallProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class BootstrapState {
    data object Checking : BootstrapState()
    data class Installing(val progress: InstallProgress?) : BootstrapState()
    data object Ready : BootstrapState()
    data class Error(val error: Throwable) : BootstrapState()
}

class BootstrapViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "BootstrapViewModel"
    }

    private val _state = MutableStateFlow<BootstrapState>(BootstrapState.Checking)
    val state: StateFlow<BootstrapState> = _state.asStateFlow()

    private val env = LinuxEnvironment(application)
    private val installer = BootstrapInstaller(application, env)

    init {
        checkAndInstall()
    }

    fun retry() {
        Log.i(TAG, "Retry requested by user. Clearing previous installation state...")
        installer.clearInstallation()
        checkAndInstall()
    }

    private fun checkAndInstall() {
        viewModelScope.launch {
            _state.value = BootstrapState.Checking
            Log.i(TAG, "Checking bootstrap installation status (rootDir: ${env.rootDir.absolutePath})...")
            
            val isAlreadyInstalled = installer.isInstalled()
            Log.i(TAG, "isInstalled: $isAlreadyInstalled")

            if (!isAlreadyInstalled) {
                _state.value = BootstrapState.Installing(null)
                Log.i(TAG, "Starting bootstrap extraction...")
                val result = installer.installBootstrap { status ->
                    _state.value = BootstrapState.Installing(status)
                }
                
                if (result.isFailure) {
                    val err = result.exceptionOrNull() ?: Exception("Unknown extraction failure")
                    Log.e(TAG, "Bootstrap extraction failed", err)
                    _state.value = BootstrapState.Error(err)
                    return@launch
                }
            }

            // Verify executable permissions
            val xournalBin = File(env.binDir, "xournalpp")
            val matchboxBin = File(env.binDir, "matchbox-window-manager")
            
            val xournalExists = xournalBin.exists()
            val xournalExec = xournalBin.canExecute()
            val matchboxExists = matchboxBin.exists()
            val matchboxExec = matchboxBin.canExecute()

            Log.i(TAG, "Verification check:")
            Log.i(TAG, " - usrDir: ${env.usrDir.absolutePath} (exists=${env.usrDir.exists()})")
            Log.i(TAG, " - binDir: ${env.binDir.absolutePath} (exists=${env.binDir.exists()})")
            Log.i(TAG, " - xournalpp: ${xournalBin.absolutePath} (exists=$xournalExists, canExecute=$xournalExec, size=${if (xournalExists) xournalBin.length() else 0})")
            Log.i(TAG, " - matchbox: ${matchboxBin.absolutePath} (exists=$matchboxExists, canExecute=$matchboxExec)")

            if (xournalExists && xournalExec) {
                Log.i(TAG, "Bootstrap verified successfully. Transitioning to Ready.")
                _state.value = BootstrapState.Ready
            } else {
                val binFiles = env.binDir.list()?.take(15)?.joinToString() ?: "none"
                val diagnostic = StringBuilder()
                    .append("Executable verification failed.\n")
                    .append("xournalpp path: ${xournalBin.absolutePath}\n")
                    .append("exists: $xournalExists, canExecute: $xournalExec\n")
                    .append("bin directory contents: $binFiles")
                    .toString()

                Log.e(TAG, diagnostic)
                _state.value = BootstrapState.Error(IllegalStateException(diagnostic))
            }
        }
    }
}
