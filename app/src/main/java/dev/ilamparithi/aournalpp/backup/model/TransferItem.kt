package dev.ilamparithi.aournalpp.backup.model

enum class TransferDirection {
    UPLOAD,
    DOWNLOAD
}

enum class TransferStatus {
    QUEUED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    FAILED,
    SKIPPED,
    CANCELLED
}

/**
 * Representation of a file currently in the transfer pipeline.
 */
data class TransferItem(
    val id: String,
    val serviceId: String,
    val serviceName: String,
    val localFilePath: String,
    val remotePath: String,
    val fileName: String,
    val direction: TransferDirection,
    val totalBytes: Long,
    val transferredBytes: Long = 0L,
    val progress: Float = 0f,
    val speedBytesPerSec: Long = 0L,
    val status: TransferStatus = TransferStatus.QUEUED,
    val errorMessage: String? = null,
    val startedAtEpochMs: Long = 0L,
    val completedAtEpochMs: Long = 0L,
    val scope: String? = null,
    val relativePath: String? = null
)
