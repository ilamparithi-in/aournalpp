package dev.ilamparithi.aournalpp.backup

import dev.ilamparithi.aournalpp.backup.engine.BackupEngine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ConfigDiffSyncTest {

    @Test
    fun testIsConfigFileDetection() {
        // Direct file names
        assertTrue(BackupEngine.isConfigFile("x11_prefs.json"))
        assertTrue(BackupEngine.isConfigFile("app_settings.json"))
        assertTrue(BackupEngine.isConfigFile("settings.xml"))
        assertTrue(BackupEngine.isConfigFile("settings.ini"))
        assertTrue(BackupEngine.isConfigFile("sync_mappings.json"))

        // Relative paths inside .config
        assertTrue(BackupEngine.isConfigFile(".config/x11_prefs.json"))
        assertTrue(BackupEngine.isConfigFile(".config/app_settings.json"))
        assertTrue(BackupEngine.isConfigFile(".config/xournalpp/settings.xml"))
        assertTrue(BackupEngine.isConfigFile(".config/xournalpp/settings.ini"))
        assertTrue(BackupEngine.isConfigFile(".config/palette.gpl"))
        assertTrue(BackupEngine.isConfigFile("home/Notes/.config/x11_prefs.json"))

        // Non-config files
        assertFalse(BackupEngine.isConfigFile("Notes/Calculus.xopp"))
        assertFalse(BackupEngine.isConfigFile("Notes/Document.pdf"))
        assertFalse(BackupEngine.isConfigFile("Notes/Meeting.txt"))
    }

    @Test
    fun testJsonSemanticDiffEquality() {
        val tempDir = Files.createTempDirectory("config_diff_json_test").toFile()
        try {
            val f1 = File(tempDir, "x11_prefs_local.json")
            val f2 = File(tempDir, "x11_prefs_remote.json")

            // Keys in different order, different spacing and indentation
            f1.writeText(
                """
                {
                    "fullscreen": true,
                    "resolution": "1920x1080",
                    "refreshRate": 60
                }
                """.trimIndent()
            )

            f2.writeText(
                """
                {
                  "refreshRate": 60,
                  "fullscreen": true,
                  "resolution": "1920x1080"
                }
                """.trimIndent()
            )

            // Even though text and key order differ, semantic JSON content is identical: 0 changes
            assertFalse(BackupEngine.hasContentChanges(f1, f2))

            // Now introduce an actual change
            f2.writeText(
                """
                {
                  "refreshRate": 120,
                  "fullscreen": true,
                  "resolution": "1920x1080"
                }
                """.trimIndent()
            )
            assertTrue(BackupEngine.hasContentChanges(f1, f2))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testXmlDiffEquality() {
        val tempDir = Files.createTempDirectory("config_diff_xml_test").toFile()
        try {
            val f1 = File(tempDir, "settings_local.xml")
            val f2 = File(tempDir, "settings_remote.xml")

            f1.writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <settings>
                    <property name="autoloadMostRecent" value="false"/>
                    <property name="defaultSaveDir" value="/storage/emulated/0/Documents/Notes"/>
                </settings>
                """.trimIndent()
            )

            // Different indentation and trailing newlines
            f2.writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                  <settings>
                      <property name="autoloadMostRecent" value="false"/>
                      <property name="defaultSaveDir" value="/storage/emulated/0/Documents/Notes"/>
                  </settings>
                """.trimIndent()
            )

            assertFalse(BackupEngine.hasContentChanges(f1, f2))

            // Introduce actual change
            f2.writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <settings>
                    <property name="autoloadMostRecent" value="true"/>
                    <property name="defaultSaveDir" value="/storage/emulated/0/Documents/Notes"/>
                </settings>
                """.trimIndent()
            )
            assertTrue(BackupEngine.hasContentChanges(f1, f2))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testIniDiffEquality() {
        val tempDir = Files.createTempDirectory("config_diff_ini_test").toFile()
        try {
            val f1 = File(tempDir, "settings_local.ini")
            val f2 = File(tempDir, "settings_remote.ini")

            f1.writeText(
                """
                [Settings]
                gtk-theme-name=Adwaita-dark
                gtk-font-name=Sans 11
                """.trimIndent()
            )

            f2.writeText(
                """
                [Settings]
                  gtk-theme-name=Adwaita-dark
                  gtk-font-name=Sans 11
                """.trimIndent()
            )

            assertFalse(BackupEngine.hasContentChanges(f1, f2))

            f2.writeText(
                """
                [Settings]
                gtk-theme-name=Adwaita-light
                gtk-font-name=Sans 11
                """.trimIndent()
            )
            assertTrue(BackupEngine.hasContentChanges(f1, f2))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testModifiedTimestampDifferenceWithIdenticalContent() {
        val tempDir = Files.createTempDirectory("config_diff_mtime_test").toFile()
        try {
            val f1 = File(tempDir, "app_settings.json")
            val f2 = File(tempDir, "app_settings_remote.json")

            val content = """{"pref_pen_color": "#FF0000", "pref_stroke_width": 2.5}"""
            f1.writeText(content)
            f2.writeText(content)

            // Simulate file being touched/accessed at different times
            f1.setLastModified(1700000000000L)
            f2.setLastModified(1700099999000L)

            // Content diff should be false despite large timestamp difference
            assertFalse(BackupEngine.hasContentChanges(f1, f2))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testTextBasedFileDetection() {
        assertTrue(BackupEngine.isTextBasedFile("note.md"))
        assertTrue(BackupEngine.isTextBasedFile("Notes/lecture.MD"))
        assertTrue(BackupEngine.isTextBasedFile("readme.markdown"))
        assertTrue(BackupEngine.isTextBasedFile("quick_note.txt"))
        assertTrue(BackupEngine.isTextBasedFile("x11_prefs.json"))
        assertTrue(BackupEngine.isTextBasedFile(".config/xournalpp/settings.xml"))
        assertTrue(BackupEngine.isTextBasedFile("report.csv"))

        assertFalse(BackupEngine.isTextBasedFile("Notes/Calculus.xopp"))
        assertFalse(BackupEngine.isTextBasedFile("Notes/Document.pdf"))
        assertFalse(BackupEngine.isTextBasedFile("Audio/lecture.m4a"))
        assertFalse(BackupEngine.isTextBasedFile("Images/screenshot.png"))
    }

    @Test
    fun testMarkdownDiffEquality() {
        val tempDir = Files.createTempDirectory("config_diff_md_test").toFile()
        try {
            val f1 = File(tempDir, "note_local.md")
            val f2 = File(tempDir, "note_remote.md")

            f1.writeText(
                """
                # My Markdown Notes
                
                - Point 1
                - Point 2
                
                End of note.
                """.trimIndent()
            )

            // Identical textual content, different line endings (CRLF vs LF) and spacing
            f2.writeText("# My Markdown Notes\r\n\r\n- Point 1\r\n- Point 2  \r\n\r\nEnd of note.\r\n")

            // Diff should report 0 changes (identical content)
            assertFalse(BackupEngine.hasContentChanges(f1, f2))

            // Introduce actual textual change
            f2.writeText(
                """
                # My Markdown Notes
                
                - Point 1
                - Point 2 (edited on cloud)
                
                End of note.
                """.trimIndent()
            )
            assertTrue(BackupEngine.hasContentChanges(f1, f2))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testMarkdownTimestampDifferenceWithIdenticalContent() {
        val tempDir = Files.createTempDirectory("config_diff_md_mtime_test").toFile()
        try {
            val f1 = File(tempDir, "todo_local.md")
            val f2 = File(tempDir, "todo_remote.md")

            val content = "# Daily Tasks\n- Write code\n- Run tests\n"
            f1.writeText(content)
            f2.writeText(content)

            f1.setLastModified(1710000000000L)
            f2.setLastModified(1710099999000L)

            // Content diff should be false despite different timestamps
            assertFalse(BackupEngine.hasContentChanges(f1, f2))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
