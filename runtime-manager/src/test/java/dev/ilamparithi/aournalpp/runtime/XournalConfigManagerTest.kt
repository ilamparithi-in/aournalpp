package dev.ilamparithi.aournalpp.runtime

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class XournalConfigManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test ConfigFileType properties`() {
        val types = ConfigFileType.entries
        assertEquals(5, types.size)

        assertEquals("settings.xml", ConfigFileType.SETTINGS_XML.fileName)
        assertEquals("toolbar.ini", ConfigFileType.TOOLBAR_INI.fileName)
        assertEquals("palette.gpl", ConfigFileType.PALETTE_GPL.fileName)
        assertEquals("colornames.ini", ConfigFileType.COLORNAMES_INI.fileName)
        assertEquals("print-settings.ini", ConfigFileType.PRINT_SETTINGS_INI.fileName)
    }

    @Test
    fun `test detectConfigFileType from path`() {
        val env = mockk<LinuxEnvironment>(relaxed = true)
        val manager = XournalConfigManager(env)

        assertEquals(
            ConfigFileType.TOOLBAR_INI,
            manager.detectConfigFileType("/storage/emulated/0/Download/toolbar.ini", "")
        )
        assertEquals(
            ConfigFileType.PALETTE_GPL,
            manager.detectConfigFileType("/storage/emulated/0/Download/palette.gpl", "")
        )
        assertEquals(
            ConfigFileType.COLORNAMES_INI,
            manager.detectConfigFileType("/storage/emulated/0/Download/colornames.ini", "")
        )
        assertEquals(
            ConfigFileType.PRINT_SETTINGS_INI,
            manager.detectConfigFileType("/storage/emulated/0/Download/print-settings.ini", "")
        )
        assertEquals(
            ConfigFileType.SETTINGS_XML,
            manager.detectConfigFileType("/storage/emulated/0/Download/settings.xml", "")
        )
        assertNull(
            manager.detectConfigFileType("/storage/emulated/0/Download/unknown.txt", "Some random text")
        )
    }

    @Test
    fun `test detectConfigFileType from content headers`() {
        val env = mockk<LinuxEnvironment>(relaxed = true)
        val manager = XournalConfigManager(env)

        assertEquals(
            ConfigFileType.SETTINGS_XML,
            manager.detectConfigFileType(null, "<?xml version=\"1.0\"?><settings></settings>")
        )
        assertEquals(
            ConfigFileType.PALETTE_GPL,
            manager.detectConfigFileType(null, "GIMP Palette\nName: Custom\n")
        )
        assertEquals(
            ConfigFileType.TOOLBAR_INI,
            manager.detectConfigFileType(null, "[Toolbars]\nitem1=pen\n")
        )
        assertEquals(
            ConfigFileType.PRINT_SETTINGS_INI,
            manager.detectConfigFileType(null, "[Print Settings]\npage_setup=A4\n")
        )
    }

    @Test
    fun `test validateIniStructure accepts active settings and rejects empty or comment-only`() {
        val env = mockk<LinuxEnvironment>(relaxed = true)
        val manager = XournalConfigManager(env)

        // Valid INI
        manager.validateIniStructure("[General]\nautosave=true\n")

        // Invalid: comment only
        var threw = false
        try {
            manager.validateIniStructure("# Just a comment\n; Another comment\n")
        } catch (e: IllegalStateException) {
            threw = true
        }
        assertTrue("Expected failure for comment-only INI", threw)
    }

    @Test
    fun `test validateGplStructure accepts GIMP Palette header and rejects others`() {
        val env = mockk<LinuxEnvironment>(relaxed = true)
        val manager = XournalConfigManager(env)

        // Valid GPL
        manager.validateGplStructure("GIMP Palette\n#Columns: 4\n255 0 0 Red\n")

        // Invalid GPL
        var threw = false
        try {
            manager.validateGplStructure("Not A Palette\n255 0 0\n")
        } catch (e: IllegalStateException) {
            threw = true
        }
        assertTrue("Expected failure for missing GIMP Palette header", threw)
    }

    @Test
    fun `test readConfigText produces fallback when file does not exist`() {
        val root = tempFolder.newFolder("config")
        val env = mockk<LinuxEnvironment>()
        every { env.xournalConfigDir } returns root

        val manager = XournalConfigManager(env)

        val result = manager.readConfigText(ConfigFileType.SETTINGS_XML)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.contains("settings.xml has not been generated yet") == true)

        val toolbarResult = manager.readConfigText(ConfigFileType.TOOLBAR_INI)
        assertTrue(toolbarResult.isSuccess)
        assertTrue(toolbarResult.getOrNull()?.contains("toolbar.ini has not been created yet") == true)
    }

    @Test
    fun `test readConfigText reads actual content when file exists`() {
        val root = tempFolder.newFolder("config")
        val env = mockk<LinuxEnvironment>()
        every { env.xournalConfigDir } returns root

        val settingsFile = File(root, "settings.xml")
        settingsFile.writeText("<settings><property name=\"test\" value=\"1\"/></settings>")

        val manager = XournalConfigManager(env)
        val result = manager.readConfigText(ConfigFileType.SETTINGS_XML)

        assertTrue(result.isSuccess)
        assertEquals("<settings><property name=\"test\" value=\"1\"/></settings>", result.getOrNull())
    }
}
