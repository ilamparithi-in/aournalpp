package dev.ilamparithi.aournalpp.backup.db

import androidx.room.Entity

/**
 * Room database entity storing file synchronization hashes and timestamps
 * to facilitate fast differential transfers across multiple services.
 */
@Entity(
    tableName = "sync_metadata",
    primaryKeys = ["serviceId", "relativePath"]
)
data class SyncMetadataEntity(
    val serviceId: String,
    val relativePath: String,
    val scope: String,
    val localSha256: String,
    val remoteHash: String?,
    val localLastModified: Long,
    val sizeBytes: Long,
    val lastSyncedAt: Long
)
