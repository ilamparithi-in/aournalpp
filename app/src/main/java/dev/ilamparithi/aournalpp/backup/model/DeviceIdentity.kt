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

    /**
     * Gets a friendly human-readable name for this device (e.g. "Google Pixel 9 Pro").
     */
    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
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
            deviceName = getDeviceName(),
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
