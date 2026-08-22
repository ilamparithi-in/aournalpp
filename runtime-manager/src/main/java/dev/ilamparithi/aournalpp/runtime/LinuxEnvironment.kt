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

    fun setNotesDirectory(newPath: String) {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY_NOTES_DIR, newPath).apply()
        ensureDirectoryTree()
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

        // Self-heal: Compile GLib GSettings schemas if gschemas.compiled is missing
        val schemasDir = File(shareDir, "glib-2.0/schemas")
        val compileSchemasBin = File(binDir, "glib-compile-schemas")
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

        // Self-heal: Generate GDK Pixbuf loaders.cache if missing
        val gdkDir = File(libDir, "gdk-pixbuf-2.0/2.10.0")
        val loadersCache = File(gdkDir, "loaders.cache")
        val loadersDir = File(gdkDir, "loaders")
        val queryLoadersBin = File(binDir, "gdk-pixbuf-query-loaders")
        if (!loadersCache.exists() && queryLoadersBin.exists() && loadersDir.exists()) {
            try {
                val loaderFiles = loadersDir.listFiles { _, name -> name.endsWith(".so") }
                if (!loaderFiles.isNullOrEmpty()) {
                    val cmd = mutableListOf(queryLoadersBin.absolutePath)
                    loaderFiles.forEach { cmd.add(it.absolutePath) }
                    val pb = ProcessBuilder(cmd)
                        .redirectOutput(loadersCache)
                        .redirectErrorStream(true)
                    pb.environment().putAll(getEnvMap())
                    val p = pb.start()
                    p.waitFor()
                    Log.i(TAG, "Self-healed & generated gdk-pixbuf loaders.cache")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to generate loaders.cache during ensureDirectoryTree", e)
            }
        }

        // Provision xopp-title-watcher binary from assets if available
        val titleWatcherBin = File(binDir, "xopp-title-watcher")
        if (!titleWatcherBin.exists() || !titleWatcherBin.canExecute()) {
            try {
                context.assets.open("bin/xopp-title-watcher").use { input ->
                    titleWatcherBin.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                titleWatcherBin.setExecutable(true, false)
                Log.i(TAG, "Provisioned xopp-title-watcher binary")
            } catch (e: Exception) {
                // If not in assets, it may already be in bootstrap
            }
        }

        setupStorageSymlinks()
        writeGtkBookmarks()
        ensureXournalppSettings()
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
                      <property name="autoloadLastFile" value="false"/>
                    </settings>
                """.trimIndent()
                settingsFile.writeText(initialXml)
                Log.i(TAG, "Provisioned initial settings.xml with defaultSaveDir=$defaultNotesPath")
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

                if (modified) {
                    settingsFile.writeText(content)
                    Log.i(TAG, "Updated existing settings.xml with defaultSaveDir=$defaultNotesPath")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to configure Xournal++ settings.xml", e)
        }
    }

    fun getEnvMap(): Map<String, String> {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        val selectedScale = prefs.getString("pref_ui_scale", "1.0") ?: "1.0"
        val (gdkScale, gdkDpiScale) = when (selectedScale) {
            "1.25" -> Pair("1", "1.25")
            "1.5" -> Pair("1", "1.5")
            "1.75" -> Pair("1", "1.75")
            "2.0" -> Pair("2", "1.0")
            "2.5" -> Pair("2", "1.25")
            else -> Pair("1", "1.0")
        }

        return mapOf(
            "PREFIX" to usrDir.absolutePath,
            "HOME" to homeDir.absolutePath,
            "PATH" to "${binDir.absolutePath}:/system/bin:/system/xbin",
            "LD_LIBRARY_PATH" to "${libDir.absolutePath}:/system/lib64",
            "XDG_CONFIG_HOME" to configDir.absolutePath,
            "XDG_DATA_DIRS" to "${shareDir.absolutePath}:/usr/share",
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
            "LANG" to "en_US.UTF-8",
            // CRITICAL: Disable desktop portal lookup to prevent D-Bus freeze
            "GTK_USE_PORTAL" to "0",
            "GIO_USE_VFS" to "local"
        )
    }
}
