package dev.ilamparithi.aournalpp.runtime

import android.content.Context
import android.os.Environment
import android.system.Os
import android.util.Log
import java.io.File
import java.nio.file.Files

class LinuxEnvironment(private val context: Context) {
    companion object {
        private const val TAG = "LinuxEnvironment"
        const val PREF_KEY_NOTES_DIR = "pref_notes_dir"
        const val PREF_KEY_APP_THEME = "pref_app_theme"
        const val PREF_KEY_GTK_THEME = "pref_gtk_theme"
        const val PREF_KEY_WALLPAPER_MODE = "pref_canvas_wallpaper_mode"
        const val PREF_KEY_PENDING_AUTOLOAD_NOTIFICATION = "pref_pending_autoload_conflict_notification"
        const val PREF_KEY_AUDIO_DIR = "pref_special_audio_dir"
        const val PREF_KEY_IMPORTED_DIR = "pref_special_imported_dir"
        const val PREF_KEY_EMERGENCY_DIR = "pref_special_emergency_dir"
        const val PREF_KEY_ONBOARDING_COMPLETED = "pref_onboarding_completed"
        const val PREF_KEY_REDUCE_ANIMATIONS = "pref_reduce_animations"
    }

    fun isReduceAnimations(): Boolean {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_KEY_REDUCE_ANIMATIONS, false)
    }

    fun setReduceAnimations(reduce: Boolean) {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_KEY_REDUCE_ANIMATIONS, reduce).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    val rootDir: File = context.filesDir
    val usrDir: File = File(rootDir, "usr")
    val binDir: File = File(usrDir, "bin")
    val libDir: File = File(usrDir, "lib")
    val shareDir: File = File(usrDir, "share")
    val tmpDir: File = File(usrDir, "tmp")
    val homeDir: File = File(rootDir, "home")
    val configDir: File = File(homeDir, ".config")
    val xournalConfigDir: File = File(configDir, "xournalpp")
    val openboxConfigDir: File = File(configDir, "openbox")
    val nativeLibDir: File
        get() = File(context.applicationInfo.nativeLibraryDir)

    fun resolveExecutable(name: String): File {
        val candidates = listOf(
            "lib" + name.replace('-', '_') + ".so",
            "lib$name.so",
            name
        )
        for (candidate in candidates) {
            val nativeFile = File(nativeLibDir, candidate)
            if (nativeFile.exists() && nativeFile.canExecute()) {
                return nativeFile
            }
        }
        return File(binDir, name)
    }

    val defaultNotesDir: File
        get() = File(Environment.getExternalStorageDirectory(), "Documents/Notes")

    val sharedDocumentsNotesDir: File
        get() = getNotesDirectory()

    val sharedDownloadsDir: File
        get() = File(Environment.getExternalStorageDirectory(), "Download")

    fun getNotesDirectory(): File {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        val savedPath = prefs.getString(PREF_KEY_NOTES_DIR, null)
        return if (!savedPath.isNullOrBlank()) {
            File(savedPath)
        } else {
            defaultNotesDir
        }
    }

    fun getEmergencySavesDirectory(): File {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        val savedPath = prefs.getString(PREF_KEY_EMERGENCY_DIR, null)
        val dir = if (!savedPath.isNullOrBlank()) {
            val f = File(savedPath)
            if (f.exists()) f else File(getNotesDirectory(), "Emergency Saves")
        } else {
            File(getNotesDirectory(), "Emergency Saves")
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val metaFile = File(dir, ".folder.json")
        if (!metaFile.exists()) {
            try {
                metaFile.writeText(
                    """
                    {
                      "role": "emergency",
                      "color": "#F44336",
                      "icon": "emergency"
                    }
                    """.trimIndent()
                )
            } catch (e: Exception) {
                // ignore
            }
        }
        return dir
    }

    fun getImportedDirectory(): File {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        val savedPath = prefs.getString(PREF_KEY_IMPORTED_DIR, null)
        val dir = if (!savedPath.isNullOrBlank()) {
            val f = File(savedPath)
            if (f.exists()) f else File(getNotesDirectory(), "Imported")
        } else {
            File(getNotesDirectory(), "Imported")
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val metaFile = File(dir, ".folder.json")
        if (!metaFile.exists()) {
            try {
                metaFile.writeText(
                    """
                    {
                      "role": "import",
                      "icon": "import"
                    }
                    """.trimIndent()
                )
            } catch (e: Exception) {
                // ignore
            }
        }
        return dir
    }

    fun getAudioDirectory(): File {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        val savedPath = prefs.getString(PREF_KEY_AUDIO_DIR, null)
        val dir = if (!savedPath.isNullOrBlank()) {
            val f = File(savedPath)
            if (f.exists()) f else File(getNotesDirectory(), "Audio")
        } else {
            File(getNotesDirectory(), "Audio")
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val metaFile = File(dir, ".folder.json")
        if (!metaFile.exists()) {
            try {
                metaFile.writeText(
                    """
                    {
                      "role": "audio",
                      "icon": "audio"
                    }
                    """.trimIndent()
                )
            } catch (e: Exception) {
                // ignore
            }
        }
        return dir
    }

    fun setSpecialDirectoryPath(role: String, newPath: String) {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        val prefKey = when (role.lowercase()) {
            "emergency" -> PREF_KEY_EMERGENCY_DIR
            "import", "imported" -> PREF_KEY_IMPORTED_DIR
            "audio" -> PREF_KEY_AUDIO_DIR
            else -> null
        }
        if (prefKey != null) {
            prefs.edit().putString(prefKey, newPath).apply()
        }
    }

    fun setNotesDirectory(newPath: String) {
        val oldNotesDir = getNotesDirectory()
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY_NOTES_DIR, newPath).apply()
        val newNotesDir = File(newPath)
        NotesHomeConfigManager.copyConfigToNewNotesHome(oldNotesDir, newNotesDir, context, this)
        ensureDirectoryTree()
        ensureGtkBookmarks()
    }

    /**
     * Updates PREF_KEY_NOTES_DIR and GTK bookmarks without triggering eager config copy or sync.
     * Use this during onboarding or restore flows before external configs are downloaded or restored.
     */
    fun setNotesDirectoryPathOnly(newPath: String) {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY_NOTES_DIR, newPath).apply()
        val target = File(newPath)
        if (!target.exists()) {
            target.mkdirs()
        }
        ensureGtkBookmarks()
    }

    fun ensureDirectoryTree() {
        val dirs = listOf(
            rootDir, usrDir, binDir, libDir, shareDir, tmpDir, homeDir, configDir, xournalConfigDir, openboxConfigDir
        )
        dirs.forEach {
            if (!it.exists()) {
                it.mkdirs()
            }
        }

        // Provision Openbox kiosk auto-maximization rules (normal windows maximized, dialogs with borders and center placement)
        val rcFile = File(openboxConfigDir, "rc.xml")
        rcFile.writeText(
            """<?xml version="1.0" encoding="UTF-8"?>
<openbox_config xmlns="http://openbox.org/3.4/rc">
  <theme>
    <name>Clearlooks</name>
    <titleLayout>NLIMC</titleLayout>
  </theme>
  <placement>
    <policy>smart</policy>
    <center>yes</center>
  </placement>
  <applications>
    <!-- Main application windows: maximized & borderless -->
    <application class="*" type="normal">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <!-- Dialogs & message boxes: centered with standard dialog decorations -->
    <application class="*" type="dialog">
      <decor>yes</decor>
      <maximized>no</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" type="splash">
      <decor>no</decor>
      <maximized>no</maximized>
    </application>
  </applications>
</openbox_config>
""".trimIndent()
        )
        // Symlink xournalpp share assets into homeDir for runtime lookup
        val xoppShareDir = File(shareDir, "xournalpp")
        if (xoppShareDir.exists()) {
            listOf("ui", "palettes", "plugins", "resources").forEach { subDirName ->
                val targetSub = File(xoppShareDir, subDirName)
                val homeLink = File(homeDir, subDirName)
                if (targetSub.exists() && !homeLink.exists()) {
                    try {
                        java.nio.file.Files.createSymbolicLink(homeLink.toPath(), targetSub.toPath())
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            val homeShareLink = File(homeDir, "share")
            if (!homeShareLink.exists()) {
                try {
                    java.nio.file.Files.createSymbolicLink(homeShareLink.toPath(), shareDir.toPath())
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        // Symlink share/locale into homeDir for runtime and gettext discovery
        val localeShareDir = File(shareDir, "locale")
        val homeLocaleLink = File(homeDir, "locale")
        if (localeShareDir.exists() && !homeLocaleLink.exists()) {
            try {
                java.nio.file.Files.createSymbolicLink(homeLocaleLink.toPath(), localeShareDir.toPath())
            } catch (e: Exception) {
                // ignore
            }
        }
        val homeShareLocale = File(homeDir, "share/locale")
        if (localeShareDir.exists() && !homeShareLocale.exists()) {
            try {
                homeShareLocale.parentFile?.mkdirs()
                java.nio.file.Files.createSymbolicLink(homeShareLocale.toPath(), localeShareDir.toPath())
            } catch (e: Exception) {
                // ignore
            }
        }

        // Self-heal: Compile GLib GSettings schemas if gschemas.compiled is missing
        val schemasDir = File(shareDir, "glib-2.0/schemas")
        val compileSchemasBin = resolveExecutable("glib-compile-schemas")
        val compiledSchema = File(schemasDir, "gschemas.compiled")
        if (!compiledSchema.exists() && compileSchemasBin.exists() && schemasDir.exists()) {
            try {
                val pb = ProcessBuilder(compileSchemasBin.absolutePath, schemasDir.absolutePath)
                    .redirectErrorStream(true)
                pb.environment().putAll(getEnvMap())
                val p = pb.start()
                p.waitFor()
                Log.i(TAG, "Self-healed & compiled glib schemas in ${schemasDir.absolutePath}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to compile glib schemas during ensureDirectoryTree", e)
            }
        }

        // Self-heal: Generate GDK Pixbuf loaders.cache if missing or empty
        val gdkDir = File(libDir, "gdk-pixbuf-2.0/2.10.0")
        val loadersCache = File(gdkDir, "loaders.cache")
        val loadersDir = File(gdkDir, "loaders")
        val queryLoadersBin = resolveExecutable("gdk-pixbuf-query-loaders")
        if ((!loadersCache.exists() || loadersCache.length() == 0L) && queryLoadersBin.exists() && loadersDir.exists()) {
            try {
                val loaderFiles = loadersDir.listFiles { _, name -> name.endsWith(".so") }
                if (!loaderFiles.isNullOrEmpty()) {
                    val cmd = mutableListOf(queryLoadersBin.absolutePath)
                    loaderFiles.forEach { cmd.add(it.absolutePath) }
                    val pb = ProcessBuilder(cmd)
                        .redirectOutput(loadersCache)
                    pb.environment().putAll(getEnvMap())
                    val p = pb.start()
                    p.waitFor()
                    Log.i(TAG, "Self-healed & generated gdk-pixbuf loaders.cache")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to generate loaders.cache during ensureDirectoryTree", e)
            }
        }

        val titleWatcherBin = File(binDir, "xopp-title-watcher")
        val gtkModulesDir = File(libDir, "gtk-3.0/modules")
        val imeModuleInGtkDir = File(gtkModulesDir, "libgtk-android-ime.so")
        val wallpaperBin = File(binDir, "xopp-wallpaper")

        val currentVersionCode = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (_: Exception) {
            1L
        }

        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        val lastProvisionedVersion = try {
            prefs.getLong("pref_last_provisioned_asset_version", -1L)
        } catch (_: ClassCastException) {
            val fallback = try {
                prefs.getInt("pref_last_provisioned_asset_version", -1).toLong()
            } catch (_: Exception) {
                -1L
            }
            prefs.edit().putLong("pref_last_provisioned_asset_version", fallback).apply()
            fallback
        }

        val needsAssetExtraction = lastProvisionedVersion != currentVersionCode ||
                !titleWatcherBin.exists() ||
                !imeModuleInGtkDir.exists() ||
                !wallpaperBin.exists()

        if (needsAssetExtraction) {
            // Provision xopp-title-watcher binary from assets if available
            try {
                context.assets.open("bin/xopp-title-watcher").use { input ->
                    titleWatcherBin.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                titleWatcherBin.setExecutable(true, false)
                Log.i(TAG, "Provisioned latest xopp-title-watcher binary")
            } catch (e: Exception) {
                if (titleWatcherBin.exists()) {
                    titleWatcherBin.setExecutable(true, false)
                }
            }

            // Provision libgtk-android-ime.so GTK focus bridge module
            gtkModulesDir.mkdirs()
            val imeModuleLib = File(libDir, "libgtk-android-ime.so")
            try {
                context.assets.open("lib/libgtk-android-ime.so").use { input ->
                    imeModuleLib.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                imeModuleLib.copyTo(imeModuleInGtkDir, overwrite = true)
                Log.i(TAG, "Provisioned latest libgtk-android-ime.so GTK focus bridge module")
            } catch (e: Exception) {
                // Ignore if in bootstrap
            }

            // Provision xopp-wallpaper binary from assets if available
            try {
                context.assets.open("bin/xopp-wallpaper").use { input ->
                    wallpaperBin.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                wallpaperBin.setExecutable(true, false)
                Log.i(TAG, "Provisioned latest xopp-wallpaper binary")
            } catch (e: Exception) {
                if (wallpaperBin.exists()) {
                    wallpaperBin.setExecutable(true, false)
                }
            }

            prefs.edit().putLong("pref_last_provisioned_asset_version", currentVersionCode).apply()
        }

        setupStorageSymlinks()
        writeGtkBookmarks()
        writeGtkSettings()
        ensureXournalppSettings()
        ensureMenuBarShortcuts()
        checkAndQuarantineEmergencySave()
        if (isOnboardingCompleted()) {
            NotesHomeConfigManager.sync(context, this)
        } else {
            Log.i(TAG, "Skipping NotesHomeConfigManager.sync: onboarding not completed yet")
        }
    }

    val quarantineRecoveryDir: File by lazy {
        File(context.cacheDir, "recovery").apply { mkdirs() }
    }

    fun isEmergencyRecoveryEnabled(): Boolean {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("pref_intelligent_emergency_recovery", true)
    }

    fun checkAndQuarantineEmergencySave(): File? {
        if (!isEmergencyRecoveryEnabled()) {
            // Subsystem disabled: do not quarantine or delete; leave for native X11 Xournal++ dialog
            return null
        }
        try {
            val emergencyFile = File(xournalConfigDir, "emergencysave.xopp")
            if (emergencyFile.exists() && emergencyFile.length() > 0) {
                if (isEmergencySaveBlank(emergencyFile)) {
                    Log.i(TAG, "emergencysave.xopp is a blank default template (zero user content); discarding.")
                    emergencyFile.delete()
                    clearQuarantinedEmergencySave()
                    return null
                }
                if (isEmergencySaveDuplicateOfSavedFile(emergencyFile)) {
                    Log.i(TAG, "emergencysave.xopp matches the cleanly saved active note; discarding duplicate.")
                    emergencyFile.delete()
                    clearQuarantinedEmergencySave()
                    return null
                }
                val quarantined = File(quarantineRecoveryDir, "quarantined_emergencysave.xopp")
                emergencyFile.copyTo(quarantined, overwrite = true)
                emergencyFile.delete()
                Log.i(TAG, "Quarantined genuine unsaved emergencysave.xopp to ${quarantined.absolutePath}")
                return quarantined
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to quarantine emergency save file", e)
        }
        return getQuarantinedEmergencySave()
    }

    private fun isEmergencySaveBlank(emergencyFile: File): Boolean {
        if (!emergencyFile.exists() || emergencyFile.length() == 0L) return true
        val bytes = getDecompressedBytes(emergencyFile)
        if (bytes.isEmpty()) return true
        val text = try {
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
        if (text.isBlank()) return true
        // If it contains no strokes, text items, or images, it is an empty blank document
        val hasContent = text.contains("<stroke") ||
                text.contains("<text") ||
                text.contains("<image") ||
                text.contains("<teximage")
        return !hasContent
    }

    private fun isEmergencySaveDuplicateOfSavedFile(emergencyFile: File): Boolean {
        if (!emergencyFile.exists() || emergencyFile.length() == 0L) return true

        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        val lastOpenedPath = prefs.getString("pref_last_opened_note_path", null)

        if (!lastOpenedPath.isNullOrBlank()) {
            val lastNote = File(lastOpenedPath)
            if (lastNote.exists() && lastNote.isFile && lastNote.length() > 0L) {
                val emergencyBytes = getDecompressedBytes(emergencyFile)
                val noteBytes = getDecompressedBytes(lastNote)
                if (emergencyBytes.isNotEmpty() && emergencyBytes.contentEquals(noteBytes)) {
                    return true
                }
            }
        }
        return false
    }

    private fun getDecompressedBytes(file: File): ByteArray {
        return try {
            java.util.zip.GZIPInputStream(file.inputStream()).use { it.readBytes() }
        } catch (e: Exception) {
            try {
                file.readBytes()
            } catch (e2: Exception) {
                ByteArray(0)
            }
        }
    }

    fun getQuarantinedEmergencySave(): File? {
        if (!isEmergencyRecoveryEnabled()) {
            val file = File(quarantineRecoveryDir, "quarantined_emergencysave.xopp")
            if (file.exists() && file.length() > 0) {
                // If user disabled Android recovery subsystem, restore the quarantined file back to Xournal++ config dir
                val emergencyFile = File(xournalConfigDir, "emergencysave.xopp")
                file.copyTo(emergencyFile, overwrite = true)
                file.delete()
            }
            return null
        }
        val file = File(quarantineRecoveryDir, "quarantined_emergencysave.xopp")
        if (file.exists() && file.length() > 0) {
            if (isEmergencySaveBlank(file) || isEmergencySaveDuplicateOfSavedFile(file)) {
                Log.i(TAG, "Quarantined emergency save is blank or duplicate of saved note; deleting.")
                file.delete()
                return null
            }
            return file
        }
        return null
    }

    fun clearQuarantinedEmergencySave() {
        try {
            val file = File(quarantineRecoveryDir, "quarantined_emergencysave.xopp")
            if (file.exists()) {
                file.delete()
            }
            val emergencyFile = File(xournalConfigDir, "emergencysave.xopp")
            if (emergencyFile.exists()) {
                emergencyFile.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear quarantined emergency save", e)
        }
    }

    fun setupStorageSymlinks() {
        try {
            val documentsNotes = sharedDocumentsNotesDir
            if (!documentsNotes.exists()) {
                documentsNotes.mkdirs()
            }

            val homeNotes = File(homeDir, "Notes")
            val isSymlink = try {
                Files.isSymbolicLink(homeNotes.toPath())
            } catch (e: Exception) {
                false
            }

            if (!homeNotes.exists() && !isSymlink) {
                try {
                    Os.symlink(documentsNotes.absolutePath, homeNotes.absolutePath)
                    Log.i(TAG, "Created Notes symlink: ${homeNotes.absolutePath} -> ${documentsNotes.absolutePath}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to create Notes symlink", e)
                }
            } else if (isSymlink) {
                // If symlink points to a different folder than current notes dir, update it
                try {
                    val currentTarget = Files.readSymbolicLink(homeNotes.toPath()).toString()
                    if (currentTarget != documentsNotes.absolutePath) {
                        homeNotes.delete()
                        Os.symlink(documentsNotes.absolutePath, homeNotes.absolutePath)
                        Log.i(TAG, "Updated Notes symlink target to ${documentsNotes.absolutePath}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to check or update symlink target", e)
                }
            } else if (homeNotes.exists() && !isSymlink && homeNotes.isDirectory) {
                // Migrate any existing files to shared storage, then replace folder with symlink
                try {
                    homeNotes.listFiles()?.forEach { file ->
                        val target = File(documentsNotes, file.name)
                        if (!target.exists()) {
                            file.copyTo(target, overwrite = true)
                        }
                        file.delete()
                    }
                    if (homeNotes.delete()) {
                        Os.symlink(documentsNotes.absolutePath, homeNotes.absolutePath)
                        Log.i(TAG, "Migrated legacy Notes folder to symlink -> ${documentsNotes.absolutePath}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to migrate legacy Notes folder to symlink", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting up storage symlinks", e)
        }
    }

    fun writeGtkBookmarks() {
        try {
            val gtk3ConfigDir = File(configDir, "gtk-3.0")
            if (!gtk3ConfigDir.exists()) {
                gtk3ConfigDir.mkdirs()
            }

            val bookmarksFile = File(gtk3ConfigDir, "bookmarks")
            val storageNotes = sharedDocumentsNotesDir.absolutePath
            val storageDownloads = sharedDownloadsDir.absolutePath

            val content = buildString {
                appendLine("file://$storageNotes Notes")
                appendLine("file://$storageDownloads Downloads")
            }

            bookmarksFile.writeText(content)
            Log.i(TAG, "Provisioned GTK bookmarks at ${bookmarksFile.absolutePath}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write GTK bookmarks", e)
        }
    }

    fun isGtkDarkMode(): Boolean {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        return when (prefs.getString(PREF_KEY_GTK_THEME, "system")) {
            "light" -> false
            "dark" -> true
            else -> {
                val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun writeGtkSettings() {
        try {
            val gtk3ConfigDir = File(configDir, "gtk-3.0")
            if (!gtk3ConfigDir.exists()) {
                gtk3ConfigDir.mkdirs()
            }

            val settingsFile = File(gtk3ConfigDir, "settings.ini")
            val isDark = isGtkDarkMode()
            val content = buildString {
                appendLine("[Settings]")
                appendLine("gtk-theme-name = Adwaita")
                appendLine("gtk-application-prefer-dark-theme = ${if (isDark) "1" else "0"}")
                appendLine("gtk-icon-theme-name = Adwaita")
            }

            settingsFile.writeText(content)
            Log.i(TAG, "Provisioned GTK settings.ini with isDark=$isDark at ${settingsFile.absolutePath}")
            ensureGtkBookmarks()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write GTK settings.ini", e)
        }
    }

    fun ensureGtkBookmarks() {
        try {
            val gtk3ConfigDir = File(configDir, "gtk-3.0")
            if (!gtk3ConfigDir.exists()) {
                gtk3ConfigDir.mkdirs()
            }
            val bookmarksFile = File(gtk3ConfigDir, "bookmarks")
            val notesDir = getNotesDirectory()
            val docsDir = File(Environment.getExternalStorageDirectory(), "Documents")
            val downloadsDir = sharedDownloadsDir

            val lines = mutableListOf<String>()
            lines.add("file://${notesDir.absolutePath} Notes Home")
            if (docsDir.exists() && docsDir.absolutePath != notesDir.absolutePath) {
                lines.add("file://${docsDir.absolutePath} Documents")
            }
            if (downloadsDir.exists() && downloadsDir.absolutePath != notesDir.absolutePath) {
                lines.add("file://${downloadsDir.absolutePath} Downloads")
            }

            val content = lines.joinToString("\n") + "\n"
            bookmarksFile.writeText(content)

            // Also mirror to legacy ~/.gtk-bookmarks for older GTK versions
            val legacyBookmarks = File(homeDir, ".gtk-bookmarks")
            legacyBookmarks.writeText(content)
            Log.i(TAG, "Provisioned dynamic GTK bookmarks pointing to Notes Home: ${notesDir.absolutePath}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write GTK bookmarks", e)
        }
    }

    fun hasPendingAutoloadOverrideNotification(): Boolean {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_KEY_PENDING_AUTOLOAD_NOTIFICATION, false)
    }

    fun clearPendingAutoloadOverrideNotification() {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove(PREF_KEY_PENDING_AUTOLOAD_NOTIFICATION).apply()
    }

    fun setPendingAutoloadOverrideNotification(pending: Boolean) {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_KEY_PENDING_AUTOLOAD_NOTIFICATION, pending).apply()
    }

    /**
     * Checks if Xournal++ 'autoloadMostRecent' (or legacy 'autoloadLastFile') preference in settings.xml is enabled.
     * If enabled (or != "false"), overrides it to "false" and sets the
     * pending notification flag so the user is informed about the conflict with "Continue where you left off".
     *
     * @return true if an active autoload preference was detected and overridden.
     */
    fun checkAndOverrideAutoloadPreference(): Boolean {
        try {
            val settingsFile = File(xournalConfigDir, "settings.xml")
            if (!settingsFile.exists()) {
                return false
            }

            var content = settingsFile.readText()
            var modified = false
            var overridden = false

            val propertyNames = listOf("autoloadMostRecent", "autoloadLastFile")
            for (prop in propertyNames) {
                val propRegex = Regex("""<property\b(?=[^>]*\bname\s*=\s*["']$prop["'])(?=[^>]*\bvalue\s*=\s*["']([^"']*)["'])[^>]*/>""")
                val match = propRegex.find(content)

                if (match != null) {
                    val currentValue = match.groupValues[1].trim()
                    val isEnabled = currentValue.equals("true", ignoreCase = true) ||
                            currentValue == "1" ||
                            currentValue.equals("yes", ignoreCase = true) ||
                            currentValue.equals("on", ignoreCase = true)

                    if (isEnabled) {
                        content = content.replace(match.value, "<property name=\"$prop\" value=\"false\"/>")
                        modified = true
                        overridden = true
                        Log.i(TAG, "Overrode Xournal++ $prop preference from '$currentValue' to 'false'. Marked pending notification.")
                    }
                }
            }

            if (!content.contains("autoloadMostRecent") && content.contains("</settings>")) {
                content = content.replace("</settings>", "  <property name=\"autoloadMostRecent\" value=\"false\"/>\n</settings>")
                modified = true
                Log.i(TAG, "Injected missing autoloadMostRecent='false' into settings.xml.")
            }

            if (modified) {
                settingsFile.writeText(content)
            }

            if (overridden) {
                setPendingAutoloadOverrideNotification(true)
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check and override autoload preference", e)
        }
        return false
    }

    fun ensureXournalppSettings() {
        try {
            val settingsFile = File(xournalConfigDir, "settings.xml")
            val defaultNotesPath = sharedDocumentsNotesDir.absolutePath

            if (!settingsFile.exists()) {
                xournalConfigDir.mkdirs()
                val initialXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <settings>
                      <property name="defaultSaveDir" value="$defaultNotesPath"/>
                      <property name="defaultOpenDir" value="$defaultNotesPath"/>
                      <property name="autoloadMostRecent" value="false"/>
                    </settings>
                """.trimIndent()
                settingsFile.writeText(initialXml)
                Log.i(TAG, "Provisioned initial settings.xml with defaultSaveDir=$defaultNotesPath and autoloadMostRecent=false")
            } else {
                var content = settingsFile.readText()
                var modified = false
                if (content.contains("defaultSaveDir")) {
                    content = content.replace(
                        Regex("<property\\s+name=\"defaultSaveDir\"\\s+value=\"[^\"]*\"/>"),
                        "<property name=\"defaultSaveDir\" value=\"$defaultNotesPath\"/>"
                    )
                    modified = true
                } else {
                    content = content.replace("</settings>", "  <property name=\"defaultSaveDir\" value=\"$defaultNotesPath\"/>\n</settings>")
                    modified = true
                }

                if (content.contains("defaultOpenDir")) {
                    content = content.replace(
                        Regex("<property\\s+name=\"defaultOpenDir\"\\s+value=\"[^\"]*\"/>"),
                        "<property name=\"defaultOpenDir\" value=\"$defaultNotesPath\"/>"
                    )
                    modified = true
                } else {
                    content = content.replace("</settings>", "  <property name=\"defaultOpenDir\" value=\"$defaultNotesPath\"/>\n</settings>")
                    modified = true
                }

                val savePathRegex = Regex("""<property\b(?=[^>]*\bname\s*=\s*["']lastSavePath["'])(?=[^>]*\bvalue\s*=\s*["']([^"']*)["'])[^>]*/>""")
                val savePathMatch = savePathRegex.find(content)
                if (savePathMatch != null) {
                    val currentSavePath = savePathMatch.groupValues[1].trim()
                    val saveDir = File(currentSavePath)
                    if (!saveDir.exists() || !currentSavePath.startsWith(defaultNotesPath)) {
                        content = content.replace(savePathMatch.value, "<property name=\"lastSavePath\" value=\"$defaultNotesPath\"/>")
                        modified = true
                    }
                } else if (content.contains("</settings>")) {
                    content = content.replace("</settings>", "  <property name=\"lastSavePath\" value=\"$defaultNotesPath\"/>\n</settings>")
                    modified = true
                }

                val openPathRegex = Regex("""<property\b(?=[^>]*\bname\s*=\s*["']lastOpenPath["'])(?=[^>]*\bvalue\s*=\s*["']([^"']*)["'])[^>]*/>""")
                val openPathMatch = openPathRegex.find(content)
                if (openPathMatch != null) {
                    val currentOpenPath = openPathMatch.groupValues[1].trim()
                    val openDir = File(currentOpenPath)
                    if (!openDir.exists() || !currentOpenPath.startsWith(defaultNotesPath)) {
                        content = content.replace(openPathMatch.value, "<property name=\"lastOpenPath\" value=\"$defaultNotesPath\"/>")
                        modified = true
                    }
                } else if (content.contains("</settings>")) {
                    content = content.replace("</settings>", "  <property name=\"lastOpenPath\" value=\"$defaultNotesPath\"/>\n</settings>")
                    modified = true
                }

                if (content.contains("autosaveTimeout")) {
                    content = content.replace(
                        Regex("<property\\s+name=\"autosaveTimeout\"\\s+value=\"[^\"]*\"/>"),
                        "<property name=\"autosaveTimeout\" value=\"1\"/>"
                    )
                    modified = true
                } else {
                    content = content.replace("</settings>", "  <property name=\"autosaveTimeout\" value=\"1\"/>\n</settings>")
                    modified = true
                }

                if (content.contains("autosaveEnabled")) {
                    content = content.replace(
                        Regex("<property\\s+name=\"autosaveEnabled\"\\s+value=\"[^\"]*\"/>"),
                        "<property name=\"autosaveEnabled\" value=\"true\"/>"
                    )
                    modified = true
                } else {
                    content = content.replace("</settings>", "  <property name=\"autosaveEnabled\" value=\"true\"/>\n</settings>")
                    modified = true
                }

                var overridden = false
                val propertyNames = listOf("autoloadMostRecent", "autoloadLastFile")
                for (prop in propertyNames) {
                    val propRegex = Regex("""<property\b(?=[^>]*\bname\s*=\s*["']$prop["'])(?=[^>]*\bvalue\s*=\s*["']([^"']*)["'])[^>]*/>""")
                    val autoloadMatch = propRegex.find(content)
                    if (autoloadMatch != null) {
                        val currentValue = autoloadMatch.groupValues[1].trim()
                        val isEnabled = currentValue.equals("true", ignoreCase = true) ||
                                currentValue == "1" ||
                                currentValue.equals("yes", ignoreCase = true) ||
                                currentValue.equals("on", ignoreCase = true)
                        if (isEnabled) {
                            content = content.replace(autoloadMatch.value, "<property name=\"$prop\" value=\"false\"/>")
                            modified = true
                            overridden = true
                            Log.i(TAG, "Overrode Xournal++ $prop preference from '$currentValue' to 'false' during ensureXournalppSettings.")
                        }
                    }
                }

                if (!content.contains("autoloadMostRecent") && content.contains("</settings>")) {
                    content = content.replace("</settings>", "  <property name=\"autoloadMostRecent\" value=\"false\"/>\n</settings>")
                    modified = true
                }

                // Sync preferredLocale bidirectionally between Xournal++ and Android preferences
                val xmlMatch = Regex("<property\\s+name=\"preferredLocale\"\\s+value=\"([^\"]*)\"/>").find(content)
                val prefLocaleValue = if (xmlMatch != null) {
                    val xmlLocale = xmlMatch.groupValues[1].trim()
                    val mappedTag = if (xmlLocale.isEmpty() || xmlLocale.equals("default", ignoreCase = true) || xmlLocale.equals("system", ignoreCase = true)) {
                        LinuxLocaleManager.SYSTEM_DEFAULT_TAG
                    } else if (!xmlLocale.endsWith(".UTF-8")) {
                        "${xmlLocale}.UTF-8"
                    } else {
                        xmlLocale
                    }
                    val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
                    val currentPref = prefs.getString(LinuxLocaleManager.PREF_KEY_LINUX_LOCALE, LinuxLocaleManager.SYSTEM_DEFAULT_TAG)
                    if (mappedTag != currentPref) {
                        prefs.edit().putString(LinuxLocaleManager.PREF_KEY_LINUX_LOCALE, mappedTag).apply()
                        Log.i(TAG, "Preserved and synced preferredLocale from Xournal++: $mappedTag")
                    }
                    xmlLocale
                } else {
                    val savedLocale = LinuxLocaleManager.getSavedLocale(context)
                    if (savedLocale == LinuxLocaleManager.SYSTEM_DEFAULT_TAG) "default" else savedLocale.removeSuffix(".UTF-8")
                }

                if (!content.contains("preferredLocale") && content.contains("</settings>")) {
                    content = content.replace("</settings>", "  <property name=\"preferredLocale\" value=\"$prefLocaleValue\"/>\n</settings>")
                    modified = true
                }

                if (overridden) {
                    setPendingAutoloadOverrideNotification(true)
                }

                if (modified) {
                    settingsFile.writeText(content)
                    Log.i(TAG, "Updated existing settings.xml with defaultSaveDir=$defaultNotesPath, autosaveTimeout=1, autoloadMostRecent=false, preferredLocale=$prefLocaleValue")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to configure Xournal++ settings.xml", e)
        }
    }

    fun ensureMenuBarShortcuts() {
        try {
            val menuFile = File(shareDir, "xournalpp/ui/mainmenubar.xml")
            if (menuFile.exists() && menuFile.canWrite()) {
                var content = menuFile.readText()
                if (!content.contains("<attribute name=\"accel\">&lt;Ctrl&gt;comma</attribute>")) {
                    val regex = Regex("<attribute\\s+name=\"label\"\\s+translatable=\"yes\">Preferences</attribute>\\s*<attribute\\s+name=\"action\">app\\.preferences</attribute>")
                    if (regex.containsMatchIn(content)) {
                        content = content.replace(
                            regex,
                            "<attribute name=\"label\" translatable=\"yes\">_Preferences</attribute>\n     <attribute name=\"action\">app.preferences</attribute>\n     <attribute name=\"accel\">&lt;Ctrl&gt;comma</attribute>"
                        )
                        menuFile.writeText(content)
                        Log.i(TAG, "Patched mainmenubar.xml with <Ctrl>comma shortcut and _Preferences mnemonic")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to patch mainmenubar.xml with preferences shortcut", e)
        }
    }

    fun getEnvMap(): Map<String, String> {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        val selectedScale = prefs.getString("pref_ui_scale", "1.0") ?: "1.0"
        val scaleFloat = (selectedScale.toFloatOrNull() ?: 1.0f).coerceIn(0.5f, 4.0f)
        val gdkScaleInt = if (scaleFloat >= 2.0f) scaleFloat.toInt().coerceAtLeast(1) else 1
        val gdkDpiScale = String.format(java.util.Locale.US, "%.2f", scaleFloat / gdkScaleInt)
        val gdkScale = gdkScaleInt.toString()

        val isDark = isGtkDarkMode()
        val gtkThemeEnv = if (isDark) "Adwaita:dark" else "Adwaita"

        val systemLibDir = when {
            android.os.Process.is64Bit() && File("/system/lib64").exists() -> "/system/lib64"
            else -> "/system/lib"
        }

        val shimModule = File(nativeLibDir, "libxopp_shim.so").takeIf { it.exists() } ?: File(libDir, "libxopp-shim.so")
        val imeModule = File(nativeLibDir, "libgtk-android-ime.so").takeIf { it.exists() } ?: File(libDir, "libgtk-android-ime.so")

        val envMap = mutableMapOf(
            "PREFIX" to usrDir.absolutePath,
            "HOME" to homeDir.absolutePath,
            "PATH" to "${nativeLibDir.absolutePath}:${binDir.absolutePath}:/system/bin:/system/xbin",
            "LD_LIBRARY_PATH" to "${nativeLibDir.absolutePath}:${libDir.absolutePath}:$systemLibDir",
            "XOPP_FAKE_EXE" to "${binDir.absolutePath}/xournalpp",
            "XDG_CONFIG_HOME" to configDir.absolutePath,
            "XDG_DATA_DIRS" to "${shareDir.absolutePath}:/usr/share:${homeDir.absolutePath}/share",
            "TEXTDOMAINDIR" to "${shareDir.absolutePath}/locale",
            "GSETTINGS_SCHEMA_DIR" to "${shareDir.absolutePath}/glib-2.0/schemas",
            "GDK_PIXBUF_MODULE_FILE" to "${libDir.absolutePath}/gdk-pixbuf-2.0/2.10.0/loaders.cache",
            "GDK_PIXBUF_MODULEDIR" to "${libDir.absolutePath}/gdk-pixbuf-2.0/2.10.0/loaders",
            "FONTCONFIG_PATH" to "${usrDir.absolutePath}/etc/fonts",
            "FONTCONFIG_FILE" to "${usrDir.absolutePath}/etc/fonts/fonts.conf",
            "TMPDIR" to tmpDir.absolutePath,
            "XDG_RUNTIME_DIR" to tmpDir.absolutePath,
            "MESA_SHADER_CACHE_DIR" to tmpDir.absolutePath,
            "GDK_SCALE" to gdkScale,
            "GDK_DPI_SCALE" to gdkDpiScale,
            "DISPLAY" to ":0",
            "GTK_THEME" to gtkThemeEnv,
            // CRITICAL: Disable desktop portal lookup to prevent D-Bus freeze
            "GTK_USE_PORTAL" to "0",
            "GIO_USE_VFS" to "local",
            "GTK_PATH" to "${nativeLibDir.absolutePath}:${libDir.absolutePath}/gtk-3.0",
            "ANDROID_APP_LIB_DIR" to nativeLibDir.absolutePath
        )

        val (effectiveLang, effectiveLanguage, effectiveLcAll) = LinuxLocaleManager.getEffectiveLocaleEnv(context)
        envMap["LANG"] = effectiveLang
        envMap["LANGUAGE"] = effectiveLanguage
        envMap["LC_ALL"] = effectiveLcAll
        envMap["LC_MESSAGES"] = effectiveLang

        if (shimModule.exists()) {
            envMap["LD_PRELOAD"] = shimModule.absolutePath
        }
        if (imeModule.exists()) {
            envMap["GTK_MODULES"] = imeModule.absolutePath
            envMap["GTK3_MODULES"] = imeModule.absolutePath
        }

        return envMap
    }
}
