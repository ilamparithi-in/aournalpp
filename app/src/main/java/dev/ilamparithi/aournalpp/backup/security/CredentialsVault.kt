@file:Suppress("DEPRECATION")

package dev.ilamparithi.aournalpp.backup.security

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.ilamparithi.aournalpp.backup.model.CustomFolderMapping
import dev.ilamparithi.aournalpp.backup.model.ExclusionFilterConfig
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dev.ilamparithi.aournalpp.backup.worker.BackupScheduler

/**
 * Hardware-backed encrypted credential vault for storing remote cloud credentials,
 * service configurations, custom folder mappings, and exclusion filter rules.
 */
class CredentialsVault(context: Context) {

    companion object {
        private const val TAG = "CredentialsVault"
        private const val PREFS_FILE = "secure_cloud_credentials"
        private const val KEY_SERVICES = "configured_services_json"
        private const val KEY_ACTIVE_SERVICE_ID = "active_service_id"
        private const val KEY_EXCLUSION_FILTER = "exclusion_filter_json"
        private const val KEY_PENDING_DELETIONS = "pending_deleted_service_ids"
        private val vaultScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

        @Volatile
        private var INSTANCE: CredentialsVault? = null

        fun getInstance(context: Context): CredentialsVault {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CredentialsVault(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    @Volatile
    private var cachedServices: List<ServiceConfig>? = null

    @Volatile
    private var cachedPendingDeletions: Set<String>? = null

    @Volatile
    private var cachedExclusionFilter: ExclusionFilterConfig? = null

    @Volatile
    private var cachedActiveServiceId: String? = null
    @Volatile
    private var activeServiceIdLoaded: Boolean = false

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = try {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w(TAG, "Failed to initialize EncryptedSharedPreferences with MasterKey, falling back to standard private prefs", e)
        context.getSharedPreferences("${PREFS_FILE}_fallback", Context.MODE_PRIVATE)
    }

    @Synchronized
    fun getAllServices(): List<ServiceConfig> {
        cachedServices?.let { return it }
        val jsonStr = securePrefs.getString(KEY_SERVICES, null) ?: return emptyList<ServiceConfig>().also { cachedServices = it }
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ServiceConfig>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(deserializeService(obj))
            }
            cachedServices = list
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing services", e)
            cachedServices = emptyList()
            emptyList()
        }
    }

    @Synchronized
    fun getService(serviceId: String): ServiceConfig? {
        return getAllServices().firstOrNull { it.id == serviceId }
    }

    @Synchronized
    fun saveService(service: ServiceConfig) {
        val current = getAllServices().toMutableList()
        val index = current.indexOfFirst { it.id == service.id }
        if (index >= 0) {
            current[index] = service
        } else {
            current.add(service)
        }
        persistServices(current)
    }

    @Synchronized
    fun deleteService(serviceId: String) {
        val current = getAllServices().filterNot { it.id == serviceId }
        persistServices(current)
        if (getActiveServiceId() == serviceId) {
            setActiveServiceId(current.firstOrNull()?.id)
        }
    }

    @Synchronized
    fun getPendingDeletedServiceIds(): Set<String> {
        cachedPendingDeletions?.let { return it }
        val ids = securePrefs.getStringSet(KEY_PENDING_DELETIONS, emptySet())?.toSet() ?: emptySet()
        cachedPendingDeletions = ids
        return ids
    }

    @Synchronized
    fun markServicePendingDeletion(serviceId: String) {
        val current = getPendingDeletedServiceIds().toMutableSet()
        current.add(serviceId)
        cachedPendingDeletions = current
        securePrefs.edit().putStringSet(KEY_PENDING_DELETIONS, current).apply()
    }

    @Synchronized
    fun restorePendingDeletedService(serviceId: String) {
        val current = getPendingDeletedServiceIds().toMutableSet()
        current.remove(serviceId)
        cachedPendingDeletions = current
        securePrefs.edit().putStringSet(KEY_PENDING_DELETIONS, current).apply()
    }

    @Synchronized
    fun isServicePendingDeletion(serviceId: String): Boolean {
        return getPendingDeletedServiceIds().contains(serviceId)
    }

    @Synchronized
    fun getActiveConfiguredServices(): List<ServiceConfig> {
        val pending = getPendingDeletedServiceIds()
        return getAllServices().filterNot { it.id in pending }
    }

    /**
     * Permanently purges a cloud service from the device, including:
     * 1. Removal from CredentialsVault (services JSON and active service ID).
     * 2. Deletion of all Room database sync_metadata checksum records.
     * 3. Deletion of all custom folder mappings in CustomMappingRepository.
     * 4. Cancellation and clearing of file transfers in FileTransferQueueManager.
     * 5. Updating WorkManager backup schedules.
     */
    fun purgeServicePermanently(serviceId: String, context: Context) {
        // 1. Remove from vault
        deleteService(serviceId)

        // Also remove from pending deletions set
        val pending = getPendingDeletedServiceIds().toMutableSet()
        if (pending.remove(serviceId)) {
            cachedPendingDeletions = pending
            securePrefs.edit().putStringSet(KEY_PENDING_DELETIONS, pending).apply()
        }

        // 2. Delete Room database metadata
        try {
            val syncDb = dev.ilamparithi.aournalpp.backup.db.SyncDatabase.getInstance(context)
            vaultScope.launch {
                syncDb.syncMetadataDao().deleteAllForService(serviceId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete Room sync metadata for $serviceId", e)
        }

        // 3. Remove custom mappings
        try {
            val mappingRepo = CustomMappingRepository(context)
            mappingRepo.removeMappingsForService(serviceId)
            val env = dev.ilamparithi.aournalpp.runtime.LinuxEnvironment(context)
            mappingRepo.syncToNotesHome(env.getNotesDirectory())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove custom mappings for $serviceId", e)
        }

        // 4. Cancel & clear transfers
        try {
            dev.ilamparithi.aournalpp.backup.queue.FileTransferQueueManager.removeForService(serviceId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear transfer queue for $serviceId", e)
        }

        // 5. Update schedules
        try {
            BackupScheduler.updateSchedules(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update backup schedules after purging $serviceId", e)
        }
    }

    /**
     * Purges all services marked as pending deletion from previous app sessions.
     * Called on application startup.
     */
    fun purgePendingDeletedServices(context: Context) {
        val pendingIds = getPendingDeletedServiceIds()
        if (pendingIds.isNotEmpty()) {
            Log.i(TAG, "Purging ${pendingIds.size} pending deleted cloud service(s) on startup: $pendingIds")
            for (id in pendingIds) {
                purgeServicePermanently(id, context)
            }
        }
    }

    @Synchronized
    fun getActiveServiceId(): String? {
        if (activeServiceIdLoaded) return cachedActiveServiceId
        val id = securePrefs.getString(KEY_ACTIVE_SERVICE_ID, null)
        cachedActiveServiceId = id
        activeServiceIdLoaded = true
        return id
    }

    @Synchronized
    fun setActiveServiceId(serviceId: String?) {
        cachedActiveServiceId = serviceId
        activeServiceIdLoaded = true
        if (serviceId == null) {
            securePrefs.edit().remove(KEY_ACTIVE_SERVICE_ID).apply()
        } else {
            securePrefs.edit().putString(KEY_ACTIVE_SERVICE_ID, serviceId).apply()
        }
    }

    @Synchronized
    fun getExclusionFilter(): ExclusionFilterConfig {
        cachedExclusionFilter?.let { return it }
        val jsonStr = securePrefs.getString(KEY_EXCLUSION_FILTER, null)
        if (jsonStr == null) {
            val defaultFilter = ExclusionFilterConfig.DEFAULT
            cachedExclusionFilter = defaultFilter
            return defaultFilter
        }
        return try {
            val obj = JSONObject(jsonStr)
            val regexArray = obj.optJSONArray("regexPatterns") ?: JSONArray()
            val regexList = (0 until regexArray.length()).map { regexArray.getString(it) }

            val extArray = obj.optJSONArray("excludedExtensions") ?: JSONArray()
            val extSet = (0 until extArray.length()).map { extArray.getString(it) }.toSet()

            val incArray = obj.optJSONArray("includedExtensions")
            val incSet = incArray?.let { arr -> (0 until arr.length()).map { arr.getString(it) }.toSet() }

            val folderArray = obj.optJSONArray("excludedFolderPaths") ?: JSONArray()
            val folderSet = (0 until folderArray.length()).map { folderArray.getString(it) }.toSet()

            val skipDefault = obj.optBoolean("skipDefaultTransient", true)
            val isWhitelist = obj.optBoolean("isWhitelistMode", false)
            val syncTrash = obj.optBoolean("syncTrash", false)

            val filter = ExclusionFilterConfig(
                isWhitelistMode = isWhitelist,
                syncTrash = syncTrash,
                regexPatterns = regexList,
                excludedExtensions = extSet,
                includedExtensions = incSet,
                excludedFolderPaths = folderSet,
                skipDefaultTransient = skipDefault
            )
            cachedExclusionFilter = filter
            filter
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing exclusion filter", e)
            val defaultFilter = ExclusionFilterConfig.DEFAULT
            cachedExclusionFilter = defaultFilter
            defaultFilter
        }
    }

    @Synchronized
    fun saveExclusionFilter(config: ExclusionFilterConfig) {
        cachedExclusionFilter = config
        try {
            val obj = JSONObject()
            obj.put("isWhitelistMode", config.isWhitelistMode)
            obj.put("syncTrash", config.syncTrash)
            obj.put("regexPatterns", JSONArray(config.regexPatterns))
            obj.put("excludedExtensions", JSONArray(config.excludedExtensions))
            config.includedExtensions?.let { obj.put("includedExtensions", JSONArray(it)) }
            obj.put("excludedFolderPaths", JSONArray(config.excludedFolderPaths))
            obj.put("skipDefaultTransient", config.skipDefaultTransient)

            securePrefs.edit().putString(KEY_EXCLUSION_FILTER, obj.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error serializing exclusion filter", e)
        }
    }

    private fun persistServices(services: List<ServiceConfig>) {
        cachedServices = services
        try {
            val array = JSONArray()
            for (s in services) {
                array.put(serializeService(s))
            }
            securePrefs.edit().putString(KEY_SERVICES, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error serializing services", e)
        }
    }

    private fun serializeService(s: ServiceConfig): JSONObject {
        val obj = JSONObject()
        obj.put("id", s.id)
        obj.put("name", s.name)
        obj.put("providerType", s.providerType.id)
        obj.put("serverUrl", s.serverUrl)
        obj.put("host", s.host)
        obj.put("port", s.port)
        obj.put("username", s.username)
        obj.put("passwordOrSecret", s.passwordOrSecret)
        obj.put("privateKey", s.privateKey)
        obj.put("privateKeyPassphrase", s.privateKeyPassphrase)
        obj.put("authToken", s.authToken)
        obj.put("refreshToken", s.refreshToken)
        obj.put("tokenExpiryEpochMs", s.tokenExpiryEpochMs)
        obj.put("accountIdentifier", s.accountIdentifier)
        obj.put("shareName", s.shareName)
        obj.put("domain", s.domain)
        obj.put("remoteBasePath", s.remoteBasePath)
        obj.put("isFtpsImplicit", s.isFtpsImplicit)
        obj.put("isFtpsExplicit", s.isFtpsExplicit)
        obj.put("isCompleteBackupEnabled", s.isCompleteBackupEnabled)
        obj.put("isEnabled", s.isEnabled)
        obj.put("lastSyncedAtEpochMs", s.lastSyncedAtEpochMs)
        obj.put("lastSyncStatus", s.lastSyncStatus ?: "")

        val mappingsArray = JSONArray()
        for (m in s.customMappings) {
            val mObj = JSONObject()
            mObj.put("id", m.id)
            mObj.put("serviceId", m.serviceId)
            mObj.put("name", m.name)
            mObj.put("localFolderPath", m.localFolderPath)
            mObj.put("remoteFolderPath", m.remoteFolderPath)
            mObj.put("isEnabled", m.isEnabled)
            mappingsArray.put(mObj)
        }
        obj.put("customMappings", mappingsArray)

        return obj
    }

    private fun deserializeService(obj: JSONObject): ServiceConfig {
        val mappingsArray = obj.optJSONArray("customMappings") ?: JSONArray()
        val mappings = mutableListOf<CustomFolderMapping>()
        for (i in 0 until mappingsArray.length()) {
            val mObj = mappingsArray.getJSONObject(i)
            mappings.add(
                CustomFolderMapping(
                    id = mObj.getString("id"),
                    serviceId = mObj.getString("serviceId"),
                    name = mObj.optString("name", ""),
                    localFolderPath = mObj.getString("localFolderPath"),
                    remoteFolderPath = mObj.getString("remoteFolderPath"),
                    isEnabled = mObj.optBoolean("isEnabled", true)
                )
            )
        }

        return ServiceConfig(
            id = obj.getString("id"),
            name = obj.getString("name"),
            providerType = StorageProviderType.fromId(obj.getString("providerType")),
            serverUrl = obj.optString("serverUrl", ""),
            host = obj.optString("host", ""),
            port = obj.optInt("port", 443),
            username = obj.optString("username", ""),
            passwordOrSecret = obj.optString("passwordOrSecret", ""),
            privateKey = obj.optString("privateKey", ""),
            privateKeyPassphrase = obj.optString("privateKeyPassphrase", ""),
            authToken = obj.optString("authToken", ""),
            refreshToken = obj.optString("refreshToken", ""),
            tokenExpiryEpochMs = obj.optLong("tokenExpiryEpochMs", 0L),
            accountIdentifier = obj.optString("accountIdentifier", ""),
            shareName = obj.optString("shareName", ""),
            domain = obj.optString("domain", ""),
            remoteBasePath = obj.optString("remoteBasePath", ""),
            isFtpsImplicit = obj.optBoolean("isFtpsImplicit", false),
            isFtpsExplicit = obj.optBoolean("isFtpsExplicit", true),
            isCompleteBackupEnabled = obj.optBoolean("isCompleteBackupEnabled", true),
            isEnabled = obj.optBoolean("isEnabled", true),
            lastSyncedAtEpochMs = obj.optLong("lastSyncedAtEpochMs", 0L),
            lastSyncStatus = obj.optString("lastSyncStatus", "").ifEmpty { null },
            customMappings = mappings
        )
    }
}
