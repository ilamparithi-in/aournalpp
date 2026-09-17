package dev.ilamparithi.aournalpp.backup.engine

import android.content.Context
import android.util.Log
import dev.ilamparithi.aournalpp.backup.model.FileConflictGroup
import dev.ilamparithi.aournalpp.backup.model.FileVersionItem
import dev.ilamparithi.aournalpp.backup.model.FileVersionSource
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Manages in-memory and persistent disk storage of detected unresolved file conflicts.
 * Ensures conflicts detected during background or manual sync survive process termination.
 */
class ConflictPersistenceManager internal constructor(storageDir: File) {

    private constructor(context: Context) : this(context.filesDir)

    companion object {
        private const val TAG = "ConflictPersistence"
        private const val CONFLICTS_FILE_NAME = "unresolved_cloud_conflicts.json"

        @Volatile
        private var INSTANCE: ConflictPersistenceManager? = null

        fun getInstance(context: Context): ConflictPersistenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConflictPersistenceManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun createForTesting(testDir: File): ConflictPersistenceManager {
            return ConflictPersistenceManager(testDir)
        }
    }

    private val conflictsFile: File = File(storageDir, CONFLICTS_FILE_NAME)
    private val _unresolvedConflicts = MutableStateFlow<List<FileConflictGroup>>(emptyList())
    val unresolvedConflicts: StateFlow<List<FileConflictGroup>> = _unresolvedConflicts.asStateFlow()

    init {
        loadFromDisk()
    }

    @Synchronized
    fun setConflicts(conflicts: List<FileConflictGroup>) {
        _unresolvedConflicts.value = conflicts
        saveToDisk(conflicts)
    }

    @Synchronized
    fun saveConflicts(conflicts: List<FileConflictGroup>) {
        setConflicts(conflicts)
    }

    @Synchronized
    fun addConflicts(newConflicts: List<FileConflictGroup>) {
        if (newConflicts.isEmpty()) return
        val current = _unresolvedConflicts.value.toMutableList()
        for (newC in newConflicts) {
            val existingIndex = current.indexOfFirst { it.relativePath == newC.relativePath }
            if (existingIndex >= 0) {
                current[existingIndex] = newC
            } else {
                current.add(newC)
            }
        }
        _unresolvedConflicts.value = current
        saveToDisk(current)
    }

    @Synchronized
    fun removeConflict(conflictGroupId: String) {
        val current = _unresolvedConflicts.value.filterNot { it.id == conflictGroupId }
        _unresolvedConflicts.value = current
        saveToDisk(current)
    }

    @Synchronized
    fun clearConflicts() {
        _unresolvedConflicts.value = emptyList()
        try {
            if (conflictsFile.exists()) {
                conflictsFile.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete conflicts file", e)
        }
    }

    private fun loadFromDisk() {
        try {
            if (!conflictsFile.exists()) return
            val jsonStr = conflictsFile.readText()
            if (jsonStr.isBlank()) return
            val array = JSONArray(jsonStr)
            val list = mutableListOf<FileConflictGroup>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                deserializeConflictGroup(obj)?.let { list.add(it) }
            }
            _unresolvedConflicts.value = list
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load conflicts from disk", e)
        }
    }

    private fun saveToDisk(conflicts: List<FileConflictGroup>) {
        try {
            if (conflicts.isEmpty()) {
                if (conflictsFile.exists()) conflictsFile.delete()
                return
            }
            val array = JSONArray()
            for (c in conflicts) {
                array.put(serializeConflictGroup(c))
            }
            conflictsFile.writeText(array.toString(2))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save conflicts to disk", e)
        }
    }

    private fun serializeConflictGroup(group: FileConflictGroup): JSONObject {
        return JSONObject().apply {
            put("id", group.id)
            put("relativePath", group.relativePath)
            group.localVersion?.let { put("localVersion", serializeVersionItem(it)) }
            val remotesArray = JSONArray()
            for (rv in group.remoteVersions) {
                remotesArray.put(serializeVersionItem(rv))
            }
            put("remoteVersions", remotesArray)
            group.description?.let { put("description", it) }
            group.localFilePath?.let { put("localFilePath", it) }
            group.remoteFilePath?.let { put("remoteFilePath", it) }
        }
    }

