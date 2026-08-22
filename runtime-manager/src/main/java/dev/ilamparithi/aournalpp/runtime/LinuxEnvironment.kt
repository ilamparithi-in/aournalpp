package dev.ilamparithi.aournalpp.runtime

import android.content.Context
import java.io.File

class LinuxEnvironment(private val context: Context) {
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

    fun ensureDirectoryTree() {
        val dirs = listOf(
            rootDir, usrDir, binDir, libDir, shareDir, tmpDir, homeDir, configDir, xournalConfigDir, openboxConfigDir
        )
        dirs.forEach {
            if (!it.exists()) {
                it.mkdirs()
            }
        }

        // Provision Openbox kiosk auto-maximization rules (normal windows maximized, dialogs with borders)
        val rcFile = File(openboxConfigDir, "rc.xml")
        rcFile.writeText(
            """<?xml version="1.0" encoding="UTF-8"?>
<openbox_config xmlns="http://openbox.org/3.4/rc">
  <theme>
    <name>Clearlooks</name>
    <titleLayout>NLIMC</titleLayout>
  </theme>
  <applications>
    <!-- Main application windows: maximized & borderless -->
    <application class="*" type="normal">
      <decor>no</decor>
      <maximized>true</maximized>
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
            "LD_LIBRARY_PATH" to libDir.absolutePath,
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
            "DISPLAY" to "127.0.0.1:0",
            "LANG" to "en_US.UTF-8"
        )
    }
}
