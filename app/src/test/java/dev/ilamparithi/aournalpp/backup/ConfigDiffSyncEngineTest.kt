package dev.ilamparithi.aournalpp.backup

import dev.ilamparithi.aournalpp.backup.engine.ConfigDiffSyncEngine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ConfigDiffSyncEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testIsConfigFile() {
        assertTrue(ConfigDiffSyncEngine.isConfigFile("x11_prefs.json"))
        assertTrue(ConfigDiffSyncEngine.isConfigFile("app_settings.json"))
        assertTrue(ConfigDiffSyncEngine.isConfigFile("settings.xml"))
        assertTrue(ConfigDiffSyncEngine.isConfigFile(".config/xournalpp/settings.ini"))
        assertFalse(ConfigDiffSyncEngine.isConfigFile("my_notes.xopp"))
        assertFalse(ConfigDiffSyncEngine.isConfigFile("drawing.pdf"))
    }

    @Test
    fun testJsonStructuralEquality() {
        val f1 = tempFolder.newFile("test1.json").apply {
            writeText("""{"name": "test", "active": true, "count": 42}""")
        }
        val f2 = tempFolder.newFile("test2.json").apply {
            // Reordered keys with extra whitespace
            writeText("""
                {
                   "count": 42,
                   "active": true,
                   "name": "test"
                }
            """.trimIndent())
        }
        val f3 = tempFolder.newFile("test3.json").apply {
            writeText("""{"name": "test", "active": false, "count": 42}""")
        }

        assertTrue(ConfigDiffSyncEngine.areJsonFilesEqual(f1, f2))
        assertFalse(ConfigDiffSyncEngine.hasContentChanges(f1, f2))
        assertTrue(ConfigDiffSyncEngine.hasContentChanges(f1, f3))
    }
}
