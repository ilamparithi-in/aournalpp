package dev.ilamparithi.aournalpp.runtime

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject
import java.io.File

object NotesHomeConfigManager {
    private const val TAG = "NotesHomeConfigManager"
    private const val CONFIG_FOLDER_NAME = ".config"
    private const val APP_SETTINGS_FILE = "app_settings.json"
    private const val X11_PREFS_FILE = "x11_prefs.json"
    private const val XOURNALPP_DIR_NAME = "xournalpp"
    private const val SYNC_METADATA_FILE = "notes_home_sync_meta.json"

    fun getConfigDirectory(notesHomeDir: File): File {
        return File(notesHomeDir, CONFIG_FOLDER_NAME)
    }

    /**
     * Performs a bidirectional synchronization between internal app storage and Notes Home (.config):
     * - If external files were modified more recently than the last sync, imports them.
     * - Otherwise, exports the latest internal settings to Notes Home.
     */
    @Synchronized
    fun sync(context: Context, env: LinuxEnvironment) {
        try {
            val notesHome = env.getNotesDirectory()
            if (!notesHome.exists()) {
                notesHome.mkdirs()
            }

            val configDir = getConfigDirectory(notesHome)
            if (!configDir.exists()) {
                configDir.mkdirs()
            }

            val metaFile = File(context.filesDir, SYNC_METADATA_FILE)
            val meta = loadSyncMetadata(metaFile)

            // 1. App UI Settings Sync (aournal_prefs)
            syncAppSettings(context, configDir, meta)

            // 2. Termux-X11 Preferences Sync
            syncX11Prefs(context, configDir, meta)

            // 3. Xournal++ & GTK Settings Sync
            syncXournalppConfigs(env, configDir, meta)

            // Save updated sync metadata
            saveSyncMetadata(metaFile, meta)
        } catch (e: Exception) {
            Log.w(TAG, "Error during Notes Home config synchronization", e)
        }
    }

    /**
     * Copies the .config directory from the previous Notes Home to the new Notes Home without deleting the original.
     */
    fun copyConfigToNewNotesHome(oldNotesDir: File, newNotesDir: File, context: Context, env: LinuxEnvironment) {
        try {
            val oldConfigDir = getConfigDirectory(oldNotesDir)
            val newConfigDir = getConfigDirectory(newNotesDir)

            if (!newNotesDir.exists()) {
                newNotesDir.mkdirs()
            }

            if (oldConfigDir.exists() && oldConfigDir.isDirectory) {
                Log.i(TAG, "Copying .config from ${oldConfigDir.absolutePath} to ${newConfigDir.absolutePath}")
                oldConfigDir.copyRecursively(newConfigDir, overwrite = true)
            } else {
                Log.i(TAG, "Old .config directory did not exist; creating fresh .config at ${newConfigDir.absolutePath}")
                newConfigDir.mkdirs()
            }

            // Export active internal settings to ensure the new folder is fully populated
            sync(context, env)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy .config to new Notes Home", e)
        }
    }

    private fun syncAppSettings(context: Context, configDir: File, meta: MutableMap<String, Long>) {
        val externalFile = File(configDir, APP_SETTINGS_FILE)
        val appPrefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        val lastSyncTime = meta[APP_SETTINGS_FILE] ?: 0L

        if (externalFile.exists() && externalFile.lastModified() > lastSyncTime && lastSyncTime > 0L) {
            // External file was edited since last sync -> Import into app
            Log.i(TAG, "External $APP_SETTINGS_FILE was modified. Importing to SharedPreferences...")
            importJsonToSharedPreferences(externalFile, appPrefs)
            meta[APP_SETTINGS_FILE] = externalFile.lastModified()
        } else {
            // Internal settings are newer or external doesn't exist -> Export
            exportSharedPreferencesToJson(appPrefs, externalFile)
            meta[APP_SETTINGS_FILE] = externalFile.lastModified()
        }
    }

    private fun syncX11Prefs(context: Context, configDir: File, meta: MutableMap<String, Long>) {
        val externalFile = File(configDir, X11_PREFS_FILE)
        val x11Prefs = context.getSharedPreferences("com.termux.x11_preferences", Context.MODE_PRIVATE)
        val lastSyncTime = meta[X11_PREFS_FILE] ?: 0L

        if (externalFile.exists() && externalFile.lastModified() > lastSyncTime && lastSyncTime > 0L) {
            Log.i(TAG, "External $X11_PREFS_FILE was modified. Importing to X11 Preferences...")
            importJsonToSharedPreferences(externalFile, x11Prefs)
            meta[X11_PREFS_FILE] = externalFile.lastModified()
        } else {
            exportSharedPreferencesToJson(x11Prefs, externalFile)
            meta[X11_PREFS_FILE] = externalFile.lastModified()
        }
    }

    private fun syncXournalppConfigs(env: LinuxEnvironment, configDir: File, meta: MutableMap<String, Long>) {
        val extXoppDir = File(configDir, XOURNALPP_DIR_NAME)
        if (!extXoppDir.exists()) {
            extXoppDir.mkdirs()
        }

        val internalXoppDir = env.xournalConfigDir
        if (!internalXoppDir.exists()) {
            internalXoppDir.mkdirs()
        }

        val configFiles = listOf("settings.xml", "toolbar.ini", "palette.gpl", "colornames.ini", "settings.ini")

        for (fileName in configFiles) {
            val internalFile = if (fileName == "settings.ini") {
                File(File(env.configDir, "gtk-3.0"), "settings.ini")
            } else {
                File(internalXoppDir, fileName)
            }

            val externalFile = File(extXoppDir, fileName)
            val metaKey = "xopp_$fileName"
            val lastSyncTime = meta[metaKey] ?: 0L

            if (externalFile.exists() && externalFile.lastModified() > lastSyncTime && lastSyncTime > 0L) {
                // External file was edited -> Import to internal
                Log.i(TAG, "External Xournal++ config $fileName was modified. Importing...")
                internalFile.parentFile?.mkdirs()
                externalFile.copyTo(internalFile, overwrite = true)
                meta[metaKey] = externalFile.lastModified()
            } else if (internalFile.exists()) {
                // Internal file exists -> Export copy to external
                internalFile.copyTo(externalFile, overwrite = true)
                meta[metaKey] = externalFile.lastModified()
            }
        }
    }

    private fun exportSharedPreferencesToJson(prefs: SharedPreferences, destFile: File) {
        try {
            val allEntries = prefs.all
            val json = JSONObject()
            for ((key, value) in allEntries) {
                json.put(key, value)
            }
            destFile.writeText(json.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to export SharedPreferences to ${destFile.name}", e)
        }
    }

    private fun importJsonToSharedPreferences(sourceFile: File, prefs: SharedPreferences) {
        try {
            val text = sourceFile.readText(Charsets.UTF_8)
            val json = JSONObject(text)
            val editor = prefs.edit()

            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                when (val value = json.get(key)) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is String -> editor.putString(key, value)
                }
            }
            editor.apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to import JSON from ${sourceFile.name} to SharedPreferences", e)
        }
    }

    private fun loadSyncMetadata(file: File): MutableMap<String, Long> {
        val map = mutableMapOf<String, Long>()
        if (file.exists()) {
            try {
                val json = JSONObject(file.readText(Charsets.UTF_8))
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = json.optLong(k, 0L)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read sync metadata", e)
            }
        }
        return map
    }

    private fun saveSyncMetadata(file: File, meta: Map<String, Long>) {
        try {
            val json = JSONObject()
            for ((k, v) in meta) {
                json.put(k, v)
            }
            file.writeText(json.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save sync metadata", e)
        }
    }
}
