package dev.ilamparithi.aournalpp.backup.model

/**
 * Supported remote storage providers and protocols.
 * Ordered with dedicated services first (alphabetically), then standard protocols (alphabetically).
 */
enum class StorageProviderType(
    val id: String,
    val displayName: String,
    val isDedicatedService: Boolean,
    val defaultPort: Int? = null,
    val defaultScheme: String = "https"
) {
    // Dedicated Cloud Services (Alphabetical)
    GOOGLE_DRIVE("gdrive", "Google Drive", isDedicatedService = true, defaultPort = null, defaultScheme = "https"),
    NEXTCLOUD("nextcloud", "Nextcloud", isDedicatedService = true, defaultPort = 443, defaultScheme = "https"),

    // Standard Protocols (Alphabetical)
    FTP("ftp", "FTP / FTPS", isDedicatedService = false, defaultPort = 21, defaultScheme = "ftp"),
    SFTP("sftp", "SFTP", isDedicatedService = false, defaultPort = 22, defaultScheme = "sftp"),
    SMB3("smb3", "SMB3 / Samba", isDedicatedService = false, defaultPort = 445, defaultScheme = "smb"),
    WEBDAV("webdav", "WebDAV", isDedicatedService = false, defaultPort = 443, defaultScheme = "https");

    companion object {
        fun fromId(id: String): StorageProviderType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: WEBDAV
        }

        /**
         * Returns all provider types ordered with dedicated services alphabetically first,
         * followed by standard protocols alphabetically.
         */
        fun getOrderedTypes(): List<StorageProviderType> {
            val services = entries.filter { it.isDedicatedService }.sortedBy { it.displayName }
            val protocols = entries.filter { !it.isDedicatedService }.sortedBy { it.displayName }
            return services + protocols
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
    val id: String = java.util.UUID.randomUUID().toString(),
    val serviceId: String,
    val name: String = "",
    val localFolderPath: String,
    val remoteFolderPath: String,
    val isEnabled: Boolean = true
)

/**
 * Item within a reusable mapping template set.
 */
data class MappingTemplateItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val localFolderPath: String,
    val remoteFolderPath: String,
    val isEnabled: Boolean = true
)

/**
 * Named reusable profile or template containing multiple folder mapping items.
 */
data class MappingSet(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val items: List<MappingTemplateItem> = emptyList()
)

/**
 * Result of checking whether local and remote folders in a custom mapping exist.
 */
data class FolderValidationResult(
    val mappingId: String,
    val localExists: Boolean,
    val remoteExists: Boolean,
    val checkedAtEpochMs: Long = System.currentTimeMillis()
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
    val refreshToken: String = "",
    val tokenExpiryEpochMs: Long = 0L,
    val accountIdentifier: String = "", // e.g. email or username for uniqueness checks
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
) {
    /**
     * Generates a deterministic unique key identifying the account/endpoint.
     * Prevents multiple configurations from targeting the exact same account on the same server.
     */
    fun getAccountKey(): String {
        return when (providerType) {
            StorageProviderType.GOOGLE_DRIVE -> {
                val acc = accountIdentifier.ifBlank { username }.ifBlank { name }.trim().lowercase()
                "gdrive::$acc"
            }
            StorageProviderType.NEXTCLOUD,
            StorageProviderType.WEBDAV -> {
                val cleanUrl = serverUrl.trim().trimEnd('/').lowercase()
                val user = username.trim().lowercase()
                "${providerType.id}::$cleanUrl::$user"
            }
            StorageProviderType.SFTP,
            StorageProviderType.FTP -> {
                val h = host.trim().lowercase()
                val user = username.trim().lowercase()
                "${providerType.id}::$h:$port::$user"
            }
            StorageProviderType.SMB3 -> {
                val h = host.trim().lowercase()
                val share = shareName.trim().lowercase()
                val user = username.trim().lowercase()
                "smb3::$h:$port/$share::$user"
            }
        }
    }
}

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

/**
 * Sync comparison status between local workspace config and remote cloud backup.
 */
enum class ConfigSyncStatus {
    IN_SYNC,
    MISMATCH,
    NO_LOCAL_CONFIG,
    NO_REMOTE_CONFIG
}

/**
 * Origin source for a file version.
 */
sealed class FileVersionSource {
    data object LOCAL : FileVersionSource()
    data class REMOTE(
        val serviceId: String,
        val serviceName: String,
        val providerType: StorageProviderType,
        val mappingId: String? = null,
        val mappingRemotePath: String? = null
    ) : FileVersionSource()

    val displayName: String
        get() = when (this) {
            is LOCAL -> "This Device (Local)"
            is REMOTE -> {
                val cleanMapping = mappingRemotePath?.trim()?.trim('/')
                if (!cleanMapping.isNullOrEmpty()) {
                    "$serviceName ($cleanMapping)"
                } else {
                    serviceName
                }
            }
        }

    val sanitizedFileSuffix: String
        get() = when (this) {
            is LOCAL -> "Local"
            is REMOTE -> {
                val cleanMapping = mappingRemotePath?.trim()?.trim('/')?.replace('/', '_')
                if (!cleanMapping.isNullOrEmpty()) {
                    "$serviceName - $cleanMapping"
                } else {
                    serviceName
                }
            }
        }
}

/**
 * Representation of a specific version of a file (local or on a cloud service).
 */
data class FileVersionItem(
    val source: FileVersionSource,
    val fileName: String,
    val relativePath: String,
    val localFilePath: String,
    val sizeBytes: Long,
    val lastModifiedEpochMs: Long,
    val contentHash: String? = null,
    val remotePath: String? = null
)

/**
 * Group of conflicting versions for a specific relative file path.
 */
data class FileConflictGroup(
    val id: String = java.util.UUID.randomUUID().toString(),
    val relativePath: String,
    val localVersion: FileVersionItem?,
    val remoteVersions: List<FileVersionItem>,
    val description: String? = null,
    val localFilePath: String? = null,
    val remoteFilePath: String? = null
) {
    val allVersions: List<FileVersionItem>
        get() = listOfNotNull(localVersion) + remoteVersions

    val fileName: String
        get() = allVersions.firstOrNull()?.fileName ?: java.io.File(relativePath).name
}

/**
 * Action chosen by the user to resolve a file conflict.
 */
sealed class ConflictResolutionAction {
    data class ChoosePrimary(val chosenVersion: FileVersionItem) : ConflictResolutionAction()
    data class KeepAlongside(val versionsToKeep: List<FileVersionItem>) : ConflictResolutionAction()
    data class ResolveSelection(
        val primaryVersion: FileVersionItem,
        val alongsideVersions: List<FileVersionItem>
    ) : ConflictResolutionAction()
    data object KeepBoth : ConflictResolutionAction()
    data object Skip : ConflictResolutionAction()
}

/**
 * Resolution instruction for a conflict group.
 */
data class FileConflictResolution(
    val conflictGroupId: String,
    val relativePath: String,
    val action: ConflictResolutionAction
)

/**
 * Result report after executing conflict resolutions.
 */
data class ConflictResolutionReport(
    val filesUpdated: Int,
    val filesSavedAlongside: Int,
    val filesSkipped: Int,
    val errors: List<String> = emptyList()
) {
    val isSuccess: Boolean get() = errors.isEmpty()
}

