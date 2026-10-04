package dev.ilamparithi.aournalpp.logging

import android.content.Context
import dev.ilamparithi.aournalpp.backup.engine.BackupEngine
import org.json.JSONObject
import java.io.File

/**
 * Manages the persistence, retrieval, deletion, and auto-rotation of crash reports
 * in a hermetic app-private storage directory.
 *
 * Implements SEC-03 canonical path traversal defenses.
 */
class CrashLogStorage(
    val storageDir: File
) {
    constructor(context: Context) : this(File(context.filesDir, "logs/crashes"))

    companion object {
        const val MAX_CRASH_REPORTS = 20
        const val CRASH_FILE_EXT = ".json"
    }

    init {
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
    }

    @Synchronized
    fun saveCrashReport(report: CrashReport): File {
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        val safeFileName = "${report.id}$CRASH_FILE_EXT".replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val destFile = BackupEngine.resolveSafeChild(storageDir, safeFileName)

        destFile.writeText(report.toJson().toString(2), Charsets.UTF_8)
        pruneExcessCrashReports()
        return destFile
    }

    @Synchronized
    fun getAllCrashReports(): List<CrashReport> {
        if (!storageDir.exists()) return emptyList()
        val files = storageDir.listFiles { file -> file.isFile && file.name.endsWith(CRASH_FILE_EXT) }
            ?: return emptyList()

        return files.mapNotNull { file ->
            try {
                val json = JSONObject(file.readText(Charsets.UTF_8))
                CrashReport.fromJson(json)
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.timestamp }
    }

    @Synchronized
    fun getCrashReport(id: String): CrashReport? {
        val safeFileName = "${id}$CRASH_FILE_EXT".replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val file = try {
            BackupEngine.resolveSafeChild(storageDir, safeFileName)
        } catch (_: SecurityException) {
            return null
        }
        if (!file.exists()) return null
        return try {
            val json = JSONObject(file.readText(Charsets.UTF_8))
            CrashReport.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    @Synchronized
    fun deleteCrashReport(id: String): Boolean {
        val safeFileName = "${id}$CRASH_FILE_EXT".replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val file = try {
            BackupEngine.resolveSafeChild(storageDir, safeFileName)
        } catch (_: SecurityException) {
            return false
        }
        return if (file.exists()) file.delete() else false
    }

    @Synchronized
    fun clearAllCrashReports(): Int {
        if (!storageDir.exists()) return 0
        val files = storageDir.listFiles { file -> file.isFile && file.name.endsWith(CRASH_FILE_EXT) }
            ?: return 0
        var deletedCount = 0
        for (f in files) {
            if (f.delete()) deletedCount++
        }
        return deletedCount
    }

    @Synchronized
    fun getCrashCount(): Int {
        if (!storageDir.exists()) return 0
        return storageDir.listFiles { file -> file.isFile && file.name.endsWith(CRASH_FILE_EXT) }?.size ?: 0
    }

    private fun pruneExcessCrashReports() {
        val files = storageDir.listFiles { file -> file.isFile && file.name.endsWith(CRASH_FILE_EXT) }
            ?: return
        if (files.size <= MAX_CRASH_REPORTS) return

        // Sort by last modified ascending (oldest first)
        val sortedFiles = files.sortedBy { it.lastModified() }
        val toRemove = files.size - MAX_CRASH_REPORTS
        for (i in 0 until toRemove) {
            sortedFiles[i].delete()
        }
    }
}
