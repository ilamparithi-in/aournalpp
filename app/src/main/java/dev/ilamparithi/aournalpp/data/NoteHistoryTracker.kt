package dev.ilamparithi.aournalpp.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Manages tracking, persistence, and querying of recently opened notes.
 */
class NoteHistoryTracker(
    private val context: Context,
    private val prefs: SharedPreferences,
    private val cache: DocumentCache,
    private val scope: CoroutineScope,
    private val isExcludedFromRecents: (File) -> Boolean,
    private val isWithinRootDirectory: (File) -> Boolean
) {
    companion object {
        const val PREF_OPENED_NOTES_HISTORY_JSON = "pref_opened_notes_history_json"
        const val PREF_OPENED_NOTES_TIMESTAMPS_JSON = "pref_opened_notes_timestamps_json"
        const val PREF_LAST_OPENED_NOTE_PATH = "pref_last_opened_note_path"
    }

    private var cachedOpenedNotesHistory: List<String>?
        get() = cache.cachedOpenedNotesHistory
        set(value) { cache.cachedOpenedNotesHistory = value }

    private var cachedOpenedNotesTimestamps: Map<String, Long>?
        get() = cache.cachedOpenedNotesTimestamps
        set(value) { cache.cachedOpenedNotesTimestamps = value }

    fun recordNoteOpened(path: String) {
        if (path.isBlank() || path.contains("staged_imports") || path.contains("/cache/") || path.contains("/.Trash/")) return
        val file = File(path)
        if (isExcludedFromRecents(file)) return

        val currentList = (cachedOpenedNotesHistory ?: getRecentlyOpenedHistoryFromPrefs()).toMutableList()
        currentList.remove(path)
        currentList.add(0, path)
        val trimmed = currentList.take(50)
        cachedOpenedNotesHistory = trimmed

        val timestampsMap = (cachedOpenedNotesTimestamps ?: getOpenedNotesTimestamps()).toMutableMap()
        val now = System.currentTimeMillis()
        timestampsMap[path] = now
        cachedOpenedNotesTimestamps = timestampsMap

        scope.launch {
            try {
                val jsonArray = JSONArray()
                trimmed.forEach { jsonArray.put(it) }

                val timestampsObj = JSONObject()
                timestampsMap.forEach { (k, v) -> timestampsObj.put(k, v) }

                prefs.edit()
                    .putString(PREF_OPENED_NOTES_HISTORY_JSON, jsonArray.toString())
                    .putString(PREF_OPENED_NOTES_TIMESTAMPS_JSON, timestampsObj.toString())
                    .putString(PREF_LAST_OPENED_NOTE_PATH, path)
                    .apply()

                try {
                    AppPreferences.getGeneral(context)
                        .edit()
                        .putString(PREF_LAST_OPENED_NOTE_PATH, path)
                        .apply()
                } catch (e: Exception) {
                    // ignore
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun getRecentlyOpenedHistoryFromPrefs(): List<String> {
        val raw = prefs.getString(PREF_OPENED_NOTES_HISTORY_JSON, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val l = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val p = array.optString(i)
                if (p.isNotBlank() && !p.contains("staged_imports") && !p.contains("/cache/")) {
                    l.add(p)
                }
            }
            l
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun removeOpenedNoteHistory(path: String) {
        if (path.isBlank()) return
        val currentList = (cachedOpenedNotesHistory ?: getRecentlyOpenedHistoryFromPrefs()).toMutableList()
        if (currentList.remove(path)) {
            cachedOpenedNotesHistory = currentList
            val jsonArray = JSONArray()
            currentList.forEach { jsonArray.put(it) }
            prefs.edit().putString(PREF_OPENED_NOTES_HISTORY_JSON, jsonArray.toString()).apply()
        }
    }

    fun updateOpenedNotePath(oldPath: String, newPath: String) {
        if (oldPath.isBlank() || newPath.isBlank()) return
        val currentList = (cachedOpenedNotesHistory ?: getRecentlyOpenedHistoryFromPrefs()).toMutableList()
        val index = currentList.indexOf(oldPath)
        if (index != -1) {
            currentList[index] = newPath
            cachedOpenedNotesHistory = currentList
            val jsonArray = JSONArray()
            currentList.forEach { jsonArray.put(it) }
            prefs.edit().putString(PREF_OPENED_NOTES_HISTORY_JSON, jsonArray.toString()).apply()
        }
        if (prefs.getString(PREF_LAST_OPENED_NOTE_PATH, null) == oldPath) {
            prefs.edit().putString(PREF_LAST_OPENED_NOTE_PATH, newPath).apply()
        }
    }

    fun getRecentlyOpenedPaths(strictlyWithinRoot: Boolean = true): List<String> {
        var baseList = cachedOpenedNotesHistory
        if (baseList == null) {
            baseList = getRecentlyOpenedHistoryFromPrefs()
            cachedOpenedNotesHistory = baseList
        }

        val list = mutableListOf<String>()
        for (p in baseList) {
            if (!list.contains(p) && !p.contains("staged_imports") && !p.contains("/cache/")) {
                if (!strictlyWithinRoot || isWithinRootDirectory(File(p))) {
                    list.add(p)
                }
            }
        }

        val mainPrefsLastOpened = try {
            AppPreferences.getGeneral(context)
                .getString(PREF_LAST_OPENED_NOTE_PATH, null)
        } catch (e: Exception) {
            null
        }
        if (!mainPrefsLastOpened.isNullOrBlank() && !mainPrefsLastOpened.contains("staged_imports") && !mainPrefsLastOpened.contains("/cache/") && !list.contains(mainPrefsLastOpened)) {
            val validPath: String = mainPrefsLastOpened
            if (!strictlyWithinRoot || isWithinRootDirectory(File(validPath))) {
                list.add(0, validPath)
            }
        }
        val docHubLastOpened = prefs.getString(PREF_LAST_OPENED_NOTE_PATH, null)
        if (!docHubLastOpened.isNullOrBlank() && !docHubLastOpened.contains("staged_imports") && !docHubLastOpened.contains("/cache/") && !list.contains(docHubLastOpened)) {
            val validDocHubPath: String = docHubLastOpened
            if (!strictlyWithinRoot || isWithinRootDirectory(File(validDocHubPath))) {
                list.add(0, validDocHubPath)
            }
        }
        return list
    }

    fun getOpenedNotesTimestamps(): Map<String, Long> {
        var map = cachedOpenedNotesTimestamps
        if (map == null) {
            val raw = prefs.getString(PREF_OPENED_NOTES_TIMESTAMPS_JSON, null)
            map = if (raw != null) {
                try {
                    val obj = JSONObject(raw)
                    val m = HashMap<String, Long>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        m[k] = obj.optLong(k)
                    }
                    m
                } catch (e: Exception) {
                    emptyMap()
                }
            } else {
                emptyMap()
            }
            cachedOpenedNotesTimestamps = map
        }
        return map
    }
}
