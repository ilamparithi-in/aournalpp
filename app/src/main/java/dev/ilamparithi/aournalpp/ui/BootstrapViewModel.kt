package dev.ilamparithi.aournalpp.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.ilamparithi.aournalpp.runtime.BootstrapInstaller
import dev.ilamparithi.aournalpp.runtime.InstallProgress
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface BootstrapState {
    data object Checking : BootstrapState
    data class UpdatePrompt(
        val installedVersion: Long,
        val newVersion: Long,
        val countdownSeconds: Int
    ) : BootstrapState
    data class Installing(
        val progress: InstallProgress? = null,
        val message: String = ""
    ) : BootstrapState
    data object Ready : BootstrapState
    data class Error(val throwable: Throwable) : BootstrapState
}

class BootstrapViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "BootstrapViewModel"
        private const val UPDATE_TIMEOUT_SECONDS = 10
    }

    private val env = LinuxEnvironment(application)
    private val installer = BootstrapInstaller(application, env)

    private val _uiState = MutableStateFlow<BootstrapState>(BootstrapState.Checking)
    val uiState: StateFlow<BootstrapState> = _uiState.asStateFlow()
    val state: StateFlow<BootstrapState> get() = uiState

    private val _isOnboardingCompleted = MutableStateFlow(env.isOnboardingCompleted())
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private var updateCountdownJob: Job? = null
    private var installJob: Job? = null

    init {
        checkAndInitialize()
    }

    fun completeOnboarding() {
        env.setOnboardingCompleted(true)
        _isOnboardingCompleted.value = true
        Log.i(TAG, "Onboarding marked as completed.")
    }

    fun retry() {
        Log.i(TAG, "Retry requested by user. Clearing previous installation state...")
        updateCountdownJob?.cancel()
        updateCountdownJob = null
        installer.clearInstallation()
        performInstallOrUpgrade()
    }

    fun startInstallOrUpgrade() {
        Log.i(TAG, "Starting install/upgrade immediately...")
        updateCountdownJob?.cancel()
        updateCountdownJob = null
        performInstallOrUpgrade()
    }

    fun skipUpdateForCurrentSession() {
        Log.i(TAG, "User chose to skip environment update for this launch.")
        updateCountdownJob?.cancel()
        updateCountdownJob = null
        if (installer.hasValidInstallation()) {
            _uiState.value = BootstrapState.Ready
        } else {
            performInstallOrUpgrade()
        }
    }

    fun checkAndInitialize() {
        viewModelScope.launch {
            _uiState.value = BootstrapState.Checking
            Log.i(TAG, "Checking bootstrap status (rootDir: ${env.rootDir.absolutePath})...")

            val needsUpgrade = installer.needsBootstrap()
            val isUpgrade = installer.isUpgradeAvailable()
            val onboardingDone = env.isOnboardingCompleted()

            Log.i(TAG, "needsBootstrap: $needsUpgrade, isUpgradeAvailable: $isUpgrade, onboardingDone: $onboardingDone (currentAppVersion: ${installer.getCurrentAppVersionCode()})")

            if (!onboardingDone) {
                // First install: extract in background while user views onboarding
                if (needsUpgrade) {
                    performInstallOrUpgrade()
                } else {
                    verifyAndSetReady()
                }
            } else if (isUpgrade) {
                // Existing install update: show prompt with 10s countdown
                val installedVer = installer.getInstalledVersion() ?: 0L
                val currentVer = installer.getCurrentAppVersionCode()
                startUpdateCountdown(installedVer, currentVer)
            } else if (needsUpgrade) {
                // Missing / incomplete install: extract immediately
                performInstallOrUpgrade()
            } else {
                // All up to date
                verifyAndSetReady()
            }
        }
    }

    private fun startUpdateCountdown(installedVersion: Long, newVersion: Long) {
        updateCountdownJob?.cancel()
        updateCountdownJob = viewModelScope.launch {
            for (sec in UPDATE_TIMEOUT_SECONDS downTo 1) {
                _uiState.value = BootstrapState.UpdatePrompt(installedVersion, newVersion, sec)
                delay(1000L)
            }
            Log.i(TAG, "Update countdown timer expired. Auto-starting update...")
            performInstallOrUpgrade()
        }
    }

    private fun performInstallOrUpgrade() {
        installJob?.cancel()
        installJob = viewModelScope.launch {
            _uiState.value = BootstrapState.Installing(message = "Starting runtime extraction...")
            Log.i(TAG, "Starting bootstrap extraction/upgrade...")
            val result = installer.installOrUpgrade { progress ->
                _uiState.value = BootstrapState.Installing(
                    progress = progress,
                    message = "Extracting ${progress.currentFile}..."
                )
            }

            if (result.isFailure) {
                val err = result.exceptionOrNull() ?: Exception("Unknown extraction failure")
                Log.e(TAG, "Bootstrap installation/upgrade failed", err)
                _uiState.value = BootstrapState.Error(err)
                return@launch
            }

            verifyAndSetReady()
        }
    }

    private fun verifyAndSetReady() {
        val xournalBin = env.resolveExecutable("xournalpp")
        val openboxBin = env.resolveExecutable("openbox")

        val xournalExists = xournalBin.exists()
        val xournalExec = xournalBin.canExecute()
        val openboxExists = openboxBin.exists()
        val openboxExec = openboxBin.canExecute()

        Log.i(TAG, "Verification check:")
        Log.i(TAG, " - usrDir: ${env.usrDir.absolutePath} (exists=${env.usrDir.exists()})")
        Log.i(TAG, " - binDir: ${env.binDir.absolutePath} (exists=${env.binDir.exists()})")
        Log.i(TAG, " - nativeLibDir: ${env.nativeLibDir.absolutePath} (exists=${env.nativeLibDir.exists()})")
        Log.i(TAG, " - xournalpp: ${xournalBin.absolutePath} (exists=$xournalExists, canExecute=$xournalExec, size=${if (xournalExists) xournalBin.length() else 0})")
        Log.i(TAG, " - openbox: ${openboxBin.absolutePath} (exists=$openboxExists, canExecute=$openboxExec)")

        if (xournalExists && xournalExec) {
            Log.i(TAG, "Bootstrap verified successfully. Transitioning to Ready.")
            _uiState.value = BootstrapState.Ready
        } else {
            val binFiles = env.binDir.list()?.take(15)?.joinToString() ?: "none"
            val diagnostic = StringBuilder()
                .append("Executable verification failed.\n")
                .append("xournalpp path: ${xournalBin.absolutePath}\n")
                .append("exists: $xournalExists, canExecute: $xournalExec\n")
                .append("bin directory contents: $binFiles")
                .toString()

            Log.e(TAG, diagnostic)
            _uiState.value = BootstrapState.Error(IllegalStateException(diagnostic))
        }
    }
}
