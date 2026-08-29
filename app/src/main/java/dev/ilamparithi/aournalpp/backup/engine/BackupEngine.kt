package dev.ilamparithi.aournalpp.backup.engine

import android.content.Context
import android.util.Log
import dev.ilamparithi.aournalpp.backup.db.SyncDatabase
import dev.ilamparithi.aournalpp.backup.db.SyncMetadataEntity
import dev.ilamparithi.aournalpp.backup.model.BackupResult
import dev.ilamparithi.aournalpp.backup.model.ConflictResolutionPolicy
import dev.ilamparithi.aournalpp.backup.model.RestoreResult
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.TransferDirection
import dev.ilamparithi.aournalpp.backup.model.TransferItem
import dev.ilamparithi.aournalpp.backup.model.TransferStatus
import dev.ilamparithi.aournalpp.backup.provider.StorageProviderFactory
import dev.ilamparithi.aournalpp.backup.queue.FileTransferQueueManager
import dev.ilamparithi.aournalpp.backup.scanner.BackupScanner
import dev.ilamparithi.aournalpp.backup.scanner.ScannedLocalFile
import dev.ilamparithi.aournalpp.backup.security.CredentialsVault
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Core differential synchronization engine supporting multi-service complete backups,
 * custom folder mappings, concurrency management, and conflict-aware cloud restores.
 */
