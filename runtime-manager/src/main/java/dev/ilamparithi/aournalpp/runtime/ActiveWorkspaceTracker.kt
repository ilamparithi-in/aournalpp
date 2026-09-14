package dev.ilamparithi.aournalpp.runtime

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

/**
 * Multi-process safe state manager for the active workspace.
 * Synchronizes window hierarchy, split/snap layout configurations, and
 * conflict statuses between the `:canvas` rendering process and the main
 * application process.
 */
object ActiveWorkspaceTracker {
    private const val TAG = "ActiveWorkspaceTracker"
    private const val WORKSPACE_FILE_NAME = ".active_workspace_state.json"

    private val workspaceEventTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun notifyWorkspaceChanged() {
        workspaceEventTrigger.tryEmit(Unit)
    }

    private fun getWorkspaceFile(baseDir: File): File {
        return File(baseDir, WORKSPACE_FILE_NAME)
    }

    fun getWorkspaceFile(env: LinuxEnvironment): File {
        return getWorkspaceFile(env.tmpDir)
    }

    fun getWorkspaceFile(context: Context): File {
        return getWorkspaceFile(LinuxEnvironment(context))
    }

    fun setWorkspaceState(baseDir: File, state: ActiveWorkspaceState) {
        try {
            if (!baseDir.exists()) {
                baseDir.mkdirs()
            }
            val targetFile = getWorkspaceFile(baseDir)
            val tmpFile = File(baseDir, "$WORKSPACE_FILE_NAME.tmp")

            tmpFile.writeText(state.toJson().toString(2), Charsets.UTF_8)
            if (!tmpFile.renameTo(targetFile)) {
                // Fallback direct write
                targetFile.writeText(tmpFile.readText(), Charsets.UTF_8)
                tmpFile.delete()
            }
            notifyWorkspaceChanged()
        } catch (e: Exception) {
            logError("Failed to persist workspace state to $baseDir", e)
        }
    }

    fun setWorkspaceState(context: Context, env: LinuxEnvironment, state: ActiveWorkspaceState) {
        setWorkspaceState(env.tmpDir, state)
    }

    fun getWorkspaceState(baseDir: File): ActiveWorkspaceState? {
        val file = getWorkspaceFile(baseDir)
        if (!file.exists()) return null

        return try {
            val content = file.readText(Charsets.UTF_8)
            val json = JSONObject(content)
            val state = ActiveWorkspaceState.fromJson(json)

            // Validate PID liveness to avoid stale workspace ghost state
            if (!ActiveSessionTracker.isPidAlive(state.sessionInfo.pid)) {
                logWarn("Session PID ${state.sessionInfo.pid} dead, pruning stale workspace state")
                clearWorkspaceState(baseDir)
                return null
            }
            state
        } catch (e: Exception) {
            logError("Failed to parse active workspace state from $file", e)
            null
        }
    }

    fun getWorkspaceState(context: Context, env: LinuxEnvironment = LinuxEnvironment(context)): ActiveWorkspaceState? {
        return getWorkspaceState(env.tmpDir)
    }

    fun clearWorkspaceState(baseDir: File) {
        try {
            val file = getWorkspaceFile(baseDir)
            if (file.exists()) {
                file.delete()
            }
            val tmpFile = File(baseDir, "$WORKSPACE_FILE_NAME.tmp")
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
            notifyWorkspaceChanged()
        } catch (e: Exception) {
            logError("Failed to clear workspace state", e)
        }
    }

    private fun logWarn(msg: String) {
        try { Log.w(TAG, msg) } catch (_: Throwable) {}
    }

    private fun logError(msg: String, e: Throwable? = null) {
        try { Log.e(TAG, msg, e) } catch (_: Throwable) {}
    }

    fun clearWorkspaceState(context: Context, env: LinuxEnvironment) {
        clearWorkspaceState(env.tmpDir)
    }

    fun workspaceStateFlow(
        baseDir: File,
        pollIntervalMs: Long = 400L
    ): Flow<ActiveWorkspaceState?> = channelFlow {
        // Send initial state immediately
        send(getWorkspaceState(baseDir))

        val triggerJob = launch {
            workspaceEventTrigger.collect {
                send(getWorkspaceState(baseDir))
            }
        }

        while (isActive) {
            delay(pollIntervalMs.milliseconds)
            send(getWorkspaceState(baseDir))
        }
        triggerJob.cancel()
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    fun workspaceStateFlow(
        context: Context,
        env: LinuxEnvironment = LinuxEnvironment(context),
        pollIntervalMs: Long = 400L
    ): Flow<ActiveWorkspaceState?> = workspaceStateFlow(env.tmpDir, pollIntervalMs)
}
