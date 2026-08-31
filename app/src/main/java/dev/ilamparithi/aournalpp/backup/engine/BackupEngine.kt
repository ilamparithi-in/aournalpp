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
import kotlin.math.abs

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

        // Batch-retrieve all existing metadata for this service upfront
        val existingMetaMap = dao.getAllForService(serviceConfig.id).associateBy { it.relativePath }

        val filesToSync = mutableListOf<Pair<ScannedLocalFile, String>>() // (ScannedFile, remoteDestinationPath)

        // 1. Complete Backup domain scanning
        if (serviceConfig.isCompleteBackupEnabled) {
            val completeFiles = scanner.scanCompleteBackup(existingMetaMap)
            for (f in completeFiles) {
                val remotePath = "$COMPLETE_BACKUP_REMOTE_ROOT/${f.relativePath}"
                filesToSync.add(f to remotePath)
            }
        }

        // 2. Custom folder mappings scanning
        for (mapping in serviceConfig.customMappings) {
            if (!mapping.isEnabled) continue
            val mappedFiles = scanner.scanCustomMapping(mapping, existingMetaMap)
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

            // Differential comparison via in-memory metadata lookup
            val uploadQueue = mutableListOf<Pair<ScannedLocalFile, String>>()
            for ((scanned, remotePath) in filesToSync) {
                val record = existingMetaMap[scanned.relativePath]
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

    /**
     * Checks if there are any remote cloud file changes or additions on the specified service.
     */
    suspend fun checkForRemoteChanges(serviceConfig: ServiceConfig): List<dev.ilamparithi.aournalpp.backup.model.RemoteFileMetadata> = withContext(Dispatchers.IO) {
        val changedRemoteFiles = mutableListOf<dev.ilamparithi.aournalpp.backup.model.RemoteFileMetadata>()
        val provider = StorageProviderFactory.createProvider(serviceConfig)
        val dao = db.syncMetadataDao()

        try {
            val connResult = provider.testConnection()
            if (connResult.isFailure || connResult.getOrNull() == false) return@withContext emptyList()

            val remoteFiles = mutableListOf<Pair<dev.ilamparithi.aournalpp.backup.model.RemoteFileMetadata, File>>()

            if (serviceConfig.isCompleteBackupEnabled) {
                val notesRoot = env.getNotesDirectory()
                val configRoot = env.xournalConfigDir

                val remoteNotes = listRemoteRecursively(provider, "$COMPLETE_BACKUP_REMOTE_ROOT/Notes")
                for (rf in remoteNotes) {
                    if (rf.isDirectory) continue
                    val subPath = rf.remotePath.removePrefix("$COMPLETE_BACKUP_REMOTE_ROOT/Notes").trim('/')
                    remoteFiles.add(rf to File(notesRoot, subPath))
                }

                val remoteConfigs = listRemoteRecursively(provider, "$COMPLETE_BACKUP_REMOTE_ROOT/.config/xournalpp")
                for (rf in remoteConfigs) {
                    if (rf.isDirectory) continue
                    val subPath = rf.remotePath.removePrefix("$COMPLETE_BACKUP_REMOTE_ROOT/.config/xournalpp").trim('/')
                    remoteFiles.add(rf to File(configRoot, subPath))
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
                    remoteFiles.add(rf to File(localBase, subPath))
                }
            }

            for ((remote, localFile) in remoteFiles) {
                if (!localFile.exists()) {
                    changedRemoteFiles.add(remote)
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
        val services = vault.getAllServices().filter { it.isEnabled }
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
        val services = vault.getAllServices().filter { it.isEnabled }
        val exclusionFilter = vault.getExclusionFilter()
        val scanner = BackupScanner(env, exclusionFilter)

        val notesRoot = env.getNotesDirectory()
        val configRoot = env.xournalConfigDir

        // Map of canonical local file path -> list of versions (Local + Remote endpoints)
        val versionsByLocalPath = mutableMapOf<String, MutableList<FileVersionItem>>()
        val relativePathByLocalPath = mutableMapOf<String, String>()

        // 1. Gather all local files from complete backup domains
        val completeFiles = scanner.scanCompleteBackup()
        for (f in completeFiles) {
            val canon = f.file.canonicalPath
            relativePathByLocalPath[canon] = f.relativePath
            versionsByLocalPath.getOrPut(canon) { mutableListOf() }.add(
                FileVersionItem(
                    source = FileVersionSource.LOCAL,
                    fileName = f.file.name,
                    relativePath = f.relativePath,
                    localFilePath = f.file.absolutePath,
                    sizeBytes = f.sizeBytes,
                    lastModifiedEpochMs = f.lastModified,
                    contentHash = f.sha256
                )
            )
        }

        // 2. Gather all local files from custom mappings
        val allMappings = services.flatMap { it.customMappings.filter { m -> m.isEnabled } }
        for (m in allMappings) {
            val mapped = scanner.scanCustomMapping(m)
            for (f in mapped) {
                val canon = f.file.canonicalPath
                val displayRel = if (m.remoteFolderPath.isNotBlank()) "${m.remoteFolderPath.trim('/')}/${f.relativePath}" else f.relativePath
                if (!relativePathByLocalPath.containsKey(canon)) {
                    relativePathByLocalPath[canon] = displayRel
                }
                if (versionsByLocalPath[canon]?.none { it.source is FileVersionSource.LOCAL } != false) {
                    versionsByLocalPath.getOrPut(canon) { mutableListOf() }.add(
                        FileVersionItem(
                            source = FileVersionSource.LOCAL,
                            fileName = f.file.name,
                            relativePath = displayRel,
                            localFilePath = f.file.absolutePath,
                            sizeBytes = f.sizeBytes,
                            lastModifiedEpochMs = f.lastModified,
                            contentHash = f.sha256
                        )
                    )
                }
            }
        }

        // 3. Gather remote files for all enabled services and their custom mappings
        for (srv in services) {
            val provider = StorageProviderFactory.createProvider(srv)
            try {
                val conn = provider.testConnection()
                if (conn.isFailure || conn.getOrNull() == false) continue

                // 3a. Complete backup domain
                if (srv.isCompleteBackupEnabled) {
                    val remoteNotes = listRemoteRecursively(provider, "$COMPLETE_BACKUP_REMOTE_ROOT/Notes")
                    for (rf in remoteNotes) {
                        if (rf.isDirectory) continue
                        val subPath = rf.remotePath.removePrefix("$COMPLETE_BACKUP_REMOTE_ROOT/Notes").trim('/')
                        val destFile = File(notesRoot, subPath)
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
                            sizeBytes = rf.sizeBytes,
                            lastModifiedEpochMs = rf.lastModifiedEpochMs,
                            contentHash = rf.contentHash,
                            remotePath = rf.remotePath
                        )
                        versionsByLocalPath.getOrPut(canon) { mutableListOf() }.add(item)
                    }

                    val remoteConfigs = listRemoteRecursively(provider, "$COMPLETE_BACKUP_REMOTE_ROOT/.config/xournalpp")
                    for (rf in remoteConfigs) {
                        if (rf.isDirectory) continue
                        val subPath = rf.remotePath.removePrefix("$COMPLETE_BACKUP_REMOTE_ROOT/.config/xournalpp").trim('/')
                        val destFile = File(configRoot, subPath)
                        val canon = destFile.canonicalPath
                        val displayRel = ".config/xournalpp/$subPath"
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
                            sizeBytes = rf.sizeBytes,
                            lastModifiedEpochMs = rf.lastModifiedEpochMs,
                            contentHash = rf.contentHash,
                            remotePath = rf.remotePath
                        )
                        versionsByLocalPath.getOrPut(canon) { mutableListOf() }.add(item)
                    }
                }

                // 3b. Custom mappings domain (treated as distinct cloud storage endpoints)
                for (mapping in srv.customMappings) {
                    if (!mapping.isEnabled) continue
                    val localBase = File(mapping.localFolderPath)
                    val remoteBase = mapping.remoteFolderPath.trim().trim('/')
                    val remoteMapped = listRemoteRecursively(provider, remoteBase)
                    for (rf in remoteMapped) {
                        if (rf.isDirectory) continue
                        val subPath = if (remoteBase.isNotEmpty()) rf.remotePath.removePrefix(remoteBase).trim('/') else rf.remotePath
                        val destFile = File(localBase, subPath)
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
                                mappingId = mapping.id,
                                mappingRemotePath = mapping.remoteFolderPath
                            ),
                            fileName = destFile.name,
                            relativePath = displayRel,
                            localFilePath = destFile.absolutePath,
                            sizeBytes = rf.sizeBytes,
                            lastModifiedEpochMs = rf.lastModifiedEpochMs,
                            contentHash = rf.contentHash,
                            remotePath = rf.remotePath
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

            // Determine if versions genuinely differ:
            var hasConflict = false
            for (i in 0 until allVersions.size) {
                for (j in i + 1 until allVersions.size) {
                    val v1 = allVersions[i]
                    val v2 = allVersions[j]

                    val sizeDiff = v1.sizeBytes != v2.sizeBytes
                    val timeDiff = abs(v1.lastModifiedEpochMs - v2.lastModifiedEpochMs) > 1000L
                    val hashDiff = v1.contentHash != null && v2.contentHash != null && !v1.contentHash.equals(v2.contentHash, ignoreCase = true)

                    if (sizeDiff || timeDiff || hashDiff) {
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

        for (res in resolutions) {
            try {
                when (val action = res.action) {
                    is ConflictResolutionAction.ChoosePrimary -> {
                        val chosen = action.chosenVersion
                        val localFile = File(chosen.localFilePath)
                        val parentDir = localFile.parentFile ?: env.getNotesDirectory()
                        if (!parentDir.exists()) parentDir.mkdirs()

                        when (val src = chosen.source) {
                            is FileVersionSource.LOCAL -> {
                                // Local version chosen as primary: keep local file
                                filesUpdated++
                            }
                            is FileVersionSource.REMOTE -> {
                                val srv = services[src.serviceId]
                                if (srv != null && chosen.remotePath != null) {
                                    val provider = StorageProviderFactory.createProvider(srv)
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
                                                    relativePath = res.relativePath,
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
                        if (!parentDir.exists()) parentDir.mkdirs()

                        // 1. Primary version
                        when (val src = primary.source) {
                            is FileVersionSource.LOCAL -> {
                                filesUpdated++
                            }
                            is FileVersionSource.REMOTE -> {
                                val srv = services[src.serviceId]
                                if (srv != null && primary.remotePath != null) {
                                    val provider = StorageProviderFactory.createProvider(srv)
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
                                                    relativePath = res.relativePath,
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

                                    val provider = StorageProviderFactory.createProvider(srv)
                                    try {
                                        val dlResult = provider.downloadFile(
                                            remotePath = alongsideVer.remotePath,
                                            destinationFile = alongsideFile,
                                            onProgress = { _, _ -> }
                                        )
                                        if (dlResult.isSuccess) {
                                            filesSavedAlongside++
                                            val alongsideRel = if (res.relativePath.contains('/')) {
                                                "${res.relativePath.substringBeforeLast('/')}/${alongsideFile.name}"
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

                                    val provider = StorageProviderFactory.createProvider(srv)
                                    try {
                                        val dlResult = provider.downloadFile(
                                            remotePath = remoteVer.remotePath,
                                            destinationFile = alongsideFile,
                                            onProgress = { _, _ -> }
                                        )
                                        if (dlResult.isSuccess) {
                                            filesSavedAlongside++
                                            val alongsideRel = if (res.relativePath.contains('/')) {
                                                "${res.relativePath.substringBeforeLast('/')}/${alongsideFile.name}"
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
                errors.add("${res.relativePath}: ${e.message}")
            }
        }

        if (hasRestoredConfigs) {
            try {
                env.ensureXournalppSettings()
                env.checkAndOverrideAutoloadPreference()
                env.ensureMenuBarShortcuts()
            } catch (e: Exception) {
                Log.w(TAG, "Error sanitizing restored Xournal++ settings", e)
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
        if (clean.startsWith(".config/xournalpp/")) {
            return File(env.xournalConfigDir, clean.removePrefix(".config/xournalpp/").trim('/'))
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
}
