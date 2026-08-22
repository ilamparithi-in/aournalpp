package dev.ilamparithi.aournalpp.runtime

import android.content.Context
import java.io.File

class LinuxEnvironment(context: Context) {
    val rootDir: File = context.filesDir
    val usrDir: File = File(rootDir, "usr")
    val binDir: File = File(usrDir, "bin")
    val libDir: File = File(usrDir, "lib")
    val shareDir: File = File(usrDir, "share")
    val tmpDir: File = File(usrDir, "tmp")
    val homeDir: File = File(rootDir, "home")
    val configDir: File = File(homeDir, ".config")
    val xournalConfigDir: File = File(configDir, "xournalpp")

    fun ensureDirectoryTree() {
        val dirs = listOf(
            rootDir, usrDir, binDir, libDir, shareDir, tmpDir, homeDir, configDir, xournalConfigDir
        )
        dirs.forEach {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    }

    fun getEnvMap(): Map<String, String> {
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
            "DISPLAY" to "127.0.0.1:0",
            "LANG" to "en_US.UTF-8"
        )
    }
}
