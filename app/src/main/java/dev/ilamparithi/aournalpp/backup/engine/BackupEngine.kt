package dev.ilamparithi.aournalpp.backup.engine

import android.content.Context
import android.util.Log
import dev.ilamparithi.aournalpp.backup.db.SyncDatabase
import dev.ilamparithi.aournalpp.backup.db.SyncMetadataEntity
import dev.ilamparithi.aournalpp.backup.model.BackupResult
import dev.ilamparithi.aournalpp.backup.model.ConflictResolutionAction
import dev.ilamparithi.aournalpp.backup.model.ConflictResolutionPolicy
import dev.ilamparithi.aournalpp.backup.model.ConflictResolutionReport
import dev.ilamparithi.aournalpp.backup.model.CustomFolderMapping
import dev.ilamparithi.aournalpp.backup.model.FileConflictGroup
import dev.ilamparithi.aournalpp.backup.model.FileConflictResolution
import dev.ilamparithi.aournalpp.backup.model.FileVersionItem
import dev.ilamparithi.aournalpp.backup.model.FileVersionSource
import dev.ilamparithi.aournalpp.backup.model.RemoteFileMetadata
import dev.ilamparithi.aournalpp.backup.model.RestoreResult
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.TransferDirection
import dev.ilamparithi.aournalpp.backup.model.TransferItem
import dev.ilamparithi.aournalpp.backup.model.TransferStatus
import dev.ilamparithi.aournalpp.backup.provider.CloudStorageProvider
import dev.ilamparithi.aournalpp.backup.provider.StorageProviderFactory
import dev.ilamparithi.aournalpp.backup.queue.FileTransferQueueManager
import dev.ilamparithi.aournalpp.backup.scanner.BackupScanner
import dev.ilamparithi.aournalpp.backup.scanner.ScannedLocalFile
import dev.ilamparithi.aournalpp.backup.security.CredentialsVault
import dev.ilamparithi.aournalpp.runtime.CrossProcessLock
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import org.json.JSONArray
import org.json.JSONObject
import dev.ilamparithi.aournalpp.backup.model.ConfigSyncStatus
import dev.ilamparithi.aournalpp.backup.model.BackupScope

/**
 * Core differential synchronization engine supporting multi-service complete backups,
 * custom folder mappings, concurrency management, and conflict-aware cloud restores.
 */
