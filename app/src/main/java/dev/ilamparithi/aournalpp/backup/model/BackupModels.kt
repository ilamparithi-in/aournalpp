package dev.ilamparithi.aournalpp.backup.model

/**
 * Supported remote storage providers and protocols.
 */
enum class StorageProviderType(
    val id: String,
    val displayName: String,
    val defaultPort: Int? = null,
    val defaultScheme: String = "https"
) {
    NEXTCLOUD("nextcloud", "Nextcloud", 443, "https"),
    WEBDAV("webdav", "Generic WebDAV", 443, "https"),
    GOOGLE_DRIVE("gdrive", "Google Drive", null, "https"),
    SFTP("sftp", "Generic SFTP", 22, "sftp"),
    SMB3("smb3", "Generic SMB3 / Samba", 445, "smb"),
    FTP("ftp", "Generic FTP / FTPS", 21, "ftp");

    companion object {
        fun fromId(id: String): StorageProviderType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: WEBDAV
        }
    }
}

/**
 * Metadata representation of a file or directory on a remote storage provider.
 */
data class RemoteFileMetadata(
    val remotePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModifiedEpochMs: Long,
    val contentHash: String? = null // ETag, MD5, or SHA-256
)

/**
 * Target domain scopes for complete backup mirroring.
 */
enum class BackupScope(val id: String, val displayName: String) {
    NOTES("notes", "Notes & Documents (~/Notes)"),
    CONFIG("config", "Settings & Palettes (~/.config/xournalpp)")
}

/**
 * Policy for resolving file collisions during cloud restore.
 */
enum class ConflictResolutionPolicy(val id: String, val displayName: String, val description: String) {
    KEEP_NEWER(
        id = "keep_newer",
        displayName = "Keep Newer (Timestamp-based)",
        description = "Overwrite only if the incoming file has a newer last-modified timestamp"
    ),
    OVERWRITE_LOCAL(
        id = "overwrite_local",
        displayName = "Overwrite Local",
        description = "Always overwrite local files with the remote cloud versions"
    ),
    SKIP_CONFLICTS(
        id = "skip_conflicts",
        displayName = "Skip Conflicts",
        description = "Do not overwrite existing local files; only download missing files"
    );

    companion object {
        fun fromId(id: String): ConflictResolutionPolicy {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: KEEP_NEWER
        }
    }
}

/**
 * Custom mapping between a local folder and a remote directory.
 */
data class CustomFolderMapping(
    val id: String,
    val serviceId: String,
    val localFolderPath: String,
    val remoteFolderPath: String,
    val isEnabled: Boolean = true
)

/**
 * Configurable exclusion filter for scanning local files.
 */
data class ExclusionFilterConfig(
    val regexPatterns: List<String> = emptyList(),
    val excludedExtensions: Set<String> = emptySet(),
    val includedExtensions: Set<String>? = null, // null means all allowed by scope
    val excludedFolderPaths: Set<String> = emptySet(),
    val skipDefaultTransient: Boolean = true
) {
    companion object {
        val DEFAULT = ExclusionFilterConfig(
            regexPatterns = listOf(
                "^\\..*\\.autosave\\.xopp$",
                "^\\..*\\.xopp~$",
                "^\\.X0-lock$",
                "^\\.X11-unix$",
                "^bootstrap_installed\\.ver$"
            ),
            excludedExtensions = setOf("sock", "swp", "tmp", "cache", "log"),
            skipDefaultTransient = true
        )
    }
}

/**
 * Configuration parameters for a configured cloud storage service.
 */
data class ServiceConfig(
    val id: String,
    val name: String,
    val providerType: StorageProviderType,
    val serverUrl: String = "",
    val host: String = "",
    val port: Int = providerType.defaultPort ?: 443,
    val username: String = "",
    val passwordOrSecret: String = "",
    val privateKey: String = "",
    val privateKeyPassphrase: String = "",
    val authToken: String = "",
    val shareName: String = "",
    val domain: String = "",
    val remoteBasePath: String = "",
    val isFtpsImplicit: Boolean = false,
    val isFtpsExplicit: Boolean = true,
    val isCompleteBackupEnabled: Boolean = true,
    val isEnabled: Boolean = true,
    val lastSyncedAtEpochMs: Long = 0L,
    val lastSyncStatus: String? = null,
    val customMappings: List<CustomFolderMapping> = emptyList()
)

/**
 * Summary result of a completed or partial backup operation.
 */
data class BackupResult(
    val serviceId: String,
    val serviceName: String,
    val totalFilesScanned: Int,
    val filesUploaded: Int,
    val filesSkipped: Int,
    val filesFailed: Int,
    val totalBytesTransferred: Long,
    val durationMs: Long,
    val errors: List<String> = emptyList()
) {
    val isSuccess: Boolean get() = filesFailed == 0 && errors.isEmpty()
}

/**
 * Summary result of a cloud restore operation.
 */
data class RestoreResult(
    val serviceId: String,
    val serviceName: String,
    val totalFilesDiscovered: Int,
    val filesRestored: Int,
    val filesSkipped: Int,
    val filesFailed: Int,
    val totalBytesDownloaded: Long,
    val durationMs: Long,
    val errors: List<String> = emptyList()
) {
    val isSuccess: Boolean get() = filesFailed == 0 && errors.isEmpty()
}
