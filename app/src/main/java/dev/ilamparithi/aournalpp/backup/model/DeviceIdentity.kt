package dev.ilamparithi.aournalpp.backup.model

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import org.json.JSONObject
import java.util.UUID

/**
 * Stable device identification and hardware metadata for per-device cloud synchronization.
 */
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val manufacturer: String,
    val model: String,
    val osVersion: Int,
    val appVersion: String,
    val lastSyncedAtEpochMs: Long = 0L
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("deviceId", deviceId)
        put("deviceName", deviceName)
        put("manufacturer", manufacturer)
        put("model", model)
        put("osVersion", osVersion)
        put("appVersion", appVersion)
        put("lastSyncedAtEpochMs", lastSyncedAtEpochMs)
    }

    fun toJsonString(): String = toJson().toString(2)

    companion object {
        fun fromJson(json: JSONObject): DeviceInfo = DeviceInfo(
            deviceId = json.optString("deviceId", "unknown"),
            deviceName = json.optString("deviceName", "Unknown Device"),
            manufacturer = json.optString("manufacturer", ""),
            model = json.optString("model", ""),
            osVersion = json.optInt("osVersion", 0),
            appVersion = json.optString("appVersion", "1.0"),
            lastSyncedAtEpochMs = json.optLong("lastSyncedAtEpochMs", 0L)
        )
    }
}

object DeviceIdentity {
    private const val PREFS_NAME = "aournal_device_identity"
    private const val KEY_DEVICE_ID = "stable_device_id"

    @Volatile
    private var cachedDeviceId: String? = null

    /**
     * Returns a stable, persistent UUID for this installation.
     * Stored in SharedPreferences to remain constant across app restarts.
     */
    fun getDeviceId(context: Context): String {
        cachedDeviceId?.let { return it }
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        cachedDeviceId = id
        return id
    }

    private const val KEY_CUSTOM_DEVICE_NAME = "custom_device_name"

    fun getCustomDeviceName(context: Context): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_DEVICE_NAME, null)?.takeIf { it.isNotBlank() }
    }

    fun setCustomDeviceName(context: Context, name: String?) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val trimmed = name?.trim()
        if (trimmed.isNullOrBlank()) {
            prefs.edit().remove(KEY_CUSTOM_DEVICE_NAME).apply()
        } else {
            prefs.edit().putString(KEY_CUSTOM_DEVICE_NAME, trimmed).apply()
        }
    }

    /**
     * Gets a friendly human-readable name for this device (e.g. "Google Pixel 9 Pro").
     */
    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER?.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() } ?: "Android"
        val model = Build.MODEL?.takeIf { it.isNotBlank() } ?: "Device"
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    /**
     * Returns the effective device name. Prefers user-configured custom device name if present,
     * otherwise falls back to hardware-detected device name.
     * If [includeShortUid] is true, appends the first 4 characters of the unique device ID
     * (e.g. "Lenovo Tab P12 [a629]") to disambiguate identical devices.
     */
    fun getEffectiveDeviceName(context: Context, includeShortUid: Boolean = false): String {
        val baseName = getCustomDeviceName(context) ?: getDeviceName()
        return if (includeShortUid) {
            val shortUid = getDeviceId(context).take(4)
            "$baseName [$shortUid]"
        } else {
            baseName
        }
    }

    /**
     * Constructs full [DeviceInfo] metadata for the current running device.
     */
    fun getDeviceInfo(context: Context, lastSyncedAt: Long = System.currentTimeMillis()): DeviceInfo {
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
            null
        }
        val appVer = packageInfo?.versionName ?: "1.0"
        return DeviceInfo(
            deviceId = getDeviceId(context),
            deviceName = getEffectiveDeviceName(context),
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            osVersion = Build.VERSION.SDK_INT,
            appVersion = appVer,
            lastSyncedAtEpochMs = lastSyncedAt
        )
    }

    fun resetForTesting() {
        cachedDeviceId = null
    }
}
