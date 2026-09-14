package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.runtime.ActiveNotesTracker
import dev.ilamparithi.aournalpp.runtime.ActiveSessionInfo
import dev.ilamparithi.aournalpp.runtime.ActiveSessionTracker
import dev.ilamparithi.aournalpp.runtime.ActiveWindowEntry
import dev.ilamparithi.aournalpp.runtime.ActiveWorkspaceState
import dev.ilamparithi.aournalpp.runtime.ActiveWorkspaceTracker
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class ActiveNotesTrackerTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File.createTempFile("active_notes_test_", "").apply {
            delete()
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun getCurrentProcessPid(): Int {
        return try {
            ProcessHandle.current().pid().toInt()
        } catch (_: Throwable) {
            1
        }
    }

    @Test
    fun `test findActiveNote returns null when session is not active`() {
        val testFile = File(tempDir, "Lecture1.xopp").apply { writeText("dummy") }
        val match = ActiveNotesTracker.findActiveNote(tempDir, testFile)
        assertNull(match)
        assertFalse(ActiveNotesTracker.isNoteOpen(tempDir, testFile))
    }

    @Test
    fun `test findActiveNote matches by filePath in workspace state`() {
        val currentPid = getCurrentProcessPid()
        val noteFile = File(tempDir, "Lecture1.xopp").apply { writeText("dummy") }

        val sessionInfo = ActiveSessionInfo(
            isRunning = true,
            pid = currentPid,
            activeNotePath = noteFile.absolutePath,
            documentTitle = "Lecture1",
            openWindowCount = 1
        )
        ActiveSessionTracker.setActiveSession(tempDir, sessionInfo)

        val window = ActiveWindowEntry(
            id = "0xabc123",
            title = "Lecture1 - Xournal++",
            cleanTitle = "Lecture1",
            filePath = noteFile.absolutePath,
            isActive = true
        )
        val workspaceState = ActiveWorkspaceState(
            sessionInfo = sessionInfo,
            windows = listOf(window)
        )
        ActiveWorkspaceTracker.setWorkspaceState(tempDir, workspaceState)

        val match = ActiveNotesTracker.findActiveNote(tempDir, noteFile)
        assertNotNull(match)
        assertEquals("0xabc123", match?.windowId)
        assertEquals("Lecture1", match?.title)
        assertEquals(noteFile.absolutePath, match?.filePath)
        assertTrue(ActiveNotesTracker.isNoteOpen(tempDir, noteFile))
    }

    @Test
    fun `test findActiveNote matches by cleanTitle in workspace state`() {
        val currentPid = getCurrentProcessPid()
        val noteFile = File(tempDir, "Calculus_Homework.xopp").apply { writeText("dummy") }

        val sessionInfo = ActiveSessionInfo(
            isRunning = true,
            pid = currentPid,
            activeNotePath = null,
            documentTitle = null,
            openWindowCount = 1
        )
        ActiveSessionTracker.setActiveSession(tempDir, sessionInfo)

        val window = ActiveWindowEntry(
            id = "0xdef456",
            title = "Calculus_Homework - Xournal++",
            cleanTitle = "Calculus_Homework",
            filePath = null,
            isActive = true
        )
        val workspaceState = ActiveWorkspaceState(
            sessionInfo = sessionInfo,
            windows = listOf(window)
        )
        ActiveWorkspaceTracker.setWorkspaceState(tempDir, workspaceState)

        val match = ActiveNotesTracker.findActiveNote(tempDir, noteFile)
        assertNotNull(match)
        assertEquals("0xdef456", match?.windowId)
        assertEquals("Calculus_Homework", match?.title)
    }

    @Test
    fun `test findActiveNote matches by sanitized window title with dirty asterisk and autosave`() {
        val currentPid = getCurrentProcessPid()
        val noteFile = File(tempDir, "Physics Notes.xopp").apply { writeText("dummy") }

        val sessionInfo = ActiveSessionInfo(
            isRunning = true,
            pid = currentPid,
            activeNotePath = null,
            documentTitle = null,
            openWindowCount = 1
        )
        ActiveSessionTracker.setActiveSession(tempDir, sessionInfo)

        val window = ActiveWindowEntry(
            id = "0x789xyz",
            title = "*Physics Notes.xopp [autosaved] - Xournal++",
            cleanTitle = "",
            filePath = null,
            isActive = true
        )
        val workspaceState = ActiveWorkspaceState(
            sessionInfo = sessionInfo,
            windows = listOf(window)
        )
        ActiveWorkspaceTracker.setWorkspaceState(tempDir, workspaceState)

        val match = ActiveNotesTracker.findActiveNote(tempDir, noteFile)
        assertNotNull(match)
        assertEquals("0x789xyz", match?.windowId)
        assertEquals("Physics Notes", match?.title)
    }

    @Test
    fun `test findActiveNote matches by ActiveSessionInfo path and title in single-window fallback`() {
        val currentPid = getCurrentProcessPid()
        val noteFile = File(tempDir, "Standalone.xopp").apply { writeText("dummy") }

        val sessionInfo = ActiveSessionInfo(
            isRunning = true,
            pid = currentPid,
            activeNotePath = noteFile.absolutePath,
            documentTitle = "Standalone",
            openWindowCount = 1
        )
        ActiveSessionTracker.setActiveSession(tempDir, sessionInfo)

        val match = ActiveNotesTracker.findActiveNote(tempDir, noteFile)
        assertNotNull(match)
        assertNull(match?.windowId)
        assertEquals("Standalone", match?.title)
        assertEquals(noteFile.absolutePath, match?.filePath)
    }

    @Test
    fun `test findActiveNote returns null when note is not in active session`() {
        val currentPid = getCurrentProcessPid()
        val openNote = File(tempDir, "OpenDoc.xopp").apply { writeText("dummy") }
        val closedNote = File(tempDir, "DifferentDoc.xopp").apply { writeText("dummy") }

        val sessionInfo = ActiveSessionInfo(
            isRunning = true,
            pid = currentPid,
            activeNotePath = openNote.absolutePath,
            documentTitle = "OpenDoc",
            openWindowCount = 1
        )
        ActiveSessionTracker.setActiveSession(tempDir, sessionInfo)

        val window = ActiveWindowEntry(
            id = "0x111",
            title = "OpenDoc - Xournal++",
            cleanTitle = "OpenDoc",
            filePath = openNote.absolutePath,
            isActive = true
        )
        val workspaceState = ActiveWorkspaceState(
            sessionInfo = sessionInfo,
            windows = listOf(window)
        )
        ActiveWorkspaceTracker.setWorkspaceState(tempDir, workspaceState)

        val match = ActiveNotesTracker.findActiveNote(tempDir, closedNote)
        assertNull(match)
        assertFalse(ActiveNotesTracker.isNoteOpen(tempDir, closedNote))
    }

    @Test
    fun `test findActiveNote ignores generic titles like Untitled, New Note, Unsaved Document`() {
        val currentPid = getCurrentProcessPid()
        val customNote = File(tempDir, "Untitled.xopp").apply { writeText("dummy") }

        val sessionInfo = ActiveSessionInfo(
            isRunning = true,
            pid = currentPid,
            activeNotePath = null,
            documentTitle = null,
            openWindowCount = 1
        )
        ActiveSessionTracker.setActiveSession(tempDir, sessionInfo)

        val window = ActiveWindowEntry(
            id = "0x222",
            title = "Untitled Note - Xournal++",
            cleanTitle = "Untitled Note",
            filePath = null,
            isActive = true
        )
        val workspaceState = ActiveWorkspaceState(
            sessionInfo = sessionInfo,
            windows = listOf(window)
        )
        ActiveWorkspaceTracker.setWorkspaceState(tempDir, workspaceState)

        val otherNote = File(tempDir, "RealDocument.xopp").apply { writeText("dummy") }
        val match = ActiveNotesTracker.findActiveNote(tempDir, otherNote)
        assertNull(match)
    }
}
