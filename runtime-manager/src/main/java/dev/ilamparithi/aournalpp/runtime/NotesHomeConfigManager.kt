package dev.ilamparithi.aournalpp.runtime

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

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
     * Performs a bidirectional content-hash synchronization between internal app storage and Notes Home (.config):
     * - Content hashing (SHA-256) guarantees that external changes (pasted, overwritten, or modified files)
     *   are immediately imported without being clobbered by timestamp discrepancies.
     * - Sanitizes Android storage paths on settings.xml import to prevent desktop path corruption.
     * - Supports full recursive syncing of subdirectories (palettes, ui, plugins, etc.).
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

            // 3. Xournal++ & GTK Settings Sync (Recursive)
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

    private fun syncAppSettings(context: Context, configDir: File, meta: MutableMap<String, String>) {
        val externalFile = File(configDir, APP_SETTINGS_FILE)
        val appPrefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        val metaKey = "hash_$APP_SETTINGS_FILE"
        val lastSyncHash = meta[metaKey] ?: ""

        val extHash = getFileHash(externalFile)

        if (externalFile.exists() && extHash.isNotEmpty() && extHash != lastSyncHash) {
            Log.i(TAG, "External $APP_SETTINGS_FILE modified. Importing to SharedPreferences...")
            importJsonToSharedPreferences(externalFile, appPrefs)
            meta[metaKey] = getFileHash(externalFile)
        } else {
            exportSharedPreferencesToJson(appPrefs, externalFile)
            meta[metaKey] = getFileHash(externalFile)
        }
    }

    private fun syncX11Prefs(context: Context, configDir: File, meta: MutableMap<String, String>) {
        val externalFile = File(configDir, X11_PREFS_FILE)
        val x11Prefs = context.getSharedPreferences("com.termux.x11_preferences", Context.MODE_PRIVATE)
        val metaKey = "hash_$X11_PREFS_FILE"
        val lastSyncHash = meta[metaKey] ?: ""

        val extHash = getFileHash(externalFile)

        if (externalFile.exists() && extHash.isNotEmpty() && extHash != lastSyncHash) {
            Log.i(TAG, "External $X11_PREFS_FILE modified. Importing to X11 Preferences...")
            importJsonToSharedPreferences(externalFile, x11Prefs)
            meta[metaKey] = getFileHash(externalFile)
        } else {
            exportSharedPreferencesToJson(x11Prefs, externalFile)
            meta[metaKey] = getFileHash(externalFile)
        }
    }

    private fun syncXournalppConfigs(env: LinuxEnvironment, configDir: File, meta: MutableMap<String, String>) {
        val extXoppDir = File(configDir, XOURNALPP_DIR_NAME)
        if (!extXoppDir.exists()) {
            extXoppDir.mkdirs()
        }

        val internalXoppDir = env.xournalConfigDir
        if (!internalXoppDir.exists()) {
            internalXoppDir.mkdirs()
        }

        // Collect all relative paths from internal and external trees
        val relativePaths = mutableSetOf<String>()

        if (internalXoppDir.exists()) {
            internalXoppDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val rel = file.relativeTo(internalXoppDir).path
                if (!rel.startsWith(".") && !rel.endsWith(".tmp") && rel != "emergencysave.xopp") {
                    relativePaths.add(rel)
                }
            }
        }

        if (extXoppDir.exists()) {
            extXoppDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val rel = file.relativeTo(extXoppDir).path
                if (!rel.startsWith(".") && !rel.endsWith(".tmp") && rel != "emergencysave.xopp") {
                    relativePaths.add(rel)
                }
            }
        }

        // Base configuration names
        relativePaths.addAll(listOf("settings.xml", "toolbar.ini", "palette.gpl", "colornames.ini", "print-settings.ini", "settings.ini"))

        var settingsXmlImported = false

        for (relPath in relativePaths) {
            val internalFile = if (relPath == "settings.ini") {
                File(File(env.configDir, "gtk-3.0"), "settings.ini")
            } else {
                File(internalXoppDir, relPath)
            }

            val externalFile = File(extXoppDir, relPath)
            val metaKey = "hash_xopp_$relPath"

            val imported = syncSingleFile(internalFile, externalFile, metaKey, meta)
            if (imported && (relPath == "settings.xml" || relPath.endsWith("/settings.xml"))) {
                settingsXmlImported = true
            }
        }

        if (settingsXmlImported) {
            Log.i(TAG, "Sanitizing Android storage paths and autoload preference for imported settings.xml...")
            env.ensureXournalppSettings()
            env.checkAndOverrideAutoloadPreference()
            env.ensureMenuBarShortcuts()
            val intSettings = File(internalXoppDir, "settings.xml")
            val extSettings = File(extXoppDir, "settings.xml")
            if (intSettings.exists() && extSettings.exists()) {
                intSettings.copyTo(extSettings, overwrite = true)
                meta["hash_xopp_settings.xml"] = getFileHash(intSettings)
            }
        }
    }

    /**
     * Synchronizes a single file using SHA-256 hash detection.
     * Returns true if external was imported into internal.
     */
    private fun syncSingleFile(
        internalFile: File,
        externalFile: File,
        metaKey: String,
        meta: MutableMap<String, String>
    ): Boolean {
        val lastSyncHash = meta[metaKey] ?: ""
        val extHash = getFileHash(externalFile)
        val intHash = getFileHash(internalFile)

        if (extHash.isNotEmpty() && extHash == intHash) {
            meta[metaKey] = extHash
            return false
        }

        if (externalFile.exists() && !internalFile.exists()) {
            // New external file -> Import
            Log.i(TAG, "Importing new external config: ${externalFile.name}")
            internalFile.parentFile?.mkdirs()
            externalFile.copyTo(internalFile, overwrite = true)
            meta[metaKey] = getFileHash(internalFile)
            return true
        }

        if (internalFile.exists() && !externalFile.exists()) {
            // New internal file -> Export
            externalFile.parentFile?.mkdirs()
            internalFile.copyTo(externalFile, overwrite = true)
            meta[metaKey] = getFileHash(externalFile)
            return false
        }

        if (externalFile.exists() && internalFile.exists()) {
            if (extHash.isNotEmpty() && extHash != lastSyncHash && intHash == lastSyncHash) {
                // User modified external file -> Import
                Log.i(TAG, "User modified external config ${externalFile.name}. Importing...")
                internalFile.parentFile?.mkdirs()
                externalFile.copyTo(internalFile, overwrite = true)
                meta[metaKey] = getFileHash(internalFile)
                return true
            } else if (intHash.isNotEmpty() && intHash != lastSyncHash && extHash == lastSyncHash) {
                // Internal file modified by app -> Export
                externalFile.parentFile?.mkdirs()
                internalFile.copyTo(externalFile, overwrite = true)
                meta[metaKey] = getFileHash(externalFile)
                return false
            } else {
                // Conflict or initial sync without hash: compare timestamps
                if (externalFile.lastModified() >= internalFile.lastModified()) {
                    Log.i(TAG, "External config ${externalFile.name} is newer. Importing...")
                    internalFile.parentFile?.mkdirs()
                    externalFile.copyTo(internalFile, overwrite = true)
                    meta[metaKey] = getFileHash(internalFile)
                    return true
                } else {
                    Log.i(TAG, "Internal config ${internalFile.name} is newer. Exporting...")
                    externalFile.parentFile?.mkdirs()
                    internalFile.copyTo(externalFile, overwrite = true)
                    meta[metaKey] = getFileHash(externalFile)
                    return false
                }
            }
        }

        return false
    }

    private fun getFileHash(file: File): String {
        if (!file.exists() || !file.isFile || file.length() == 0L) return ""
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    private val EXCLUDED_SHARED_PREFS_KEYS = setOf(
        LinuxEnvironment.PREF_KEY_NOTES_DIR,
        LinuxEnvironment.PREF_KEY_ONBOARDING_COMPLETED,
        "pref_last_opened_note_path",
        "pref_last_crash_timestamp"
    )

    private fun exportSharedPreferencesToJson(prefs: SharedPreferences, destFile: File) {
        try {
            val allEntries = prefs.all
            val json = JSONObject()
            for ((key, value) in allEntries) {
                if (key !in EXCLUDED_SHARED_PREFS_KEYS) {
                    json.put(key, value)
                }
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
                if (key in EXCLUDED_SHARED_PREFS_KEYS) continue
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

    private fun loadSyncMetadata(file: File): MutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        if (file.exists()) {
            try {
                val json = JSONObject(file.readText(Charsets.UTF_8))
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = json.optString(k, "")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read sync metadata", e)
            }
        }
        return map
    }

    private fun saveSyncMetadata(file: File, meta: Map<String, String>) {
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
