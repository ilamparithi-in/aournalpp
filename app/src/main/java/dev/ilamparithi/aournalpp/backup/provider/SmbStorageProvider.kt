package dev.ilamparithi.aournalpp.backup.provider

import android.util.Log
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import dev.ilamparithi.aournalpp.backup.model.RemoteFileMetadata
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.EnumSet

/**
 * Storage provider for Generic SMB2/SMB3 Windows and Samba network shares using smbj.
 */
class SmbStorageProvider(
    private val config: ServiceConfig
) : CloudStorageProvider {

    companion object {
        private const val TAG = "SmbStorageProvider"
    }

    override val providerType: StorageProviderType = StorageProviderType.SMB3

    private var client: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var share: DiskShare? = null

    @Synchronized
    private fun getOrConnectShare(): DiskShare {
        val currentShare = share
        if (currentShare != null && currentShare.isConnected) {
            return currentShare
        }

        disconnectInternal()

        val host = config.host.ifBlank { config.serverUrl.removePrefix("smb://").substringBefore(':').substringBefore('/') }.trim()
        val port = if (config.port > 0) config.port else 445
        val shareName = config.shareName.trim().trim('/')
        if (shareName.isEmpty()) {
            error("SMB share name is required (e.g. 'Backups' or 'Documents')")
        }

        val smbClient = SMBClient()
        val conn = smbClient.connect(host, port)
        val auth = AuthenticationContext(
            config.username.trim(),
            config.passwordOrSecret.toCharArray(),
            config.domain.trim()
        )
        val sess = conn.authenticate(auth)
        val diskShare = sess.connectShare(shareName) as? DiskShare
            ?: error("Share '$shareName' is not a valid disk share")

        this.client = smbClient
        this.connection = conn
        this.session = sess
        this.share = diskShare
        return diskShare
    }

    @Synchronized
    private fun disconnectInternal() {
        try { share?.close() } catch (e: Exception) { Log.d(TAG, "Error closing share", e) }
        try { session?.close() } catch (e: Exception) { Log.d(TAG, "Error closing session", e) }
        try { connection?.close() } catch (e: Exception) { Log.d(TAG, "Error closing connection", e) }
        try { client?.close() } catch (e: Exception) { Log.d(TAG, "Error closing client", e) }
        share = null
        session = null
        connection = null
        client = null
    }

    private fun normalizePath(path: String): String {
        val base = config.remoteBasePath.trim().trim('/', '\\').replace('/', '\\')
        val rel = path.trim().trim('/', '\\').replace('/', '\\')
        return if (base.isEmpty()) rel else if (rel.isEmpty()) base else "$base\\$rel"
    }

    override suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val diskShare = getOrConnectShare()
            diskShare.isConnected
        }
    }

    override suspend fun listFiles(remoteDirectory: String): Result<List<RemoteFileMetadata>> = withContext(Dispatchers.IO) {
        runCatching {
            val diskShare = getOrConnectShare()
            val smbPath = normalizePath(remoteDirectory)
            if (smbPath.isNotEmpty() && !diskShare.folderExists(smbPath)) {
                return@runCatching emptyList()
            }

            val fileInfoList = if (smbPath.isEmpty()) diskShare.list("") else diskShare.list(smbPath)
            val list = mutableListOf<RemoteFileMetadata>()

            val cleanRelDir = remoteDirectory.trim('/').replace('\\', '/')

            for (fileInfo in fileInfoList) {
                val name = fileInfo.fileName
                if (name == "." || name == "..") continue

                val isDir = (fileInfo.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
                val size = fileInfo.endOfFile
                val mtimeEpoch = fileInfo.changeTime.toEpochMillis()
                val itemRelPath = if (cleanRelDir.isEmpty()) name else "$cleanRelDir/$name"

                list.add(
                    RemoteFileMetadata(
                        remotePath = itemRelPath,
                        isDirectory = isDir,
                        sizeBytes = size,
                        lastModifiedEpochMs = mtimeEpoch,
                        contentHash = null
                    )
                )
            }
            list
        }
    }

    override suspend fun createDirectory(remoteDirectory: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val diskShare = getOrConnectShare()
            val smbPath = normalizePath(remoteDirectory)
            if (smbPath.isEmpty()) return@runCatching

            val parts = smbPath.split('\\')
            var currentPath = ""
            for (part in parts) {
                if (part.isEmpty()) continue
                currentPath = if (currentPath.isEmpty()) part else "$currentPath\\$part"
                if (!diskShare.folderExists(currentPath)) {
                    diskShare.mkdir(currentPath)
                }
            }
        }
    }

    override suspend fun uploadFile(
        localFile: File,
        remotePath: String,
        onProgress: (bytesTransferred: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val diskShare = getOrConnectShare()
            val smbPath = normalizePath(remotePath)
            val parentPath = smbPath.substringBeforeLast('\\', "")
            if (parentPath.isNotEmpty() && !diskShare.folderExists(parentPath)) {
                createDirectory(parentPath.replace('\\', '/')).getOrThrow()
            }

            val totalBytes = localFile.length()
            val smbFile = diskShare.openFile(
                smbPath,
                EnumSet.of(AccessMask.GENERIC_WRITE, AccessMask.GENERIC_READ),
                EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ, SMB2ShareAccess.FILE_SHARE_WRITE),
                SMB2CreateDisposition.FILE_OVERWRITE_IF,
                null
            )

            try {
                smbFile.outputStream.use { out ->
                    localFile.inputStream().use { input ->
                        val buffer = ByteArray(16384)
                        var bytesRead: Int
                        var uploaded = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            out.write(buffer, 0, bytesRead)
                            uploaded += bytesRead
                            onProgress(uploaded, totalBytes)
                        }
                    }
                }
            } finally {
                smbFile.close()
            }
        }
    }

    override suspend fun downloadFile(
        remotePath: String,
        destinationFile: File,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val diskShare = getOrConnectShare()
            val smbPath = normalizePath(remotePath)
            val smbFile = diskShare.openFile(
                smbPath,
                EnumSet.of(AccessMask.GENERIC_READ),
                EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                null
            )

            destinationFile.parentFile?.mkdirs()
            val tempFile = File(destinationFile.parentFile, "${destinationFile.name}.download.tmp")

            try {
                val totalBytes = smbFile.fileInformation.standardInformation.endOfFile
                smbFile.inputStream.use { input ->
                    tempFile.outputStream().use { out ->
                        val buffer = ByteArray(16384)
                        var bytesRead: Int
                        var downloaded = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            out.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            onProgress(downloaded, totalBytes)
                        }
                    }
                }
            } finally {
                smbFile.close()
            }

            if (destinationFile.exists()) destinationFile.delete()
            tempFile.renameTo(destinationFile)
            Unit
        }
    }

    override suspend fun deleteFile(remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val diskShare = getOrConnectShare()
            val smbPath = normalizePath(remotePath)
            if (diskShare.fileExists(smbPath)) {
                diskShare.rm(smbPath)
            } else if (diskShare.folderExists(smbPath)) {
                diskShare.rmdir(smbPath, true)
            }
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        disconnectInternal()
    }
}
