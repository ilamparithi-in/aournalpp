package dev.ilamparithi.aournalpp.backup.security

import android.content.Context
import dev.ilamparithi.aournalpp.backup.engine.BackupEngine
import dev.ilamparithi.aournalpp.backup.model.CustomFolderMapping
import dev.ilamparithi.aournalpp.backup.model.FolderValidationResult
import dev.ilamparithi.aournalpp.backup.model.MappingSet
import dev.ilamparithi.aournalpp.backup.model.MappingTemplateItem
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository providing non-volatile persistence for custom folder mappings and reusable mapping sets.
 * Stores configuration in `context.filesDir/sync_mappings.json` and syncs with `.config/sync_mappings.json`
 * in the active notes home directory for cloud backup.
 */
class CustomMappingRepository(
    private val baseDir: File,
    private val notesHomeDir: File? = null
) {
    constructor(context: Context) : this(
        baseDir = context.filesDir,
        notesHomeDir = try { LinuxEnvironment(context).getNotesDirectory() } catch (_: Exception) { null }
    )

    private val configFile: File
        get() = File(baseDir, "sync_mappings.json")

    // In-memory cache for remote path existence to prevent network spam: key -> (exists, epochMs)
    private val remoteExistenceCache = ConcurrentHashMap<String, Pair<Boolean, Long>>()
    private val remoteCheckSemaphore = Semaphore(2)

    private val lock = Any()

    /**
     * Loads all mappings for a specific cloud service.
     */
    fun getMappingsForService(serviceId: String): List<CustomFolderMapping> {
        synchronized(lock) {
            val root = readJsonRoot()
            val servicesObj = root.optJSONObject("services") ?: return emptyList()
            val array = servicesObj.optJSONArray(serviceId) ?: return emptyList()

            val list = mutableListOf<CustomFolderMapping>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                list.add(
                    CustomFolderMapping(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        serviceId = obj.optString("serviceId", serviceId),
                        name = obj.optString("name", ""),
                        localFolderPath = obj.optString("localFolderPath", ""),
                        remoteFolderPath = obj.optString("remoteFolderPath", ""),
                        isEnabled = obj.optBoolean("isEnabled", true)
                    )
                )
            }
            return list
        }
    }

    /**
     * Saves mappings for a specific cloud service.
     */
    fun saveMappingsForService(serviceId: String, mappings: List<CustomFolderMapping>) {
        synchronized(lock) {
            val root = readJsonRoot()
            val servicesObj = root.optJSONObject("services") ?: JSONObject().also { root.put("services", it) }

            val array = JSONArray()
            for (m in mappings) {
                val obj = JSONObject().apply {
                    put("id", m.id)
                    put("serviceId", m.serviceId)
                    put("name", m.name)
                    put("localFolderPath", m.localFolderPath)
                    put("remoteFolderPath", m.remoteFolderPath)
                    put("isEnabled", m.isEnabled)
                }
                array.put(obj)
            }
            servicesObj.put(serviceId, array)
            writeJsonRoot(root)
        }
    }

    /**
     * Retrieves all saved reusable mapping sets (templates).
     */
    fun getAllMappingSets(): List<MappingSet> {
        synchronized(lock) {
            val root = readJsonRoot()
            val setsArray = root.optJSONArray("mappingSets") ?: return emptyList()

            val list = mutableListOf<MappingSet>()
            for (i in 0 until setsArray.length()) {
                val obj = setsArray.optJSONObject(i) ?: continue
                val itemsArray = obj.optJSONArray("items") ?: JSONArray()
                val items = mutableListOf<MappingTemplateItem>()
                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.optJSONObject(j) ?: continue
                    items.add(
                        MappingTemplateItem(
                            id = itemObj.optString("id", UUID.randomUUID().toString()),
                            name = itemObj.optString("name", ""),
                            localFolderPath = itemObj.optString("localFolderPath", ""),
                            remoteFolderPath = itemObj.optString("remoteFolderPath", ""),
                            isEnabled = itemObj.optBoolean("isEnabled", true)
                        )
                    )
                }

                list.add(
                    MappingSet(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Untitled Set"),
                        description = obj.optString("description", ""),
                        createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis()),
                        items = items
                    )
                )
            }
            return list
        }
    }

    /**
     * Saves or updates a reusable mapping set.
     */
    fun saveMappingSet(set: MappingSet) {
        synchronized(lock) {
            val existing = getAllMappingSets().toMutableList()
            val idx = existing.indexOfFirst { it.id == set.id }
            if (idx >= 0) {
                existing[idx] = set
            } else {
                existing.add(set)
            }

            val root = readJsonRoot()
            val setsArray = JSONArray()
            for (s in existing) {
                val sObj = JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("description", s.description)
                    put("createdAtEpochMs", s.createdAtEpochMs)

                    val itemsArr = JSONArray()
                    for (item in s.items) {
                        val itemObj = JSONObject().apply {
                            put("id", item.id)
                            put("name", item.name)
                            put("localFolderPath", item.localFolderPath)
                            put("remoteFolderPath", item.remoteFolderPath)
                            put("isEnabled", item.isEnabled)
                        }
                        itemsArr.put(itemObj)
                    }
                    put("items", itemsArr)
                }
                setsArray.put(sObj)
            }
            root.put("mappingSets", setsArray)
            writeJsonRoot(root)
        }
    }

    /**
     * Deletes a mapping set by ID.
     */
    fun deleteMappingSet(setId: String) {
        synchronized(lock) {
            val existing = getAllMappingSets().filterNot { it.id == setId }
            val root = readJsonRoot()
            val setsArray = JSONArray()
            for (s in existing) {
                val sObj = JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("description", s.description)
                    put("createdAtEpochMs", s.createdAtEpochMs)

                    val itemsArr = JSONArray()
                    for (item in s.items) {
                        val itemObj = JSONObject().apply {
                            put("id", item.id)
                            put("name", item.name)
                            put("localFolderPath", item.localFolderPath)
                            put("remoteFolderPath", item.remoteFolderPath)
                            put("isEnabled", item.isEnabled)
                        }
                        itemsArr.put(itemObj)
                    }
                    put("items", itemsArr)
                }
                setsArray.put(sObj)
            }
            root.put("mappingSets", setsArray)
            writeJsonRoot(root)
        }
    }

    /**
     * Applies a mapping set to a target service, returning the updated list of mappings.
     * @param replace If true, replaces existing mappings. If false, appends new mappings.
     */
    fun applyMappingSetToService(serviceId: String, set: MappingSet, replace: Boolean): List<CustomFolderMapping> {
        val existing = if (replace) emptyList() else getMappingsForService(serviceId)
        val newMappings = set.items.map { item ->
            CustomFolderMapping(
                id = UUID.randomUUID().toString(),
                serviceId = serviceId,
                name = item.name,
                localFolderPath = item.localFolderPath,
                remoteFolderPath = item.remoteFolderPath,
                isEnabled = item.isEnabled
            )
        }
        val combined = existing + newMappings
        saveMappingsForService(serviceId, combined)
        return combined
    }

    /**
     * Synchronizes internal `sync_mappings.json` to `.config/sync_mappings.json` in notes home.
     */
    fun syncToNotesHome(notesHomeDir: File) {
        try {
            if (!configFile.exists()) return
            val configDir = File(notesHomeDir, ".config").apply { if (!exists()) mkdirs() }
            val target = File(configDir, "sync_mappings.json")
            configFile.copyTo(target, overwrite = true)
        } catch (_: Exception) {}
    }

    /**
     * Restores internal `sync_mappings.json` from `.config/sync_mappings.json` in notes home if available.
     */
    fun restoreFromNotesHome(notesHomeDir: File) {
        try {
            val source = File(File(notesHomeDir, ".config"), "sync_mappings.json")
            if (source.exists() && source.isFile) {
                source.copyTo(configFile, overwrite = true)
            }
        } catch (_: Exception) {}
    }

    /**
     * Validates local and remote folder existence for a list of mappings.
     * Includes anti-spam safeguards: 60s cache and concurrency limiting (max 2 parallel requests).
     */
    suspend fun validateFolders(
        service: ServiceConfig,
        mappings: List<CustomFolderMapping>,
        engine: BackupEngine
    ): Map<String, FolderValidationResult> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, FolderValidationResult>()
        val now = System.currentTimeMillis()

        for (mapping in mappings) {
            // Local path existence check
            val localExists = try {
                val f = File(mapping.localFolderPath)
                f.exists() && f.isDirectory
            } catch (_: Exception) {
                false
            }

            // Remote path existence check with cache (TTL 60s)
            val cacheKey = "${service.id}:${mapping.remoteFolderPath.trim().trim('/')}"
            val cached = remoteExistenceCache[cacheKey]
            val remoteExists = if (cached != null && (now - cached.second) < 60_000L) {
                cached.first
            } else {
                remoteCheckSemaphore.withPermit {
                    val checkedVal = try {
                        val provider = engine.getStorageProvider(service)
                        try {
                            val cleanPath = mapping.remoteFolderPath.trim().trim('/')
                            if (cleanPath.isEmpty()) {
                                true
                            } else {
                                val parent = if (cleanPath.contains('/')) cleanPath.substringBeforeLast('/') else ""
                                val folderName = if (cleanPath.contains('/')) cleanPath.substringAfterLast('/') else cleanPath
                                val filesResult = provider.listFiles(parent)
                                val files = filesResult.getOrNull() ?: emptyList()
                                files.any {
                                    val itemName = it.remotePath.trimEnd('/').substringAfterLast('/')
                                    itemName.equals(folderName, ignoreCase = true) && it.isDirectory
                                }
                            }
                        } finally {
                            provider.disconnect()
                        }
                    } catch (_: Exception) {
                        false
                    }
                    remoteExistenceCache[cacheKey] = Pair(checkedVal, now)
                    checkedVal
                }
            }

            results[mapping.id] = FolderValidationResult(
                mappingId = mapping.id,
                localExists = localExists,
                remoteExists = remoteExists,
                checkedAtEpochMs = now
            )
        }

        results
    }

    private fun readJsonRoot(): JSONObject {
        if (!configFile.exists() && notesHomeDir != null) {
            restoreFromNotesHome(notesHomeDir)
        }
        return try {
            if (configFile.exists()) {
                val text = configFile.readText(Charsets.UTF_8)
                JSONObject(text)
            } else {
                JSONObject().apply { put("version", 1) }
            }
        } catch (_: Exception) {
            JSONObject().apply { put("version", 1) }
        }
    }

    private fun writeJsonRoot(root: JSONObject) {
        try {
            configFile.writeText(root.toString(2), Charsets.UTF_8)
            notesHomeDir?.let { syncToNotesHome(it) }
        } catch (_: Exception) {}
    }
}
