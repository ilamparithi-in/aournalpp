package dev.ilamparithi.aournalpp.backup.provider

import android.util.Log
import dev.ilamparithi.aournalpp.backup.model.RemoteFileMetadata
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.StreamCopier
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.xfer.FileSystemFile
import net.schmizz.sshj.xfer.TransferListener
import java.io.File

/**
 * Storage provider for Generic SFTP servers using SSHJ.
 * Supports password authentication and SSH private key authentication with optional passphrase.
 */
class SftpStorageProvider(
    private val config: ServiceConfig
) : CloudStorageProvider {

    companion object {
        private const val TAG = "SftpStorageProvider"
    }

    override val providerType: StorageProviderType = StorageProviderType.SFTP

    private var sshClient: SSHClient? = null
    private var sftpClient: SFTPClient? = null

    @Synchronized
    private fun getOrConnectSftp(): SFTPClient {
        val currentSftp = sftpClient
        val currentSsh = sshClient
        if (currentSsh != null && currentSsh.isConnected && currentSsh.isAuthenticated && currentSftp != null) {
            return currentSftp
        }

        disconnectInternal()

        val host = config.host.ifBlank { config.serverUrl.removePrefix("sftp://").substringBefore(':').substringBefore('/') }.trim()
        val port = if (config.port > 0) config.port else 22
        val user = config.username.trim()

        val ssh = SSHClient()
        ssh.addHostKeyVerifier(PromiscuousVerifier())
        ssh.connectTimeout = 30000
        ssh.timeout = 30000

        ssh.connect(host, port)

        if (config.privateKey.isNotBlank()) {
            val keyProvider = if (config.privateKeyPassphrase.isNotBlank()) {
                ssh.loadKeys(config.privateKey, config.privateKeyPassphrase.toCharArray())
            } else {
                ssh.loadKeys(config.privateKey, null as CharArray?)
            }
            ssh.authPublickey(user, keyProvider)
        } else if (config.passwordOrSecret.isNotBlank()) {
            ssh.authPassword(user, config.passwordOrSecret)
        } else {
            error("No password or private key specified for SFTP connection")
        }

        val sftp = ssh.newSFTPClient()
        this.sshClient = ssh
        this.sftpClient = sftp
        return sftp
    }

    @Synchronized
    private fun disconnectInternal() {
        try {
            sftpClient?.close()
        } catch (e: Exception) {
            Log.d(TAG, "Error closing SFTPClient", e)
        }
        try {
            sshClient?.disconnect()
        } catch (e: Exception) {
            Log.d(TAG, "Error closing SSHClient", e)
        }
        sftpClient = null
        sshClient = null
    }

    private fun resolveRemotePath(path: String): String {
        val base = config.remoteBasePath.trim().trimEnd('/')
        val rel = path.trim().trimStart('/').replace('\\', '/')
        return if (base.isEmpty()) rel else "$base/$rel"
    }

    override suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val sftp = getOrConnectSftp()
            val base = config.remoteBasePath.trim().trimEnd('/')
            if (base.isNotEmpty()) {
                try {
                    sftp.statExistence(base)
                } catch (e: Exception) {
                    sftp.mkdirs(base)
                }
            }
            true
        }
    }

    override suspend fun listFiles(remoteDirectory: String): Result<List<RemoteFileMetadata>> = withContext(Dispatchers.IO) {
        runCatching {
            val sftp = getOrConnectSftp()
            val targetDir = resolveRemotePath(remoteDirectory)
            val stat = sftp.statExistence(targetDir) ?: return@runCatching emptyList()
            if (stat.type != FileMode.Type.DIRECTORY) {
                return@runCatching emptyList()
            }

            val entries = sftp.ls(targetDir)
            val list = mutableListOf<RemoteFileMetadata>()

            val cleanRelDir = remoteDirectory.trim('/').replace('\\', '/')

            for (entry in entries) {
                val name = entry.name
                if (name == "." || name == "..") continue

                val isDir = entry.attributes.type == FileMode.Type.DIRECTORY
                val size = entry.attributes.size
                val mtimeEpoch = entry.attributes.mtime * 1000L
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
            val sftp = getOrConnectSftp()
            val targetDir = resolveRemotePath(remoteDirectory)
            sftp.mkdirs(targetDir)
        }
    }

    override suspend fun uploadFile(
        localFile: File,
        remotePath: String,
        onProgress: (bytesTransferred: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sftp = getOrConnectSftp()
            val targetPath = resolveRemotePath(remotePath)
            val parentDir = File(targetPath).parent?.replace('\\', '/')
            if (!parentDir.isNullOrEmpty()) {
                sftp.mkdirs(parentDir)
            }

            val totalBytes = localFile.length()
            val listener = object : TransferListener {
                override fun directory(name: String?): TransferListener = this
                override fun file(name: String?, size: Long): StreamCopier.Listener {
                    return StreamCopier.Listener { transferred ->
                        onProgress(transferred, totalBytes)
                    }
                }
            }

            sftp.fileTransfer.transferListener = listener
            sftp.put(FileSystemFile(localFile), targetPath)
        }
    }

    override suspend fun downloadFile(
        remotePath: String,
        destinationFile: File,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sftp = getOrConnectSftp()
            val targetPath = resolveRemotePath(remotePath)
            val stat = sftp.stat(targetPath)
            val totalBytes = stat.size

            destinationFile.parentFile?.mkdirs()
            val tempFile = File(destinationFile.parentFile, "${destinationFile.name}.download.tmp")

            val listener = object : TransferListener {
                override fun directory(name: String?): TransferListener = this
                override fun file(name: String?, size: Long): StreamCopier.Listener {
                    return StreamCopier.Listener { transferred ->
                        onProgress(transferred, totalBytes)
                    }
                }
            }

            sftp.fileTransfer.transferListener = listener
            sftp.get(targetPath, FileSystemFile(tempFile))

            if (destinationFile.exists()) destinationFile.delete()
            tempFile.renameTo(destinationFile)
            Unit
        }
    }

    override suspend fun deleteFile(remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sftp = getOrConnectSftp()
            val targetPath = resolveRemotePath(remotePath)
            val stat = sftp.statExistence(targetPath) ?: return@runCatching
            if (stat.type == FileMode.Type.DIRECTORY) {
                sftp.rmdir(targetPath)
            } else {
                sftp.rm(targetPath)
            }
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        disconnectInternal()
    }
}