    private fun serializeVersionItem(item: FileVersionItem): JSONObject {
        return JSONObject().apply {
            put("fileName", item.fileName)
            put("relativePath", item.relativePath)
            put("localFilePath", item.localFilePath)
            put("sizeBytes", item.sizeBytes)
            put("lastModifiedEpochMs", item.lastModifiedEpochMs)
            item.contentHash?.let { put("contentHash", it) }
            item.remotePath?.let { put("remotePath", it) }
            when (val src = item.source) {
                is FileVersionSource.LOCAL -> {
                    put("sourceType", "LOCAL")
                }
                is FileVersionSource.REMOTE -> {
                    put("sourceType", "REMOTE")
                    put("serviceId", src.serviceId)
                    put("serviceName", src.serviceName)
                    put("providerType", src.providerType.id)
                    src.mappingId?.let { put("mappingId", it) }
                    src.mappingRemotePath?.let { put("mappingRemotePath", it) }
                }
            }
        }
    }

    private fun deserializeConflictGroup(obj: JSONObject): FileConflictGroup? {
        val id = obj.optString("id", java.util.UUID.randomUUID().toString())
        val relativePath = obj.optString("relativePath", "")
        if (relativePath.isEmpty()) return null

        val localObj = obj.optJSONObject("localVersion")
        val localVersion = localObj?.let { deserializeVersionItem(it) }

        val remotesArray = obj.optJSONArray("remoteVersions")
        val remoteVersions = mutableListOf<FileVersionItem>()
        if (remotesArray != null) {
            for (i in 0 until remotesArray.length()) {
                val rObj = remotesArray.optJSONObject(i) ?: continue
                deserializeVersionItem(rObj)?.let { remoteVersions.add(it) }
            }
        }

        return FileConflictGroup(
            id = id,
            relativePath = relativePath,
            localVersion = localVersion,
            remoteVersions = remoteVersions,
            description = obj.optString("description").takeIf { it.isNotEmpty() },
            localFilePath = obj.optString("localFilePath").takeIf { it.isNotEmpty() },
            remoteFilePath = obj.optString("remoteFilePath").takeIf { it.isNotEmpty() }
        )
    }

    private fun deserializeVersionItem(obj: JSONObject): FileVersionItem? {
        val fileName = obj.optString("fileName", "")
        val relativePath = obj.optString("relativePath", "")
        val localFilePath = obj.optString("localFilePath", "")
        val sizeBytes = obj.optLong("sizeBytes", 0L)
        val lastModified = obj.optLong("lastModifiedEpochMs", 0L)
        val contentHash = obj.optString("contentHash").takeIf { it.isNotEmpty() }
        val remotePath = obj.optString("remotePath").takeIf { it.isNotEmpty() }

        val sourceType = obj.optString("sourceType", "LOCAL")
        val source = if (sourceType == "REMOTE") {
            val sId = obj.optString("serviceId", "")
            val sName = obj.optString("serviceName", "")
            val pTypeId = obj.optString("providerType", "webdav")
            val pType = StorageProviderType.fromId(pTypeId)
            val mId = obj.optString("mappingId").takeIf { it.isNotEmpty() }
            val mPath = obj.optString("mappingRemotePath").takeIf { it.isNotEmpty() }
            FileVersionSource.REMOTE(sId, sName, pType, mId, mPath)
        } else {
            FileVersionSource.LOCAL
        }

        return FileVersionItem(
            source = source,
            fileName = fileName,
            relativePath = relativePath,
            localFilePath = localFilePath,
            sizeBytes = sizeBytes,
            lastModifiedEpochMs = lastModified,
            contentHash = contentHash,
            remotePath = remotePath
        )
    }
}
