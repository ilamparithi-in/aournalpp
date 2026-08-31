package dev.ilamparithi.aournalpp.backup.scanner

import android.util.Log
import dev.ilamparithi.aournalpp.backup.db.SyncMetadataEntity
import dev.ilamparithi.aournalpp.backup.model.BackupScope
import dev.ilamparithi.aournalpp.backup.model.CustomFolderMapping
import dev.ilamparithi.aournalpp.backup.model.ExclusionFilterConfig
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import java.io.File
import java.security.MessageDigest
import java.util.regex.Pattern

data class ScannedLocalFile(
    val file: File,
    val scope: String,
    val relativePath: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val sha256: String
)

/**
 * High-performance local file scanner that traverses target backup directories,
 * enforces exclusion filters, and computes SHA-256 metadata checksums.
 */
class BackupScanner(
    private val env: LinuxEnvironment? = null,
    private val exclusionFilter: ExclusionFilterConfig = ExclusionFilterConfig.DEFAULT
) {
    companion object {
        private const val TAG = "BackupScanner"
        private val DEFAULT_NOTES_EXTENSIONS = setOf("xopp", "xoj", "pdf")
        private val DEFAULT_CONFIG_EXTENSIONS = setOf("xml", "ini", "gpl", "lua")
    }

    private val compiledRegexes = exclusionFilter.regexPatterns.mapNotNull {
        try {
            Pattern.compile(it, Pattern.CASE_INSENSITIVE)
        } catch (e: Exception) {
            Log.w(TAG, "Invalid regex pattern in exclusion filter: $it", e)
            null
        }
    }

    /**
     * Scans complete backup domains: Notes (including in-situ Emergency Saves) and .config.
     */
    fun scanCompleteBackup(cachedMetadata: Map<String, SyncMetadataEntity>? = null): List<ScannedLocalFile> {
        val environment = env ?: return emptyList()
        val results = mutableListOf<ScannedLocalFile>()

        // 1. Scan Notes Directory (~/Notes)
        val notesDir = environment.getNotesDirectory()
        if (notesDir.exists() && notesDir.isDirectory) {
            scanDirectoryRecursively(
                rootDir = notesDir,
                prefix = "Notes",
                scope = BackupScope.NOTES.id,
                allowedExtensions = exclusionFilter.includedExtensions ?: DEFAULT_NOTES_EXTENSIONS,
                results = results,
                cachedMetadata = cachedMetadata
            )
        }

        // 2. Scan Config Directory (~/.config/xournalpp)
        val configDir = environment.xournalConfigDir
        if (configDir.exists() && configDir.isDirectory) {
            scanDirectoryRecursively(
                rootDir = configDir,
                prefix = ".config/xournalpp",
                scope = BackupScope.CONFIG.id,
                allowedExtensions = exclusionFilter.includedExtensions ?: DEFAULT_CONFIG_EXTENSIONS,
                results = results,
                cachedMetadata = cachedMetadata
            )
        }

        return results
    }

    /**
     * Scans a custom mapped local folder.
     */
    fun scanCustomMapping(
        mapping: CustomFolderMapping,
        cachedMetadata: Map<String, SyncMetadataEntity>? = null
    ): List<ScannedLocalFile> {
        val results = mutableListOf<ScannedLocalFile>()
        val localDir = File(mapping.localFolderPath)
        if (localDir.exists() && localDir.isDirectory) {
            scanDirectoryRecursively(
                rootDir = localDir,
                prefix = "",
                scope = "custom_${mapping.id}",
                allowedExtensions = exclusionFilter.includedExtensions, // null means allow all valid non-excluded
                results = results,
                cachedMetadata = cachedMetadata
            )
        }
        return results
    }

    private fun scanDirectoryRecursively(
        rootDir: File,
        prefix: String,
        scope: String,
        allowedExtensions: Set<String>?,
        results: MutableList<ScannedLocalFile>,
        cachedMetadata: Map<String, SyncMetadataEntity>? = null
    ) {
        val files = rootDir.walkTopDown().filter { it.isFile }.toList()
        for (file in files) {
            if (shouldExcludeFile(file, rootDir)) {
                continue
            }

            val extension = file.extension.lowercase()
            if (allowedExtensions != null && extension !in allowedExtensions) {
                continue
            }

            val relFromRoot = file.relativeTo(rootDir).path.replace('\\', '/')
            val relativePath = if (prefix.isNotEmpty()) "$prefix/$relFromRoot" else relFromRoot
            val size = file.length()
            val lastModified = file.lastModified()

            val cached = cachedMetadata?.get(relativePath)
            val hash = if (cached != null && cached.sizeBytes == size && cached.localLastModified == lastModified && cached.localSha256.isNotEmpty()) {
                cached.localSha256
            } else {
                calculateSha256(file)
            }

            if (hash.isNotEmpty()) {
                results.add(
                    ScannedLocalFile(
                        file = file,
                        scope = scope,
                        relativePath = relativePath,
                        sizeBytes = size,
                        lastModified = lastModified,
                        sha256 = hash
                    )
                )
            }
        }
    }

    /**
     * Evaluates file against exclusion filters and transient patterns.
     */
    fun shouldExcludeFile(file: File, rootDir: File): Boolean {
        val name = file.name
        val extension = file.extension.lowercase()
        val path = file.absolutePath

        // 1. Transient and lock files
        if (exclusionFilter.skipDefaultTransient) {
            if (name.endsWith(".autosave.xopp") || name.endsWith(".xopp~") || name.endsWith(".tmp") || name.endsWith(".swp") || name.endsWith(".sock")) {
                return true
            }
            if (name == ".X0-lock" || name == ".X11-unix" || name == "bootstrap_installed.ver" || name.startsWith("quarantined_emergencysave.xopp")) {
                return true
            }
            // Skip hidden dot-files in notes unless explicit
            if (name.startsWith(".") && !name.startsWith(".folder.json")) {
                return true
            }
        }

        // 2. Excluded extensions
        if (extension in exclusionFilter.excludedExtensions) {
            return true
        }

        // 3. Excluded folder paths
        for (excludedFolder in exclusionFilter.excludedFolderPaths) {
            if (path.startsWith(excludedFolder)) {
                return true
            }
        }

        // 4. Custom regex patterns
        for (pattern in compiledRegexes) {
            if (pattern.matcher(name).find() || pattern.matcher(path).find()) {
                return true
            }
        }

        return false
    }

    private fun calculateSha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute SHA-256 for ${file.absolutePath}", e)
            ""
        }
    }
}
