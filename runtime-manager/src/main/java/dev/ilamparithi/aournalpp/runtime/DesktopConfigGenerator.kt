package dev.ilamparithi.aournalpp.runtime

import android.os.Environment
import android.util.Log
import java.io.File

/**
 * Dedicated generator for Linux desktop environment configuration files, including
 * Openbox window manager XML rules (kiosk mode, auto-maximization, window snapping)
 * and GTK 3 settings / bookmarks.
 */
object DesktopConfigGenerator {

    private const val TAG = "DesktopConfigGenerator"

    fun generateOpenboxConfig(snapLayoutActive: Boolean = false): String {
        val fullscreenRules = if (!snapLayoutActive) {
            """
    <!-- Fullscreen-marked windows in single window mode (borderless fullscreen) -->
    <application class="*" title="*Choose*Image*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*Choose*image*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*choose*image*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*Export*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*export*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*Annotate*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*annotate*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*Save*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*save*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*Open*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*open*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*Print*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*print*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*Page*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*page*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*Settings*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*settings*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*Preferences*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*preferences*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*Find*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*find*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*Replace*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*replace*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*Color*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*color*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*Font*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*font*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*About*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" title="*about*">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" type="dialog">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
    <application class="*" type="normal">
      <decor>no</decor>
      <maximized>true</maximized>
      <focus>yes</focus>
    </application>
            """.trimIndent()
        } else {
            """
    <!-- In split/multitask mode, allow normal window borders and placement -->
    <application class="*" type="dialog">
      <decor>yes</decor>
      <maximized>no</maximized>
      <focus>yes</focus>
      <position force="no">
        <x>center</x>
        <y>center</y>
      </position>
    </application>
    <application class="*" type="normal">
      <decor>yes</decor>
      <maximized>no</maximized>
      <focus>yes</focus>
    </application>
            """.trimIndent()
        }

        return """
<?xml version="1.0" encoding="UTF-8"?>
<openbox_config xmlns="http://openbox.org/3.4/rc">
  <theme>
    <name>Clearlooks</name>
    <titleLayout>NLIMC</titleLayout>
  </theme>
  <mouse>
    <dragThreshold>1</dragThreshold>
    <doubleClickTime>500</doubleClickTime>
    <screenEdgeWarpTime>0</screenEdgeWarpTime>
    <context name="Titlebar">
      <mousebind button="Left" action="Press">
        <action name="Focus"/>
        <action name="Raise"/>
      </mousebind>
      <mousebind button="Left" action="Drag">
        <action name="Move"/>
      </mousebind>
    </context>
  </mouse>
  <applications>
    <!-- Always center small popup dialogs and customize toolbar window -->
    <application class="*" title="*Audio Player*">
      <decor>yes</decor>
      <maximized>no</maximized>
      <focus>yes</focus>
      <position force="no">
        <x>center</x>
        <y>center</y>
      </position>
    </application>
    <application class="*" title="*Customize Toolbar*">
      <decor>yes</decor>
      <maximized>no</maximized>
      <focus>yes</focus>
      <position force="no">
        <x>center</x>
        <y>center</y>
      </position>
    </application>
    $fullscreenRules
  </applications>
</openbox_config>
""".trimIndent()
    }

    fun updateOpenboxSnapMode(openboxConfigDir: File, snapLayoutActive: Boolean) {
        if (!openboxConfigDir.exists()) {
            openboxConfigDir.mkdirs()
        }
        val rcFile = File(openboxConfigDir, "rc.xml")
        rcFile.writeText(generateOpenboxConfig(snapLayoutActive))
    }

    fun generateGtkSettings(isDark: Boolean): String = buildString {
        appendLine("[Settings]")
        appendLine("gtk-theme-name = Adwaita")
        appendLine("gtk-application-prefer-dark-theme = ${if (isDark) "1" else "0"}")
        appendLine("gtk-icon-theme-name = Adwaita")
    }

    fun writeGtkSettings(configDir: File, isDark: Boolean, onEnsureBookmarks: () -> Unit = {}) {
        try {
            val gtk3ConfigDir = File(configDir, "gtk-3.0")
            if (!gtk3ConfigDir.exists()) {
                gtk3ConfigDir.mkdirs()
            }
            val settingsFile = File(gtk3ConfigDir, "settings.ini")
            settingsFile.writeText(generateGtkSettings(isDark))
            Log.i(TAG, "Provisioned GTK settings.ini with isDark=$isDark at ${settingsFile.absolutePath}")
            onEnsureBookmarks()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write GTK settings.ini", e)
        }
    }

    fun writeGtkBookmarks(configDir: File, sharedDocumentsNotesDir: File, sharedDownloadsDir: File) {
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

    fun ensureGtkBookmarks(
        configDir: File,
        homeDir: File,
        notesDir: File,
        sharedDownloadsDir: File
    ) {
        try {
            val gtk3ConfigDir = File(configDir, "gtk-3.0")
            if (!gtk3ConfigDir.exists()) {
                gtk3ConfigDir.mkdirs()
            }
            val bookmarksFile = File(gtk3ConfigDir, "bookmarks")
            val docsDir = File(Environment.getExternalStorageDirectory(), "Documents")

            val lines = mutableListOf<String>()
            lines.add("file://${notesDir.absolutePath} Notes Home")
            if (docsDir.exists() && docsDir.absolutePath != notesDir.absolutePath) {
                lines.add("file://${docsDir.absolutePath} Documents")
            }
            if (sharedDownloadsDir.exists() && sharedDownloadsDir.absolutePath != notesDir.absolutePath) {
                lines.add("file://${sharedDownloadsDir.absolutePath} Downloads")
            }

            val content = lines.joinToString("\n") + "\n"
            bookmarksFile.writeText(content)

            val legacyBookmarks = File(homeDir, ".gtk-bookmarks")
            legacyBookmarks.writeText(content)
            Log.i(TAG, "Provisioned dynamic GTK bookmarks pointing to Notes Home: ${notesDir.absolutePath}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write GTK bookmarks", e)
        }
    }
}
