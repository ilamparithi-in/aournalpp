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

    private const val APP_SETTINGS_PREFS_NAME = "aournal_prefs"
    private const val SYNC_MAPPINGS_FILE = "sync_mappings.json"
    private const val META_TYPES_KEY = "__meta_types"

    fun getX11Prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
    }

    fun notifyX11PreferencesChanged(context: Context, key: String = "all") {
        try {
            val intent = android.content.Intent("com.termux.x11.ACTION_PREFERENCES_CHANGED").apply {
                putExtra("key", key)
                putExtra("fromBroadcast", true)
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {}
    }

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

            // 3. Custom Folder Mappings Sync (sync_mappings.json)
            syncMappingsFile(context, configDir, meta)

            // 4. Xournal++ & GTK Settings Sync (Recursive)
            syncXournalppConfigs(env, configDir, meta)

            // Save updated sync metadata
            saveSyncMetadata(metaFile, meta)
        } catch (e: Exception) {
            Log.w(TAG, "Error during Notes Home config synchronization", e)
        }
    }

    /**
     * Determines whether the given folder is an existing Aournal++ compatible workspace.
     * A folder is compatible if it contains a .config directory with app_settings.json,
     * x11_prefs.json, sync_mappings.json, settings.ini, or a non-empty xournalpp config directory (such as settings.xml).
     */
    fun isAournalCompatible(notesHomeDir: File): Boolean {
        if (!notesHomeDir.exists() || !notesHomeDir.isDirectory) return false
        val configDir = getConfigDirectory(notesHomeDir)
        if (!configDir.exists() || !configDir.isDirectory) return false

        val hasAppSettings = File(configDir, APP_SETTINGS_FILE).exists()
        val hasX11Prefs = File(configDir, X11_PREFS_FILE).exists()
        val xoppDir = File(configDir, XOURNALPP_DIR_NAME)
        val hasXopp = xoppDir.exists() && (xoppDir.list()?.isNotEmpty() == true)
        val hasDirectSettings = File(configDir, "settings.xml").exists()
        val hasSyncMappings = File(configDir, SYNC_MAPPINGS_FILE).exists()
        val hasGtkSettings = File(configDir, "settings.ini").exists()

        return hasAppSettings || hasX11Prefs || hasXopp || hasDirectSettings || hasSyncMappings || hasGtkSettings
    }

    /**
     * Safely restores settings and configurations from an existing Aournal++ compatible folder into internal app storage.
     * Imports app preferences, Termux-X11 settings, and Xournal++ configuration without overwriting them with blank defaults.
     */
    @Synchronized
    fun restoreSettingsFromNotesHome(notesHomeDir: File, context: Context, env: LinuxEnvironment): Boolean {
        try {
            val configDir = getConfigDirectory(notesHomeDir)
            if (!isAournalCompatible(notesHomeDir)) return false

            val metaFile = File(context.filesDir, SYNC_METADATA_FILE)
            val meta = loadSyncMetadata(metaFile)

            // 1. Force import app settings
            val appSettingsFile = File(configDir, APP_SETTINGS_FILE)
            if (appSettingsFile.exists()) {
                val appPrefs = context.getSharedPreferences(APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
                importJsonToSharedPreferences(appSettingsFile, appPrefs)
                meta["hash_$APP_SETTINGS_FILE"] = getFileHash(appSettingsFile)
            }

            // 2. Force import X11 preferences
            val x11PrefsFile = File(configDir, X11_PREFS_FILE)
            if (x11PrefsFile.exists()) {
                val x11Prefs = getX11Prefs(context)
                importJsonToSharedPreferences(x11PrefsFile, x11Prefs)
                meta["hash_$X11_PREFS_FILE"] = getFileHash(x11PrefsFile)
                notifyX11PreferencesChanged(context, "all")
            }

            // 3. Force import sync_mappings.json if present
            val syncMappingsFile = File(configDir, SYNC_MAPPINGS_FILE)
            if (syncMappingsFile.exists()) {
                val internalMappingsFile = File(context.filesDir, SYNC_MAPPINGS_FILE)
                syncMappingsFile.copyTo(internalMappingsFile, overwrite = true)
                meta["hash_$SYNC_MAPPINGS_FILE"] = getFileHash(syncMappingsFile)
            }

            // 4. Force import Xournal++ and GTK configurations
            val extXoppDir = File(configDir, XOURNALPP_DIR_NAME)
            val internalXoppDir = env.xournalConfigDir
            if (!internalXoppDir.exists()) {
                internalXoppDir.mkdirs()
            }

            if (extXoppDir.exists() && extXoppDir.isDirectory) {
                extXoppDir.copyRecursively(internalXoppDir, overwrite = true)
                extXoppDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val rel = file.relativeTo(extXoppDir).path
                    meta["hash_xopp_$rel"] = getFileHash(file)
                }
            }

            // Also check if settings.ini (GTK) exists in external configDir or xoppDir
            val extGtkSettings = File(configDir, "settings.ini")
            val extXoppGtkSettings = File(extXoppDir, "settings.ini")
            val gtkSource = when {
                extGtkSettings.exists() -> extGtkSettings
                extXoppGtkSettings.exists() -> extXoppGtkSettings
                else -> null
            }
            if (gtkSource != null) {
                val intGtkDir = File(env.configDir, "gtk-3.0")
                if (!intGtkDir.exists()) intGtkDir.mkdirs()
                val intGtkSettings = File(intGtkDir, "settings.ini")
                gtkSource.copyTo(intGtkSettings, overwrite = true)
                meta["hash_xopp_settings.ini"] = getFileHash(intGtkSettings)
            }

            // Sanitize paths and autoload preferences
            env.ensureXournalppSettings()
            env.checkAndOverrideAutoloadPreference()
            env.ensureMenuBarShortcuts()

            val intSettings = File(internalXoppDir, "settings.xml")
            val extSettings = File(extXoppDir, "settings.xml")
            if (intSettings.exists() && extSettings.exists()) {
                intSettings.copyTo(extSettings, overwrite = true)
                meta["hash_xopp_settings.xml"] = getFileHash(intSettings)
            }

            saveSyncMetadata(metaFile, meta)
            Log.i(TAG, "Successfully restored settings from ${notesHomeDir.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Error restoring settings from ${notesHomeDir.absolutePath}", e)
            return false
        }
    }

    /**
     * Copies the .config directory from the previous Notes Home to the new Notes Home without deleting the original.
     * If the new Notes Home is already Aournal++ compatible, its settings are restored into the app instead of overwritten.
     */
    fun copyConfigToNewNotesHome(oldNotesDir: File, newNotesDir: File, context: Context, env: LinuxEnvironment) {
        try {
            val oldConfigDir = getConfigDirectory(oldNotesDir)
            val newConfigDir = getConfigDirectory(newNotesDir)

            if (!newNotesDir.exists()) {
                newNotesDir.mkdirs()
            }

            if (isAournalCompatible(newNotesDir)) {
                Log.i(TAG, "New notes directory is already Aournal++ compatible. Restoring configs...")
                restoreSettingsFromNotesHome(newNotesDir, context, env)
                return
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
        val appPrefs = context.getSharedPreferences(APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
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
        val x11Prefs = getX11Prefs(context)
        val metaKey = "hash_$X11_PREFS_FILE"
        val lastSyncHash = meta[metaKey] ?: ""

        val extHash = getFileHash(externalFile)

        if (externalFile.exists() && extHash.isNotEmpty() && extHash != lastSyncHash) {
            Log.i(TAG, "External $X11_PREFS_FILE modified. Importing to X11 Preferences...")
            importJsonToSharedPreferences(externalFile, x11Prefs)
            meta[metaKey] = getFileHash(externalFile)
            notifyX11PreferencesChanged(context, "all")
        } else {
            exportSharedPreferencesToJson(x11Prefs, externalFile)
            meta[metaKey] = getFileHash(externalFile)
        }
    }

    private fun syncMappingsFile(context: Context, configDir: File, meta: MutableMap<String, String>) {
        val internalFile = File(context.filesDir, SYNC_MAPPINGS_FILE)
        val externalFile = File(configDir, SYNC_MAPPINGS_FILE)
        val metaKey = "hash_$SYNC_MAPPINGS_FILE"
        syncSingleFile(internalFile, externalFile, metaKey, meta)
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
        "pref_last_crash_timestamp",
        "pref_last_provisioned_asset_version"
    )

    fun exportSharedPreferencesToJson(prefs: SharedPreferences, destFile: File) {
        try {
            val allEntries = prefs.all
            val json = JSONObject()
            val metaTypes = JSONObject()

            for ((key, value) in allEntries) {
                if (key in EXCLUDED_SHARED_PREFS_KEYS || key == META_TYPES_KEY) continue
                when (value) {
                    is Boolean -> {
                        metaTypes.put(key, "BOOLEAN")
                        json.put(key, value)
                    }
                    is Int -> {
                        metaTypes.put(key, "INT")
                        json.put(key, value)
                    }
                    is Long -> {
                        metaTypes.put(key, "LONG")
                        json.put(key, value)
                    }
                    is Float -> {
                        metaTypes.put(key, "FLOAT")
                        json.put(key, value.toDouble())
                    }
                    is Double -> {
                        metaTypes.put(key, "FLOAT")
                        json.put(key, value)
                    }
                    is String -> {
                        metaTypes.put(key, "STRING")
                        json.put(key, value)
                    }
                    is Set<*> -> {
                        metaTypes.put(key, "STRING_SET")
                        val arr = org.json.JSONArray()
                        for (item in value) {
                            if (item != null) arr.put(item.toString())
                        }
                        json.put(key, arr)
                    }
                    else -> {
                        if (value != null) {
                            metaTypes.put(key, "STRING")
                            json.put(key, value.toString())
                        }
                    }
                }
            }
            json.put(META_TYPES_KEY, metaTypes)
            destFile.writeText(json.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to export SharedPreferences to ${destFile.name}", e)
        }
    }

    fun importJsonToSharedPreferences(sourceFile: File, prefs: SharedPreferences) {
        try {
            val text = sourceFile.readText(Charsets.UTF_8)
            val json = JSONObject(text)
            val metaTypesObj = json.optJSONObject(META_TYPES_KEY)
            val editor = prefs.edit()
            val existingEntries = prefs.all

            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key in EXCLUDED_SHARED_PREFS_KEYS || key == META_TYPES_KEY) continue
                val rawValue = json.get(key)
                val existing = existingEntries[key]
                val recordedType = metaTypesObj?.optString(key, "") ?: ""

                if (recordedType.isNotEmpty()) {
                    when (recordedType) {
                        "BOOLEAN" -> {
                            val b = (rawValue as? Boolean) ?: rawValue.toString().toBooleanStrictOrNull()
                            if (b != null) editor.putBoolean(key, b)
                        }
                        "INT" -> {
                            val num = (rawValue as? Number)?.toInt() ?: rawValue.toString().toIntOrNull()
                            if (num != null) editor.putInt(key, num)
                        }
                        "LONG" -> {
                            val num = (rawValue as? Number)?.toLong() ?: rawValue.toString().toLongOrNull()
                            if (num != null) editor.putLong(key, num)
                        }
                        "FLOAT" -> {
                            val num = (rawValue as? Number)?.toFloat() ?: rawValue.toString().toFloatOrNull()
                            if (num != null) editor.putFloat(key, num)
                        }
                        "STRING" -> {
                            editor.putString(key, rawValue.toString())
                        }
                        "STRING_SET" -> {
                            val set = mutableSetOf<String>()
                            val arr = rawValue as? org.json.JSONArray
                            if (arr != null) {
                                for (i in 0 until arr.length()) {
                                    set.add(arr.getString(i))
                                }
                            }
                            editor.putStringSet(key, set)
                        }
                        else -> {
                            editor.putString(key, rawValue.toString())
                        }
                    }
                } else if (existing != null) {
                    when (existing) {
                        is Long -> {
                            val num = (rawValue as? Number)?.toLong() ?: rawValue.toString().toLongOrNull()
                            if (num != null) editor.putLong(key, num)
                        }
                        is Int -> {
                            val num = (rawValue as? Number)?.toInt() ?: rawValue.toString().toIntOrNull()
                            if (num != null) editor.putInt(key, num)
                        }
                        is Float -> {
                            val num = (rawValue as? Number)?.toFloat() ?: rawValue.toString().toFloatOrNull()
                            if (num != null) editor.putFloat(key, num)
                        }
                        is Boolean -> {
                            val b = (rawValue as? Boolean) ?: rawValue.toString().toBooleanStrictOrNull()
                            if (b != null) editor.putBoolean(key, b)
                        }
                        is String -> {
                            editor.putString(key, rawValue.toString())
                        }
                        is Set<*> -> {
                            val set = mutableSetOf<String>()
                            val arr = rawValue as? org.json.JSONArray
                            if (arr != null) {
                                for (i in 0 until arr.length()) {
                                    set.add(arr.getString(i))
                                }
                            }
                            editor.putStringSet(key, set)
                        }
                    }
                } else {
                    // Fallback for legacy JSON files without __meta_types and no existing preference
                    when {
                        rawValue is Boolean -> editor.putBoolean(key, rawValue)
                        isKnownFloatKey(key) -> {
                            val num = (rawValue as? Number)?.toFloat() ?: rawValue.toString().toFloatOrNull()
                            if (num != null) editor.putFloat(key, num)
                        }
                        isKnownLongKey(key) -> {
                            val num = (rawValue as? Number)?.toLong() ?: rawValue.toString().toLongOrNull()
                            if (num != null) editor.putLong(key, num)
                        }
                        rawValue is Int -> editor.putInt(key, rawValue)
                        rawValue is Long -> editor.putLong(key, rawValue)
                        rawValue is Float -> editor.putFloat(key, rawValue)
                        rawValue is Double -> editor.putFloat(key, rawValue.toFloat())
                        rawValue is org.json.JSONArray -> {
                            val set = mutableSetOf<String>()
                            for (i in 0 until rawValue.length()) {
                                set.add(rawValue.getString(i))
                            }
                            editor.putStringSet(key, set)
                        }
                        else -> editor.putString(key, rawValue.toString())
                    }
                }
            }
            editor.apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to import JSON from ${sourceFile.name} to SharedPreferences", e)
        }
    }

    private fun isKnownFloatKey(key: String): Boolean {
        return key == "toolbarPosXRatio" ||
                key == "toolbarPosYRatio" ||
                key.endsWith("Ratio") ||
                key.endsWith("_ratio")
    }

    private fun isKnownLongKey(key: String): Boolean {
        return key.endsWith("_timestamp") ||
                key.endsWith("_time") ||
                key.endsWith("_version") ||
                key == "pref_last_provisioned_asset_version"
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
