package dev.ilamparithi.aournalpp.backup.provider

import dev.ilamparithi.aournalpp.backup.model.RemoteFileMetadata
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import java.io.File

/**
 * Universal transport protocol interface for remote cloud storage endpoints.
 */
interface CloudStorageProvider {
    val providerType: StorageProviderType

    /**
     * Validates connection credentials and remote reachability.
     */
    suspend fun testConnection(): Result<Boolean>

    /**
     * Lists files and folders within the specified remote directory.
     * @param remoteDirectory Relative or absolute path on the remote server (e.g., "Aournalpp/Notes").
     */
    suspend fun listFiles(remoteDirectory: String): Result<List<RemoteFileMetadata>>

    /**
     * Recursively creates remote directory structure if not already present.
     */
    suspend fun createDirectory(remoteDirectory: String): Result<Unit>

    /**
     * Uploads a local file to the specified remote destination path with progress reporting.
     * @param localFile Source file on the local filesystem.
     * @param remotePath Target destination path on the remote storage.
     * @param onProgress Callback invoked periodically with progress fraction (0.0 to 1.0) and transferred byte count.
     */
    suspend fun uploadFile(
        localFile: File,
        remotePath: String,
        onProgress: (bytesTransferred: Long, totalBytes: Long) -> Unit
    ): Result<Unit>

    /**
     * Downloads a file from remote storage to a local destination with progress reporting.
     * @param remotePath Target source path on the remote storage.
     * @param destinationFile Local file destination.
     * @param onProgress Callback invoked periodically with progress fraction (0.0 to 1.0) and downloaded byte count.
     */
    suspend fun downloadFile(
        remotePath: String,
        destinationFile: File,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<Unit>

    /**
     * Deletes a remote file or folder.
     */
    suspend fun deleteFile(remotePath: String): Result<Unit>

    /**
     * Gracefully closes network streams, sessions, and client connections.
     */
    suspend fun disconnect()
}
