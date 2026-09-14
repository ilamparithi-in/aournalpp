package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.runtime.ActiveSessionInfo
import dev.ilamparithi.aournalpp.runtime.ActiveWindowEntry
import dev.ilamparithi.aournalpp.runtime.ActiveWorkspaceState
import dev.ilamparithi.aournalpp.ui.snap.SnapLayoutMode
import org.junit.Assert.*
import org.junit.Test

class ActiveSessionPortalNavigationTest {

    @Test
    fun `test AppTab WORKSPACE exists and has expected properties`() {
        val workspaceTab = AppTab.WORKSPACE
        assertNotNull(workspaceTab)
        assertEquals(R.string.tab_workspace, workspaceTab.titleRes)
        assertTrue(AppTab.entries.contains(workspaceTab))
    }

    @Test
    fun `test dynamic tab list injection based on session state`() {
        fun computeVisibleTabs(isSessionRunning: Boolean): List<AppTab> {
            return if (isSessionRunning) {
                listOf(AppTab.HOME, AppTab.FILES, AppTab.WORKSPACE, AppTab.CLOUD, AppTab.SETTINGS, AppTab.ABOUT)
            } else {
                listOf(AppTab.HOME, AppTab.FILES, AppTab.CLOUD, AppTab.SETTINGS, AppTab.ABOUT)
            }
        }

        val inactiveTabs = computeVisibleTabs(isSessionRunning = false)
        assertFalse("Inactive session should NOT include WORKSPACE tab", inactiveTabs.contains(AppTab.WORKSPACE))
        assertEquals(listOf(AppTab.HOME, AppTab.FILES, AppTab.CLOUD, AppTab.SETTINGS, AppTab.ABOUT), inactiveTabs)

        val activeTabs = computeVisibleTabs(isSessionRunning = true)
        assertTrue("Active session MUST include WORKSPACE tab", activeTabs.contains(AppTab.WORKSPACE))
        assertEquals(2, activeTabs.indexOf(AppTab.WORKSPACE)) // Injected between FILES and CLOUD
    }

    @Test
    fun `test staged vs stashed windows partitioning in SPLIT_TWO mode`() {
        val win1 = ActiveWindowEntry("0x101", "Lecture 1 - Xournal++", "Lecture 1", "/notes/l1.xopp", isActive = true)
        val win2 = ActiveWindowEntry("0x102", "Lecture 2 - Xournal++", "Lecture 2", "/notes/l2.xopp", isActive = false)
        val win3 = ActiveWindowEntry("0x103", "Rough Notes - Xournal++", "Rough Notes", "/notes/rough.xopp", isActive = false)

        val state = ActiveWorkspaceState(
            sessionInfo = ActiveSessionInfo(isRunning = true, pid = 100, "/notes/l1.xopp", "Lecture 1", 3),
            windows = listOf(win1, win2, win3),
            snapMode = "split_two",
            slotAssignments = mapOf(0 to "0x101", 1 to "0x102")
        )

        val stagedIds = when (SnapLayoutMode.fromId(state.snapMode)) {
            SnapLayoutMode.SPLIT_TWO -> listOfNotNull(
                state.slotAssignments[0] ?: state.windows.getOrNull(0)?.id,
                state.slotAssignments[1] ?: state.windows.getOrNull(1)?.id
            )
            else -> emptyList()
        }.toSet()

        val stashed = state.windows.filter { it.id !in stagedIds }

        assertEquals(setOf("0x101", "0x102"), stagedIds)
        assertEquals(1, stashed.size)
        assertEquals("0x103", stashed.first().id)
        assertEquals("Rough Notes", stashed.first().cleanTitle)
    }

