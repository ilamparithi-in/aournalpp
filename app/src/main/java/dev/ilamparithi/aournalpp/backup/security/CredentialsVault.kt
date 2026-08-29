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
    }

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
    fun getActiveServiceId(): String? {
        return securePrefs.getString(KEY_ACTIVE_SERVICE_ID, null)
    }

    @Synchronized
    fun setActiveServiceId(serviceId: String?) {
        if (serviceId == null) {
            securePrefs.edit().remove(KEY_ACTIVE_SERVICE_ID).apply()
        } else {
            securePrefs.edit().putString(KEY_ACTIVE_SERVICE_ID, serviceId).apply()
        }
    }

    @Synchronized
    fun getExclusionFilter(): ExclusionFilterConfig {
        val jsonStr = securePrefs.getString(KEY_EXCLUSION_FILTER, null) ?: return ExclusionFilterConfig.DEFAULT
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

            ExclusionFilterConfig(
                regexPatterns = regexList,
                excludedExtensions = extSet,
                includedExtensions = incSet,
                excludedFolderPaths = folderSet,
                skipDefaultTransient = skipDefault
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing exclusion filter", e)
            ExclusionFilterConfig.DEFAULT
        }
    }

    @Synchronized
    fun saveExclusionFilter(config: ExclusionFilterConfig) {
        try {
            val obj = JSONObject()
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
