package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.runtime.ActiveSessionInfo
import dev.ilamparithi.aournalpp.runtime.ActiveSessionTracker
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class ActiveSessionTrackerTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File.createTempFile("active_session_test_", "").apply {
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
    fun `test setActiveSession writes valid metadata and reads back`() {
        val currentPid = getCurrentProcessPid()
        val info = ActiveSessionInfo(
            isRunning = true,
            pid = currentPid,
            activeNotePath = "/test/path/note.xopp",
            documentTitle = "Physics 101",
            openWindowCount = 1
        )

        ActiveSessionTracker.setActiveSession(tempDir, info)

        val readBack = ActiveSessionTracker.getActiveSession(tempDir)
        assertNotNull(readBack)
        assertTrue(readBack!!.isRunning)
        assertEquals(currentPid, readBack.pid)
        assertEquals("/test/path/note.xopp", readBack.activeNotePath)
        assertEquals("Physics 101", readBack.documentTitle)
        assertEquals(1, readBack.openWindowCount)
    }

    @Test
    fun `test updateTitle updates document title in active session`() {
        val currentPid = getCurrentProcessPid()
        val info = ActiveSessionInfo(
            isRunning = true,
            pid = currentPid,
            activeNotePath = "/test/note.xopp",
            documentTitle = "Initial Note",
            openWindowCount = 1
        )
        ActiveSessionTracker.setActiveSession(tempDir, info)

        ActiveSessionTracker.updateTitle(tempDir, "Renamed Note")

        val updated = ActiveSessionTracker.getActiveSession(tempDir)
        assertNotNull(updated)
        assertEquals("Renamed Note", updated!!.documentTitle)
    }

    @Test
    fun `test updateWindowCount updates multi-window count`() {
        val currentPid = getCurrentProcessPid()
        val info = ActiveSessionInfo(
            isRunning = true,
            pid = currentPid,
            activeNotePath = null,
            documentTitle = "New Note",
            openWindowCount = 1
        )
        ActiveSessionTracker.setActiveSession(tempDir, info)

        ActiveSessionTracker.updateWindowCount(tempDir, 3)

        val updated = ActiveSessionTracker.getActiveSession(tempDir)
        assertNotNull(updated)
        assertEquals(3, updated!!.openWindowCount)
    }

    @Test
    fun `test dead PID cleans up stale session file`() {
        // PID 99999999 is dead / non-existent
        val deadPid = 99999999
        val info = ActiveSessionInfo(
            isRunning = true,
            pid = deadPid,
            activeNotePath = "/test/dead.xopp",
            documentTitle = "Ghost Session",
            openWindowCount = 1
        )
        ActiveSessionTracker.setActiveSession(tempDir, info)

        // Session file exists before checking
        val sessionFile = File(tempDir, ".active_canvas_session.json")
        assertTrue(sessionFile.exists())

        // Reading should detect dead PID, delete file, and return null
        val result = ActiveSessionTracker.getActiveSession(tempDir)
        assertNull(result)
        assertFalse(sessionFile.exists())
    }

    @Test
    fun `test clearActiveSession removes session file`() {
        val currentPid = getCurrentProcessPid()
        val info = ActiveSessionInfo(
            isRunning = true,
            pid = currentPid,
            activeNotePath = "/test/note.xopp",
            documentTitle = "Active Note",
            openWindowCount = 1
        )
        ActiveSessionTracker.setActiveSession(tempDir, info)

        assertTrue(ActiveSessionTracker.isSessionActive(tempDir))

        ActiveSessionTracker.clearActiveSession(tempDir)
        assertFalse(ActiveSessionTracker.isSessionActive(tempDir))
    }
}