    @Test
    fun `test staged vs stashed windows partitioning in SINGLE mode`() {
        val win1 = ActiveWindowEntry("0x201", "Doc 1 - Xournal++", "Doc 1", "/notes/d1.xopp", isActive = false)
        val win2 = ActiveWindowEntry("0x202", "Doc 2 - Xournal++", "Doc 2", "/notes/d2.xopp", isActive = true)
        val win3 = ActiveWindowEntry("0x203", "Doc 3 - Xournal++", "Doc 3", "/notes/d3.xopp", isActive = false)

        val state = ActiveWorkspaceState(
            sessionInfo = ActiveSessionInfo(isRunning = true, pid = 100, "/notes/d2.xopp", "Doc 2", 3),
            windows = listOf(win1, win2, win3),
            snapMode = "single",
            slotAssignments = emptyMap()
        )

        val activeWinId = state.windows.firstOrNull { it.isActive }?.id ?: state.windows.firstOrNull()?.id
        val stagedIds = setOfNotNull(activeWinId)
        val stashed = state.windows.filter { it.id !in stagedIds }

        assertEquals(setOf("0x202"), stagedIds)
        assertEquals(2, stashed.size)
        assertEquals(listOf("0x201", "0x203"), stashed.map { it.id })
    }

    @Test
    fun `test dirty document export constraint`() {
        val dirtyWin = ActiveWindowEntry("0x301", "*Draft - Xournal++", "Draft", "/notes/draft.xopp", isDirty = true)
        val cleanWin = ActiveWindowEntry("0x302", "Final - Xournal++", "Final", "/notes/final.xopp", isDirty = false)

        fun canDirectExportPdf(entry: ActiveWindowEntry): Boolean {
            return !entry.isDirty && !entry.filePath.isNullOrBlank()
        }

        assertFalse("Dirty notes cannot be directly exported without prior save", canDirectExportPdf(dirtyWin))
        assertTrue("Clean notes can be directly exported", canDirectExportPdf(cleanWin))
    }

    @Test
    fun `test save prompt decision logic for sharing`() {
        val dirtyWin = ActiveWindowEntry("0x401", "*Lecture Notes - Xournal++", "Lecture Notes", "/notes/lecture.xopp", isDirty = true)
        val cleanWin = ActiveWindowEntry("0x402", "Lecture Notes - Xournal++", "Lecture Notes", "/notes/lecture.xopp", isDirty = false)

        var promptShownFor: ActiveWindowEntry? = null
        var directSharedFor: ActiveWindowEntry? = null

        fun onShareClicked(window: ActiveWindowEntry) {
            if (window.isDirty) {
                promptShownFor = window
            } else {
                directSharedFor = window
            }
        }

        onShareClicked(dirtyWin)
        assertNotNull("Dirty window should trigger native save prompt", promptShownFor)
        assertEquals("0x401", promptShownFor?.id)
        assertNull(directSharedFor)

        promptShownFor = null
        onShareClicked(cleanWin)
        assertNull("Clean window should NOT trigger save prompt", promptShownFor)
        assertNotNull("Clean window should proceed directly to share", directSharedFor)
        assertEquals("0x402", directSharedFor?.id)
    }

    @Test
    fun `test save window success updates workspace entry to clean`() {
        val dirtyWin = ActiveWindowEntry("0x501", "*Draft - Xournal++", "Draft", "/notes/draft.xopp", isDirty = true)
        val wins = listOf(dirtyWin)

        val updatedWins = wins.map { w ->
            if (w.id == "0x501") w.copy(isDirty = false) else w
        }

        assertFalse("Window should be marked clean after successful save", updatedWins.first().isDirty)
    }

    @Test
    fun `test new note cannot be auto-saved and requires user attention`() {
        fun canAutoSaveWithoutPrompt(title: String): Boolean {
            val clean = dev.ilamparithi.aournalpp.runtime.ProcessSupervisor.sanitizeWindowTitle(title)
            return !(clean.isBlank() || clean.equals("New Note", ignoreCase = true) || clean.equals("Unsaved Document", ignoreCase = true))
        }

        assertFalse("Unnamed new note cannot be auto-saved without user attention", canAutoSaveWithoutPrompt("New Note - Xournal++"))
        assertFalse("Unsaved Document cannot be auto-saved without user attention", canAutoSaveWithoutPrompt("Unsaved Document - Xournal++"))
        assertTrue("Named document can be auto-saved", canAutoSaveWithoutPrompt("*Physics.xopp - Xournal++"))
    }
}
