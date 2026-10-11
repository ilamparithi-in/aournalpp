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

    private val FALLBACK_CUSTOMIZE_TOOLBAR_TITLES = setOf(
        "Customize Toolbars", "Customize Toolbar",
        "Werkzeugleisten anpassen", "Personnaliser les barres d'outils",
        "Personalizar barras de herramientas", "Personalizar Barras de Ferramentas",
        "Personalizar as barras de ferramentas", "Personalizza barra degli strumenti",
        "Настройка панелей инструментов", "Налаштування панелі інструментів",
        "Dostosuj Paski Narzędzi", "Upravit panely nástrojů",
        "Werkbalken aanpassen", "Anpassa verktygsfält",
        "Tilpas Værktøjslinjer", "Eszköztárak testreszabása",
        "Araç Çubuklarını Özelleştir", "自定义工具栏", "自訂工具列",
        "ツールバーをカスタマイズ", "툴바 사용자 정의"
    )

    internal val FALLBACK_PREFERENCES_TITLES = setOf(
        "Preferences", "Xournal++ Preferences", "Settings",
        "Einstellungen", "Xournal++ Einstellungen",
        "Préférences", "Préférences de Xournal++",
        "Preferencias", "Preferencias de Xournal++",
        "Preferências", "Preferências Xournal++",
        "Preferenze", "Preferenze Xournal++",
        "Настройки", "Настройки Xournal++",
        "Налаштування", "Налаштування Xournal++",
        "Ustawienia Xournal++", "Preferencje",
        "Nastavení Xournal++", "Voorkeuren", "Xournal++-voorkeuren",
        "Inställningar", "Inställningar för Xournal++",
        "Beállítások", "Xournal++ beállításai",
        "Tercihler", "Xournal++ Tercihleri",
        "首选项", "Xournal++ 首选项", "偏好設定", "Xournal++偏好設定",
        "Xournal++の設定", "설정"
    )

    /**
     * Resolves all known and dynamically discovered title strings for the Toolbar Customization dialog.
     */
    fun resolveCustomizeToolbarTitles(localeDir: File? = null, activeLocale: String? = null): Set<String> {
        val titles = mutableSetOf<String>()
        titles.addAll(FALLBACK_CUSTOMIZE_TOOLBAR_TITLES)

        if (localeDir != null && localeDir.exists() && localeDir.isDirectory) {
            val dynamic = MoReader.findTranslations(
                localeDir = localeDir,
                keys = listOf("Customize Toolbars", "Customize Toolbar"),
                activeLocale = activeLocale
            )
            dynamic.values.forEach { titles.addAll(it) }
        }

        return titles
    }

    /**
     * Resolves all known and dynamically discovered title strings for the Preferences dialog.
     */
    fun resolvePreferencesTitles(localeDir: File? = null, activeLocale: String? = null): Set<String> {
        val titles = mutableSetOf<String>()
        titles.addAll(FALLBACK_PREFERENCES_TITLES)

        if (localeDir != null && localeDir.exists() && localeDir.isDirectory) {
            val dynamic = MoReader.findTranslations(
                localeDir = localeDir,
                keys = listOf("Xournal++ Preferences", "Preferences"),
                activeLocale = activeLocale
            )
            dynamic.values.forEach { titles.addAll(it) }
        }

        return titles
    }

    fun generateOpenboxConfig(
        snapLayoutActive: Boolean = false,
        toolbarEnabled: Boolean = true,
        viewportWidth: Int = 0,
        viewportHeight: Int = 0,
        uiScale: Float = 1.0f,
        localeDir: File? = null,
        activeLocale: String? = null
    ): String {
        val customizeToolbarTitles = resolveCustomizeToolbarTitles(localeDir, activeLocale)
        val preferencesTitles = resolvePreferencesTitles(localeDir, activeLocale)

        // Evaluate whether Preferences can fit in fullscreen under current viewport and UI scale
        val canFitPreferencesFullscreen = if (viewportWidth > 0 && viewportHeight > 0) {
            WindowOrientationAdvisor.canFitPreferencesFullscreen(viewportWidth, viewportHeight, uiScale)
        } else {
            true // Default to fullscreen if viewport is not yet measured
        }

        val fullscreenPreferencesRules = if (canFitPreferencesFullscreen) {
            buildString {
                appendLine("    <application class=\"*\" title=\"*Preferences*\">")
                appendLine("      <decor>no</decor>")
                appendLine("      <maximized>true</maximized>")
                appendLine("      <focus>yes</focus>")
                appendLine("    </application>")
                appendLine("    <application class=\"*\" title=\"*preferences*\">")
                appendLine("      <decor>no</decor>")
                appendLine("      <maximized>true</maximized>")
                appendLine("      <focus>yes</focus>")
                appendLine("    </application>")
                for (t in preferencesTitles) {
                    appendLine("    <application class=\"*\" title=\"*$t*\">")
                    appendLine("      <decor>no</decor>")
                    appendLine("      <maximized>true</maximized>")
                    appendLine("      <focus>yes</focus>")
                    appendLine("    </application>")
                }
            }.trimEnd()
        } else {
            ""
        }

        val fullscreenRules = if (!snapLayoutActive) {
            val normalDecor = if (toolbarEnabled) "no" else "yes"
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
$fullscreenPreferencesRules
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
      <decor>$normalDecor</decor>
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

        // Windowed override rules: Must be placed at the VERY END of <applications> so
        // Openbox applies them with final precedence over generic dialog/normal catch-alls!
        val windowedOverrideRules = buildString {
            appendLine("    <!-- Always center audio player -->")
            appendLine("    <application class=\"*\" title=\"*Audio Player*\">")
            appendLine("      <decor>yes</decor>")
            appendLine("      <maximized>no</maximized>")
            appendLine("      <focus>yes</focus>")
            appendLine("      <position force=\"no\">")
            appendLine("        <x>center</x>")
            appendLine("        <y>center</y>")
            appendLine("      </position>")
            appendLine("    </application>")

            appendLine("    <!-- Customize Toolbar: ALWAYS windowed with titlebar and centered across all locales -->")
            appendLine("    <application class=\"*\" title=\"*Customize Toolbar*\">")
            appendLine("      <decor>yes</decor>")
            appendLine("      <maximized>no</maximized>")
            appendLine("      <focus>yes</focus>")
            appendLine("      <position force=\"no\">")
            appendLine("        <x>center</x>")
            appendLine("        <y>center</y>")
            appendLine("      </position>")
            appendLine("    </application>")
            appendLine("    <application class=\"*\" title=\"*Customize Toolbars*\">")
            appendLine("      <decor>yes</decor>")
            appendLine("      <maximized>no</maximized>")
            appendLine("      <focus>yes</focus>")
            appendLine("      <position force=\"no\">")
            appendLine("        <x>center</x>")
            appendLine("        <y>center</y>")
            appendLine("      </position>")
            appendLine("    </application>")
            for (t in customizeToolbarTitles) {
                appendLine("    <application class=\"*\" title=\"*$t*\">")
                appendLine("      <decor>yes</decor>")
                appendLine("      <maximized>no</maximized>")
                appendLine("      <focus>yes</focus>")
                appendLine("      <position force=\"no\">")
                appendLine("        <x>center</x>")
                appendLine("        <y>center</y>")
                appendLine("      </position>")
                appendLine("    </application>")
            }

            // In single window mode, if viewport cannot fit Preferences in fullscreen,
            // force Preferences into windowed mode with titlebar so user can drag and reach action buttons!
            if (!snapLayoutActive && !canFitPreferencesFullscreen) {
                appendLine("    <!-- Preferences: Windowed mode when viewport is insufficient for fullscreen -->")
                appendLine("    <application class=\"*\" title=\"*Preferences*\">")
                appendLine("      <decor>yes</decor>")
                appendLine("      <maximized>no</maximized>")
                appendLine("      <focus>yes</focus>")
                appendLine("      <position force=\"no\">")
                appendLine("        <x>center</x>")
                appendLine("        <y>center</y>")
                appendLine("      </position>")
                appendLine("    </application>")
                appendLine("    <application class=\"*\" title=\"*preferences*\">")
                appendLine("      <decor>yes</decor>")
                appendLine("      <maximized>no</maximized>")
                appendLine("      <focus>yes</focus>")
                appendLine("      <position force=\"no\">")
                appendLine("        <x>center</x>")
                appendLine("        <y>center</y>")
                appendLine("      </position>")
                appendLine("    </application>")
                appendLine("    <application class=\"*\" title=\"*Settings*\">")
                appendLine("      <decor>yes</decor>")
                appendLine("      <maximized>no</maximized>")
                appendLine("      <focus>yes</focus>")
                appendLine("      <position force=\"no\">")
                appendLine("        <x>center</x>")
                appendLine("        <y>center</y>")
                appendLine("      </position>")
                appendLine("    </application>")
                for (t in preferencesTitles) {
                    appendLine("    <application class=\"*\" title=\"*$t*\">")
                    appendLine("      <decor>yes</decor>")
                    appendLine("      <maximized>no</maximized>")
                    appendLine("      <focus>yes</focus>")
                    appendLine("      <position force=\"no\">")
                    appendLine("        <x>center</x>")
                    appendLine("        <y>center</y>")
                    appendLine("      </position>")
                    appendLine("    </application>")
                }
            }
        }.trimEnd()

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
    $fullscreenRules
    $windowedOverrideRules
  </applications>
</openbox_config>
""".trimIndent()
    }

    fun updateOpenboxSnapMode(
        openboxConfigDir: File,
        snapLayoutActive: Boolean,
        toolbarEnabled: Boolean = true,
        viewportWidth: Int = 0,
        viewportHeight: Int = 0,
        uiScale: Float = 1.0f,
        localeDir: File? = null,
        activeLocale: String? = null
    ) {
        if (!openboxConfigDir.exists()) {
            openboxConfigDir.mkdirs()
        }
        val rcFile = File(openboxConfigDir, "rc.xml")
        rcFile.writeText(
            generateOpenboxConfig(
                snapLayoutActive = snapLayoutActive,
                toolbarEnabled = toolbarEnabled,
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight,
                uiScale = uiScale,
                localeDir = localeDir,
                activeLocale = activeLocale
            )
        )
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