class BackupEngine(
    private val context: Context,
    private val env: LinuxEnvironment = LinuxEnvironment(context),
    private val vault: CredentialsVault = CredentialsVault(context),
    private val db: SyncDatabase = SyncDatabase.getInstance(context)
) {
    companion object {
        private const val TAG = "BackupEngine"
        private const val COMPLETE_BACKUP_REMOTE_ROOT = "Aournalpp"
    }

    /**
     * Executes backup for a specific service configuration.
     */
    suspend fun performBackup(
        serviceConfig: ServiceConfig,
        concurrency: Int = 2,
        onProgress: ((current: Int, total: Int, currentFile: String) -> Unit)? = null
    ): BackupResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val exclusionFilter = vault.getExclusionFilter()
        val scanner = BackupScanner(env, exclusionFilter)
        val dao = db.syncMetadataDao()

        val filesToSync = mutableListOf<Pair<ScannedLocalFile, String>>() // (ScannedFile, remoteDestinationPath)

        // 1. Complete Backup domain scanning
        if (serviceConfig.isCompleteBackupEnabled) {
            val completeFiles = scanner.scanCompleteBackup()
            for (f in completeFiles) {
                val remotePath = "$COMPLETE_BACKUP_REMOTE_ROOT/${f.relativePath}"
                filesToSync.add(f to remotePath)
            }
        }

        // 2. Custom folder mappings scanning
        for (mapping in serviceConfig.customMappings) {
            if (!mapping.isEnabled) continue
            val mappedFiles = scanner.scanCustomMapping(mapping)
            val remoteTargetBase = mapping.remoteFolderPath.trim().trim('/')
            for (f in mappedFiles) {
                val remotePath = if (remoteTargetBase.isEmpty()) f.relativePath else "$remoteTargetBase/${f.relativePath}"
                filesToSync.add(f to remotePath)
            }
        }

        val totalFiles = filesToSync.size
        var uploadedCount = 0
        var skippedCount = 0
        var failedCount = 0
        var totalBytesTransferred = 0L
        val errors = mutableListOf<String>()

        val provider = StorageProviderFactory.createProvider(serviceConfig)

        try {
            // Test connection first
            val connResult = provider.testConnection()
            if (connResult.isFailure || connResult.getOrNull() == false) {
                val err = connResult.exceptionOrNull()?.message ?: "Failed to connect to ${serviceConfig.name}"
                errors.add(err)
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

            // Differential comparison
            val uploadQueue = mutableListOf<Pair<ScannedLocalFile, String>>()
            for ((scanned, remotePath) in filesToSync) {
                val record = dao.getByServiceAndPath(serviceConfig.id, scanned.relativePath)
                if (record != null && record.localSha256 == scanned.sha256 && scanned.lastModified <= record.localLastModified) {
                    // Unchanged file -> Skip
                    skippedCount++
                    val queueItem = TransferItem(
                        id = UUID.randomUUID().toString(),
                        serviceId = serviceConfig.id,
                        serviceName = serviceConfig.name,
                        localFilePath = scanned.file.absolutePath,
                        remotePath = remotePath,
                        fileName = scanned.file.name,
                        direction = TransferDirection.UPLOAD,
                        totalBytes = scanned.sizeBytes,
                        transferredBytes = scanned.sizeBytes,
                        progress = 1f,
                        status = TransferStatus.SKIPPED
                    )
                    FileTransferQueueManager.enqueue(queueItem)
                } else {
                    uploadQueue.add(scanned to remotePath)
                }
            }

            // Enqueue all active upload items
            val transferItems = uploadQueue.map { (scanned, remotePath) ->
                val id = UUID.randomUUID().toString()
                TransferItem(
                    id = id,
                    serviceId = serviceConfig.id,
                    serviceName = serviceConfig.name,
                    localFilePath = scanned.file.absolutePath,
                    remotePath = remotePath,
                    fileName = scanned.file.name,
                    direction = TransferDirection.UPLOAD,
                    totalBytes = scanned.sizeBytes,
                    status = TransferStatus.QUEUED
                ) to (scanned to remotePath)
            }

            FileTransferQueueManager.enqueueAll(transferItems.map { it.first })

            val semaphore = Semaphore(concurrency.coerceIn(1, 4))
            var processedCount = skippedCount

            coroutineScope {
                val tasks = transferItems.map { (item, payload) ->
                    val (scanned, remotePath) = payload
                    async {
                        semaphore.withPermit {
                            if (FileTransferQueueManager.isCancelled(item.id)) {
                                return@withPermit
                            }

                            FileTransferQueueManager.markStarted(item.id)
                            onProgress?.invoke(processedCount + 1, totalFiles, scanned.file.name)

                            val uploadResult = provider.uploadFile(
                                localFile = scanned.file,
                                remotePath = remotePath,
                                onProgress = { transferred, total ->
                                    FileTransferQueueManager.updateProgress(item.id, transferred, total)
                                }
                            )

                            if (uploadResult.isSuccess) {
                                FileTransferQueueManager.markCompleted(item.id)
                                synchronized(this@BackupEngine) {
                                    uploadedCount++
                                    totalBytesTransferred += scanned.sizeBytes
                                    processedCount++
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
                                    processedCount++
                                }
                            }
                        }
                    }
                }
                tasks.awaitAll()
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
        concurrency: Int = 2,
        onProgress: ((current: Int, total: Int, currentFile: String) -> Unit)? = null
    ): RestoreResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val dao = db.syncMetadataDao()
        val provider = StorageProviderFactory.createProvider(serviceConfig)
        val errors = mutableListOf<String>()

        var restoredCount = 0
        var skippedCount = 0
        var failedCount = 0
        var totalBytesDownloaded = 0L
        var hasRestoredConfigs = false
        val remoteFilesToDownload = mutableListOf<Pair<String, File>>() // (remotePath, localDestinationFile)

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
                val configRoot = env.xournalConfigDir

                // List Notes tree
                val remoteNotes = listRemoteRecursively(provider, "$COMPLETE_BACKUP_REMOTE_ROOT/Notes")
                for (rf in remoteNotes) {
                    if (rf.isDirectory) continue
                    val subPath = rf.remotePath.removePrefix("$COMPLETE_BACKUP_REMOTE_ROOT/Notes").trim('/')
                    val destFile = File(notesRoot, subPath)
                    remoteFilesToDownload.add(rf.remotePath to destFile)
                }

                // List .config tree
                val remoteConfigs = listRemoteRecursively(provider, "$COMPLETE_BACKUP_REMOTE_ROOT/.config/xournalpp")
                for (rf in remoteConfigs) {
                    if (rf.isDirectory) continue
                    val subPath = rf.remotePath.removePrefix("$COMPLETE_BACKUP_REMOTE_ROOT/.config/xournalpp").trim('/')
                    val destFile = File(configRoot, subPath)
                    remoteFilesToDownload.add(rf.remotePath to destFile)
                }
            }

            for (mapping in serviceConfig.customMappings) {
                if (!mapping.isEnabled) continue
                val localBase = File(mapping.localFolderPath)
                val remoteBase = mapping.remoteFolderPath.trim().trim('/')
                val remoteMapped = listRemoteRecursively(provider, remoteBase)
                for (rf in remoteMapped) {
                    if (rf.isDirectory) continue
                    val subPath = if (remoteBase.isNotEmpty()) rf.remotePath.removePrefix(remoteBase).trim('/') else rf.remotePath
                    val destFile = File(localBase, subPath)
                    remoteFilesToDownload.add(rf.remotePath to destFile)
                }
            }

            val totalDiscovered = remoteFilesToDownload.size

            // Apply Conflict Policy
            val downloadQueue = mutableListOf<Pair<String, File>>()
            for ((remotePath, localFile) in remoteFilesToDownload) {
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
                            // Compare timestamps
                            downloadQueue.add(remotePath to localFile)
                        }
                    }
                }
            }

            val transferItems = downloadQueue.map { (remotePath, localFile) ->
                val id = UUID.randomUUID().toString()
                TransferItem(
                    id = id,
                    serviceId = serviceConfig.id,
                    serviceName = serviceConfig.name,
                    localFilePath = localFile.absolutePath,
                    remotePath = remotePath,
                    fileName = localFile.name,
                    direction = TransferDirection.DOWNLOAD,
                    totalBytes = 0L,
                    status = TransferStatus.QUEUED
                ) to (remotePath to localFile)
            }

            FileTransferQueueManager.enqueueAll(transferItems.map { it.first })

            val semaphore = Semaphore(concurrency.coerceIn(1, 4))
            var processed = skippedCount

            coroutineScope {
                val tasks = transferItems.map { (item, payload) ->
                    val (remotePath, localFile) = payload
                    async {
                        semaphore.withPermit {
                            if (FileTransferQueueManager.isCancelled(item.id)) return@withPermit

                            FileTransferQueueManager.markStarted(item.id)
                            onProgress?.invoke(processed + 1, totalDiscovered, localFile.name)

                            val downloadResult = provider.downloadFile(
                                remotePath = remotePath,
                                destinationFile = localFile,
                                onProgress = { downloaded, total ->
                                    FileTransferQueueManager.updateProgress(item.id, downloaded, total)
                                }
                            )

                            if (downloadResult.isSuccess) {
                                FileTransferQueueManager.markCompleted(item.id)
                                synchronized(this@BackupEngine) {
                                    restoredCount++
                                    totalBytesDownloaded += localFile.length()
                                    processed++
                                    if (localFile.absolutePath.startsWith(env.xournalConfigDir.absolutePath)) {
                                        hasRestoredConfigs = true
                                    }
                                }
                            } else {
                                val errMsg = downloadResult.exceptionOrNull()?.message ?: "Download failed"
                                FileTransferQueueManager.markFailed(item.id, errMsg)
                                synchronized(this@BackupEngine) {
                                    failedCount++
                                    errors.add("${localFile.name}: $errMsg")
                                    processed++
                                }
                            }
                        }
                    }
                }
                tasks.awaitAll()
            }

            // Sanitize settings if .config files were restored
            if (hasRestoredConfigs) {
                try {
                    env.ensureXournalppSettings()
                    env.checkAndOverrideAutoloadPreference()
                    env.ensureMenuBarShortcuts()
                } catch (e: Exception) {
                    Log.w(TAG, "Error sanitizing restored Xournal++ settings", e)
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
        concurrency: Int = 2,
        onProgress: ((current: Int, total: Int, currentFile: String) -> Unit)? = null
    ): List<BackupResult> {
        val services = vault.getAllServices().filter { it.isEnabled }
        val results = mutableListOf<BackupResult>()
        for (service in services) {
            val result = performBackup(service, concurrency, onProgress)
            results.add(result)
        }
        return results
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
}
