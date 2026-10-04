package dev.ilamparithi.aournalpp.logging

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Indicates the source/domain of a crash.
 */
enum class CrashType {
    JVM_EXCEPTION,
    NATIVE_PROCESS
}

/**
 * Encapsulates immutable device, operating system, and hardware specs
 * captured when an unhandled crash or process fault occurs.
 */
data class DeviceMetadata(
    val manufacturer: String,
    val model: String,
    val device: String,
    val androidVersion: String,
    val sdkInt: Int,
    val supportedAbis: List<String>,
    val appVersionName: String,
    val appVersionCode: Long,
    val availableRamMb: Long = -1L,
    val totalRamMb: Long = -1L
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("manufacturer", manufacturer)
        put("model", model)
        put("device", device)
        put("androidVersion", androidVersion)
        put("sdkInt", sdkInt)
        put("supportedAbis", JSONArray(supportedAbis))
        put("appVersionName", appVersionName)
        put("appVersionCode", appVersionCode)
        put("availableRamMb", availableRamMb)
        put("totalRamMb", totalRamMb)
    }

    companion object {
        fun fromJson(json: JSONObject): DeviceMetadata {
            val abis = mutableListOf<String>()
            val abisArray = json.optJSONArray("supportedAbis")
            if (abisArray != null) {
                for (i in 0 until abisArray.length()) {
                    abis.add(abisArray.optString(i))
                }
            }
            return DeviceMetadata(
                manufacturer = json.optString("manufacturer", "Unknown"),
                model = json.optString("model", "Unknown"),
                device = json.optString("device", "Unknown"),
                androidVersion = json.optString("androidVersion", "Unknown"),
                sdkInt = json.optInt("sdkInt", 0),
                supportedAbis = abis,
                appVersionName = json.optString("appVersionName", "Unknown"),
                appVersionCode = json.optLong("appVersionCode", 0L),
                availableRamMb = json.optLong("availableRamMb", -1L),
                totalRamMb = json.optLong("totalRamMb", -1L)
            )
        }
    }
}

/**
 * Standard structured crash report model for Aournal++.
 */
data class CrashReport(
    val id: String,
    val timestamp: Long,
    val type: CrashType,
    val processName: String,
    val threadName: String? = null,
    val errorClass: String,
    val errorMessage: String,
    val stackTrace: String,
    val deviceMetadata: DeviceMetadata,
    val recentLogs: List<String> = emptyList(),
    val exitCode: Int? = null
) {
    val formattedTimestamp: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("timestamp", timestamp)
        put("type", type.name)
        put("processName", processName)
        put("threadName", threadName ?: "")
        put("errorClass", errorClass)
        put("errorMessage", errorMessage)
        put("stackTrace", stackTrace)
        put("deviceMetadata", deviceMetadata.toJson())
        put("recentLogs", JSONArray(recentLogs))
        if (exitCode != null) {
            put("exitCode", exitCode)
        }
    }

    fun toMarkdown(): String {
        val sb = StringBuilder()
        sb.appendLine("# Aournal++ Crash Report")
        sb.appendLine()
        sb.appendLine("- **Report ID:** `$id`")
        sb.appendLine("- **Timestamp:** $formattedTimestamp (`$timestamp`)")
        sb.appendLine("- **Type:** `${type.name}`")
        sb.appendLine("- **Process:** `$processName`")
        if (!threadName.isNullOrBlank()) {
            sb.appendLine("- **Thread:** `$threadName`")
        }
        if (exitCode != null) {
            sb.appendLine("- **Exit Code:** `$exitCode`")
        }
        sb.appendLine()
        sb.appendLine("## Device & App Environment")
        sb.appendLine("- **App Version:** ${deviceMetadata.appVersionName} (${deviceMetadata.appVersionCode})")
        sb.appendLine("- **Device:** ${deviceMetadata.manufacturer} ${deviceMetadata.model} (`${deviceMetadata.device}`)")
        sb.appendLine("- **Android OS:** ${deviceMetadata.androidVersion} (API ${deviceMetadata.sdkInt})")
        sb.appendLine("- **ABIs:** ${deviceMetadata.supportedAbis.joinToString(", ")}")
        if (deviceMetadata.totalRamMb > 0) {
            sb.appendLine("- **Memory:** Available ${deviceMetadata.availableRamMb} MB / Total ${deviceMetadata.totalRamMb} MB")
        }
        sb.appendLine()
        sb.appendLine("## Error Summary")
        sb.appendLine("```")
        sb.appendLine("$errorClass: $errorMessage")
        sb.appendLine("```")
        sb.appendLine()
        sb.appendLine("## Stack Trace / Diagnostic Output")
        sb.appendLine("```")
        sb.appendLine(stackTrace.ifBlank { "(No stack trace available)" })
        sb.appendLine("```")
        if (recentLogs.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("## Contextual Logcat Tail")
            sb.appendLine("```")
            for (line in recentLogs) {
                sb.appendLine(line)
            }
            sb.appendLine("```")
        }
        return sb.toString()
    }

    companion object {
        fun fromJson(json: JSONObject): CrashReport {
            val logs = mutableListOf<String>()
            val logsArray = json.optJSONArray("recentLogs")
            if (logsArray != null) {
                for (i in 0 until logsArray.length()) {
                    logs.add(logsArray.optString(i))
                }
            }
            val metaObj = json.optJSONObject("deviceMetadata") ?: JSONObject()
            val typeStr = json.optString("type", CrashType.JVM_EXCEPTION.name)
            val crashType = try {
                CrashType.valueOf(typeStr)
            } catch (_: Exception) {
                CrashType.JVM_EXCEPTION
            }

            return CrashReport(
                id = json.optString("id"),
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                type = crashType,
                processName = json.optString("processName", "unknown"),
                threadName = json.optString("threadName").takeIf { it.isNotBlank() },
                errorClass = json.optString("errorClass", "UnknownError"),
                errorMessage = json.optString("errorMessage", "No message"),
                stackTrace = json.optString("stackTrace", ""),
                deviceMetadata = DeviceMetadata.fromJson(metaObj),
                recentLogs = logs,
                exitCode = if (json.has("exitCode")) json.optInt("exitCode") else null
            )
        }
    }
}
