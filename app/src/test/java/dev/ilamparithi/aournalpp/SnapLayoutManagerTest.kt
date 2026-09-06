package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.ui.snap.DividerOrientation
import dev.ilamparithi.aournalpp.ui.snap.SnapLayoutConfigFile
import dev.ilamparithi.aournalpp.ui.snap.SnapLayoutManager
import dev.ilamparithi.aournalpp.ui.snap.SnapLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SnapLayoutManagerTest {

    private lateinit var manager: SnapLayoutManager

    @Before
    fun setUp() {
        manager = SnapLayoutManager(null)
    }

    @Test
    fun testWindowCountFiltering() {
        // 1 open window: only SINGLE and UNLOCKED
        val modes1 = manager.getAvailableModes(1)
        assertEquals(listOf(SnapLayoutMode.SINGLE, SnapLayoutMode.UNLOCKED), modes1)

        // 2 open windows: SINGLE, UNLOCKED, SPLIT_TWO
        val modes2 = manager.getAvailableModes(2)
        assertEquals(listOf(SnapLayoutMode.SINGLE, SnapLayoutMode.UNLOCKED, SnapLayoutMode.SPLIT_TWO), modes2)

        // 3 open windows: + SPLIT_THREE
        val modes3 = manager.getAvailableModes(3)
        assertEquals(listOf(SnapLayoutMode.SINGLE, SnapLayoutMode.UNLOCKED, SnapLayoutMode.SPLIT_TWO, SnapLayoutMode.SPLIT_THREE), modes3)

        // 4+ open windows: all modes including GRID_FOUR
        val modes4 = manager.getAvailableModes(4)
        assertEquals(listOf(SnapLayoutMode.SINGLE, SnapLayoutMode.UNLOCKED, SnapLayoutMode.SPLIT_TWO, SnapLayoutMode.SPLIT_THREE, SnapLayoutMode.GRID_FOUR), modes4)
    }

    @Test
    fun testSplitTwoLandscapeVsPortrait() {
        manager.setMode(SnapLayoutMode.SPLIT_TWO)

        // Landscape (1920 x 1080)
        val landSlots = manager.calculateGeometries(1920, 1080)
        assertEquals(2, landSlots.size)
        // Left column
        assertEquals(0, landSlots[0].x)
        assertEquals(0, landSlots[0].y)
        assertEquals(960, landSlots[0].width)
        assertEquals(1080, landSlots[0].height)
        // Right column
        assertEquals(960, landSlots[1].x)
        assertEquals(0, landSlots[1].y)
        assertEquals(960, landSlots[1].width)
        assertEquals(1080, landSlots[1].height)

        // Portrait (1080 x 1920)
        val portSlots = manager.calculateGeometries(1080, 1920)
        assertEquals(2, portSlots.size)
        // Top row
        assertEquals(0, portSlots[0].x)
        assertEquals(0, portSlots[0].y)
        assertEquals(1080, portSlots[0].width)
        assertEquals(960, portSlots[0].height)
        // Bottom row
        assertEquals(0, portSlots[1].x)
        assertEquals(960, portSlots[1].y)
        assertEquals(1080, portSlots[1].width)
        assertEquals(960, portSlots[1].height)
    }

    @Test
    fun testSplitThreeStandardAndMirrored() {
        manager.setMode(SnapLayoutMode.SPLIT_THREE, mirrored = false)

        // Landscape Standard: Left (slot 0) | Right Top (slot 1), Right Bottom (slot 2)
        val landSlots = manager.calculateGeometries(2000, 1000)
        assertEquals(3, landSlots.size)
        val slot0 = landSlots.find { it.slotIndex == 0 }!!
        val slot1 = landSlots.find { it.slotIndex == 1 }!!
        val slot2 = landSlots.find { it.slotIndex == 2 }!!

        assertEquals(0, slot0.x)
        assertEquals(1000, slot0.width)
        assertEquals(1000, slot0.height)

        assertEquals(1000, slot1.x)
        assertEquals(0, slot1.y)
        assertEquals(1000, slot1.width)
        assertEquals(500, slot1.height)

        assertEquals(1000, slot2.x)
        assertEquals(500, slot2.y)
        assertEquals(1000, slot2.width)
        assertEquals(500, slot2.height)

        // Landscape Mirrored: Left Top (slot 1), Left Bottom (slot 2) | Right (slot 0)
        manager.toggleMirrored()
        assertTrue(manager.isMirrored)
        val mirSlots = manager.calculateGeometries(2000, 1000)
        val mirSlot0 = mirSlots.find { it.slotIndex == 0 }!!
        val mirSlot1 = mirSlots.find { it.slotIndex == 1 }!!
        val mirSlot2 = mirSlots.find { it.slotIndex == 2 }!!

        // slot 0 is now on the right
        assertEquals(1000, mirSlot0.x)
        assertEquals(0, mirSlot0.y)
        assertEquals(1000, mirSlot0.width)
        assertEquals(1000, mirSlot0.height)

        // slot 1 is on top left
        assertEquals(0, mirSlot1.x)
        assertEquals(0, mirSlot1.y)
        assertEquals(1000, mirSlot1.width)
        assertEquals(500, mirSlot1.height)

        // slot 2 is on bottom left
        assertEquals(0, mirSlot2.x)
        assertEquals(500, mirSlot2.y)
        assertEquals(1000, mirSlot2.width)
        assertEquals(500, mirSlot2.height)
    }

    @Test
    fun testGridFourGeometries() {
        manager.setMode(SnapLayoutMode.GRID_FOUR)
        val slots = manager.calculateGeometries(1000, 1000)
        assertEquals(4, slots.size)

        // Top-Left
        assertEquals(0, slots[0].x)
        assertEquals(0, slots[0].y)
        assertEquals(500, slots[0].width)
        assertEquals(500, slots[0].height)

        // Top-Right
        assertEquals(500, slots[1].x)
        assertEquals(0, slots[1].y)
        assertEquals(500, slots[1].width)
        assertEquals(500, slots[1].height)

        // Bottom-Left
        assertEquals(0, slots[2].x)
        assertEquals(500, slots[2].y)
        assertEquals(500, slots[2].width)
        assertEquals(500, slots[2].height)

        // Bottom-Right
        assertEquals(500, slots[3].x)
        assertEquals(500, slots[3].y)
        assertEquals(500, slots[3].width)
        assertEquals(500, slots[3].height)
    }

    @Test
    fun testGridFourFourHandlebarsIndependentControl() {
        manager.setMode(SnapLayoutMode.GRID_FOUR)
        val dividers = manager.calculateDividerGeometries(1000, 1000)
        assertEquals(4, dividers.size)
        val divMap = dividers.associateBy { it.id }
        assertTrue(divMap.containsKey("v_top"))
        assertTrue(divMap.containsKey("v_bottom"))
        assertTrue(divMap.containsKey("h_left"))
        assertTrue(divMap.containsKey("h_right"))

        // Independently resize v_top to 0.4
        manager.updateDividerRatio("v_top", 0.4f)
        var slots = manager.calculateGeometries(1000, 1000)
        // Top row widths changed
        assertEquals(400, slots[0].width)
        assertEquals(600, slots[1].width)
        // Bottom row widths unchanged at 500
        assertEquals(500, slots[2].width)
        assertEquals(500, slots[3].width)

        // Independently resize h_left to 0.6
        manager.updateDividerRatio("h_left", 0.6f)
        slots = manager.calculateGeometries(1000, 1000)
        // Left column heights changed
        assertEquals(600, slots[0].height)
        assertEquals(400, slots[2].height)
        // Right column heights unchanged at 500
        assertEquals(500, slots[1].height)
        assertEquals(500, slots[3].height)
    }

    @Test
    fun testRatioUpdatesAndClamping() {
        manager.setMode(SnapLayoutMode.SPLIT_TWO)

        // Update to 0.7
        manager.updateDividerRatio("v1", 0.7f)
        var slots = manager.calculateGeometries(1000, 1000)
        assertEquals(700, slots[0].width)
        assertEquals(300, slots[1].width)

        // Clamping to min (0.15)
        manager.updateDividerRatio("v1", 0.05f)
        slots = manager.calculateGeometries(1000, 1000)
        assertEquals(150, slots[0].width)
        assertEquals(850, slots[1].width)

        // Clamping to max (0.85)
        manager.updateDividerRatio("v1", 0.95f)
        slots = manager.calculateGeometries(1000, 1000)
        assertEquals(850, slots[0].width)
        assertEquals(150, slots[1].width)

        // Reset
        manager.resetDividerRatios()
        slots = manager.calculateGeometries(1000, 1000)
        assertEquals(500, slots[0].width)
        assertEquals(500, slots[1].width)
    }

    @Test
    fun testRotationPreservesSpatialOrientationAcrossAllFourOrientations() {
        manager.setMode(SnapLayoutMode.SPLIT_TWO)

        // 0° (Landscape: 1920x1080) -> Vertical split (Left / Right)
        val rot0 = manager.calculateGeometries(1920, 1080)
        assertEquals(960, rot0[0].width)
        assertEquals(1080, rot0[0].height)

        // 90° (Portrait: 1080x1920) -> Horizontal split (Top / Bottom)
        val rot90 = manager.calculateGeometries(1080, 1920)
        assertEquals(1080, rot90[0].width)
        assertEquals(960, rot90[0].height)

        // 180° (Reverse Landscape: 1920x1080) -> Vertical split (Left / Right)
        val rot180 = manager.calculateGeometries(1920, 1080)
        assertEquals(960, rot180[0].width)
        assertEquals(1080, rot180[0].height)

        // 270° (Reverse Portrait: 1080x1920) -> Horizontal split (Top / Bottom)
        val rot270 = manager.calculateGeometries(1080, 1920)
        assertEquals(1080, rot270[0].width)
        assertEquals(960, rot270[0].height)

        // No inversion: slot 0 is ALWAYS the first spatial segment (top or left), slot 1 is ALWAYS the second (bottom or right)
        assertEquals(0, rot0[0].x)
        assertEquals(960, rot0[1].x)
        assertEquals(0, rot90[0].y)
        assertEquals(960, rot90[1].y)
        assertEquals(0, rot180[0].x)
        assertEquals(960, rot180[1].x)
        assertEquals(0, rot270[0].y)
        assertEquals(960, rot270[1].y)
    }

    @Test
    fun testSnapAssignmentsMapping() {
        manager.setMode(SnapLayoutMode.SPLIT_TWO)
        val windows = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = true),
            ProcessSupervisor.X11WindowInfo(id = "win2", title = "Note 2", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win3", title = "Note 3", isActive = false)
        )

        val assignments = manager.buildSnapAssignments(1000, 1000, windows)
        assertEquals(2, assignments.size)
        // Slot 0 assigned to active window (win1)
        assertEquals("win1", assignments[0].windowId)
        assertEquals(0, assignments[0].x)
        assertEquals(500, assignments[0].width)

        // Slot 1 assigned to next window (win2)
        assertEquals("win2", assignments[1].windowId)
        assertEquals(500, assignments[1].x)
        assertEquals(500, assignments[1].width)
    }

    @Test
    fun testExpressionAnchorResolution() {
        val ratios = mapOf("v1" to 0.4f, "h1" to 0.6f)
        assertEquals(0.4f, manager.resolveAnchor("v1", ratios, 0f), 0.001f)
        assertEquals(0.6f, manager.resolveAnchor("1.0 - v1", ratios, 0f), 0.001f)
        assertEquals(0.4f, manager.resolveAnchor("1 - h1", ratios, 0f), 0.001f)
        assertEquals(0.5f, manager.resolveAnchor("v1 + 0.1", ratios, 0f), 0.001f)
    }

    @Test
    fun testDisabledLayoutFiltering() {
        val defaults = SnapLayoutConfigFile.createDefaultConfigurations()
        // Disable SPLIT_TWO
        val modified = defaults.map {
            if (it.id == "split_two") it.copy(enabled = false) else it
        }
        manager.setConfigs(modified)

        // Even with 4 open windows, SPLIT_TWO should not be available
        val modes = manager.getAvailableModes(4)
        assertFalse(modes.contains(SnapLayoutMode.SPLIT_TWO))
        assertTrue(modes.contains(SnapLayoutMode.SPLIT_THREE))
        assertTrue(modes.contains(SnapLayoutMode.GRID_FOUR))
    }

    @Test
    fun testGridFourWindowCloseWithBackgroundWindowReplacesSlot() {
        manager.setMode(SnapLayoutMode.GRID_FOUR)
        manager.assignWindowToSlot(0, "win1")
        manager.assignWindowToSlot(1, "win2")
        manager.assignWindowToSlot(2, "win3")
        manager.assignWindowToSlot(3, "win4")

        // 5 open windows: win1..win4 in slots, win5 in background
        // win2 is closed -> remaining windows: win1, win3, win4, win5 (count = 4 >= 4)
        val remaining = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = true),
            ProcessSupervisor.X11WindowInfo(id = "win3", title = "Note 3", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win4", title = "Note 4", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win5", title = "Note 5", isActive = false)
        )

        val resolution = manager.handleWindowClosed(remaining)
        assertFalse(resolution.modeChanged)
        assertEquals(SnapLayoutMode.GRID_FOUR, resolution.newMode)
        assertEquals(1, resolution.replacedSlotIndex)
        assertEquals("win5", resolution.replacementWindowId)

        // Slot 1 got win5, other slots unchanged
        assertEquals("win1", manager.slotAssignments[0])
        assertEquals("win5", manager.slotAssignments[1])
        assertEquals("win3", manager.slotAssignments[2])
        assertEquals("win4", manager.slotAssignments[3])
    }

    @Test
    fun testWindowCloseWithMultipleBackgroundWindowsRespectsMruOrder() {
        manager.setMode(SnapLayoutMode.GRID_FOUR)
        manager.assignWindowToSlot(0, "win1")
        manager.assignWindowToSlot(1, "win2")
        manager.assignWindowToSlot(2, "win3")
        manager.assignWindowToSlot(3, "win4")

        // 6 windows: win5 and win6 in background.
        // MRU order indicates win6 was focused more recently than win5
        val mru = listOf("win2", "win4", "win1", "win3", "win6", "win5")
        val remaining = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = true),
            ProcessSupervisor.X11WindowInfo(id = "win3", title = "Note 3", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win4", title = "Note 4", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win5", title = "Note 5", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win6", title = "Note 6", isActive = false)
        )

        val resolution = manager.handleWindowClosed(remaining, mruOrder = mru)
        assertFalse(resolution.modeChanged)
        assertEquals(SnapLayoutMode.GRID_FOUR, resolution.newMode)
        assertEquals(1, resolution.replacedSlotIndex)
        assertEquals("win6", resolution.replacementWindowId)
        assertEquals("win6", manager.slotAssignments[1])
    }

    @Test
    fun testGridFourDropsToSplitThreeWhenCountDropsBelowFour() {
        manager.setMode(SnapLayoutMode.GRID_FOUR)
        manager.assignWindowToSlot(0, "win1")
        manager.assignWindowToSlot(1, "win2")
        manager.assignWindowToSlot(2, "win3")
        manager.assignWindowToSlot(3, "win4")

        // win2 is closed, total count drops to 3 (< 4)
        val remaining = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = true),
            ProcessSupervisor.X11WindowInfo(id = "win3", title = "Note 3", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win4", title = "Note 4", isActive = false)
        )

        val resolution = manager.handleWindowClosed(remaining)
        assertTrue(resolution.modeChanged)
        assertEquals(SnapLayoutMode.SPLIT_THREE, resolution.newMode)
        assertEquals(SnapLayoutMode.SPLIT_THREE, manager.activeMode)

        // Surviving windows win1, win3, win4 fill slots 0, 1, 2 of SPLIT_THREE
        assertEquals("win1", manager.slotAssignments[0])
        assertEquals("win3", manager.slotAssignments[1])
        assertEquals("win4", manager.slotAssignments[2])
    }

    @Test
    fun testSplitThreeWindowCloseWithBackgroundWindowReplacesSlot() {
        manager.setMode(SnapLayoutMode.SPLIT_THREE)
        manager.assignWindowToSlot(0, "win1")
        manager.assignWindowToSlot(1, "win2")
        manager.assignWindowToSlot(2, "win3")

        // 4 windows total, win1 in slot 0 closes, win4 in background
        val remaining = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win2", title = "Note 2", isActive = true),
            ProcessSupervisor.X11WindowInfo(id = "win3", title = "Note 3", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win4", title = "Note 4", isActive = false)
        )

        val resolution = manager.handleWindowClosed(remaining)
        assertFalse(resolution.modeChanged)
        assertEquals(SnapLayoutMode.SPLIT_THREE, resolution.newMode)
        assertEquals(0, resolution.replacedSlotIndex)
        assertEquals("win4", resolution.replacementWindowId)

        assertEquals("win4", manager.slotAssignments[0])
        assertEquals("win2", manager.slotAssignments[1])
        assertEquals("win3", manager.slotAssignments[2])
    }

    @Test
    fun testSplitThreeDropsToSplitTwoWhenCountDropsBelowThree() {
        manager.setMode(SnapLayoutMode.SPLIT_THREE)
        manager.assignWindowToSlot(0, "win1")
        manager.assignWindowToSlot(1, "win2")
        manager.assignWindowToSlot(2, "win3")

        // win2 closes, count drops to 2 (< 3)
        val remaining = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = true),
            ProcessSupervisor.X11WindowInfo(id = "win3", title = "Note 3", isActive = false)
        )

        val resolution = manager.handleWindowClosed(remaining)
        assertTrue(resolution.modeChanged)
        assertEquals(SnapLayoutMode.SPLIT_TWO, resolution.newMode)
        assertEquals(SnapLayoutMode.SPLIT_TWO, manager.activeMode)

        // Surviving windows win1 and win3 fill slots 0 and 1
        assertEquals("win1", manager.slotAssignments[0])
        assertEquals("win3", manager.slotAssignments[1])
    }

    @Test
    fun testSplitTwoWindowCloseWithBackgroundWindowReplacesSlot() {
        manager.setMode(SnapLayoutMode.SPLIT_TWO)
        manager.assignWindowToSlot(0, "win1")
        manager.assignWindowToSlot(1, "win2")

        // 3 windows, win1 in slot 0 closes, win3 in background
        val remaining = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win2", title = "Note 2", isActive = true),
            ProcessSupervisor.X11WindowInfo(id = "win3", title = "Note 3", isActive = false)
        )

        val resolution = manager.handleWindowClosed(remaining)
        assertFalse(resolution.modeChanged)
        assertEquals(SnapLayoutMode.SPLIT_TWO, resolution.newMode)
        assertEquals(0, resolution.replacedSlotIndex)
        assertEquals("win3", resolution.replacementWindowId)

        assertEquals("win3", manager.slotAssignments[0])
        assertEquals("win2", manager.slotAssignments[1])
    }

    @Test
    fun testSplitTwoDropsToSingleWhenCountDropsBelowTwo() {
        manager.setMode(SnapLayoutMode.SPLIT_TWO)
        manager.assignWindowToSlot(0, "win1")
        manager.assignWindowToSlot(1, "win2")

        // win1 closes, count drops to 1 (< 2)
        val remaining = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win2", title = "Note 2", isActive = true)
        )

        val resolution = manager.handleWindowClosed(remaining)
        assertTrue(resolution.modeChanged)
        assertEquals(SnapLayoutMode.SINGLE, resolution.newMode)
        assertEquals(SnapLayoutMode.SINGLE, manager.activeMode)
        assertEquals("win2", manager.slotAssignments[0])
    }

    @Test
    fun testBackgroundWindowCloseLeavesSlottedWindowsIntact() {
        manager.setMode(SnapLayoutMode.GRID_FOUR)
        manager.assignWindowToSlot(0, "win1")
        manager.assignWindowToSlot(1, "win2")
        manager.assignWindowToSlot(2, "win3")
        manager.assignWindowToSlot(3, "win4")

        // win5 (background) closes, count drops to 4 (>= 4)
        val remaining = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = true),
            ProcessSupervisor.X11WindowInfo(id = "win2", title = "Note 2", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win3", title = "Note 3", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win4", title = "Note 4", isActive = false)
        )

        val resolution = manager.handleWindowClosed(remaining)
        assertFalse(resolution.modeChanged)
        assertEquals(SnapLayoutMode.GRID_FOUR, resolution.newMode)
        assertEquals(null, resolution.replacedSlotIndex)
        assertEquals(null, resolution.replacementWindowId)

        assertEquals("win1", manager.slotAssignments[0])
        assertEquals("win2", manager.slotAssignments[1])
        assertEquals("win3", manager.slotAssignments[2])
        assertEquals("win4", manager.slotAssignments[3])
    }

    @Test
    fun testBuildSnapAssignmentsTwoPassStability() {
        manager.setMode(SnapLayoutMode.SPLIT_TWO)
        manager.assignWindowToSlot(0, "win1")
        manager.assignWindowToSlot(1, "win2")

        val windows = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win2", title = "Note 2", isActive = true),
            ProcessSupervisor.X11WindowInfo(id = "win3", title = "Note 3", isActive = false)
        )

        val assignments = manager.buildSnapAssignments(1000, 1000, windows)
        assertEquals(2, assignments.size)
        // slot 0 remains win1, slot 1 remains win2 even though win2 is active
        assertEquals("win1", assignments[0].windowId)
        assertEquals("win2", assignments[1].windowId)
    }

    @Test
    fun testAutoFillNthSlotWhenWindowsEqualSlotsForSplitTwo() {
        val windows = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win2", title = "Note 2", isActive = false)
        )

        // Select slot 0 -> slot 1 must auto-fill with win2
        val completed = manager.assignSlotAndAutoFillNthIfExact(
            slotIndex = 0,
            windowId = "win1",
            totalSlots = 2,
            allOpenWindows = windows
        )

        assertTrue(completed)
        assertEquals("win1", manager.slotAssignments[0])
        assertEquals("win2", manager.slotAssignments[1])
    }

    @Test
    fun testAutoFillNthSlotWhenWindowsEqualSlotsForSplitThree() {
        val windows = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win2", title = "Note 2", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win3", title = "Note 3", isActive = false)
        )

        // Select slot 1 with win2
        val step1 = manager.assignSlotAndAutoFillNthIfExact(
            slotIndex = 1,
            windowId = "win2",
            totalSlots = 3,
            allOpenWindows = windows
        )
        assertFalse(step1)
        assertEquals("win2", manager.slotAssignments[1])
        assertEquals(null, manager.slotAssignments[0])
        assertEquals(null, manager.slotAssignments[2])

        // Select slot 0 with win1 -> 1 slot remains (slot 2) -> auto-fill with win3
        val step2 = manager.assignSlotAndAutoFillNthIfExact(
            slotIndex = 0,
            windowId = "win1",
            totalSlots = 3,
            allOpenWindows = windows
        )
        assertTrue(step2)
        assertEquals("win1", manager.slotAssignments[0])
        assertEquals("win2", manager.slotAssignments[1])
        assertEquals("win3", manager.slotAssignments[2])
    }

    @Test
    fun testAutoFillNthSlotWhenWindowsEqualSlotsForGridFour() {
        val windows = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win2", title = "Note 2", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win3", title = "Note 3", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win4", title = "Note 4", isActive = false)
        )

        assertFalse(manager.assignSlotAndAutoFillNthIfExact(0, "win1", 4, windows))
        assertFalse(manager.assignSlotAndAutoFillNthIfExact(2, "win3", 4, windows))
        // 3 slots filled: 0=win1, 2=win3, 3=win4. Slot 1 must auto-fill with win2!
        val step3 = manager.assignSlotAndAutoFillNthIfExact(3, "win4", 4, windows)
        assertTrue(step3)
        assertEquals("win1", manager.slotAssignments[0])
        assertEquals("win2", manager.slotAssignments[1])
        assertEquals("win3", manager.slotAssignments[2])
        assertEquals("win4", manager.slotAssignments[3])
    }

    @Test
    fun testNoAutoFillWhenWindowsGreaterThanSlots() {
        val windows = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win2", title = "Note 2", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win3", title = "Note 3", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win4", title = "Note 4", isActive = false)
        )

        // 4 windows for a 3-slot layout
        assertFalse(manager.assignSlotAndAutoFillNthIfExact(0, "win1", 3, windows))
        val step2 = manager.assignSlotAndAutoFillNthIfExact(1, "win2", 3, windows)
        // Even though 2 of 3 slots are filled, W > n, so slot 2 MUST NOT be auto-filled
        assertFalse(step2)
        assertEquals(null, manager.slotAssignments[2])

        // The remaining unassigned gallery has openWindows - n + 1 = 4 - 3 + 1 = 2 windows
        val assignedIds = manager.slotAssignments.values.toSet()
        val remaining = windows.filter { it.id !in assignedIds }
        assertEquals(2, remaining.size)
        assertEquals(listOf("win3", "win4"), remaining.map { it.id })

        // User explicitly selects the 3rd slot
        val step3 = manager.assignSlotAndAutoFillNthIfExact(2, "win4", 3, windows)
        assertTrue(step3)
        assertEquals("win4", manager.slotAssignments[2])
    }

    @Test
    fun testReassignSlotReplacesAssignmentAndFreesPrevious() {
        val windows = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win2", title = "Note 2", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win3", title = "Note 3", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win4", title = "Note 4", isActive = false)
        )

        // Assign win1 to slot 0
        manager.assignSlotAndAutoFillNthIfExact(0, "win1", 3, windows)
        assertEquals("win1", manager.slotAssignments[0])

        // User changes note on slot 0: select win4
        manager.assignSlotAndAutoFillNthIfExact(0, "win4", 3, windows)
        assertEquals("win4", manager.slotAssignments[0])

        // win1 is now unassigned and available again
        val assignedIds = manager.slotAssignments.values.toSet()
        val remaining = windows.filter { it.id !in assignedIds }
        assertEquals(3, remaining.size)
        assertTrue(remaining.any { it.id == "win1" })
        assertFalse(remaining.any { it.id == "win4" })
    }

    @Test
    fun testRapidConsecutiveWindowClosuresFromGridFourToSingle() {
        manager.setMode(SnapLayoutMode.GRID_FOUR)
        manager.assignWindowToSlot(0, "win1")
        manager.assignWindowToSlot(1, "win2")
        manager.assignWindowToSlot(2, "win3")
        manager.assignWindowToSlot(3, "win4")

        // 1. Rapid close win4 -> drops to SPLIT_THREE
        val openAfterWin4 = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = true),
            ProcessSupervisor.X11WindowInfo(id = "win2", title = "Note 2", isActive = false),
            ProcessSupervisor.X11WindowInfo(id = "win3", title = "Note 3", isActive = false)
        )
        val res1 = manager.handleWindowClosed(openAfterWin4)
        assertTrue(res1.modeChanged)
        assertEquals(SnapLayoutMode.SPLIT_THREE, res1.newMode)
        assertEquals(SnapLayoutMode.SPLIT_THREE, manager.activeMode)
        assertEquals(mapOf(0 to "win1", 1 to "win2", 2 to "win3"), manager.slotAssignments)

        // 2. Rapid close win3 immediately -> drops to SPLIT_TWO
        val openAfterWin3 = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = true),
            ProcessSupervisor.X11WindowInfo(id = "win2", title = "Note 2", isActive = false)
        )
        val res2 = manager.handleWindowClosed(openAfterWin3)
        assertTrue(res2.modeChanged)
        assertEquals(SnapLayoutMode.SPLIT_TWO, res2.newMode)
        assertEquals(SnapLayoutMode.SPLIT_TWO, manager.activeMode)
        assertEquals(mapOf(0 to "win1", 1 to "win2"), manager.slotAssignments)

        // 3. Rapid close win2 immediately -> drops to SINGLE
        val openAfterWin2 = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = true)
        )
        val res3 = manager.handleWindowClosed(openAfterWin2)
        assertTrue(res3.modeChanged)
        assertEquals(SnapLayoutMode.SINGLE, res3.newMode)
        assertEquals(SnapLayoutMode.SINGLE, manager.activeMode)
        assertEquals("win1", manager.slotAssignments[0])
    }

    @Test
    fun testRapidMultiWindowClosureFromGridFourToSplitTwo() {
        manager.setMode(SnapLayoutMode.GRID_FOUR)
        manager.assignWindowToSlot(0, "win1")
        manager.assignWindowToSlot(1, "win2")
        manager.assignWindowToSlot(2, "win3")
        manager.assignWindowToSlot(3, "win4")

        // win3 and win4 close simultaneously -> only win1 and win2 remain
        val remaining = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = true),
            ProcessSupervisor.X11WindowInfo(id = "win2", title = "Note 2", isActive = false)
        )

        val res = manager.handleWindowClosed(remaining)
        assertTrue(res.modeChanged)
        assertEquals(SnapLayoutMode.SPLIT_TWO, res.newMode)
        assertEquals(SnapLayoutMode.SPLIT_TWO, manager.activeMode)
        assertEquals(mapOf(0 to "win1", 1 to "win2"), manager.slotAssignments)

        val assignments = manager.buildSnapAssignments(1000, 1000, remaining)
        assertEquals(2, assignments.size)
        assertEquals("win1", assignments[0].windowId)
        assertEquals("win2", assignments[1].windowId)
    }

    @Test
    fun testRapidMultiWindowClosureFromSplitThreeToSingle() {
        manager.setMode(SnapLayoutMode.SPLIT_THREE)
        manager.assignWindowToSlot(0, "win1")
        manager.assignWindowToSlot(1, "win2")
        manager.assignWindowToSlot(2, "win3")

        // win2 and win3 close simultaneously
        val remaining = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = true)
        )

        val res = manager.handleWindowClosed(remaining)
        assertTrue(res.modeChanged)
        assertEquals(SnapLayoutMode.SINGLE, res.newMode)
        assertEquals(SnapLayoutMode.SINGLE, manager.activeMode)
        assertEquals("win1", manager.slotAssignments[0])
    }

    @Test
    fun testSlotAssignmentsPurgesStaleIndicesAndClosedWindows() {
        manager.setMode(SnapLayoutMode.SPLIT_TWO)
        // Manually introduce stale slots from a prior layout
        manager.assignWindowToSlot(0, "win1")
        manager.assignWindowToSlot(1, "win2")
        manager.assignWindowToSlot(2, "win3")
        manager.assignWindowToSlot(3, "win4")

        val openWindows = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = true),
            ProcessSupervisor.X11WindowInfo(id = "win2", title = "Note 2", isActive = false)
        )

        val assignments = manager.buildSnapAssignments(1000, 1000, openWindows)
        assertEquals(2, assignments.size)
        assertFalse(manager.slotAssignments.containsKey(2))
        assertFalse(manager.slotAssignments.containsKey(3))
        assertEquals(mapOf(0 to "win1", 1 to "win2"), manager.slotAssignments)
    }

    @Test
    fun testDesynchronizedWindowCountRecovery() {
        // Active mode is SPLIT_THREE, but only 2 windows exist
        manager.setMode(SnapLayoutMode.SPLIT_THREE)
        manager.assignWindowToSlot(0, "win1")
        manager.assignWindowToSlot(1, "win2")
        manager.assignWindowToSlot(2, "win3")

        val openWindows = listOf(
            ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1", isActive = true),
            ProcessSupervisor.X11WindowInfo(id = "win2", title = "Note 2", isActive = false)
        )

        val res = manager.handleWindowClosed(openWindows)
        assertTrue(res.modeChanged)
        assertEquals(SnapLayoutMode.SPLIT_TWO, res.newMode)
        assertEquals(SnapLayoutMode.SPLIT_TWO, manager.activeMode)
        assertEquals(mapOf(0 to "win1", 1 to "win2"), manager.slotAssignments)
    }
}

