package dev.ilamparithi.aournalpp.backup.provider

import android.util.Log
import dev.ilamparithi.aournalpp.backup.model.RemoteFileMetadata
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.ftp.FTPSClient
import java.io.File
import java.io.FilterInputStream
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Storage provider for Generic FTP and FTPS (Explicit/Implicit TLS) servers using Apache Commons Net.
 */
class FtpStorageProvider(
    private val config: ServiceConfig
) : CloudStorageProvider {

    companion object {
        private const val TAG = "FtpStorageProvider"
        private const val PROGRESS_STEP_BYTES = 32 * 1024L
        private const val PROGRESS_INTERVAL_MS = 100L
    }

    override val providerType: StorageProviderType = StorageProviderType.FTP

    private var ftpClient: FTPClient? = null

    @Synchronized
    private fun getOrConnectFtp(): FTPClient {
        val current = ftpClient
        if (current != null && current.isConnected && current.isAvailable) {
            return current
        }

        disconnectInternal()

        val host = config.host.ifBlank { config.serverUrl.removePrefix("ftp://").removePrefix("ftps://").substringBefore(':').substringBefore('/') }.trim()
        val port = if (config.port > 0) config.port else 21

        val client: FTPClient = if (config.isFtpsImplicit) {
            FTPSClient(true)
        } else if (config.isFtpsExplicit) {
            FTPSClient(false)
        } else {
            Log.w(TAG, "Security Warning: Cleartext FTP is configured for host '$host'. Credentials and notes will be sent across the network without encryption.")
            FTPClient()
        }

        client.connectTimeout = 30000
        client.defaultTimeout = 30000
        client.dataTimeout = java.time.Duration.ofSeconds(60)
        client.controlEncoding = "UTF-8"

        client.connect(host, port)
        val reply = client.replyCode
        if (!FTPReply.isPositiveCompletion(reply)) {
            client.disconnect()
            error("FTP server refused connection with reply code: $reply")
        }

        val user = config.username.ifBlank { "anonymous" }
        val pass = config.passwordOrSecret
        if (!client.login(user, pass)) {
            client.disconnect()
            error("FTP login failed for user: $user")
        }

        client.enterLocalPassiveMode()
        client.setFileType(FTP.BINARY_FILE_TYPE)

        this.ftpClient = client
        return client
    }

    @Synchronized
    private fun disconnectInternal() {
        try {
            ftpClient?.logout()
        } catch (e: Exception) {
            Log.d(TAG, "Error logging out from FTP", e)
        }
        try {
            ftpClient?.disconnect()
        } catch (e: Exception) {
            Log.d(TAG, "Error disconnecting FTP", e)
        }
        ftpClient = null
    }

    internal fun resolveRemotePath(path: String): String {
        val base = config.remoteBasePath.trim().trim('/')
        val cleanPath = path.trim().trim('/').replace('\\', '/')
        if (base.isEmpty()) return cleanPath
        if (cleanPath.isEmpty()) return base
        if (cleanPath == base || cleanPath.startsWith("$base/")) {
            return cleanPath
        }
        return "$base/$cleanPath"
    }

    override suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val client = getOrConnectFtp()
            client.sendNoOp()
        }
    }

    override suspend fun listFiles(remoteDirectory: String): Result<List<RemoteFileMetadata>> = withContext(Dispatchers.IO) {
        runCatching {
            val client = getOrConnectFtp()
            val targetPath = resolveRemotePath(remoteDirectory)
            val files = client.listFiles(targetPath) ?: emptyArray()

            val list = mutableListOf<RemoteFileMetadata>()
            val cleanRelDir = remoteDirectory.trim('/').replace('\\', '/')

            for (file in files) {
                val name = file.name
                if (name == "." || name == "..") continue

                val isDir = file.isDirectory
                val size = file.size
                val mtimeEpoch = file.timestamp?.timeInMillis ?: 0L
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
            val client = getOrConnectFtp()
            val targetPath = resolveRemotePath(remoteDirectory)
            if (targetPath.isEmpty()) return@runCatching

            val parts = targetPath.split('/')
            var currentPath = ""
            for (part in parts) {
                if (part.isEmpty()) continue
                currentPath = if (currentPath.isEmpty()) part else "$currentPath/$part"
                client.makeDirectory(currentPath)
            }
        }
    }

    override suspend fun uploadFile(
        localFile: File,
        remotePath: String,
        onProgress: (bytesTransferred: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val client = getOrConnectFtp()
            val targetPath = resolveRemotePath(remotePath)
            val parentDir = File(targetPath).parent?.replace('\\', '/')
            if (!parentDir.isNullOrEmpty()) {
                createDirectory(parentDir).getOrThrow()
            }

            val totalBytes = localFile.length()
            val countingInput = CountingInputStream(localFile.inputStream()) { transferred ->
                onProgress(transferred, totalBytes)
            }

            countingInput.use { input ->
                val success = client.storeFile(targetPath, input)
                if (!success) {
                    error("FTP upload failed for $targetPath: ${client.replyString}")
                }
            }
        }
    }

    override suspend fun downloadFile(
        remotePath: String,
        destinationFile: File,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val client = getOrConnectFtp()
            val targetPath = resolveRemotePath(remotePath)
            val ftpFile = client.mlistFile(targetPath)
            val totalBytes = ftpFile?.size ?: -1L

            destinationFile.parentFile?.mkdirs()
            val tempFile = File(destinationFile.parentFile, "${destinationFile.name}.download.tmp")

            val countingOutput = CountingOutputStream(tempFile.outputStream()) { downloaded ->
                onProgress(downloaded, if (totalBytes > 0) totalBytes else downloaded)
            }

            countingOutput.use { out ->
                val success = client.retrieveFile(targetPath, out)
                if (!success) {
                    error("FTP download failed for $targetPath: ${client.replyString}")
                }
            }

            if (destinationFile.exists()) destinationFile.delete()
            tempFile.renameTo(destinationFile)
            val mtime = ftpFile?.timestamp?.timeInMillis
            if (mtime != null && mtime > 0L) {
                destinationFile.setLastModified(mtime)
            }
            Unit
        }
    }

    override suspend fun deleteFile(remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val client = getOrConnectFtp()
            val targetPath = resolveRemotePath(remotePath)
            val deleted = client.deleteFile(targetPath)
            if (!deleted) {
                client.removeDirectory(targetPath)
            }
            Unit
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        disconnectInternal()
    }

    private class CountingInputStream(
        input: InputStream,
        private val onProgress: (Long) -> Unit
    ) : FilterInputStream(input) {
        private var bytesReadTotal = 0L
        private var lastReportedBytes = 0L
        private var lastReportedTime = 0L

        private fun notifyProgressIfNeeded(force: Boolean = false) {
            val now = System.currentTimeMillis()
            if (force || (bytesReadTotal - lastReportedBytes >= PROGRESS_STEP_BYTES) || (now - lastReportedTime >= PROGRESS_INTERVAL_MS)) {
                if (bytesReadTotal != lastReportedBytes || force) {
                    lastReportedBytes = bytesReadTotal
                    lastReportedTime = now
                    onProgress(bytesReadTotal)
                }
            }
        }

        override fun read(): Int {
            val b = super.read()
            if (b != -1) {
                bytesReadTotal++
                notifyProgressIfNeeded()
            } else {
                notifyProgressIfNeeded(force = true)
            }
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val count = super.read(b, off, len)
            if (count != -1) {
                bytesReadTotal += count
                notifyProgressIfNeeded()
            } else {
                notifyProgressIfNeeded(force = true)
            }
            return count
        }

        override fun close() {
            try {
                notifyProgressIfNeeded(force = true)
            } finally {
                super.close()
            }
        }
    }

    private class CountingOutputStream(
        output: OutputStream,
        private val onProgress: (Long) -> Unit
    ) : FilterOutputStream(output) {
        private var bytesWrittenTotal = 0L
        private var lastReportedBytes = 0L
        private var lastReportedTime = 0L

        private fun notifyProgressIfNeeded(force: Boolean = false) {
            val now = System.currentTimeMillis()
            if (force || (bytesWrittenTotal - lastReportedBytes >= PROGRESS_STEP_BYTES) || (now - lastReportedTime >= PROGRESS_INTERVAL_MS)) {
                if (bytesWrittenTotal != lastReportedBytes || force) {
                    lastReportedBytes = bytesWrittenTotal
                    lastReportedTime = now
                    onProgress(bytesWrittenTotal)
                }
            }
        }

        override fun write(b: Int) {
            super.write(b)
            bytesWrittenTotal++
            notifyProgressIfNeeded()
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            bytesWrittenTotal += len
            notifyProgressIfNeeded()
        }

        override fun close() {
            try {
                notifyProgressIfNeeded(force = true)
            } finally {
                super.close()
            }
        }
    }
}
