package dev.ilamparithi.aournalpp.runtime

import android.content.Context
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ParallelCloseConflictTest {

    private lateinit var tempDir: File
    private lateinit var mockContext: Context
    private lateinit var mockEnv: LinuxEnvironment
    private lateinit var mockSupervisor: ProcessSupervisor
    private val testScope = TestScope()

    @Before
    fun setUp() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.i(any(), any()) } returns 0
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0

        tempDir = File.createTempFile("parallel_close_test_", "").apply {
            delete()
            mkdirs()
        }
        mockContext = mockk(relaxed = true)
        mockEnv = mockk(relaxed = true) {
            every { tmpDir } returns tempDir
        }
        mockSupervisor = mockk(relaxed = true)

        mockkObject(ActiveSessionTracker)
        every { ActiveSessionTracker.isPidAlive(any()) } returns true
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
        unmockkStatic(android.util.Log::class)
        unmockkAll()
    }

    @Test
    fun `test conflict detection aborts parallel close and triggers onConflictDetected`() = testScope.runTest {
        // Setup state with 2 windows pointing to the same note path
        val conflictingPath = File(tempDir, "Lecture.xopp").absolutePath
        val state = ActiveWorkspaceState(
            sessionInfo = ActiveSessionInfo(isRunning = true, pid = 1234, null, null),
            windows = listOf(
                ActiveWindowEntry(
                    id = "0x111",
                    title = "Lecture - Xournal++",
                    cleanTitle = "Lecture",
                    filePath = conflictingPath,
                    isActive = true
                ),
                ActiveWindowEntry(
                    id = "0x222",
                    title = "*Lecture - Xournal++",
                    cleanTitle = "Lecture",
                    filePath = conflictingPath,
                    isActive = false,
                    isDirty = true
                )
            )
        )
        ActiveWorkspaceTracker.setWorkspaceState(tempDir, state)

        val testDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
        val sessionManager = CanvasSessionManager(
            context = mockContext,
            env = mockEnv,
            supervisor = mockSupervisor,
            scope = this,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        ).apply { isSessionRunning = true }

        var conflictNoteName: String? = null
        var conflictWindowId: String? = null
        var allClosedFired = false

        sessionManager.initiateParallelClose(
            onAllClosed = { allClosedFired = true },
            onConflictDetected = { noteName, targetWid ->
                conflictNoteName = noteName
                conflictWindowId = targetWid
            },
            onPromptBlocking = {}
        )

        testScheduler.advanceUntilIdle()

        assertFalse("All closed should NOT fire on conflict", allClosedFired)
        assertEquals("Lecture.xopp", conflictNoteName)
        assertEquals("0x111", conflictWindowId)
        // Verify supervisor.closeWindow was never called
        verify(exactly = 0) { mockSupervisor.closeWindow(any()) }
    }

    @Test
    fun `test parallel close dispatches close to all windows when no conflicts`() = testScope.runTest {
        // Setup clean workspace state with distinct notes
        val state = ActiveWorkspaceState(
            sessionInfo = ActiveSessionInfo(isRunning = true, pid = 1234, null, null),
            windows = listOf(
                ActiveWindowEntry(
                    id = "0xAAA",
                    title = "Note1 - Xournal++",
                    cleanTitle = "Note1",
                    filePath = File(tempDir, "Note1.xopp").absolutePath,
                    isActive = true
                ),
                ActiveWindowEntry(
                    id = "0xBBB",
                    title = "Note2 - Xournal++",
                    cleanTitle = "Note2",
                    filePath = File(tempDir, "Note2.xopp").absolutePath,
                    isActive = false
                )
            )
        )
        ActiveWorkspaceTracker.setWorkspaceState(tempDir, state)

        every { mockSupervisor.getVisibleXournalWindowIds() } returnsMany listOf(
            listOf("0xAAA", "0xBBB"), // initial check
            emptyList() // after close signals, all closed
        )
        every { mockSupervisor.closeWindow(any()) } returns true
        every { mockSupervisor.isXournalRunning() } returns false

        val testDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
        val sessionManager = CanvasSessionManager(
            context = mockContext,
            env = mockEnv,
            supervisor = mockSupervisor,
            scope = this,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        ).apply { isSessionRunning = true }

        var allClosedFired = false
        var conflictFired = false

        sessionManager.initiateParallelClose(
            onAllClosed = { allClosedFired = true },
            onConflictDetected = { _, _ -> conflictFired = true },
            onPromptBlocking = {}
        )

        testScheduler.advanceUntilIdle()

        assertFalse("Conflict should NOT fire", conflictFired)
        assertTrue("onAllClosed should fire", allClosedFired)
        verify(exactly = 1) { mockSupervisor.closeWindow("0xAAA") }
        verify(exactly = 1) { mockSupervisor.closeWindow("0xBBB") }
    }

    @Test
    fun `test parallel close auto-confirms save confirmation dialog`() = testScope.runTest {
        val state = ActiveWorkspaceState(
            sessionInfo = ActiveSessionInfo(isRunning = true, pid = 1234, null, null),
            windows = listOf(
                ActiveWindowEntry(
                    id = "0xCCC",
                    title = "*Draft - Xournal++",
                    cleanTitle = "Draft",
                    filePath = File(tempDir, "Draft.xopp").absolutePath,
                    isActive = true,
                    isDirty = true
                )
            )
        )
        ActiveWorkspaceTracker.setWorkspaceState(tempDir, state)

        every { mockSupervisor.getVisibleXournalWindowIds() } returnsMany listOf(
            listOf("0xCCC"),
            listOf("0xCCC"),
            emptyList()
        )
        every { mockSupervisor.getVisibleXournalDialogWindowIds() } returnsMany listOf(
            listOf("0xDIALOG1"),
            emptyList()
        )
        every { mockSupervisor.confirmSaveDialog("0xDIALOG1") } returns true
        every { mockSupervisor.closeWindow("0xCCC") } returns true
        every { mockSupervisor.isXournalRunning() } returnsMany listOf(true, false)

        val testDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
        val sessionManager = CanvasSessionManager(
            context = mockContext,
            env = mockEnv,
            supervisor = mockSupervisor,
            scope = this,
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        ).apply { isSessionRunning = true }

        var allClosedFired = false

        sessionManager.initiateParallelClose(
            onAllClosed = { allClosedFired = true },
            onConflictDetected = { _, _ -> },
            onPromptBlocking = {}
        )

        testScheduler.advanceUntilIdle()

        assertTrue("onAllClosed should fire after dialog auto-confirmation", allClosedFired)
        verify(atLeast = 1) { mockSupervisor.confirmSaveDialog("0xDIALOG1") }
    }
}
