package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.runtime.ConfigFileType
import dev.ilamparithi.aournalpp.ui.settings.SettingsCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemMaintenanceSettingsTest {

    @Test
    fun testSettingsCategorySystemMaintenance() {
        val category = SettingsCategory.SYSTEM_MAINTENANCE
        assertEquals("System & Maintenance", category.title)
        assertTrue(category.description.contains("Language"))
        assertTrue(category.description.contains("config backups"))
        assertTrue(category.description.contains("diagnostics"))
        assertNotNull(category.icon)
    }

    @Test
    fun testConfigFileTypesCompleteness() {
        val types = ConfigFileType.entries
        assertEquals(5, types.size)

        val settingsXml = types.firstOrNull { it == ConfigFileType.SETTINGS_XML }
        assertNotNull(settingsXml)
        assertEquals("settings.xml", settingsXml!!.fileName)
        assertEquals("text/xml", settingsXml.mimeType)

        val toolbarIni = types.firstOrNull { it == ConfigFileType.TOOLBAR_INI }
        assertNotNull(toolbarIni)
        assertEquals("toolbar.ini", toolbarIni!!.fileName)

        val paletteGpl = types.firstOrNull { it == ConfigFileType.PALETTE_GPL }
        assertNotNull(paletteGpl)
        assertEquals("palette.gpl", paletteGpl!!.fileName)

        val colorNamesIni = types.firstOrNull { it == ConfigFileType.COLORNAMES_INI }
        assertNotNull(colorNamesIni)
        assertEquals("colornames.ini", colorNamesIni!!.fileName)

        val printSettingsIni = types.firstOrNull { it == ConfigFileType.PRINT_SETTINGS_INI }
        assertNotNull(printSettingsIni)
        assertEquals("print-settings.ini", printSettingsIni!!.fileName)
    }
}
