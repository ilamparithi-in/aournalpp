package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import org.junit.Assert.assertEquals
import org.junit.Test

class ProcessSupervisorTitleTest {

    @Test
    fun `clean document title without dirty indicator`() {
        val result = ProcessSupervisor.sanitizeWindowTitle("MyLectureNotes.xopp - Xournal++")
        assertEquals("MyLectureNotes.xopp", result)
    }

    @Test
    fun `dirty document title preserves prefix asterisk`() {
        val result = ProcessSupervisor.sanitizeWindowTitle("*MyLectureNotes.xopp - Xournal++")
        assertEquals("*MyLectureNotes.xopp", result)
    }

    @Test
    fun `dirty document title with suffix asterisk preserves prefix asterisk`() {
        val result = ProcessSupervisor.sanitizeWindowTitle("MyLectureNotes.xopp * - Xournal++")
        assertEquals("*MyLectureNotes.xopp", result)
    }

    @Test
    fun `clean unsaved document converts to New Note`() {
        val result = ProcessSupervisor.sanitizeWindowTitle("Unsaved Document - Xournal++")
        assertEquals("New Note", result)
    }

    @Test
    fun `dirty unsaved document converts to asterisk New Note`() {
        val result = ProcessSupervisor.sanitizeWindowTitle("*Unsaved Document - Xournal++")
        assertEquals("*New Note", result)
    }

    @Test
    fun `dirty untitled document converts to asterisk New Note`() {
        val result = ProcessSupervisor.sanitizeWindowTitle("*Untitled - Xournal++")
        assertEquals("*New Note", result)
    }

    @Test
    fun `autosaved tag is stripped but dirty status preserved`() {
        val result = ProcessSupervisor.sanitizeWindowTitle("*MyNotes.xopp [autosaved] - Xournal++")
        assertEquals("*MyNotes.xopp", result)
    }

    @Test
    fun `dialog titles are filtered out`() {
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Preferences"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Xournal++ Preferences"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Preferences - Xournal++"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("About Xournal++"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("About"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Plugin Manager"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Manage Plugins"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Page Background"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Set Page Background"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Select Font"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Font Selection"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Select Color"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Color Selection"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Save File"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Save As"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Open Document"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Export as PDF"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Export PDF"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Question"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Warning"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Error"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Openbox"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Save changes to document \"Lecture.xopp\" before closing?"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Error saving document"))
    }

    @Test
    fun `notes with preference or dialog words in document name are preserved`() {
        val result1 = ProcessSupervisor.sanitizeWindowTitle("My_Preferences_Lecture.xopp - Xournal++")
        assertEquals("My_Preferences_Lecture.xopp", result1)

        val result2 = ProcessSupervisor.sanitizeWindowTitle("*Preferences.xopp - Xournal++")
        assertEquals("*Preferences.xopp", result2)

        val result3 = ProcessSupervisor.sanitizeWindowTitle("Export_Settings.xopp - Xournal++")
        assertEquals("Export_Settings.xopp", result3)
    }

    @Test
    fun `cleanNoteTitle strips asterisks and application suffixes cleanly`() {
        assertEquals("MyNotes.xopp", ProcessSupervisor.cleanNoteTitle("*MyNotes.xopp - Xournal++"))
        assertEquals("MyNotes.xopp", ProcessSupervisor.cleanNoteTitle("MyNotes.xopp * - Xournal++"))
        assertEquals("MyNotes.xopp", ProcessSupervisor.cleanNoteTitle("*MyNotes.xopp [autosaved] - Xournal++"))
        assertEquals("Lecture Note", ProcessSupervisor.cleanNoteTitle("*Lecture Note - Xournal++"))
    }

    @Test
    fun `resolveNoteFile locates note files regardless of asterisk or extension variations`() {
        val tempDir = java.nio.file.Files.createTempDirectory("note_test").toFile()
        try {
            val testFile = java.io.File(tempDir, "2026-09-08-Note-14-14.xopp")
            testFile.writeText("test content")

            val resolvedFromDirtyTitle = ProcessSupervisor.resolveNoteFile(tempDir, "*2026-09-08-Note-14-14.xopp - Xournal++")
            assertEquals(testFile.absolutePath, resolvedFromDirtyTitle?.absolutePath)

            val resolvedFromCleanTitle = ProcessSupervisor.resolveNoteFile(tempDir, "2026-09-08-Note-14-14.xopp")
            assertEquals(testFile.absolutePath, resolvedFromCleanTitle?.absolutePath)

            val resolvedFromNoExt = ProcessSupervisor.resolveNoteFile(tempDir, "*2026-09-08-Note-14-14")
            assertEquals(testFile.absolutePath, resolvedFromNoExt?.absolutePath)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
