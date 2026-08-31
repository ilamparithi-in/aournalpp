package dev.ilamparithi.aournalpp.runtime

import android.content.Context
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.io.File

/**
 * Metadata representing an active background or foreground canvas session.
 */
data class ActiveSessionInfo(
    val isRunning: Boolean,
    val pid: Int,
    val activeNotePath: String?,
    val documentTitle: String?,
    val openWindowCount: Int = 1,
    val lastTimestamp: Long = System.currentTimeMillis()
)

/**
 * Multi-process safe tracker for the active X11/Canvas session.
 * Stores session metadata in a temporary file and verifies PID liveness
 * to prevent stale or ghost sessions across app restarts or crashes.
 */
object ActiveSessionTracker {
    private const val TAG = "ActiveSessionTracker"
    private const val SESSION_FILE_NAME = ".active_canvas_session.json"

    private fun getSessionFile(baseDir: File): File {
        return File(baseDir, SESSION_FILE_NAME)
    }

    private fun getSessionFile(env: LinuxEnvironment): File {
        return getSessionFile(env.tmpDir)
    }

    private fun getSessionFile(context: Context): File {
        val env = LinuxEnvironment(context)
        return getSessionFile(env)
    }

    fun isPidAlive(pid: Int): Boolean {
        if (pid <= 0) return false
        val procFile = File("/proc/$pid")
        if (procFile.exists()) return true
        return try {
            Os.kill(pid, 0)
            true
        } catch (e: ErrnoException) {
            false
        } catch (_: Throwable) {
            false
        }
    }

    private fun logDebug(message: String) {
        try {
            Log.d(TAG, message)
        } catch (_: Throwable) {}
    }

    private fun logError(message: String, throwable: Throwable? = null) {
        try {
            Log.e(TAG, message, throwable)
        } catch (_: Throwable) {}
    }

    private fun logWarn(message: String) {
        try {
            Log.w(TAG, message)
        } catch (_: Throwable) {}
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
    }

    private fun unescapeJson(str: String): String {
        return str.replace("\\n", "\n").replace("\\r", "\r").replace("\\\"", "\"").replace("\\\\", "\\")
    }

    private fun extractString(json: String, key: String): String? {
        val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return regex.find(json)?.groupValues?.get(1)?.let { unescapeJson(it) }?.takeIf { it.isNotEmpty() }
    }

    private fun extractInt(json: String, key: String, default: Int): Int {
        val regex = Regex("\"$key\"\\s*:\\s*([0-9-]+)")
        return regex.find(json)?.groupValues?.get(1)?.toIntOrNull() ?: default
    }

    private fun extractLong(json: String, key: String, default: Long): Long {
        val regex = Regex("\"$key\"\\s*:\\s*([0-9-]+)")
        return regex.find(json)?.groupValues?.get(1)?.toLongOrNull() ?: default
    }

    private fun extractBoolean(json: String, key: String, default: Boolean): Boolean {
        val regex = Regex("\"$key\"\\s*:\\s*(true|false)")
        return regex.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull() ?: default
    }

    @Synchronized
    fun setActiveSession(
        baseDir: File,
        info: ActiveSessionInfo
    ) {
        try {
            val file = getSessionFile(baseDir)
            if (!baseDir.exists()) baseDir.mkdirs()
            val serialized = buildString {
                append("{\n")
                append("  \"isRunning\": ${info.isRunning},\n")
                append("  \"pid\": ${info.pid},\n")
                append("  \"activeNotePath\": \"${info.activeNotePath?.let { escapeJson(it) } ?: ""}\",\n")
                append("  \"documentTitle\": \"${info.documentTitle?.let { escapeJson(it) } ?: ""}\",\n")
                append("  \"openWindowCount\": ${info.openWindowCount},\n")
                append("  \"lastTimestamp\": ${info.lastTimestamp}\n")
                append("}")
            }
            file.writeText(serialized)
            logDebug("Active session recorded: pid=${info.pid}, title=${info.documentTitle}, windows=${info.openWindowCount}")
        } catch (e: Exception) {
            logError("Failed to write active session state", e)
        }
    }