class BackupEngine(
    private val context: Context,
    private val env: LinuxEnvironment = LinuxEnvironment(context),
    private val vault: CredentialsVault = CredentialsVault.getInstance(context),
    private val db: SyncDatabase = SyncDatabase.getInstance(context)
) {
    companion object {
        private const val TAG = "BackupEngine"
        const val COMPLETE_BACKUP_REMOTE_ROOT = "Aournalpp"
        private const val SYNC_LOCK_FILE_NAME = ".cloud_sync.lock"

        fun getCompleteBackupRemoteRoot(serviceConfig: ServiceConfig): String {
            return serviceConfig.remoteBasePath.trim().trim('/').ifBlank { COMPLETE_BACKUP_REMOTE_ROOT }
        }

        fun isConfigFile(pathOrName: String): Boolean =
            ConfigDiffSyncEngine.isConfigFile(pathOrName)

        fun hasContentChanges(f1: File, f2: File): Boolean =
            ConfigDiffSyncEngine.hasContentChanges(f1, f2)

        fun areJsonFilesEqual(f1: File, f2: File): Boolean =
            ConfigDiffSyncEngine.areJsonFilesEqual(f1, f2)

        fun areJsonObjectsEqual(j1: JSONObject, j2: JSONObject): Boolean =
            ConfigDiffSyncEngine.areJsonObjectsEqual(j1, j2)

        fun areJsonValuesEqual(v1: Any?, v2: Any?): Boolean =
            ConfigDiffSyncEngine.areJsonValuesEqual(v1, v2)

        fun areJsonArraysEqual(a1: JSONArray, a2: JSONArray): Boolean =
            ConfigDiffSyncEngine.areJsonArraysEqual(a1, a2)

        fun calculateFileHash(file: File): String =
            ConfigDiffSyncEngine.calculateFileHash(file)

        /**
         * Resolves [subPath] safely relative to [baseDir], strictly preventing path traversal.
         * @throws SecurityException if directory traversal sequences attempt to escape [baseDir].
         */
        fun resolveSafeChild(baseDir: File, subPath: String): File {
            val cleanSubPath = subPath.trim().trim('/')
            if (cleanSubPath.isEmpty()) {
                throw SecurityException("Empty subpath provided")
            }
            val canonicalBase = baseDir.canonicalFile
            val targetFile = File(baseDir, cleanSubPath)
            val canonicalTarget = targetFile.canonicalFile
            if (!canonicalTarget.toPath().startsWith(canonicalBase.toPath())) {
                throw SecurityException("Path traversal detected: subpath '$subPath' escapes base directory '${baseDir.path}'")
            }
            return canonicalTarget
        }
    }

    private val syncLockFile get() = File(context.filesDir, SYNC_LOCK_FILE_NAME)

    fun isSyncInProgress(): Boolean = CrossProcessLock.isLocked(syncLockFile)

    /**
     * Executes backup for a specific service configuration with cross-process concurrency locking.
     */
    suspend fun performBackup(
        serviceConfig: ServiceConfig,
        concurrency: Int? = null,
        onProgress: ((current: Int, total: Int, currentFile: String) -> Unit)? = null,
        clearCompletedQueue: Boolean = true
    ): BackupResult {
        if (concurrency != null) {
            FileTransferQueueManager.setConcurrencyWorkers(concurrency)
        }
        val lock = CrossProcessLock.tryAcquire(syncLockFile)
        if (lock == null) {
            Log.w(TAG, "Cloud sync is already active in another window or process. Skipping concurrent sync for ${serviceConfig.name}.")
            return BackupResult(
                serviceId = serviceConfig.id,
                serviceName = serviceConfig.name,
                totalFilesScanned = 0,
                filesUploaded = 0,
                filesSkipped = 0,
                filesFailed = 0,
                totalBytesTransferred = 0L,
                durationMs = 0L,
                errors = listOf("Sync already in progress in another window or process")
            )
        }
        try {
            return performBackupInternal(serviceConfig, concurrency, onProgress, clearCompletedQueue)
        } finally {
            lock.release()
        }
    }

    private suspend fun performBackupInternal(
        serviceConfig: ServiceConfig,
        concurrency: Int? = null,
        onProgress: ((current: Int, total: Int, currentFile: String) -> Unit)? = null,
        clearCompletedQueue: Boolean = true
    ): BackupResult = withContext(Dispatchers.IO) {
        if (concurrency != null) {
            FileTransferQueueManager.setConcurrencyWorkers(concurrency)
        }
        if (clearCompletedQueue) {
            FileTransferQueueManager.clearCompleted()
        }
        val startTime = System.currentTimeMillis()
        val exclusionFilter = vault.getExclusionFilter()
        val scanner = BackupScanner(env, exclusionFilter)
        val dao = db.syncMetadataDao()

        // Batch-retrieve all existing metadata for this service upfront
        val allMetadata = dao.getAllForService(serviceConfig.id)
        val existingMetaMap = allMetadata.associateBy { "${it.scope}:${it.relativePath}" }

        val filesToSync = mutableListOf<Pair<ScannedLocalFile, String>>() // (ScannedFile, remoteDestinationPath)
        val seenRemotePaths = mutableSetOf<String>()

        // 1. Complete Backup domain scanning
        if (serviceConfig.isCompleteBackupEnabled) {
            val completeCache = allMetadata
                .filter { it.scope == BackupScope.NOTES.id || it.scope == BackupScope.CONFIG.id }
                .associateBy { it.relativePath }
            val completeFiles = scanner.scanCompleteBackup(completeCache)
            val remoteRoot = getCompleteBackupRemoteRoot(serviceConfig)
            for (f in completeFiles) {
                val remotePath = "$remoteRoot/${f.relativePath}"
                if (seenRemotePaths.add(remotePath)) {
                    filesToSync.add(f to remotePath)
                }
            }
        }

        // 2. Custom folder mappings scanning
        val activeMappings = serviceConfig.customMappings.ifEmpty {
            try {
                dev.ilamparithi.aournalpp.backup.security.CustomMappingRepository(context).getMappingsForService(serviceConfig.id)
            } catch (_: Exception) {
                emptyList()
            }
        }
        for (mapping in activeMappings) {
            if (!mapping.isEnabled) continue
            val mappingScope = "custom_${mapping.id}"
            val mappingCache = allMetadata
                .filter { it.scope == mappingScope }
                .associateBy { it.relativePath }
            val mappedFiles = scanner.scanCustomMapping(mapping, mappingCache)
            val remoteTargetBase = mapping.remoteFolderPath.trim().trim('/')
            for (f in mappedFiles) {
                val remotePath = if (remoteTargetBase.isEmpty()) f.relativePath else "$remoteTargetBase/${f.relativePath}"
                if (seenRemotePaths.add(remotePath)) {
                    filesToSync.add(f to remotePath)
                }
            }
        }

        val totalFiles = filesToSync.size
        var uploadedCount = 0
        var skippedCount = 0
        var failedCount = 0
        var totalBytesTransferred = 0L
        val errors = mutableListOf<String>()

        val provider = getStorageProvider(serviceConfig)

        try {
            // Test connection first
            val connResult = provider.testConnection()
            if (connResult.isFailure || connResult.getOrNull() == false) {
                val err = connResult.exceptionOrNull()?.message ?: "Failed to connect to ${serviceConfig.name}"
                errors.add(err)
                vault.saveService(
                    serviceConfig.copy(
                        lastSyncStatus = "Connection failed: $err"
                    )
                )
                return@withContext BackupResult(
                    serviceId = serviceConfig.id,
                    serviceName = serviceConfig.name,
                    totalFilesScanned = totalFiles,
                    filesUploaded = 0,
                    filesSkipped = 0,
                    filesFailed = totalFiles,
                    totalBytesTransferred = 0L,
                    durationMs = System.currentTimeMillis() - startTime,
                    errors = errors
                )
            }

            // Differential comparison via in-memory metadata lookup
            val uploadQueue = mutableListOf<Pair<ScannedLocalFile, String>>()
            val cacheDir = File(context.cacheDir, "config_diff_cache").apply { if (!exists()) mkdirs() }

            for ((scanned, remotePath) in filesToSync) {
                val record = existingMetaMap["${scanned.scope}:${scanned.relativePath}"]
                val isConfig = isConfigFile(scanned.relativePath)

                // 1. Content hash matches previous sync record: 0 diff changes -> skip
                if (record != null && record.localSha256 == scanned.sha256) {
                    skippedCount++
                    if (scanned.lastModified != record.localLastModified) {
                        dao.insertOrUpdate(record.copy(localLastModified = scanned.lastModified))
                    }
                    continue
                }

                // 2. For config files (x11_prefs, settings, app_settings), check diff against remote instead of accessed/modified date
                if (isConfig) {
                    val tempRemoteFile = File(cacheDir, "remote_${serviceConfig.id}_${scanned.file.name}")
                    val downloadRes = try {
                        provider.downloadFile(remotePath, tempRemoteFile) { _, _ -> }
                    } catch (_: Exception) {
                        Result.failure(Exception("Download failed"))
                    }

                    if (downloadRes.isSuccess && tempRemoteFile.exists() && tempRemoteFile.length() > 0L) {
                        if (!hasContentChanges(scanned.file, tempRemoteFile)) {
                            // Remote config content matches local file (0 diff changes) -> Skip upload
                            skippedCount++
                            dao.insertOrUpdate(
                                SyncMetadataEntity(
                                    serviceId = serviceConfig.id,
                                    relativePath = scanned.relativePath,
                                    scope = scanned.scope,
                                    localSha256 = scanned.sha256,
                                    remoteHash = null,
                                    localLastModified = scanned.lastModified,
                                    sizeBytes = scanned.sizeBytes,
                                    lastSyncedAt = System.currentTimeMillis()
                                )
                            )
                            tempRemoteFile.delete()
                            continue
                        }
                    }
                    tempRemoteFile.delete()
                }

                uploadQueue.add(scanned to remotePath)
            }

            // Enqueue all active upload items with deterministic IDs
            val transferItems = uploadQueue.map { (scanned, remotePath) ->
                val id = "${serviceConfig.id}_${TransferDirection.UPLOAD.name}_$remotePath"
                TransferItem(
                    id = id,
                    serviceId = serviceConfig.id,
                    serviceName = serviceConfig.name,
                    localFilePath = scanned.file.absolutePath,
                    remotePath = remotePath,
                    fileName = scanned.file.name,
                    direction = TransferDirection.UPLOAD,
                    totalBytes = scanned.sizeBytes,
                    status = TransferStatus.QUEUED,
                    scope = scanned.scope,
                    relativePath = scanned.relativePath
                ) to (scanned to remotePath)
            }

            FileTransferQueueManager.enqueueAll(transferItems.map { it.first })

            val processedCounter = AtomicInteger(skippedCount)

            processWithDynamicConcurrency(
                items = transferItems,
                concurrencyFlow = FileTransferQueueManager.concurrencyWorkers
            ) { (item, payload) ->
                val (scanned, remotePath) = payload
                if (FileTransferQueueManager.isCancelled(item.id) || FileTransferQueueManager.isPaused(item.id)) {
                    return@processWithDynamicConcurrency
                }

                FileTransferQueueManager.markStarted(item.id)
                val currentProcessed = processedCounter.incrementAndGet()
                onProgress?.invoke(currentProcessed, totalFiles, scanned.file.name)

                val uploadResult = try {
                    provider.uploadFile(
                        localFile = scanned.file,
                        remotePath = remotePath,
                        onProgress = { transferred, total ->
                            if (FileTransferQueueManager.isPaused(item.id)) {
                                throw java.io.IOException("Transfer paused by user")
                            }
                            if (FileTransferQueueManager.isCancelled(item.id)) {
                                throw java.io.IOException("Transfer cancelled by user")
                            }
                            FileTransferQueueManager.updateProgress(item.id, transferred, total)
                        }
                    )
                } catch (e: Exception) {
                    Result.failure(e)
                }

                if (FileTransferQueueManager.isPaused(item.id)) {
                    return@processWithDynamicConcurrency
                }
                if (FileTransferQueueManager.isCancelled(item.id)) {
                    return@processWithDynamicConcurrency
                }

                if (uploadResult.isSuccess) {
                    FileTransferQueueManager.markCompleted(item.id)
                    synchronized(this@BackupEngine) {
                        uploadedCount++
                        totalBytesTransferred += scanned.sizeBytes
                    }
                    dao.insertOrUpdate(
                        SyncMetadataEntity(
                            serviceId = serviceConfig.id,
                            relativePath = scanned.relativePath,
                            scope = scanned.scope,
                            localSha256 = scanned.sha256,
                            remoteHash = null,
                            localLastModified = scanned.lastModified,
                            sizeBytes = scanned.sizeBytes,
                            lastSyncedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    val errMsg = uploadResult.exceptionOrNull()?.message ?: "Upload failed"
                    FileTransferQueueManager.markFailed(item.id, errMsg)
                    synchronized(this@BackupEngine) {
                        failedCount++
                        errors.add("${scanned.file.name}: $errMsg")
                    }
                }
            }
        } finally {
            provider.disconnect()
        }

        val resultStatus = if (failedCount == 0 && errors.isEmpty()) "Success" else "Completed with $failedCount errors"
        vault.saveService(
            serviceConfig.copy(
                lastSyncedAtEpochMs = System.currentTimeMillis(),
                lastSyncStatus = resultStatus
            )
        )

        return@withContext BackupResult(
            serviceId = serviceConfig.id,
            serviceName = serviceConfig.name,
            totalFilesScanned = totalFiles,
            filesUploaded = uploadedCount,
            filesSkipped = skippedCount,
            filesFailed = failedCount,
            totalBytesTransferred = totalBytesTransferred,
            durationMs = System.currentTimeMillis() - startTime,
            errors = errors
        )
    }

    /**
     * Restores files from the remote service to local storage applying the specified conflict policy.
     */
    suspend fun performRestore(
        serviceConfig: ServiceConfig,
        conflictPolicy: ConflictResolutionPolicy = ConflictResolutionPolicy.KEEP_NEWER,
        concurrency: Int? = null,
        onProgress: ((current: Int, total: Int, currentFile: String) -> Unit)? = null,
        clearCompletedQueue: Boolean = true
    ): RestoreResult = withContext(Dispatchers.IO) {
        if (concurrency != null) {
            FileTransferQueueManager.setConcurrencyWorkers(concurrency)
        }
        if (clearCompletedQueue) {
            FileTransferQueueManager.clearCompleted()
        }
        val startTime = System.currentTimeMillis()
        val dao = db.syncMetadataDao()
        val provider = getStorageProvider(serviceConfig)
        val errors = mutableListOf<String>()

        var restoredCount = 0
        var skippedCount = 0
        var failedCount = 0
        var totalBytesDownloaded = 0L
        var hasRestoredConfigs = false
        val remoteFilesToDownload = mutableListOf<Triple<RemoteFileMetadata, String, File>>() // (rf, remotePath, localDestinationFile)
        val seenDownloadRemotePaths = mutableSetOf<String>()

        try {
            val connResult = provider.testConnection()
            if (connResult.isFailure || connResult.getOrNull() == false) {
                val err = connResult.exceptionOrNull()?.message ?: "Failed to connect to ${serviceConfig.name}"
                errors.add(err)
                return@withContext RestoreResult(
                    serviceId = serviceConfig.id,
                    serviceName = serviceConfig.name,
                    totalFilesDiscovered = 0,
                    filesRestored = 0,
                    filesSkipped = 0,
                    filesFailed = 0,
                    totalBytesDownloaded = 0L,
                    durationMs = System.currentTimeMillis() - startTime,
                    errors = errors
                )
            }

            if (serviceConfig.isCompleteBackupEnabled) {
                val notesRoot = env.getNotesDirectory()
                val notesConfigDir = File(notesRoot, ".config")
                val remoteRoot = getCompleteBackupRemoteRoot(serviceConfig)

                // 1. List Notes tree
                val remoteNotes = listRemoteRecursively(provider, "$remoteRoot/Notes")
                for (rf in remoteNotes) {
                    if (rf.isDirectory) continue
                    val subPath = rf.remotePath.removePrefix("$remoteRoot/Notes").trim('/')
                    if (subPath.isEmpty()) continue
                    val destFile = try {
                        resolveSafeChild(notesRoot, subPath)
                    } catch (secEx: SecurityException) {
                        Log.e(TAG, "Security: Path traversal rejected for remote path: ${rf.remotePath}", secEx)
                        errors.add("Security violation rejected: ${rf.remotePath}")
                        continue
                    }
                    if (seenDownloadRemotePaths.add(rf.remotePath)) {
                        remoteFilesToDownload.add(Triple(rf, rf.remotePath, destFile))
                    }
                }

                // 2. List .config tree (downloads all files including root configs and xournalpp subfolder)
                val remoteConfigs = listRemoteRecursively(provider, "$remoteRoot/.config")
                val addedRemotePaths = mutableSetOf<String>()
                for (rf in remoteConfigs) {
                    if (rf.isDirectory) continue
                    val subPath = rf.remotePath.removePrefix("$remoteRoot/.config").trim('/')
                    if (subPath.isEmpty()) continue
                    val destFile = try {
                        resolveSafeChild(notesConfigDir, subPath)
                    } catch (secEx: SecurityException) {
                        Log.e(TAG, "Security: Path traversal rejected for remote path: ${rf.remotePath}", secEx)
                        errors.add("Security violation rejected: ${rf.remotePath}")
                        continue
                    }
                    if (seenDownloadRemotePaths.add(rf.remotePath)) {
                        remoteFilesToDownload.add(Triple(rf, rf.remotePath, destFile))
                        addedRemotePaths.add(rf.remotePath)
                    }
                }

                // Fallback for legacy backups where only $remoteRoot/.config/xournalpp was backed up
                val legacyConfigs = listRemoteRecursively(provider, "$remoteRoot/.config/xournalpp")
                for (rf in legacyConfigs) {
                    if (rf.isDirectory || rf.remotePath in addedRemotePaths) continue
                    val subPath = rf.remotePath.removePrefix("$remoteRoot/.config/xournalpp").trim('/')
                    if (subPath.isEmpty()) continue
                    val destFile = try {
                        resolveSafeChild(File(notesConfigDir, "xournalpp"), subPath)
                    } catch (secEx: SecurityException) {
                        Log.e(TAG, "Security: Path traversal rejected for remote path: ${rf.remotePath}", secEx)
                        errors.add("Security violation rejected: ${rf.remotePath}")
                        continue
                    }
                    if (seenDownloadRemotePaths.add(rf.remotePath)) {
                        remoteFilesToDownload.add(Triple(rf, rf.remotePath, destFile))
                    }
                }
            }

            // Custom Folder Mappings
            for (mapping in serviceConfig.customMappings) {
                if (!mapping.isEnabled) continue
                val localBase = File(mapping.localFolderPath)
                val remoteBase = mapping.remoteFolderPath.trim().trim('/')
                val remoteMapped = listRemoteRecursively(provider, remoteBase)
                for (rf in remoteMapped) {
                    if (rf.isDirectory) continue
                    val subPath = if (remoteBase.isNotEmpty()) rf.remotePath.removePrefix(remoteBase).trim('/') else rf.remotePath
                    if (subPath.isEmpty()) continue
                    val destFile = try {
                        resolveSafeChild(localBase, subPath)
                    } catch (secEx: SecurityException) {
                        Log.e(TAG, "Security: Path traversal rejected for custom mapping remote path: ${rf.remotePath}", secEx)
                        errors.add("Security violation rejected: ${rf.remotePath}")
                        continue
                    }
                    if (seenDownloadRemotePaths.add(rf.remotePath)) {
                        remoteFilesToDownload.add(Triple(rf, rf.remotePath, destFile))
                    }
                }
            }

            val totalDiscovered = remoteFilesToDownload.size

            // Apply Conflict Policy
            val downloadQueue = mutableListOf<Pair<String, File>>()
            for ((rf, remotePath, localFile) in remoteFilesToDownload) {
                if (!localFile.exists()) {
                    downloadQueue.add(remotePath to localFile)
                } else {
                    when (conflictPolicy) {
                        ConflictResolutionPolicy.OVERWRITE_LOCAL -> {
                            downloadQueue.add(remotePath to localFile)
                        }
                        ConflictResolutionPolicy.SKIP_CONFLICTS -> {
                            skippedCount++
                        }
                        ConflictResolutionPolicy.KEEP_NEWER -> {
                            if (isConfigFile(localFile.name)) {
                                val cacheDir = File(context.cacheDir, "config_diff_cache").apply { if (!exists()) mkdirs() }
                                val tempRemote = File(cacheDir, "remote_restore_${serviceConfig.id}_${localFile.name}")
                                val dl = try {
                                    provider.downloadFile(remotePath, tempRemote) { _, _ -> }
                                } catch (_: Exception) {
                                    Result.failure(Exception("Download failed"))
                                }
                                if (dl.isSuccess && tempRemote.exists() && !hasContentChanges(localFile, tempRemote)) {
                                    skippedCount++
                                    tempRemote.delete()
                                    continue
                                }
                                tempRemote.delete()
                            }
                            if (rf.lastModifiedEpochMs > localFile.lastModified()) {
                                downloadQueue.add(remotePath to localFile)
                            } else {
                                skippedCount++
                            }
                        }
                    }
                }
            }

            val transferItems = downloadQueue.map { (remotePath, localFile) ->
                val id = "${serviceConfig.id}_${TransferDirection.DOWNLOAD.name}_$remotePath"
                TransferItem(
                    id = id,
                    serviceId = serviceConfig.id,
                    serviceName = serviceConfig.name,
                    localFilePath = localFile.absolutePath,
                    remotePath = remotePath,
                    fileName = localFile.name,
                    direction = TransferDirection.DOWNLOAD,
                    totalBytes = if (localFile.exists()) localFile.length() else 0L,
                    status = TransferStatus.QUEUED,
                    scope = "restore",
                    relativePath = remotePath
                ) to (remotePath to localFile)
            }

            FileTransferQueueManager.enqueueAll(transferItems.map { it.first })

            val processedCounter = AtomicInteger(skippedCount)

            processWithDynamicConcurrency(
                items = transferItems,
                concurrencyFlow = FileTransferQueueManager.concurrencyWorkers
            ) { (item, payload) ->
                val (remotePath, localFile) = payload
                if (FileTransferQueueManager.isCancelled(item.id) || FileTransferQueueManager.isPaused(item.id)) {
                    return@processWithDynamicConcurrency
                }

                FileTransferQueueManager.markStarted(item.id)
                val currentProcessed = processedCounter.incrementAndGet()
                onProgress?.invoke(currentProcessed, totalDiscovered, localFile.name)

                val downloadResult = try {
                    localFile.parentFile?.mkdirs()
                    provider.downloadFile(
                        remotePath = remotePath,
                        destinationFile = localFile,
                        onProgress = { transferred, total ->
                            if (FileTransferQueueManager.isPaused(item.id)) {
                                throw java.io.IOException("Transfer paused by user")
                            }
                            if (FileTransferQueueManager.isCancelled(item.id)) {
                                throw java.io.IOException("Transfer cancelled by user")
                            }
                            FileTransferQueueManager.updateProgress(item.id, transferred, total)
                        }
                    )
                } catch (e: Exception) {
                    Result.failure(e)
                }

                if (FileTransferQueueManager.isPaused(item.id)) {
                    return@processWithDynamicConcurrency
                }
                if (FileTransferQueueManager.isCancelled(item.id)) {
                    return@processWithDynamicConcurrency
                }

                if (downloadResult.isSuccess) {
                    FileTransferQueueManager.markCompleted(item.id)
                    synchronized(this@BackupEngine) {
                        restoredCount++
                        totalBytesDownloaded += localFile.length()
                        if (localFile.absolutePath.contains("/.config/") || localFile.absolutePath.startsWith(env.xournalConfigDir.absolutePath)) {
                            hasRestoredConfigs = true
                        }
                    }
                } else {
                    val errMsg = downloadResult.exceptionOrNull()?.message ?: "Download failed"
                    FileTransferQueueManager.markFailed(item.id, errMsg)
                    synchronized(this@BackupEngine) {
                        failedCount++
                        errors.add("${localFile.name}: $errMsg")
                    }
                }
            }

            // Sanitize settings and restore internal configurations if .config files were restored
            if (hasRestoredConfigs) {
                try {
                    NotesHomeConfigManager.restoreSettingsFromNotesHome(env.getNotesDirectory(), context, env)
                } catch (e: Exception) {
                    Log.w(TAG, "Error restoring settings from Notes Home after download", e)
                }
            }

        } finally {
            provider.disconnect()
        }

        return@withContext RestoreResult(
            serviceId = serviceConfig.id,
            serviceName = serviceConfig.name,
            totalFilesDiscovered = remoteFilesToDownload.size,
            filesRestored = restoredCount,
            filesSkipped = skippedCount,
            filesFailed = failedCount,
            totalBytesDownloaded = totalBytesDownloaded,
            durationMs = System.currentTimeMillis() - startTime,
            errors = errors
        )
    }

    /**
     * Runs backup across all enabled services.
     */
    suspend fun performMultiServiceBackup(
        concurrency: Int? = null,
        onProgress: ((current: Int, total: Int, currentFile: String) -> Unit)? = null
    ): List<BackupResult> {
        if (concurrency != null) {
            FileTransferQueueManager.setConcurrencyWorkers(concurrency)
        }
        val lock = CrossProcessLock.tryAcquire(syncLockFile)
        if (lock == null) {
            Log.w(TAG, "Cloud sync is already active in another window or process. Skipping concurrent multi-service sync.")
            return emptyList()
        }
        try {
            FileTransferQueueManager.clearCompleted()
            val services = vault.getActiveConfiguredServices().filter { it.isEnabled }
            val results = mutableListOf<BackupResult>()
            for (service in services) {
                try {
                    val result = performBackupInternal(service, concurrency, onProgress, clearCompletedQueue = false)
                    results.add(result)
                } catch (e: Exception) {
                    Log.e(TAG, "Sync failed for service ${service.name} (${service.id})", e)
                    vault.saveService(
                        service.copy(
                            lastSyncStatus = "Failed: ${e.message ?: "Unknown error"}"
                        )
                    )
                    results.add(
                        BackupResult(
                            serviceId = service.id,
                            serviceName = service.name,
                            totalFilesScanned = 0,
                            filesUploaded = 0,
                            filesSkipped = 0,
                            filesFailed = 1,
                            totalBytesTransferred = 0L,
                            durationMs = 0L,
                            errors = listOf(e.message ?: "Unknown error")
                        )
                    )
                }
            }
            return results
        } finally {
            lock.release()
        }
    }

    /**
     * Checks if there are any remote cloud file changes or additions on the specified service.
     */
    suspend fun checkForRemoteChanges(serviceConfig: ServiceConfig): List<dev.ilamparithi.aournalpp.backup.model.RemoteFileMetadata> = withContext(Dispatchers.IO) {
        val changedRemoteFiles = mutableListOf<dev.ilamparithi.aournalpp.backup.model.RemoteFileMetadata>()
        val provider = getStorageProvider(serviceConfig)
        val dao = db.syncMetadataDao()

        try {
            val connResult = provider.testConnection()
            if (connResult.isFailure || connResult.getOrNull() == false) return@withContext emptyList()

            val remoteFiles = mutableListOf<Pair<dev.ilamparithi.aournalpp.backup.model.RemoteFileMetadata, File>>()

            if (serviceConfig.isCompleteBackupEnabled) {
                val notesRoot = env.getNotesDirectory()
                val notesConfigDir = File(notesRoot, ".config")
                val remoteRoot = getCompleteBackupRemoteRoot(serviceConfig)

                val remoteNotes = listRemoteRecursively(provider, "$remoteRoot/Notes")
                for (rf in remoteNotes) {
                    if (rf.isDirectory) continue
                    val subPath = rf.remotePath.removePrefix("$remoteRoot/Notes").trim('/')
                    if (subPath.isEmpty()) continue
                    val destFile = try { resolveSafeChild(notesRoot, subPath) } catch (_: SecurityException) { continue }
                    remoteFiles.add(rf to destFile)
                }

                val remoteConfigs = listRemoteRecursively(provider, "$remoteRoot/.config")
                val addedRemoteConfigPaths = mutableSetOf<String>()
                for (rf in remoteConfigs) {
                    if (rf.isDirectory) continue
                    val subPath = rf.remotePath.removePrefix("$remoteRoot/.config").trim('/')
                    if (subPath.isNotEmpty()) {
                        val destFile = try { resolveSafeChild(notesConfigDir, subPath) } catch (_: SecurityException) { continue }
                        remoteFiles.add(rf to destFile)
                        addedRemoteConfigPaths.add(rf.remotePath)
                    }
                }

                // Fallback for legacy backups
                val legacyConfigs = listRemoteRecursively(provider, "$remoteRoot/.config/xournalpp")
                for (rf in legacyConfigs) {
                    if (rf.isDirectory || rf.remotePath in addedRemoteConfigPaths) continue
                    val subPath = rf.remotePath.removePrefix("$remoteRoot/.config/xournalpp").trim('/')
                    if (subPath.isNotEmpty()) {
                        val destFile = try { resolveSafeChild(File(notesConfigDir, "xournalpp"), subPath) } catch (_: SecurityException) { continue }
                        remoteFiles.add(rf to destFile)
                    }
                }
            }

            val activeMappings = serviceConfig.customMappings.ifEmpty {
                try {
                    dev.ilamparithi.aournalpp.backup.security.CustomMappingRepository(context).getMappingsForService(serviceConfig.id)
                } catch (_: Exception) {
                    emptyList()
                }
            }
            for (mapping in activeMappings) {
                if (!mapping.isEnabled) continue
                val localBase = File(mapping.localFolderPath)
                val remoteBase = mapping.remoteFolderPath.trim().trim('/')
                val remoteMapped = listRemoteRecursively(provider, remoteBase)
                for (rf in remoteMapped) {
                    if (rf.isDirectory) continue
                    val subPath = if (remoteBase.isNotEmpty()) rf.remotePath.removePrefix(remoteBase).trim('/') else rf.remotePath
                    if (subPath.isEmpty()) continue
                    val destFile = try { resolveSafeChild(localBase, subPath) } catch (_: SecurityException) { continue }
                    remoteFiles.add(rf to destFile)
                }
            }

            val cacheDir = File(context.cacheDir, "config_diff_cache").apply { if (!exists()) mkdirs() }
            for ((remote, localFile) in remoteFiles) {
                if (!localFile.exists()) {
                    changedRemoteFiles.add(remote)
                } else if (isConfigFile(localFile.name)) {
                    val tempRemote = File(cacheDir, "remote_check_${serviceConfig.id}_${localFile.name}")
                    val downloadRes = try {
                        provider.downloadFile(remote.remotePath, tempRemote) { _, _ -> }
                    } catch (_: Exception) {
                        Result.failure(Exception("Download failed"))
                    }
                    if (downloadRes.isSuccess && tempRemote.exists() && tempRemote.length() > 0L) {
                        if (hasContentChanges(localFile, tempRemote)) {
                            changedRemoteFiles.add(remote)
                        }
                    }
                    tempRemote.delete()
                } else if (remote.lastModifiedEpochMs > localFile.lastModified()) {
                    changedRemoteFiles.add(remote)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking remote changes for ${serviceConfig.name}", e)
        } finally {
            provider.disconnect()
        }

        changedRemoteFiles
    }

    /**
     * Checks all enabled services for remote changes on app launch.
     */
    suspend fun checkAllServicesForRemoteChanges(): Map<String, List<dev.ilamparithi.aournalpp.backup.model.RemoteFileMetadata>> = withContext(Dispatchers.IO) {
        val services = vault.getActiveConfiguredServices().filter { it.isEnabled }
        val results = mutableMapOf<String, List<dev.ilamparithi.aournalpp.backup.model.RemoteFileMetadata>>()
        for (service in services) {
            val changes = checkForRemoteChanges(service)
            if (changes.isNotEmpty()) {
                results[service.name] = changes
            }
        }
        results
    }

    /**
     * Scans local files and all enabled cloud services (including all custom folder mappings as distinct endpoints)
     * to detect conflicting versions of notes. Grouped by canonical local file path.
     * Uses timezone-agnostic UTC UNIX epoch millisecond timestamps and SHA-256 hashes.
     */
    suspend fun detectMultiServiceConflicts(): List<FileConflictGroup> = withContext(Dispatchers.IO) {
        val services = vault.getActiveConfiguredServices().filter { it.isEnabled }
        val exclusionFilter = vault.getExclusionFilter()
        val scanner = BackupScanner(env, exclusionFilter)

        val notesRoot = env.getNotesDirectory()
        val configRoot = env.xournalConfigDir

        // Map of canonical local file path -> list of versions (Local + Remote endpoints)
        val versionsByLocalPath = mutableMapOf<String, MutableList<FileVersionItem>>()
        val relativePathByLocalPath = mutableMapOf<String, String>()

        // 1. Gather all local files from complete backup domains
        val completeFiles = scanner.scanCompleteBackup()
        for ((file, _, relativePath, sizeBytes, lastModified, sha256) in completeFiles) {
            val canon = file.canonicalPath
            relativePathByLocalPath[canon] = relativePath
            versionsByLocalPath.getOrPut(canon) { mutableListOf() }.add(
                FileVersionItem(
                    source = FileVersionSource.LOCAL,
                    fileName = file.name,
                    relativePath = relativePath,
                    localFilePath = file.absolutePath,
                    sizeBytes = sizeBytes,
                    lastModifiedEpochMs = lastModified,
                    contentHash = sha256
                )
            )
        }

        // 2. Gather all local files from custom mappings
        val allMappings = services.flatMap { it.customMappings.filter { m -> m.isEnabled } }
        for (m in allMappings) {
            val mapped = scanner.scanCustomMapping(m)
            for ((file, _, relativePath, sizeBytes, lastModified, sha256) in mapped) {
                val canon = file.canonicalPath
                val displayRel = if (m.remoteFolderPath.isNotBlank()) "${m.remoteFolderPath.trim('/')}/$relativePath" else relativePath
                if (!relativePathByLocalPath.containsKey(canon)) {
                    relativePathByLocalPath[canon] = displayRel
                }
                if (versionsByLocalPath[canon]?.none { it.source is FileVersionSource.LOCAL } != false) {
                    versionsByLocalPath.getOrPut(canon) { mutableListOf() }.add(
                        FileVersionItem(
                            source = FileVersionSource.LOCAL,
                            fileName = file.name,
                            relativePath = displayRel,
                            localFilePath = file.absolutePath,
                            sizeBytes = sizeBytes,
                            lastModifiedEpochMs = lastModified,
                            contentHash = sha256
                        )
                    )
                }
            }
        }

        // 3. Gather remote files for all enabled services and their custom mappings
        for (srv in services) {
            val provider = getStorageProvider(srv)
            try {
                val conn = provider.testConnection()
                if (conn.isFailure || conn.getOrNull() == false) continue

                // 3a. Complete backup domain
                if (srv.isCompleteBackupEnabled) {
                    val remoteRoot = getCompleteBackupRemoteRoot(srv)
                    val remoteNotes = listRemoteRecursively(provider, "$remoteRoot/Notes")
                    for ((remotePath, isDirectory, sizeBytes, lastModifiedEpochMs, contentHash) in remoteNotes) {
                        if (isDirectory) continue
                        val subPath = remotePath.removePrefix("$remoteRoot/Notes").trim('/')
                        if (subPath.isEmpty()) continue
                        val destFile = try { resolveSafeChild(notesRoot, subPath) } catch (_: SecurityException) { continue }
                        val canon = destFile.canonicalPath
                        val displayRel = "Notes/$subPath"
                        if (!relativePathByLocalPath.containsKey(canon)) {
                            relativePathByLocalPath[canon] = displayRel
                        }
                        val item = FileVersionItem(
                            source = FileVersionSource.REMOTE(
                                serviceId = srv.id,
                                serviceName = srv.name,
                                providerType = srv.providerType,
                                mappingId = null,
                                mappingRemotePath = null
                            ),
                            fileName = destFile.name,
                            relativePath = displayRel,
                            localFilePath = destFile.absolutePath,
                            sizeBytes = sizeBytes,
                            lastModifiedEpochMs = lastModifiedEpochMs,
                            contentHash = contentHash,
                            remotePath = remotePath
                        )
                        versionsByLocalPath.getOrPut(canon) { mutableListOf() }.add(item)
                    }

                    val cacheDir = File(context.cacheDir, "config_diff_cache").apply { if (!exists()) mkdirs() }
                    val remoteConfigs = listRemoteRecursively(provider, "$remoteRoot/.config")
                    val addedRemoteConfigPaths = mutableSetOf<String>()
                    for ((remotePath, isDirectory, sizeBytes, lastModifiedEpochMs, contentHash) in remoteConfigs) {
                        if (isDirectory) continue
                        val subPath = remotePath.removePrefix("$remoteRoot/.config").trim('/')
                        if (subPath.isEmpty()) continue
                        val destFile = try { resolveSafeChild(File(notesRoot, ".config"), subPath) } catch (_: SecurityException) { continue }
                        val canon = destFile.canonicalPath
                        val displayRel = ".config/$subPath"
                        if (!relativePathByLocalPath.containsKey(canon)) {
                            relativePathByLocalPath[canon] = displayRel
                        }
                        var downloadedLocalPath = destFile.absolutePath
                        var downloadedHash = contentHash
                        if (isConfigFile(subPath)) {
                            val tempRemote = File(cacheDir, "remote_conflict_${srv.id}_${destFile.name}")
                            val dl = try {
                                provider.downloadFile(remotePath, tempRemote) { _, _ -> }
                            } catch (_: Exception) {
                                Result.failure(Exception("Download failed"))
                            }
                            if (dl.isSuccess && tempRemote.exists() && tempRemote.length() > 0L) {
                                downloadedLocalPath = tempRemote.absolutePath
                                downloadedHash = calculateFileHash(tempRemote)
                            }
                        }
                        val item = FileVersionItem(
                            source = FileVersionSource.REMOTE(
                                serviceId = srv.id,
                                serviceName = srv.name,
                                providerType = srv.providerType,
                                mappingId = null,
                                mappingRemotePath = null
                            ),
                            fileName = destFile.name,
                            relativePath = displayRel,
                            localFilePath = downloadedLocalPath,
                            sizeBytes = sizeBytes,
                            lastModifiedEpochMs = lastModifiedEpochMs,
                            contentHash = downloadedHash,
                            remotePath = remotePath
                        )
                        versionsByLocalPath.getOrPut(canon) { mutableListOf() }.add(item)
                        addedRemoteConfigPaths.add(remotePath)
                    }

                    // Fallback for legacy backups
                    val legacyConfigs = listRemoteRecursively(provider, "$remoteRoot/.config/xournalpp")
                    for ((remotePath, isDirectory, sizeBytes, lastModifiedEpochMs, contentHash) in legacyConfigs) {
                        if (isDirectory || remotePath in addedRemoteConfigPaths) continue
                        val subPath = remotePath.removePrefix("$remoteRoot/.config/xournalpp").trim('/')
                        if (subPath.isEmpty()) continue
                        val destFile = try { resolveSafeChild(File(notesRoot, ".config/xournalpp"), subPath) } catch (_: SecurityException) { continue }
                        val canon = destFile.canonicalPath
                        val displayRel = ".config/xournalpp/$subPath"
                        if (!relativePathByLocalPath.containsKey(canon)) {
                            relativePathByLocalPath[canon] = displayRel
                        }
                        var downloadedLocalPath = destFile.absolutePath
                        var downloadedHash = contentHash
                        if (isConfigFile(subPath)) {
                            val tempRemote = File(cacheDir, "remote_conflict_${srv.id}_${destFile.name}")
                            val dl = try {
                                provider.downloadFile(remotePath, tempRemote) { _, _ -> }
                            } catch (_: Exception) {
                                Result.failure(Exception("Download failed"))
                            }
                            if (dl.isSuccess && tempRemote.exists() && tempRemote.length() > 0L) {
                                downloadedLocalPath = tempRemote.absolutePath
                                downloadedHash = calculateFileHash(tempRemote)
                            }
                        }
                        val item = FileVersionItem(
                            source = FileVersionSource.REMOTE(
                                serviceId = srv.id,
                                serviceName = srv.name,
                                providerType = srv.providerType,
                                mappingId = null,
                                mappingRemotePath = null
                            ),
                            fileName = destFile.name,
                            relativePath = displayRel,
                            localFilePath = downloadedLocalPath,
                            sizeBytes = sizeBytes,
                            lastModifiedEpochMs = lastModifiedEpochMs,
                            contentHash = downloadedHash,
                            remotePath = remotePath
                        )
                        versionsByLocalPath.getOrPut(canon) { mutableListOf() }.add(item)
                    }
                }

                // 3b. Custom mappings domain (treated as distinct cloud storage endpoints)
                for ((mappingId, _, _, localFolderPath, remoteFolderPath, isEnabled) in srv.customMappings) {
                    if (!isEnabled) continue
                    val localBase = File(localFolderPath)
                    val remoteBase = remoteFolderPath.trim().trim('/')
                    val remoteMapped = listRemoteRecursively(provider, remoteBase)
                    for ((remotePath, isDirectory, sizeBytes, lastModifiedEpochMs, contentHash) in remoteMapped) {
                        if (isDirectory) continue
                        val subPath = if (remoteBase.isNotEmpty()) remotePath.removePrefix(remoteBase).trim('/') else remotePath
                        if (subPath.isEmpty()) continue
                        val destFile = try { resolveSafeChild(localBase, subPath) } catch (_: SecurityException) { continue }
                        val canon = destFile.canonicalPath
                        val displayRel = if (remoteBase.isNotEmpty()) "$remoteBase/$subPath" else subPath
                        if (!relativePathByLocalPath.containsKey(canon)) {
                            relativePathByLocalPath[canon] = displayRel
                        }
                        val item = FileVersionItem(
                            source = FileVersionSource.REMOTE(
                                serviceId = srv.id,
                                serviceName = srv.name,
                                providerType = srv.providerType,
                                mappingId = mappingId,
                                mappingRemotePath = remoteFolderPath
                            ),
                            fileName = destFile.name,
                            relativePath = displayRel,
                            localFilePath = destFile.absolutePath,
                            sizeBytes = sizeBytes,
                            lastModifiedEpochMs = lastModifiedEpochMs,
                            contentHash = contentHash,
                            remotePath = remotePath
                        )
                        versionsByLocalPath.getOrPut(canon) { mutableListOf() }.add(item)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error listing remote files for service ${srv.name}", e)
            } finally {
                provider.disconnect()
            }
        }

        // 4. Aggregate into conflict groups
        val conflictGroups = mutableListOf<FileConflictGroup>()

        for ((canonPath, allVersions) in versionsByLocalPath) {
            if (allVersions.size <= 1) continue

            val localVersion = allVersions.firstOrNull { it.source is FileVersionSource.LOCAL }
            val remoteVersions = allVersions.filter { it.source is FileVersionSource.REMOTE }

            if (remoteVersions.isEmpty()) continue

            val displayRel = relativePathByLocalPath[canonPath] ?: File(canonPath).name
            val isConfig = isConfigFile(displayRel)

            // Determine if versions genuinely differ:
            var hasConflict = false
            for ((i, v1) in allVersions.withIndex()) {
                for (j in i + 1 until allVersions.size) {
                    val v2 = allVersions[j]

                    val sameHash = v1.contentHash != null && v2.contentHash != null && v1.contentHash.equals(v2.contentHash, ignoreCase = true)
                    if (sameHash) {
                        // Hashes match: identical content, 0 changes regardless of lastModified timestamp difference
                        continue
                    }

                    // If both files exist locally to inspect, check if actual content diff has 0 changes
                    if (v1.localFilePath.isNotEmpty() && v2.localFilePath.isNotEmpty()) {
                        val f1 = File(v1.localFilePath)
                        val f2 = File(v2.localFilePath)
                        if (f1.exists() && f2.exists()) {
                            if (!hasContentChanges(f1, f2)) {
                                continue
                            } else if (isConfig) {
                                hasConflict = true
                                break
                            }
                        }
                    }

                    if (isConfig) continue

                    val sizeDiff = v1.sizeBytes != v2.sizeBytes
                    val timeDiff = kotlin.math.abs(v1.lastModifiedEpochMs - v2.lastModifiedEpochMs) > 2000L
                    val hashDiff = v1.contentHash != null && v2.contentHash != null && !v1.contentHash.equals(v2.contentHash, ignoreCase = true)
                    // If content hashes are unavailable on either side and sizes match, fallback to timestamp difference
                    val unknownHashDiff = (v1.contentHash == null || v2.contentHash == null) && timeDiff

                    // Only flag conflict if content differs (size or hash), or timestamps differ when hashes cannot verify 0 changes
                    if (sizeDiff || hashDiff || unknownHashDiff) {
                        hasConflict = true
                        break
                    }
                }
                if (hasConflict) break
            }

            if (hasConflict) {
                val displayRel = relativePathByLocalPath[canonPath] ?: File(canonPath).name
                conflictGroups.add(
                    FileConflictGroup(
                        relativePath = displayRel,
                        localVersion = localVersion,
                        remoteVersions = remoteVersions
                    )
                )
            }
        }

        conflictGroups
    }

    /**
     * Executes conflict resolutions across local storage and cloud services.
     */
    suspend fun resolveConflicts(
        resolutions: List<FileConflictResolution>
    ): ConflictResolutionReport = withContext(Dispatchers.IO) {
        val services = vault.getAllServices().associateBy { it.id }
        val dao = db.syncMetadataDao()

        var filesUpdated = 0
        var filesSavedAlongside = 0
        var filesSkipped = 0
        val errors = mutableListOf<String>()
        var hasRestoredConfigs = false

        for ((_, relativePath, action) in resolutions) {
            try {
                when (action) {
                    is ConflictResolutionAction.ChoosePrimary -> {
                        val chosen = action.chosenVersion
                        val localFile = File(chosen.localFilePath)
                        val parentDir = localFile.parentFile ?: env.getNotesDirectory()
                        parentDir.mkdirs()

                        when (val src = chosen.source) {
                            is FileVersionSource.LOCAL -> {
                                // Local version chosen as primary: keep local file
                                filesUpdated++
                            }
                            is FileVersionSource.REMOTE -> {
                                val srv = services[src.serviceId]
                                if (srv != null && chosen.remotePath != null) {
                                    val provider = getStorageProvider(srv)
                                    try {
                                        val downloadResult = provider.downloadFile(
                                            remotePath = chosen.remotePath,
                                            destinationFile = localFile,
                                            onProgress = { _, _ -> }
                                        )
                                        if (downloadResult.isSuccess) {
                                            filesUpdated++
                                            dao.insertOrUpdate(
                                                SyncMetadataEntity(
                                                    serviceId = srv.id,
                                                    relativePath = relativePath,
                                                    scope = if (localFile.absolutePath.startsWith(env.xournalConfigDir.absolutePath)) "config" else "notes",
                                                    localSha256 = chosen.contentHash ?: "",
                                                    remoteHash = chosen.contentHash,
                                                    localLastModified = chosen.lastModifiedEpochMs,
                                                    sizeBytes = localFile.length(),
                                                    lastSyncedAt = System.currentTimeMillis()
                                                )
                                            )
                                            if (localFile.absolutePath.startsWith(env.xournalConfigDir.absolutePath)) {
                                                hasRestoredConfigs = true
                                            }
                                        } else {
                                            errors.add("${localFile.name}: ${downloadResult.exceptionOrNull()?.message}")
                                        }
                                    } finally {
                                        provider.disconnect()
                                    }
                                }
                            }
                        }
                    }
                    is ConflictResolutionAction.ResolveSelection -> {
                        val primary = action.primaryVersion
                        val localFile = File(primary.localFilePath)
                        val parentDir = localFile.parentFile ?: env.getNotesDirectory()
                        parentDir.mkdirs()

                        // 1. Primary version
                        when (val src = primary.source) {
                            is FileVersionSource.LOCAL -> {
                                filesUpdated++
                            }
                            is FileVersionSource.REMOTE -> {
                                val srv = services[src.serviceId]
                                if (srv != null && primary.remotePath != null) {
                                    val provider = getStorageProvider(srv)
                                    try {
                                        val downloadResult = provider.downloadFile(
                                            remotePath = primary.remotePath,
                                            destinationFile = localFile,
                                            onProgress = { _, _ -> }
                                        )
                                        if (downloadResult.isSuccess) {
                                            filesUpdated++
                                            dao.insertOrUpdate(
                                                SyncMetadataEntity(
                                                    serviceId = srv.id,
                                                    relativePath = relativePath,
                                                    scope = if (localFile.absolutePath.startsWith(env.xournalConfigDir.absolutePath)) "config" else "notes",
                                                    localSha256 = primary.contentHash ?: "",
                                                    remoteHash = primary.contentHash,
                                                    localLastModified = primary.lastModifiedEpochMs,
                                                    sizeBytes = localFile.length(),
                                                    lastSyncedAt = System.currentTimeMillis()
                                                )
                                            )
                                            if (localFile.absolutePath.startsWith(env.xournalConfigDir.absolutePath)) {
                                                hasRestoredConfigs = true
                                            }
                                        } else {
                                            errors.add("${localFile.name}: ${downloadResult.exceptionOrNull()?.message}")
                                        }
                                    } finally {
                                        provider.disconnect()
                                    }
                                }
                            }
                        }

                        // 2. Alongside versions
                        for (alongsideVer in action.alongsideVersions) {
                            val src = alongsideVer.source
                            if (src is FileVersionSource.REMOTE && alongsideVer.remotePath != null) {
                                val srv = services[src.serviceId]
                                if (srv != null) {
                                    val nameWithoutExt = localFile.nameWithoutExtension
                                    val ext = localFile.extension.let { if (it.isNotEmpty()) ".$it" else "" }
                                    val desiredAlongsideName = "$nameWithoutExt (${src.sanitizedFileSuffix})$ext"
                                    val alongsideFile = generateNonCollidingFile(parentDir, desiredAlongsideName)

                                    val provider = getStorageProvider(srv)
                                    try {
                                        val dlResult = provider.downloadFile(
                                            remotePath = alongsideVer.remotePath,
                                            destinationFile = alongsideFile,
                                            onProgress = { _, _ -> }
                                        )
                                        if (dlResult.isSuccess) {
                                            filesSavedAlongside++
                                            val alongsideRel = if (relativePath.contains('/')) {
                                                "${relativePath.substringBeforeLast('/')}/${alongsideFile.name}"
                                            } else {
                                                alongsideFile.name
                                            }
                                            dao.insertOrUpdate(
                                                SyncMetadataEntity(
                                                    serviceId = srv.id,
                                                    relativePath = alongsideRel,
                                                    scope = if (alongsideFile.absolutePath.startsWith(env.xournalConfigDir.absolutePath)) "config" else "notes",
                                                    localSha256 = alongsideVer.contentHash ?: "",
                                                    remoteHash = alongsideVer.contentHash,
                                                    localLastModified = alongsideVer.lastModifiedEpochMs,
                                                    sizeBytes = alongsideFile.length(),
                                                    lastSyncedAt = System.currentTimeMillis()
                                                )
                                            )
                                        } else {
                                            errors.add("${alongsideFile.name}: ${dlResult.exceptionOrNull()?.message}")
                                        }
                                    } finally {
                                        provider.disconnect()
                                    }
                                }
                            }
                        }
                    }
                    is ConflictResolutionAction.KeepBoth, is ConflictResolutionAction.KeepAlongside -> {
                        val versionsToKeep = if (action is ConflictResolutionAction.KeepAlongside) {
                            action.versionsToKeep
                        } else {
                            emptyList()
                        }

                        for (remoteVer in versionsToKeep) {
                            val src = remoteVer.source
                            if (src is FileVersionSource.REMOTE && remoteVer.remotePath != null) {
                                val srv = services[src.serviceId]
                                if (srv != null) {
                                    val localFile = File(remoteVer.localFilePath)
                                    val parentDir = localFile.parentFile ?: env.getNotesDirectory()
                                    if (!parentDir.exists()) parentDir.mkdirs()

                                    val nameWithoutExt = localFile.nameWithoutExtension
                                    val ext = localFile.extension.let { if (it.isNotEmpty()) ".$it" else "" }
                                    val desiredAlongsideName = "$nameWithoutExt (${src.sanitizedFileSuffix})$ext"
                                    val alongsideFile = generateNonCollidingFile(parentDir, desiredAlongsideName)

                                    val provider = getStorageProvider(srv)
                                    try {
                                        val dlResult = provider.downloadFile(
                                            remotePath = remoteVer.remotePath,
                                            destinationFile = alongsideFile,
                                            onProgress = { _, _ -> }
                                        )
                                        if (dlResult.isSuccess) {
                                            filesSavedAlongside++
                                            val alongsideRel = if (relativePath.contains('/')) {
                                                "${relativePath.substringBeforeLast('/')}/${alongsideFile.name}"
                                            } else {
                                                alongsideFile.name
                                            }
                                            dao.insertOrUpdate(
                                                SyncMetadataEntity(
                                                    serviceId = srv.id,
                                                    relativePath = alongsideRel,
                                                    scope = if (alongsideFile.absolutePath.startsWith(env.xournalConfigDir.absolutePath)) "config" else "notes",
                                                    localSha256 = remoteVer.contentHash ?: "",
                                                    remoteHash = remoteVer.contentHash,
                                                    localLastModified = remoteVer.lastModifiedEpochMs,
                                                    sizeBytes = alongsideFile.length(),
                                                    lastSyncedAt = System.currentTimeMillis()
                                                )
                                            )
                                        } else {
                                            errors.add("${alongsideFile.name}: ${dlResult.exceptionOrNull()?.message}")
                                        }
                                    } finally {
                                        provider.disconnect()
                                    }
                                }
                            }
                        }
                    }
                    is ConflictResolutionAction.Skip -> {
                        filesSkipped++
                    }
                }
            } catch (e: Exception) {
                errors.add("$relativePath: ${e.message}")
            }
        }

        if (hasRestoredConfigs) {
            try {
                NotesHomeConfigManager.restoreSettingsFromNotesHome(env.getNotesDirectory(), context, env)
            } catch (e: Exception) {
                Log.w(TAG, "Error restoring settings from Notes Home after conflict resolution", e)
            }
        }

        ConflictResolutionReport(
            filesUpdated = filesUpdated,
            filesSavedAlongside = filesSavedAlongside,
            filesSkipped = filesSkipped,
            errors = errors
        )
    }

    private fun generateNonCollidingFile(parentDir: File, desiredName: String): File {
        val nameWithoutExt = File(desiredName).nameWithoutExtension
        val ext = File(desiredName).extension.let { if (it.isNotEmpty()) ".$it" else "" }
        var candidate = File(parentDir, desiredName)
        var counter = 1
        while (candidate.exists()) {
            candidate = File(parentDir, "$nameWithoutExt ($counter)$ext")
            counter++
        }
        return candidate
    }

    private fun resolveLocalFileForRelativePath(relativePath: String, customMappings: List<CustomFolderMapping>): File {
        val clean = relativePath.trim().trim('/')
        if (clean.startsWith("Notes/")) {
            return File(env.getNotesDirectory(), clean.removePrefix("Notes/").trim('/'))
        }
        if (clean == "Notes") {
            return env.getNotesDirectory()
        }
        if (clean.startsWith(".config/")) {
            return File(env.getNotesDirectory(), clean)
        }
        for (mapping in customMappings) {
            val mappingRemote = mapping.remoteFolderPath.trim().trim('/')
            if (clean.startsWith(mappingRemote) && mappingRemote.isNotEmpty()) {
                val sub = clean.removePrefix(mappingRemote).trim('/')
                return if (sub.isEmpty()) File(mapping.localFolderPath) else File(mapping.localFolderPath, sub)
            }
        }
        return File(env.getNotesDirectory(), clean)
    }

    private suspend fun listRemoteRecursively(
        provider: dev.ilamparithi.aournalpp.backup.provider.CloudStorageProvider,
        remoteDirectory: String
    ): List<dev.ilamparithi.aournalpp.backup.model.RemoteFileMetadata> {
        val results = mutableListOf<dev.ilamparithi.aournalpp.backup.model.RemoteFileMetadata>()
        val topResult = provider.listFiles(remoteDirectory)
        val entries = topResult.getOrNull() ?: return emptyList()

        for (entry in entries) {
            results.add(entry)
            if (entry.isDirectory) {
                val subEntries = listRemoteRecursively(provider, entry.remotePath)
                results.addAll(subEntries)
            }
        }
        return results
    }

    /**
     * Checks whether the specified remote folder on the given cloud service has an Aournal++ complete sync.
     * Looks for Notes directory, .config/xournalpp directory, or existing note files.
     */
    suspend fun checkRemoteCompleteSync(
        serviceConfig: ServiceConfig,
        remotePath: String = getCompleteBackupRemoteRoot(serviceConfig)
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val provider = getStorageProvider(serviceConfig)
        try {
            val connResult = provider.testConnection()
            if (connResult.isFailure || connResult.getOrNull() == false) {
                return@withContext Result.failure(
                    connResult.exceptionOrNull() ?: Exception("Failed to connect to ${serviceConfig.name}")
                )
            }

            val cleanRoot = remotePath.trim().trim('/')
            val rootList = provider.listFiles(cleanRoot).getOrNull() ?: emptyList()
            if (rootList.isEmpty()) {
                return@withContext Result.success(false)
            }

            // Check if Notes/ or .config/ exists directly in remoteRoot
            val hasNotesDir = rootList.any { it.isDirectory && (it.remotePath.endsWith("/Notes") || it.remotePath.equals("Notes", ignoreCase = true)) }
            val hasConfigDir = rootList.any { it.isDirectory && (it.remotePath.endsWith("/.config") || it.remotePath.equals(".config", ignoreCase = true)) }

            if (hasNotesDir || hasConfigDir) {
                return@withContext Result.success(true)
            }

            // Check if Notes or .config/xournalpp has files
            val notesFiles = provider.listFiles("$cleanRoot/Notes").getOrNull() ?: emptyList()
            if (notesFiles.any { !it.isDirectory }) {
                return@withContext Result.success(true)
            }

            val configFiles = provider.listFiles("$cleanRoot/.config/xournalpp").getOrNull() ?: emptyList()
            if (configFiles.any { !it.isDirectory }) {
                return@withContext Result.success(true)
            }

            // Also check if any note files (.xopp) exist in the root folder itself
            val hasNoteFiles = rootList.any { !it.isDirectory && it.remotePath.endsWith(".xopp", ignoreCase = true) }
            Result.success(hasNoteFiles)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            provider.disconnect()
        }
    }

    /**
     * Compares the remote settings.xml with the local settings.xml to determine if they are in sync.
     */
    suspend fun compareRemoteConfig(
        serviceConfig: ServiceConfig,
        remotePath: String = getCompleteBackupRemoteRoot(serviceConfig),
        localNotesDir: File
    ): ConfigSyncStatus = withContext(Dispatchers.IO) {
        val localConfigFile = File(File(localNotesDir, ".config/xournalpp"), "settings.xml")
        val altLocalConfigFile = File(env.xournalConfigDir, "settings.xml")
        val activeLocal = if (localConfigFile.exists()) localConfigFile else altLocalConfigFile

        if (!activeLocal.exists()) {
            return@withContext ConfigSyncStatus.NO_LOCAL_CONFIG
        }

        val provider = getStorageProvider(serviceConfig)
        val tempFile = File(context.cacheDir, "remote_test_settings_${UUID.randomUUID()}.xml")
        try {
            val cleanRoot = remotePath.trim().trim('/')
            val remoteSettingsPath = "$cleanRoot/.config/xournalpp/settings.xml"
            val downloadRes = provider.downloadFile(remoteSettingsPath, tempFile) { _, _ -> }
            if (downloadRes.isFailure || !tempFile.exists() || tempFile.length() == 0L) {
                return@withContext ConfigSyncStatus.NO_REMOTE_CONFIG
            }

            val localHash = calculateFileHash(activeLocal)
            val remoteHash = calculateFileHash(tempFile)
            if (localHash.isNotEmpty() && localHash == remoteHash) {
                ConfigSyncStatus.IN_SYNC
            } else {
                ConfigSyncStatus.MISMATCH
            }
        } catch (e: Exception) {
            ConfigSyncStatus.NO_REMOTE_CONFIG
        } finally {
            tempFile.delete()
            provider.disconnect()
        }
    }

    fun getStorageProvider(service: ServiceConfig): CloudStorageProvider {
        return StorageProviderFactory.createProvider(service) { newAccessToken, newRefreshToken, expiryEpochMs ->
            Log.i(TAG, "Refreshed tokens for ${service.name} (${service.id}), persisting to CredentialsVault")
            val updated = service.copy(
                authToken = newAccessToken,
                refreshToken = newRefreshToken ?: service.refreshToken,
                tokenExpiryEpochMs = expiryEpochMs
            )
            vault.saveService(updated)
        }
    }

    /**
     * Detects configuration conflicts across settings.xml, app_settings.json, x11_prefs.json, settings.ini, and sync_mappings.json.
     * Returns a list of FileConflictGroup populated with descriptions and downloaded remote cache files for diffing.
     */
    suspend fun detectConfigConflicts(
        serviceConfig: ServiceConfig,
        remotePath: String = getCompleteBackupRemoteRoot(serviceConfig),
        localNotesDir: File
    ): List<FileConflictGroup> = withContext(Dispatchers.IO) {
        val configFilesToCheck = listOf(
            Triple(
                ".config/xournalpp/settings.xml",
                "settings.xml",
                "Xournal++ pen styles, toolbars, page templates, and drawing preferences."
            ),
            Triple(
                ".config/app_settings.json",
                "app_settings.json",
                "Aournal++ Android settings: touch gestures, display scaling, and themes."
            ),
            Triple(
                ".config/x11_prefs.json",
                "x11_prefs.json",
                "Termux-X11 display preferences: resolution, refresh rate, and canvas mode."
            ),
            Triple(
                ".config/xournalpp/settings.ini",
                "settings.ini",
                "GTK3 interface theme, styling, and UI font configurations."
            ),
            Triple(
                ".config/sync_mappings.json",
                "sync_mappings.json",
                "Custom folder sync mappings and saved mapping template sets."
            )
        )

        val conflicts = mutableListOf<FileConflictGroup>()
        val provider = getStorageProvider(serviceConfig)
        val cleanRoot = remotePath.trim().trim('/')

        try {
            val cacheDir = File(context.cacheDir, "config_diff_cache").apply { if (!exists()) mkdirs() }

            for ((relPath, fileName, desc) in configFilesToCheck) {
                val localFile = File(localNotesDir, relPath)
                val altLocalFile = if (relPath.startsWith(".config/xournalpp/")) {
                    File(env.xournalConfigDir, fileName)
                } else if (fileName == "sync_mappings.json") {
                    File(context.filesDir, "sync_mappings.json")
                } else null

                val activeLocal = if (localFile.exists() && localFile.length() > 0L) localFile
                else if (altLocalFile != null && altLocalFile.exists() && altLocalFile.length() > 0L) altLocalFile
                else null

                val remoteFilePath = "$cleanRoot/$relPath"
                val tempRemoteFile = File(cacheDir, "remote_${serviceConfig.id}_$fileName")
                val downloadRes = provider.downloadFile(remoteFilePath, tempRemoteFile) { _, _ -> }

                val remoteExists = downloadRes.isSuccess && tempRemoteFile.exists() && tempRemoteFile.length() > 0L

                if (activeLocal != null && remoteExists) {
                    val localHash = calculateFileHash(activeLocal)
                    val remoteHash = calculateFileHash(tempRemoteFile)

                    if (localHash.isNotEmpty() && remoteHash.isNotEmpty() && localHash != remoteHash) {
                        // If diff has 0 actual changes (e.g. whitespace/CRLF or identical semantic content), don't inform the user
                        if (!hasContentChanges(activeLocal, tempRemoteFile)) {
                            continue
                        }

                        val localVersion = FileVersionItem(
                            source = FileVersionSource.LOCAL,
                            fileName = fileName,
                            relativePath = relPath,
                            lastModifiedEpochMs = activeLocal.lastModified(),
                            sizeBytes = activeLocal.length(),
                            contentHash = localHash,
                            localFilePath = activeLocal.absolutePath
                        )
                        val remoteVersion = FileVersionItem(
                            source = FileVersionSource.REMOTE(
                                serviceId = serviceConfig.id,
                                serviceName = serviceConfig.name,
                                providerType = serviceConfig.providerType
                            ),
                            fileName = fileName,
                            relativePath = relPath,
                            lastModifiedEpochMs = System.currentTimeMillis(), // remote timestamp
                            sizeBytes = tempRemoteFile.length(),
                            contentHash = remoteHash,
                            localFilePath = tempRemoteFile.absolutePath,
                            remotePath = remoteFilePath
                        )

                        conflicts.add(
                            FileConflictGroup(
                                id = "config_${serviceConfig.id}_$fileName",
                                relativePath = relPath,
                                localVersion = localVersion,
                                remoteVersions = listOf(remoteVersion),
                                description = desc,
                                localFilePath = activeLocal.absolutePath,
                                remoteFilePath = tempRemoteFile.absolutePath
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            provider.disconnect()
        }

        conflicts
    }

    /**
     * Applies resolved configuration file selections by writing chosen versions to local notes home and internal app storage.
     */
    suspend fun applyConfigResolutions(
        resolutions: Map<String, FileVersionItem>,
        serviceConfig: ServiceConfig,
        localNotesDir: File
    ) = withContext(Dispatchers.IO) {
        for ((_, chosenVer) in resolutions) {
            if (chosenVer.source is FileVersionSource.REMOTE && chosenVer.localFilePath.isNotEmpty()) {
                val downloadedCache = File(chosenVer.localFilePath)
                if (downloadedCache.exists() && downloadedCache.isFile) {
                    val targetRelPath = when (chosenVer.fileName) {
                        "settings.xml", "settings.ini" -> ".config/xournalpp/${chosenVer.fileName}"
                        else -> ".config/${chosenVer.fileName}"
                    }
                    val targetFile = File(localNotesDir, targetRelPath)
                    targetFile.parentFile?.mkdirs()
                    downloadedCache.copyTo(targetFile, overwrite = true)

                    // Also mirror into active runtime config locations if applicable
                    if (chosenVer.fileName == "sync_mappings.json") {
                        val internalFile = File(context.filesDir, "sync_mappings.json")
                        downloadedCache.copyTo(internalFile, overwrite = true)
                    }
                }
            }
        }

        try {
            NotesHomeConfigManager.restoreSettingsFromNotesHome(localNotesDir, context, env)
        } catch (e: Exception) {
            Log.w(TAG, "Error applying config resolutions to internal settings", e)
        }
    }

    /**
     * Retries or resumes a single file transfer manually.
     */
    suspend fun retryTransfer(item: TransferItem): Result<Unit> = withContext(Dispatchers.IO) {
        val service = vault.getAllServices().firstOrNull { it.id == item.serviceId }
            ?: return@withContext Result.failure(IllegalStateException("Cloud service ${item.serviceName} not found"))

        val provider = getStorageProvider(service)
        try {
            val connResult = provider.testConnection()
            if (connResult.isFailure || connResult.getOrNull() == false) {
                val err = connResult.exceptionOrNull()?.message ?: "Failed to connect to ${service.name}"
                FileTransferQueueManager.markFailed(item.id, err)
                return@withContext Result.failure(Exception(err))
            }

            FileTransferQueueManager.markRetrying(item.id)
            FileTransferQueueManager.markStarted(item.id)

            if (item.direction == TransferDirection.UPLOAD) {
                val localFile = File(item.localFilePath)
                if (!localFile.exists()) {
                    val err = "Local file not found: ${item.localFilePath}"
                    FileTransferQueueManager.markFailed(item.id, err)
                    return@withContext Result.failure(FileNotFoundException(err))
                }

                val uploadResult = try {
                    provider.uploadFile(
                        localFile = localFile,
                        remotePath = item.remotePath,
                        onProgress = { transferred, total ->
                            if (FileTransferQueueManager.isPaused(item.id)) {
                                throw java.io.IOException("Transfer paused by user")
                            }
                            if (FileTransferQueueManager.isCancelled(item.id)) {
                                throw java.io.IOException("Transfer cancelled by user")
                            }
                            FileTransferQueueManager.updateProgress(item.id, transferred, total)
                        }
                    )
                } catch (e: Exception) {
                    Result.failure(e)
                }

                if (FileTransferQueueManager.isPaused(item.id)) {
                    return@withContext Result.success(Unit)
                }
                if (FileTransferQueueManager.isCancelled(item.id)) {
                    return@withContext Result.failure(Exception("Transfer cancelled"))
                }

                if (uploadResult.isSuccess) {
                    FileTransferQueueManager.markCompleted(item.id)
                    val dao = db.syncMetadataDao()
                    val sha = BackupScanner.calculateSha256(localFile)
                    val relPath = item.relativePath ?: localFile.name
                    val scope = item.scope ?: BackupScope.NOTES.id
                    dao.insertOrUpdate(
                        SyncMetadataEntity(
                            serviceId = service.id,
                            relativePath = relPath,
                            scope = scope,
                            localSha256 = sha,
                            remoteHash = null,
                            localLastModified = localFile.lastModified(),
                            sizeBytes = localFile.length(),
                            lastSyncedAt = System.currentTimeMillis()
                        )
                    )
                    Result.success(Unit)
                } else {
                    val errMsg = uploadResult.exceptionOrNull()?.message ?: "Upload failed"
                    FileTransferQueueManager.markFailed(item.id, errMsg)
                    Result.failure(uploadResult.exceptionOrNull() ?: Exception(errMsg))
                }
            } else {
                val localFile = File(item.localFilePath)
                localFile.parentFile?.mkdirs()

                val downloadResult = try {
                    provider.downloadFile(
                        remotePath = item.remotePath,
                        destinationFile = localFile,
                        onProgress = { downloaded, total ->
                            if (FileTransferQueueManager.isPaused(item.id)) {
                                throw java.io.IOException("Transfer paused by user")
                            }
                            if (FileTransferQueueManager.isCancelled(item.id)) {
                                throw java.io.IOException("Transfer cancelled by user")
                            }
                            FileTransferQueueManager.updateProgress(item.id, downloaded, total)
                        }
                    )
                } catch (e: Exception) {
                    Result.failure(e)
                }

                if (FileTransferQueueManager.isPaused(item.id)) {
                    return@withContext Result.success(Unit)
                }
                if (FileTransferQueueManager.isCancelled(item.id)) {
                    return@withContext Result.failure(Exception("Transfer cancelled"))
                }

                if (downloadResult.isSuccess) {
                    FileTransferQueueManager.markCompleted(item.id)
                    Result.success(Unit)
                } else {
                    val errMsg = downloadResult.exceptionOrNull()?.message ?: "Download failed"
                    FileTransferQueueManager.markFailed(item.id, errMsg)
                    Result.failure(downloadResult.exceptionOrNull() ?: Exception(errMsg))
                }
            }
        } finally {
            provider.disconnect()
        }
    }

    /**
     * Retries all currently failed transfers.
     */
    suspend fun retryAllFailed(): List<Result<Unit>> = withContext(Dispatchers.IO) {
        val failedItems = FileTransferQueueManager.items.value.filter { it.status == TransferStatus.FAILED }
        failedItems.map { retryTransfer(it) }
    }

    /**
     * Resumes a paused transfer.
     */
    suspend fun resumeTransfer(item: TransferItem): Result<Unit> {
        FileTransferQueueManager.requestResume(item.id)
        return retryTransfer(item)
    }

    /**
     * Resumes all paused transfers.
     */
    suspend fun resumeAllPaused(): List<Result<Unit>> = withContext(Dispatchers.IO) {
        val pausedItems = FileTransferQueueManager.items.value.filter { it.status == TransferStatus.PAUSED }
        pausedItems.map { resumeTransfer(it) }
    }

    /**
     * Resumes the given list of paused transfers.
     */
    suspend fun resumeItems(items: List<TransferItem>): List<Result<Unit>> = withContext(Dispatchers.IO) {
        items.map { resumeTransfer(it) }
    }

    /**
     * Executes items concurrently with dynamic worker scaling based on [concurrencyFlow].
     * If concurrency is adjusted during execution (e.g. from UI settings), workers scale up
     * immediately or scale down gracefully as in-flight items complete.
     */
    private suspend fun <T> processWithDynamicConcurrency(
        items: List<T>,
        concurrencyFlow: StateFlow<Int> = FileTransferQueueManager.concurrencyWorkers,
        processItem: suspend (T) -> Unit
    ) = coroutineScope {
        if (items.isEmpty()) return@coroutineScope

        val totalItems = items.size
        val processedItems = AtomicInteger(0)

        val channel = Channel<T>(Channel.UNLIMITED)
        for (item in items) {
            channel.send(item)
        }
        channel.close()

        val activeWorkers = AtomicInteger(0)
        lateinit var supervisor: Job

        fun launchWorker() {
            launch {
                try {
                    while (isActive) {
                        if (activeWorkers.get() > concurrencyFlow.value.coerceIn(1, 4)) {
                            // Target concurrency reduced: worker gracefully steps down
                            break
                        }
                        val receive = channel.receiveCatching()
                        if (receive.isClosed) {
                            break
                        }
                        val item = receive.getOrNull() ?: break
                        try {
                            processItem(item)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error in dynamic concurrency worker", e)
                        } finally {
                            val finished = processedItems.incrementAndGet()
                            if (finished >= totalItems) {
                                supervisor.cancel()
                            }
                        }
                    }
                } finally {
                    val remaining = activeWorkers.decrementAndGet()
                    if (remaining == 0 && processedItems.get() >= totalItems) {
                        supervisor.cancel()
                    } else if (processedItems.get() < totalItems && activeWorkers.get() < concurrencyFlow.value.coerceIn(1, 4)) {
                        val current = activeWorkers.incrementAndGet()
                        if (current <= concurrencyFlow.value.coerceIn(1, 4)) {
                            launchWorker()
                        } else {
                            activeWorkers.decrementAndGet()
                        }
                    }
                }
            }
        }

        supervisor = launch {
            concurrencyFlow.collect { desired ->
                val target = desired.coerceIn(1, 4)
                while (activeWorkers.get() < target && processedItems.get() < totalItems) {
                    val current = activeWorkers.incrementAndGet()
                    if (current <= target) {
                        launchWorker()
                    } else {
                        activeWorkers.decrementAndGet()
                        break
                    }
                }
            }
        }
    }
}
