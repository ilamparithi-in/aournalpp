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
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Save File"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Open Document"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Export as PDF"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Question"))
        assertEquals("", ProcessSupervisor.sanitizeWindowTitle("Openbox"))
    }
}
