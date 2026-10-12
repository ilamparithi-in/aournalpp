package dev.ilamparithi.aournalpp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.ilamparithi.aournalpp.model.NoteFileType

/**
 * Centralized preferences accessor providing standardized SharedPreferences instances
 * across the application.
 */
object AppPreferences {
    const val PREFS_GENERAL = "aournal_prefs"
    const val PREFS_DOCUMENT_HUB = "aournalpp_document_hub"
    const val PREFS_X11 = "aournalpp_x11"
    const val PREFS_BACKUP = "aournalpp_backup"
    const val PREF_VISIBLE_FILE_TYPES = "pref_visible_file_types"
    val DEFAULT_FILE_TYPES = setOf("xopp", "pdf", "xoj")
    val ALL_NOTE_FILE_TYPES = setOf(NoteFileType.XOPP, NoteFileType.XOJ, NoteFileType.PDF)

    fun loadEnabledFileTypes(prefs: SharedPreferences): Set<NoteFileType> {
        val raw = prefs.getStringSet(PREF_VISIBLE_FILE_TYPES, DEFAULT_FILE_TYPES)
            ?: DEFAULT_FILE_TYPES
        val parsed = raw.mapNotNull { ext ->
            when (ext.lowercase()) {
                "xopp" -> NoteFileType.XOPP
                "pdf" -> NoteFileType.PDF
                "xoj" -> NoteFileType.XOJ
                else -> null
            }
        }.toSet()
        return parsed.ifEmpty { ALL_NOTE_FILE_TYPES }
    }

    fun saveEnabledFileTypes(prefs: SharedPreferences, types: Set<NoteFileType>) {
        prefs.edit().putStringSet(PREF_VISIBLE_FILE_TYPES, types.map { it.extension }.toSet()).apply()
    }

    const val ACTION_AOURNAL_PREFS_CHANGED = "dev.ilamparithi.aournalpp.ACTION_AOURNAL_PREFS_CHANGED"

    @Volatile
    private var isMigrated = false

    fun migrateIfNeeded(context: Context) {
        if (isMigrated) return
        synchronized(this) {
            if (isMigrated) return
            try {
                val legacy = context.getSharedPreferences("aournalpp_general", Context.MODE_PRIVATE)
                val allLegacy = legacy.all
                if (allLegacy.isNotEmpty()) {
                    val target = context.getSharedPreferences(PREFS_GENERAL, Context.MODE_PRIVATE)
                    val editor = target.edit()
                    for ((key, value) in allLegacy) {
                        when (value) {
                            is Boolean -> editor.putBoolean(key, value)
                            is Int -> editor.putInt(key, value)
                            is Long -> editor.putLong(key, value)
                            is Float -> editor.putFloat(key, value)
                            is String -> editor.putString(key, value)
                            is Set<*> -> @Suppress("UNCHECKED_CAST") editor.putStringSet(key, value as Set<String>)
                        }
                    }
                    editor.apply()
                    legacy.edit().clear().apply()
                }
            } catch (_: Exception) {}
            isMigrated = true
        }
    }

    fun notifyChanged(context: Context, key: String) {
        CanvasRestartNoticeManager.onPreferenceChanged(context, key)
        try {
            val prefs = getGeneral(context)
            val value = prefs.all[key]
            val intent = android.content.Intent(ACTION_AOURNAL_PREFS_CHANGED).apply {
                putExtra("key", key)
                when (value) {
                    is Boolean -> {
                        putExtra("value_type", "boolean")
                        putExtra("value_boolean", value)
                    }
                    is Int -> {
                        putExtra("value_type", "int")
                        putExtra("value_int", value)
                    }
                    is Float -> {
                        putExtra("value_type", "float")
                        putExtra("value_float", value)
                    }
                    is Long -> {
                        putExtra("value_type", "long")
                        putExtra("value_long", value)
                    }
                    is String -> {
                        putExtra("value_type", "string")
                        putExtra("value_string", value)
                    }
                }
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {}
    }

    fun getGeneral(context: Context): SharedPreferences {
        migrateIfNeeded(context)
        return context.getSharedPreferences(PREFS_GENERAL, Context.MODE_PRIVATE)
    }

    fun getGeneralPrefs(context: Context): SharedPreferences = getGeneral(context)

    fun getDocumentHub(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_DOCUMENT_HUB, Context.MODE_PRIVATE)

    fun getDocumentHubPrefs(context: Context): SharedPreferences = getDocumentHub(context)

    fun getX11(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_X11, Context.MODE_PRIVATE)

    fun getBackup(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_BACKUP, Context.MODE_PRIVATE)

    /**
     * Composable helper returning the general application preferences.
     */
    @Composable
    fun rememberPrefs(): SharedPreferences {
        val context = LocalContext.current
        return remember(context) { getGeneral(context) }
    }

    /**
     * Composable helper returning the Document Hub preferences.
     */
    @Composable
    fun rememberDocumentHubPrefs(): SharedPreferences {
        val context = LocalContext.current
        return remember(context) { getDocumentHub(context) }
    }

    /**
     * Composable helper returning the X11 preferences.
     */
    @Composable
    fun rememberX11Prefs(): SharedPreferences {
        val context = LocalContext.current
        return remember(context) { getX11(context) }
    }
}
