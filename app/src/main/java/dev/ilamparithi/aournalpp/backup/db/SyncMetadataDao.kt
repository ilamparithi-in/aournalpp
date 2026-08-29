package dev.ilamparithi.aournalpp.backup.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: SyncMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(entities: List<SyncMetadataEntity>)

    @Query("SELECT * FROM sync_metadata WHERE serviceId = :serviceId AND relativePath = :relativePath LIMIT 1")
    suspend fun getByServiceAndPath(serviceId: String, relativePath: String): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata WHERE serviceId = :serviceId")
    suspend fun getAllForService(serviceId: String): List<SyncMetadataEntity>

    @Query("SELECT * FROM sync_metadata WHERE serviceId = :serviceId AND scope = :scope")
    suspend fun getForServiceAndScope(serviceId: String, scope: String): List<SyncMetadataEntity>

    @Query("DELETE FROM sync_metadata WHERE serviceId = :serviceId AND relativePath = :relativePath")
    suspend fun delete(serviceId: String, relativePath: String)

    @Query("DELETE FROM sync_metadata WHERE serviceId = :serviceId")
    suspend fun deleteAllForService(serviceId: String)

    @Query("DELETE FROM sync_metadata")
    suspend fun clearAll()
}
