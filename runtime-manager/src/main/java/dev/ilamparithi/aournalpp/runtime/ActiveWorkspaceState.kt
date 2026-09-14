package dev.ilamparithi.aournalpp.runtime

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable

/**
 * Detailed representation of an open X11 document window in the active session.
 */
data class ActiveWindowEntry(
    val id: String,
    val title: String,
    val cleanTitle: String,
    val filePath: String? = null,
    val isActive: Boolean = false,
    val isDirty: Boolean = false,
    val hasConflict: Boolean = false,
    val isAudioRecording: Boolean = false
) : Serializable {

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("cleanTitle", cleanTitle)
        if (filePath != null) put("filePath", filePath)
        put("isActive", isActive)
        put("isDirty", isDirty)
        put("hasConflict", hasConflict)
        put("isAudioRecording", isAudioRecording)
    }

    companion object {
        fun fromJson(json: JSONObject): ActiveWindowEntry {
            return ActiveWindowEntry(
                id = json.getString("id"),
                title = json.getString("title"),
                cleanTitle = json.optString("cleanTitle", json.getString("title")),
                filePath = if (json.has("filePath") && !json.isNull("filePath")) json.getString("filePath") else null,
                isActive = json.optBoolean("isActive", false),
                isDirty = json.optBoolean("isDirty", false),
                hasConflict = json.optBoolean("hasConflict", false),
                isAudioRecording = json.optBoolean("isAudioRecording", false)
            )
        }
    }
}

/**
 * Multi-window workspace state across the active canvas session.
 * Synchronized across `:canvas` and the main process to power the
 * Return Portal, stage compound tiles, and window switching.
 */
data class ActiveWorkspaceState(
    val sessionInfo: ActiveSessionInfo,
    val windows: List<ActiveWindowEntry> = emptyList(),
    val snapMode: String = "single",
    val slotAssignments: Map<Int, String> = emptyMap(),
    val dividerRatios: Map<String, Float> = emptyMap(),
    val previewTimestamp: Long = System.currentTimeMillis()
) : Serializable {

    val hasAnyConflict: Boolean
        get() = windows.any { it.hasConflict }

    val activeWindow: ActiveWindowEntry?
        get() = windows.find { it.isActive } ?: windows.firstOrNull()

    fun toJson(): JSONObject = JSONObject().apply {
        val sessionObj = JSONObject().apply {
            put("isRunning", sessionInfo.isRunning)
            put("pid", sessionInfo.pid)
            if (sessionInfo.activeNotePath != null) put("activeNotePath", sessionInfo.activeNotePath)
            if (sessionInfo.documentTitle != null) put("documentTitle", sessionInfo.documentTitle)
            put("openWindowCount", sessionInfo.openWindowCount)
            put("lastTimestamp", sessionInfo.lastTimestamp)
        }
        put("session", sessionObj)

        val winArray = JSONArray()
        windows.forEach { win ->
            winArray.put(win.toJson())
        }
        put("windows", winArray)

        put("snapMode", snapMode)

        val slotsObj = JSONObject()
        slotAssignments.forEach { (slotIdx, winId) ->
            slotsObj.put(slotIdx.toString(), winId)
        }
        put("slotAssignments", slotsObj)

        val ratiosObj = JSONObject()
        dividerRatios.forEach { (k, v) ->
            ratiosObj.put(k, v.toDouble())
        }
        put("dividerRatios", ratiosObj)

        put("previewTimestamp", previewTimestamp)
    }

    companion object {
        fun fromJson(json: JSONObject): ActiveWorkspaceState {
            val sessionJson = json.getJSONObject("session")
            val sessionInfo = ActiveSessionInfo(
                isRunning = sessionJson.getBoolean("isRunning"),
                pid = sessionJson.getInt("pid"),
                activeNotePath = if (sessionJson.has("activeNotePath") && !sessionJson.isNull("activeNotePath")) sessionJson.getString("activeNotePath") else null,
                documentTitle = if (sessionJson.has("documentTitle") && !sessionJson.isNull("documentTitle")) sessionJson.getString("documentTitle") else null,
                openWindowCount = sessionJson.optInt("openWindowCount", 1),
                lastTimestamp = sessionJson.optLong("lastTimestamp", System.currentTimeMillis())
            )

            val winList = mutableListOf<ActiveWindowEntry>()
            val winArray = json.optJSONArray("windows")
            if (winArray != null) {
                for (i in 0 until winArray.length()) {
                    val wObj = winArray.getJSONObject(i)
                    winList.add(ActiveWindowEntry.fromJson(wObj))
                }
            }

            val snapMode = json.optString("snapMode", "single")

            val slotsMap = mutableMapOf<Int, String>()
            val slotsObj = json.optJSONObject("slotAssignments")
            if (slotsObj != null) {
                for (key in slotsObj.keys()) {
                    val idx = key.toIntOrNull() ?: continue
                    slotsMap[idx] = slotsObj.getString(key)
                }
            }

            val ratiosMap = mutableMapOf<String, Float>()
            val ratiosObj = json.optJSONObject("dividerRatios")
            if (ratiosObj != null) {
                for (key in ratiosObj.keys()) {
                    ratiosMap[key] = ratiosObj.getDouble(key).toFloat()
                }
            }

            val previewTimestamp = json.optLong("previewTimestamp", System.currentTimeMillis())

            return ActiveWorkspaceState(
                sessionInfo = sessionInfo,
                windows = winList,
                snapMode = snapMode,
                slotAssignments = slotsMap,
                dividerRatios = ratiosMap,
                previewTimestamp = previewTimestamp
            )
        }
    }
}
