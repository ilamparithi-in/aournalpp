package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.runtime.ActiveSessionInfo
import dev.ilamparithi.aournalpp.runtime.ActiveWindowEntry
import dev.ilamparithi.aournalpp.runtime.ActiveWorkspaceState
import dev.ilamparithi.aournalpp.runtime.ActiveWorkspaceTracker
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class ActiveWorkspaceTrackerTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File.createTempFile("active_workspace_test_", "").apply {
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
    fun `test ActiveWorkspaceState serialization and deserialization`() {
        val currentPid = getCurrentProcessPid()
        val sessionInfo = ActiveSessionInfo(
            isRunning = true,
            pid = currentPid,
            activeNotePath = "/notes/math.xopp",
            documentTitle = "Math",
            openWindowCount = 2
        )

        val windowA = ActiveWindowEntry(
            id = "0x100001",
            title = "Math - Xournal++",
            cleanTitle = "Math",
            filePath = "/notes/math.xopp",
            isActive = true,
            isDirty = true,
            hasConflict = false,
            isAudioRecording = true
        )

        val windowB = ActiveWindowEntry(
            id = "0x100002",
            title = "Physics - Xournal++",
            cleanTitle = "Physics",
            filePath = "/notes/physics.xopp",
            isActive = false,
            isDirty = false,
            hasConflict = false,
            isAudioRecording = false
        )

        val originalState = ActiveWorkspaceState(
            sessionInfo = sessionInfo,
            windows = listOf(windowA, windowB),
            snapMode = "split_two",
            slotAssignments = mapOf(0 to "0x100001", 1 to "0x100002"),
            dividerRatios = mapOf("d1" to 0.5f),
            previewTimestamp = 123456789L
        )

        val json = originalState.toJson()
        val deserialized = ActiveWorkspaceState.fromJson(json)

        assertEquals(originalState.snapMode, deserialized.snapMode)
        assertEquals(originalState.windows.size, deserialized.windows.size)
        assertEquals(originalState.slotAssignments, deserialized.slotAssignments)
        assertEquals(originalState.dividerRatios["d1"], deserialized.dividerRatios["d1"])
        assertEquals(originalState.previewTimestamp, deserialized.previewTimestamp)
        assertTrue(deserialized.windows[0].isDirty)
        assertTrue(deserialized.windows[0].isAudioRecording)
        assertEquals("/notes/math.xopp", deserialized.windows[0].filePath)
    }

    @Test
    fun `test setWorkspaceState and getWorkspaceState`() {
        val currentPid = getCurrentProcessPid()
        val sessionInfo = ActiveSessionInfo(
            isRunning = true,
            pid = currentPid,
            activeNotePath = "/notes/lecture.xopp",
            documentTitle = "Lecture",
            openWindowCount = 1
        )

        val state = ActiveWorkspaceState(
            sessionInfo = sessionInfo,
            windows = listOf(
                ActiveWindowEntry(
                    id = "0x200001",
                    title = "Lecture - Xournal++",
                    cleanTitle = "Lecture",
                    filePath = "/notes/lecture.xopp",
                    isActive = true
                )
            ),
            snapMode = "single"
        )

        ActiveWorkspaceTracker.setWorkspaceState(tempDir, state)

        val readBack = ActiveWorkspaceTracker.getWorkspaceState(tempDir)
        assertNotNull(readBack)
        assertEquals("single", readBack!!.snapMode)
        assertEquals(1, readBack.windows.size)
        assertEquals("0x200001", readBack.windows[0].id)
        assertFalse(readBack.hasAnyConflict)
    }

    @Test
    fun `test conflict detection flags duplicate file paths`() {
        val currentPid = getCurrentProcessPid()
        val sessionInfo = ActiveSessionInfo(
            isRunning = true,
            pid = currentPid,
            activeNotePath = "/notes/duplicate.xopp",
            documentTitle = "Duplicate",
            openWindowCount = 2
        )

        val stateWithConflict = ActiveWorkspaceState(
            sessionInfo = sessionInfo,
            windows = listOf(
                ActiveWindowEntry(
                    id = "0x300001",
                    title = "Duplicate - Xournal++",
                    cleanTitle = "Duplicate",
                    filePath = "/notes/duplicate.xopp",
                    isActive = true,
                    hasConflict = true
                ),
                ActiveWindowEntry(
                    id = "0x300002",
                    title = "*Duplicate - Xournal++",
                    cleanTitle = "Duplicate",
                    filePath = "/notes/duplicate.xopp",
                    isActive = false,
                    isDirty = true,
                    hasConflict = true
                )
            ),
            snapMode = "split_two"
        )

        assertTrue(stateWithConflict.hasAnyConflict)
    }

    @Test
    fun `test clearWorkspaceState removes state file`() {
        val currentPid = getCurrentProcessPid()
        val state = ActiveWorkspaceState(
            sessionInfo = ActiveSessionInfo(isRunning = true, pid = currentPid, null, null),
            windows = emptyList()
        )

        ActiveWorkspaceTracker.setWorkspaceState(tempDir, state)
        assertNotNull(ActiveWorkspaceTracker.getWorkspaceState(tempDir))

        ActiveWorkspaceTracker.clearWorkspaceState(tempDir)
        assertNull(ActiveWorkspaceTracker.getWorkspaceState(tempDir))
    }

    @Test
    fun `test dead PID automatically prunes workspace state`() {
        // PID 9999999 is extraordinarily likely to not exist
        val deadPid = 9999999
        val state = ActiveWorkspaceState(
            sessionInfo = ActiveSessionInfo(isRunning = true, pid = deadPid, null, null),
            windows = emptyList()
        )

        ActiveWorkspaceTracker.setWorkspaceState(tempDir, state)
        // Attempting to read with dead PID should prune and return null
        val readBack = ActiveWorkspaceTracker.getWorkspaceState(tempDir)
        assertNull(readBack)
    }
}
