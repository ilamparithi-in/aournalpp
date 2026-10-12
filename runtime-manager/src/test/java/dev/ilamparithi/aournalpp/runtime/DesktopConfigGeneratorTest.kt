package dev.ilamparithi.aournalpp.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DesktopConfigGeneratorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createSampleMoBytes(entries: Map<String, String>): ByteArray {
        val sortedKeys = entries.keys.sorted()
        val count = sortedKeys.size

        val idBytesList = sortedKeys.map { it.toByteArray(Charsets.UTF_8) }
        val strBytesList = sortedKeys.map { (entries[it] ?: "").toByteArray(Charsets.UTF_8) }

        val idOffsets = mutableListOf<Pair<Int, Int>>()
        val strOffsets = mutableListOf<Pair<Int, Int>>()

        val idsBlock = ByteArrayOutputStream()
        val strsBlock = ByteArrayOutputStream()

        val headerSize = 28
        val tableSize = count * 8 * 2
        val stringsStart = headerSize + tableSize

        var currentOffset = stringsStart
        for (b in idBytesList) {
            idOffsets.add(b.size to currentOffset)
            idsBlock.write(b)
            idsBlock.write(0)
            currentOffset += b.size + 1
        }

        for (b in strBytesList) {
            strOffsets.add(b.size to currentOffset)
            strsBlock.write(b)
            strsBlock.write(0)
            currentOffset += b.size + 1
        }

        val transTabOffset = headerSize + count * 8

        val out = ByteArrayOutputStream()
        val buf = ByteBuffer.allocate(stringsStart).order(ByteOrder.LITTLE_ENDIAN)

        buf.putInt(0x950412de.toInt())
        buf.putInt(0)
        buf.putInt(count)
        buf.putInt(headerSize)
        buf.putInt(transTabOffset)
        buf.putInt(0)
        buf.putInt(0)

        for ((len, off) in idOffsets) {
            buf.putInt(len)
            buf.putInt(off)
        }

        for ((len, off) in strOffsets) {
            buf.putInt(len)
            buf.putInt(off)
        }

        out.write(buf.array())
        out.write(idsBlock.toByteArray())
        out.write(strsBlock.toByteArray())

        return out.toByteArray()
    }

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
    fun `test generateOpenboxConfig without toolbar enables titlebar for normal windows in fullscreen`() {
        val config = DesktopConfigGenerator.generateOpenboxConfig(snapLayoutActive = false, toolbarEnabled = false)

        assertTrue(config.contains("<openbox_config"))
        assertTrue(config.contains("</openbox_config>"))
        assertTrue(config.contains("<application class=\"*\" type=\"normal\">"))
        assertTrue(config.contains("<decor>yes</decor>"))
        assertTrue(config.contains("<maximized>true</maximized>"))
    }

    @Test
    fun `test customize toolbar is placed at end of applications and always windowed across locales`() {
        val config = DesktopConfigGenerator.generateOpenboxConfig(snapLayoutActive = false)

        // General type="dialog" must appear BEFORE the windowed customize toolbar rules
        val dialogRuleIndex = config.indexOf("<application class=\"*\" type=\"dialog\">")
        val customizeToolbarIndex = config.indexOf("<application class=\"*\" title=\"*Customize Toolbar*\">")

        assertTrue("dialog catch-all must exist", dialogRuleIndex != -1)
        assertTrue("customize toolbar must exist", customizeToolbarIndex != -1)
        assertTrue(
            "Customize toolbar rule must be placed AFTER generic dialog rule to prevent override regression",
            customizeToolbarIndex > dialogRuleIndex
        )

        // Must include fallback localized titles such as German, French, Spanish, Russian, Chinese
        assertTrue(config.contains("*Werkzeugleisten anpassen*"))
        assertTrue(config.contains("*Personnaliser les barres d'outils*"))
        assertTrue(config.contains("*Personalizar barras de herramientas*"))
        assertTrue(config.contains("*Настройка панелей инструментов*"))
        assertTrue(config.contains("*自定义工具栏*"))
    }

    @Test
    fun `test preferences opens in windowed mode when viewport cannot fit it in fullscreen`() {
        // Portrait viewport: 800w x 1280h with 1.0x UI scale (required 1024x740)
        val config = DesktopConfigGenerator.generateOpenboxConfig(
            snapLayoutActive = false,
            viewportWidth = 800,
            viewportHeight = 1280,
            uiScale = 1.0f
        )

        val dialogCatchAllIndex = config.indexOf("<application class=\"*\" type=\"dialog\">")
        val windowedPrefIndex = config.lastIndexOf("<application class=\"*\" title=\"*Preferences*\">")

        assertTrue(dialogCatchAllIndex != -1)
        assertTrue(windowedPrefIndex != -1)
        assertTrue(
            "Windowed preferences override must appear AFTER generic dialog catch-all",
            windowedPrefIndex > dialogCatchAllIndex
        )

        // Find the block around windowedPrefIndex and ensure decor is yes and maximized is no
        val prefBlock = config.substring(windowedPrefIndex, windowedPrefIndex + 250)
        assertTrue(prefBlock.contains("<decor>yes</decor>"))
        assertTrue(prefBlock.contains("<maximized>no</maximized>"))
    }

    @Test
    fun `test preferences opens in fullscreen mode when viewport is sufficient for fullscreen`() {
        // Landscape viewport: 1920w x 1200h with 1.0x UI scale (required 1024x740)
        val config = DesktopConfigGenerator.generateOpenboxConfig(
            snapLayoutActive = false,
            viewportWidth = 1920,
            viewportHeight = 1200,
            uiScale = 1.0f
        )

        // Should NOT contain windowed preferences section at the bottom
        assertFalse(config.contains("Preferences: Windowed mode when viewport is insufficient for fullscreen"))

        // Should contain fullscreen preferences rule in fullscreen rules
        val fullscreenPrefIndex = config.indexOf("<application class=\"*\" title=\"*Preferences*\">")
        assertTrue(fullscreenPrefIndex != -1)
        val prefBlock = config.substring(fullscreenPrefIndex, fullscreenPrefIndex + 250)
        assertTrue(prefBlock.contains("<decor>no</decor>"))
        assertTrue(prefBlock.contains("<maximized>true</maximized>"))
    }

    @Test
    fun `test dynamic MO locale translations are included in openbox rules`() {
        val localeDir = tempFolder.newFolder("locale")
        val customDir = File(localeDir, "xx/LC_MESSAGES").apply { mkdirs() }
        val customMo = File(customDir, "xournalpp.mo")
        customMo.writeBytes(
            createSampleMoBytes(
                mapOf(
                    "Customize Toolbars" to "Custom Toolbar Localized Name",
                    "Xournal++ Preferences" to "Custom Preferences Localized Name"
                )
            )
        )

        val config = DesktopConfigGenerator.generateOpenboxConfig(
            snapLayoutActive = false,
            localeDir = localeDir,
            activeLocale = "xx"
        )

        assertTrue(config.contains("*Custom Toolbar Localized Name*"))
        assertTrue(config.contains("*Custom Preferences Localized Name*"))
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
    fun `test generateOpenboxConfig does not contain Client or Frame mouse binds to protect direct touch gestures`() {
        val singleConfig = DesktopConfigGenerator.generateOpenboxConfig(snapLayoutActive = false)
        assertFalse(singleConfig.contains("<context name=\"Client\">"))
        assertFalse(singleConfig.contains("<context name=\"Frame\">"))
        assertTrue(singleConfig.contains("<context name=\"Titlebar\">"))

        val snapConfig = DesktopConfigGenerator.generateOpenboxConfig(snapLayoutActive = true)
        assertFalse(snapConfig.contains("<context name=\"Client\">"))
        assertFalse(snapConfig.contains("<context name=\"Frame\">"))
        assertTrue(snapConfig.contains("<context name=\"Titlebar\">"))
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
