@file:Suppress("DEPRECATION")

package dev.ilamparithi.aournalpp.backup.security

import android.content.Context
import android.content.SharedPreferences
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dev.ilamparithi.aournalpp.backup.worker.BackupScheduler

/**
 * Hardware-backed encrypted credential vault for storing remote cloud credentials,
 * service configurations, custom folder mappings, and exclusion filter rules.
 */
class CredentialsVault private constructor(context: Context) {

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
                val appCtx = context.applicationContext ?: context
                INSTANCE ?: CredentialsVault(appCtx).also { INSTANCE = it }
            }
        }

        @androidx.annotation.VisibleForTesting
        fun createForTesting(context: Context): CredentialsVault {
            return CredentialsVault(context)
        }

        @androidx.annotation.VisibleForTesting
        fun resetInstanceForTesting() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }

    private val _servicesFlow = MutableStateFlow<List<ServiceConfig>>(emptyList())
    val servicesFlow: StateFlow<List<ServiceConfig>> = _servicesFlow.asStateFlow()

    private val _pendingDeletionsFlow = MutableStateFlow<Set<String>>(emptySet())
    val pendingDeletionsFlow: StateFlow<Set<String>> = _pendingDeletionsFlow.asStateFlow()

    @Volatile
    private var cachedExclusionFilter: ExclusionFilterConfig? = null

    @Volatile
    private var cachedActiveServiceId: String? = null
    @Volatile
    private var activeServiceIdLoaded: Boolean = false

    @Volatile
    private var isHardwareBacked: Boolean = true

    fun isHardwareEncrypted(): Boolean = isHardwareBacked

    private val securePrefs: SharedPreferences = initSecurePrefs(context)

    init {
        _servicesFlow.value = loadServicesFromDisk()
        _pendingDeletionsFlow.value = loadPendingDeletionsFromDisk()
    }

    private fun loadServicesFromDisk(): List<ServiceConfig> {
        val jsonStr = securePrefs.getString(KEY_SERVICES, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ServiceConfig>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(deserializeService(obj))
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing services", e)
            emptyList()
        }
    }

    private fun loadPendingDeletionsFromDisk(): Set<String> {
        return securePrefs.getStringSet(KEY_PENDING_DELETIONS, emptySet())?.toSet() ?: emptySet()
    }

    private fun initSecurePrefs(context: Context): SharedPreferences {
        val safeContext = context.applicationContext ?: context
        fun createEncrypted(): SharedPreferences {
            val masterKey = MasterKey.Builder(safeContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                safeContext,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        return try {
            createEncrypted()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize EncryptedSharedPreferences with MasterKey, attempting KeyStore recovery", e)
            try {
                // Delete corrupted preferences file
                safeContext.deleteSharedPreferences(PREFS_FILE)
                // Delete corrupted master key alias from AndroidKeyStore if present
                try {
                    val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                    if (keyStore.containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
                        keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    }
                } catch (ksEx: Exception) {
                    Log.w(TAG, "Failed to purge corrupted master key alias from AndroidKeyStore", ksEx)
                }
                createEncrypted()
            } catch (recoveryEx: Exception) {
                Log.e(TAG, "Fatal: Could not initialize EncryptedSharedPreferences after KeyStore reset. Failing closed to in-memory non-persistent storage.", recoveryEx)
                isHardwareBacked = false
                InMemorySharedPreferences()
            }
        }
    }

    @Synchronized
    fun getAllServices(): List<ServiceConfig> {
        return _servicesFlow.value
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
        return _pendingDeletionsFlow.value
    }

    @Synchronized
    fun markServicePendingDeletion(serviceId: String) {
        val current = _pendingDeletionsFlow.value.toMutableSet()
        current.add(serviceId)
        _pendingDeletionsFlow.value = current
        securePrefs.edit().putStringSet(KEY_PENDING_DELETIONS, current).commit()
    }

    @Synchronized
    fun restorePendingDeletedService(serviceId: String) {
        val current = _pendingDeletionsFlow.value.toMutableSet()
        current.remove(serviceId)
        _pendingDeletionsFlow.value = current
        securePrefs.edit().putStringSet(KEY_PENDING_DELETIONS, current).commit()
    }

    @Synchronized
    fun isServicePendingDeletion(serviceId: String): Boolean {
        return _pendingDeletionsFlow.value.contains(serviceId)
    }

    @Synchronized
    fun getActiveConfiguredServices(): List<ServiceConfig> {
        val pending = _pendingDeletionsFlow.value
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
        val pending = _pendingDeletionsFlow.value.toMutableSet()
        if (pending.remove(serviceId)) {
            _pendingDeletionsFlow.value = pending
            securePrefs.edit().putStringSet(KEY_PENDING_DELETIONS, pending).commit()
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
        _servicesFlow.value = services
        try {
            val array = JSONArray()
            for (s in services) {
                array.put(serializeService(s))
            }
            securePrefs.edit().putString(KEY_SERVICES, array.toString()).commit()
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
        for ((id, serviceId, name, localFolderPath, remoteFolderPath, isEnabled) in s.customMappings) {
            val mObj = JSONObject()
            mObj.put("id", id)
            mObj.put("serviceId", serviceId)
            mObj.put("name", name)
            mObj.put("localFolderPath", localFolderPath)
            mObj.put("remoteFolderPath", remoteFolderPath)
            mObj.put("isEnabled", isEnabled)
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
            lastSyncStatus = obj.optString("lastSyncStatus", "").ifEmpty { null }?.let { status ->
                if (dev.ilamparithi.aournalpp.utils.NetworkUtils.isTransientNetworkErrorMessage(status)) null else status
            },
            customMappings = mappings
        )
    }
}

/**
 * Transient in-memory SharedPreferences fallback used only if device KeyStore / EncryptedSharedPreferences
 * fails catastrophically, guaranteeing that cleartext credentials are NEVER written to disk.
 */
internal class InMemorySharedPreferences : SharedPreferences {
    private val map = java.util.concurrent.ConcurrentHashMap<String, Any>()

    override fun getAll(): Map<String, *> = HashMap(map)
    override fun getString(key: String, defValue: String?): String? = (map[key] as? String) ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = (map[key] as? Set<String>) ?: defValues
    override fun getInt(key: String, defValue: Int): Int = (map[key] as? Int) ?: defValue
    override fun getLong(key: String, defValue: Long): Long = (map[key] as? Long) ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = (map[key] as? Float) ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
    override fun contains(key: String): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = EditorImpl()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private inner class EditorImpl : SharedPreferences.Editor {
        private val modifications = mutableMapOf<String, Any?>()
        private var clear = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { modifications[key] = value }
        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor = apply { modifications[key] = values?.toSet() }
        override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply { modifications[key] = value }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply { modifications[key] = value }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply { modifications[key] = value }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply { modifications[key] = value }
        override fun remove(key: String): SharedPreferences.Editor = apply { modifications[key] = this }
        override fun clear(): SharedPreferences.Editor = apply { clear = true }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            synchronized(this@InMemorySharedPreferences) {
                if (clear) map.clear()
                for ((k, v) in modifications) {
                    if (v === this) {
                        map.remove(k)
                    } else if (v != null) {
                        map[k] = v
                    } else {
                        map.remove(k)
                    }
                }
            }
        }
    }
}