    @Synchronized
    fun setActiveSession(
        context: Context,
        env: LinuxEnvironment,
        info: ActiveSessionInfo
    ) = setActiveSession(env.tmpDir, info)

    @Synchronized
    fun updateTitle(
        baseDir: File,
        title: String?
    ) {
        val current = getActiveSession(baseDir) ?: return
        setActiveSession(baseDir, current.copy(documentTitle = title, lastTimestamp = System.currentTimeMillis()))
    }

    @Synchronized
    fun updateTitle(
        context: Context,
        env: LinuxEnvironment,
        title: String?
    ) = updateTitle(env.tmpDir, title)

    @Synchronized
    fun updateWindowCount(
        baseDir: File,
        count: Int
    ) {
        val current = getActiveSession(baseDir) ?: return
        setActiveSession(baseDir, current.copy(openWindowCount = count, lastTimestamp = System.currentTimeMillis()))
    }

    @Synchronized
    fun updateWindowCount(
        context: Context,
        env: LinuxEnvironment,
        count: Int
    ) = updateWindowCount(env.tmpDir, count)

    @Synchronized
    fun clearActiveSession(baseDir: File) {
        try {
            val file = getSessionFile(baseDir)
            if (file.exists()) {
                file.delete()
            }
            logDebug("Active session cleared.")
        } catch (e: Exception) {
            logWarn("Failed to clear active session file")
        }
    }

    @Synchronized
    fun clearActiveSession(context: Context, env: LinuxEnvironment) = clearActiveSession(env.tmpDir)

    @Synchronized
    fun getActiveSession(baseDir: File): ActiveSessionInfo? {
        val file = getSessionFile(baseDir)
        if (!file.exists() || !file.canRead()) return null

        return try {
            val text = file.readText().trim()
            if (text.isEmpty()) return null

            val isRunning = extractBoolean(text, "isRunning", false)
            val pid = extractInt(text, "pid", -1)
            val notePath = extractString(text, "activeNotePath")
            val title = extractString(text, "documentTitle")
            val windowCount = extractInt(text, "openWindowCount", 1)
            val timestamp = extractLong(text, "lastTimestamp", System.currentTimeMillis())

            if (isRunning && pid > 0) {
                if (isPidAlive(pid)) {
                    ActiveSessionInfo(
                        isRunning = true,
                        pid = pid,
                        activeNotePath = notePath,
                        documentTitle = title,
                        openWindowCount = windowCount,
                        lastTimestamp = timestamp
                    )
                } else {
                    logWarn("Active session PID $pid is dead. Cleaning up stale session file.")
                    file.delete()
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            logWarn("Failed to read active session file")
            null
        }
    }

    @Synchronized
    fun getActiveSession(context: Context, env: LinuxEnvironment = LinuxEnvironment(context)): ActiveSessionInfo? {
        return getActiveSession(env.tmpDir)
    }

    fun isSessionActive(baseDir: File): Boolean {
        return getActiveSession(baseDir)?.isRunning == true
    }

    fun isSessionActive(context: Context, env: LinuxEnvironment = LinuxEnvironment(context)): Boolean {
        return getActiveSession(env.tmpDir)?.isRunning == true
    }

    fun activeSessionFlow(
        context: Context,
        env: LinuxEnvironment = LinuxEnvironment(context),
        pollIntervalMs: Long = 1000L
    ): Flow<ActiveSessionInfo?> = flow {
        var lastEmitted: ActiveSessionInfo? = null
        while (true) {
            val current = getActiveSession(context, env)
            if (current != lastEmitted) {
                lastEmitted = current
                emit(current)
            }
            delay(pollIntervalMs)
        }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)
}
