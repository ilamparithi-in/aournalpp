package dev.ilamparithi.aournalpp.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DesktopConfigGeneratorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test generateOpenboxConfig without snap layout contains kiosk fullscreen rules`() {
        val config = DesktopConfigGenerator.generateOpenboxConfig(snapLayoutActive = false)

        assertTrue(config.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(config.contains("<openbox_config"))
        assertTrue(config.contains("</openbox_config>"))
        assertTrue(config.contains("<application class=\"*\" title=\"*Choose*Image*\">"))
        assertTrue(config.contains("<application class=\"*\" title=\"*Export*\">"))
        assertTrue(config.contains("<application class=\"*\" title=\"*Save*\">"))
        assertTrue(config.contains("<decor>no</decor>"))
        assertTrue(config.contains("<maximized>true</maximized>"))
    }

    @Test
    fun `test generateOpenboxConfig with snap layout omits single-window fullscreen rules`() {
        val config = DesktopConfigGenerator.generateOpenboxConfig(snapLayoutActive = true)

        assertTrue(config.contains("<openbox_config"))
        assertTrue(config.contains("</openbox_config>"))
        assertFalse(config.contains("Single window mode (borderless fullscreen)"))
        assertFalse(config.contains("<application class=\"*\" title=\"*Choose*Image*\">"))
    }

    @Test
    fun `test generateGtkSettings for light and dark modes`() {
        val darkSettings = DesktopConfigGenerator.generateGtkSettings(isDark = true)
        assertTrue(darkSettings.contains("gtk-application-prefer-dark-theme = 1"))
        assertTrue(darkSettings.contains("gtk-theme-name = Adwaita"))

        val lightSettings = DesktopConfigGenerator.generateGtkSettings(isDark = false)
        assertTrue(lightSettings.contains("gtk-application-prefer-dark-theme = 0"))
    }

    @Test
    fun `test writeGtkSettings creates valid settings ini file`() {
        val configDir = tempFolder.newFolder("config")
        DesktopConfigGenerator.writeGtkSettings(configDir, isDark = true)

        val settingsFile = File(configDir, "gtk-3.0/settings.ini")
        assertTrue(settingsFile.exists())
        val text = settingsFile.readText()
        assertTrue(text.contains("[Settings]"))
        assertTrue(text.contains("gtk-application-prefer-dark-theme = 1"))
    }

    @Test
    fun `test writeGtkBookmarks creates valid bookmarks file`() {
        val configDir = tempFolder.newFolder("config")
        val notesDir = tempFolder.newFolder("Notes")
        val downloadsDir = tempFolder.newFolder("Download")

        DesktopConfigGenerator.writeGtkBookmarks(configDir, notesDir, downloadsDir)

        val bookmarksFile = File(configDir, "gtk-3.0/bookmarks")
        assertTrue(bookmarksFile.exists())
        val text = bookmarksFile.readText()
        assertTrue(text.contains("file://${notesDir.absolutePath} Notes"))
        assertTrue(text.contains("file://${downloadsDir.absolutePath} Downloads"))
    }
}
