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

    @Test
    fun testIsTextBasedFile() {
        assertTrue(ConfigDiffSyncEngine.isTextBasedFile("readme.md"))
        assertTrue(ConfigDiffSyncEngine.isTextBasedFile("Notes/Chapter1.MD"))
        assertTrue(ConfigDiffSyncEngine.isTextBasedFile("document.markdown"))
        assertTrue(ConfigDiffSyncEngine.isTextBasedFile("Notes/todo.txt"))
        assertTrue(ConfigDiffSyncEngine.isTextBasedFile("config.json"))
        assertTrue(ConfigDiffSyncEngine.isTextBasedFile(".config/xournalpp/settings.xml"))
        assertTrue(ConfigDiffSyncEngine.isTextBasedFile("data.csv"))
        assertTrue(ConfigDiffSyncEngine.isTextBasedFile("script.sh"))
        assertFalse(ConfigDiffSyncEngine.isTextBasedFile("drawing.pdf"))
        assertFalse(ConfigDiffSyncEngine.isTextBasedFile("notes.xopp"))
        assertFalse(ConfigDiffSyncEngine.isTextBasedFile("audio.m4a"))
        assertFalse(ConfigDiffSyncEngine.isTextBasedFile("photo.png"))
    }

    @Test
    fun testMarkdownDiffEquality() {
        val f1 = tempFolder.newFile("doc1.md").apply {
            writeText(
                """
                # Title
                
                This is a paragraph.
                
                - Item 1
                - Item 2
                """.trimIndent()
            )
        }
        val f2 = tempFolder.newFile("doc2.md").apply {
            // Same content with CRLF line endings and trailing space differences
            writeText("# Title\r\n\r\nThis is a paragraph.  \r\n\r\n- Item 1\r\n- Item 2\r\n")
        }
        val f3 = tempFolder.newFile("doc3.md").apply {
            // Modified content
            writeText(
                """
                # Title
                
                This is a modified paragraph.
                
                - Item 1
                - Item 2
                """.trimIndent()
            )
        }

        assertFalse(ConfigDiffSyncEngine.hasContentChanges(f1, f2))
        assertTrue(ConfigDiffSyncEngine.hasContentChanges(f1, f3))
    }
}
