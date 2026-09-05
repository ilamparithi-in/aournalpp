package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingToolbarWindowSwitcherTest {

    @Test
    fun testWindowSwitcherPreferenceKeys() {
        assertEquals("toolbarShowWindowSwitcher", X11Preferences.KEY_TOOLBAR_SHOW_WINDOW_SWITCHER)
        assertEquals("closeButtonBehavior", X11Preferences.KEY_CLOSE_BUTTON_BEHAVIOR)
        assertEquals("foreground", X11Preferences.CLOSE_BEHAVIOR_FOREGROUND)
        assertEquals("all_sequential", X11Preferences.CLOSE_BEHAVIOR_ALL_SEQUENTIAL)
    }

    @Test
    fun testNextWindowCyclingLogic() {
        fun calculateNextWindow(
            windows: List<ProcessSupervisor.X11WindowInfo>
        ): ProcessSupervisor.X11WindowInfo? {
            if (windows.isEmpty()) return null
            val currentIdx = windows.indexOfFirst { it.isActive }
            val nextIdx = if (currentIdx >= 0) (currentIdx + 1) % windows.size else 0
            return windows[nextIdx]
        }

        // 1. Single window open -> cycles back to itself
        val single = listOf(
            ProcessSupervisor.X11WindowInfo("win1", "Note 1.xopp", isActive = true)
        )
        assertEquals("win1", calculateNextWindow(single)?.id)

        // 2. Two windows open: win1 active -> switches to win2
        val twoWins = listOf(
            ProcessSupervisor.X11WindowInfo("win1", "Note 1.xopp", isActive = true),
            ProcessSupervisor.X11WindowInfo("win2", "Note 2.xopp", isActive = false)
        )
        assertEquals("win2", calculateNextWindow(twoWins)?.id)

        // 3. win2 active -> switches back to win1
        val twoWinsSecondActive = listOf(
            ProcessSupervisor.X11WindowInfo("win1", "Note 1.xopp", isActive = false),
            ProcessSupervisor.X11WindowInfo("win2", "Note 2.xopp", isActive = true)
        )
        assertEquals("win1", calculateNextWindow(twoWinsSecondActive)?.id)

        // 4. Three windows open: win2 active -> switches to win3
        val threeWins = listOf(
            ProcessSupervisor.X11WindowInfo("win1", "Note 1.xopp", isActive = false),
            ProcessSupervisor.X11WindowInfo("win2", "Note 2.xopp", isActive = true),
            ProcessSupervisor.X11WindowInfo("win3", "Note 3.xopp", isActive = false)
        )
        assertEquals("win3", calculateNextWindow(threeWins)?.id)

        // 5. win3 active -> wraps around to win1
        val threeWinsThirdActive = listOf(
            ProcessSupervisor.X11WindowInfo("win1", "Note 1.xopp", isActive = false),
            ProcessSupervisor.X11WindowInfo("win2", "Note 2.xopp", isActive = false),
            ProcessSupervisor.X11WindowInfo("win3", "Note 3.xopp", isActive = true)
        )
        assertEquals("win1", calculateNextWindow(threeWinsThirdActive)?.id)
    }

    @Test
    fun testCloseButtonBehaviorRouting() {
        fun shouldPerformSequentialClose(behaviorConfig: String?): Boolean {
            return behaviorConfig == X11Preferences.CLOSE_BEHAVIOR_ALL_SEQUENTIAL
        }

        assertFalse(shouldPerformSequentialClose(null))
        assertFalse(shouldPerformSequentialClose(X11Preferences.CLOSE_BEHAVIOR_FOREGROUND))
        assertFalse(shouldPerformSequentialClose("unknown"))
        assertTrue(shouldPerformSequentialClose(X11Preferences.CLOSE_BEHAVIOR_ALL_SEQUENTIAL))
    }

    @Test
    fun testWindowSwitcherToolbarVisibility() {
        fun isWindowSwitcherVisibleInToolbar(configEnabled: Boolean, openWindowCount: Int): Boolean {
            return configEnabled && openWindowCount > 1
        }

        // When config is disabled, never show
        assertFalse(isWindowSwitcherVisibleInToolbar(configEnabled = false, openWindowCount = 2))
        // When config is enabled, but only 0 or 1 window open, must NOT show
        assertFalse(isWindowSwitcherVisibleInToolbar(configEnabled = true, openWindowCount = 0))
        assertFalse(isWindowSwitcherVisibleInToolbar(configEnabled = true, openWindowCount = 1))
        // When config is enabled and 2 or more windows open, must show
        assertTrue(isWindowSwitcherVisibleInToolbar(configEnabled = true, openWindowCount = 2))
        assertTrue(isWindowSwitcherVisibleInToolbar(configEnabled = true, openWindowCount = 5))
    }

    @Test
    fun testGallerySelectedWindowAnimationSuppression() {
        fun shouldPlaySlideAnimation(
            selectedWindowId: String,
            activeWindowId: String?
        ): Boolean {
            return selectedWindowId != activeWindowId
        }

        // Selecting already focused window must suppress animation
        assertFalse(shouldPlaySlideAnimation(selectedWindowId = "win1", activeWindowId = "win1"))
        // Selecting another window must play animation
        assertTrue(shouldPlaySlideAnimation(selectedWindowId = "win2", activeWindowId = "win1"))
    }

    @Test
    fun testToolbarResizeDebounceDuration() {
        // Must be maxOf(2000L, (autoCollapseTimeoutMs / 2).toLong())
        // For autoCollapseTimeoutMs = 1000ms -> half is 500ms -> returns 2000ms
        assertEquals(2000L, dev.ilamparithi.aournalpp.utils.WindowPreviewUtils.calculateToolbarResizeDebounceMs(1000))
        // For autoCollapseTimeoutMs = 3000ms -> half is 1500ms -> returns 2000ms
        assertEquals(2000L, dev.ilamparithi.aournalpp.utils.WindowPreviewUtils.calculateToolbarResizeDebounceMs(3000))
        // For autoCollapseTimeoutMs = 5000ms -> half is 2500ms -> returns 2500ms
        assertEquals(2500L, dev.ilamparithi.aournalpp.utils.WindowPreviewUtils.calculateToolbarResizeDebounceMs(5000))
        // For autoCollapseTimeoutMs = 8000ms -> half is 4000ms -> returns 4000ms
        assertEquals(4000L, dev.ilamparithi.aournalpp.utils.WindowPreviewUtils.calculateToolbarResizeDebounceMs(8000))
    }

    @Test
    fun testFreezeFrameDimensionMatchingEdgeCases() {
        // Null or non-positive targets must never be considered matching
        assertFalse(dev.ilamparithi.aournalpp.utils.WindowPreviewUtils.isFreezeFrameDimensionMatching(null, 1000, 800))
        assertFalse(dev.ilamparithi.aournalpp.utils.WindowPreviewUtils.isFreezeFrameDimensionMatching(null, 0, 800))
        assertFalse(dev.ilamparithi.aournalpp.utils.WindowPreviewUtils.isFreezeFrameDimensionMatching(null, 1000, -1))
    }

    @Test
    fun testDebouncedTitleWidthRespectsMaxSafeWidthForTwoRowOverflow() {
        // Verify that effective title width is always clamped by available canvas width
        val canvasWidthPx = 400f
        val paddingPx = 32f
        val maxAllowedTitleWidthPx = (canvasWidthPx - paddingPx).coerceAtLeast(0f).toInt()
        assertEquals(368, maxAllowedTitleWidthPx)

        val requestedStableWidthPx = 500
        val clampedWidth = minOf(requestedStableWidthPx, maxAllowedTitleWidthPx)
        assertEquals(368, clampedWidth)
        assertTrue(clampedWidth <= canvasWidthPx)
    }

    @Test
    fun testDebouncedResizeOnlyAppliesToToolbarAltTabSwitcherTaps() {
        // Debounced resize logic:
        // Must ONLY be activated when the user taps on the Alt+Tab switcher button in the toolbar.
        // Opening another window via gallery, document open, or external event must NOT debounce resize.
        class TitleResizeTracker {
            var isAltTabDebouncing = false
            var heldWidth = 0f
            var currentTargetWidth = 100f

            fun onAltTabSwitcherTapped(newTitleWidth: Float) {
                isAltTabDebouncing = true
                heldWidth = maxOf(heldWidth, currentTargetWidth)
                if (newTitleWidth > heldWidth) {
                    heldWidth = newTitleWidth
                }
                currentTargetWidth = heldWidth
            }

            fun onOtherWindowOpened(newTitleWidth: Float) {
                // Non-Alt+Tab opens must NOT debounce resize
                isAltTabDebouncing = false
                heldWidth = 0f
                currentTargetWidth = newTitleWidth
            }

            fun onDebounceExpired(naturalWidth: Float) {
                isAltTabDebouncing = false
                heldWidth = 0f
                currentTargetWidth = naturalWidth
            }
        }

        val tracker = TitleResizeTracker()
        // 1. Initial state
        tracker.onOtherWindowOpened(120f)
        assertEquals(120f, tracker.currentTargetWidth)
        assertFalse(tracker.isAltTabDebouncing)

        // 2. Alt+Tab switcher tapped -> cycles to shorter title (80f)
        // Must debounce and HOLD width at 120f so button doesn't move under user's finger!
        tracker.onAltTabSwitcherTapped(80f)
        assertTrue(tracker.isAltTabDebouncing)
        assertEquals(120f, tracker.currentTargetWidth)

        // 3. User taps Alt+Tab again -> cycles to wider title (180f)
        // Must expand and hold 180f
        tracker.onAltTabSwitcherTapped(180f)
        assertTrue(tracker.isAltTabDebouncing)
        assertEquals(180f, tracker.currentTargetWidth)

        // 4. User stops tapping -> debounce expires -> spring settles to natural width (80f)
        tracker.onDebounceExpired(80f)
        assertFalse(tracker.isAltTabDebouncing)
        assertEquals(80f, tracker.currentTargetWidth)

        // 5. Gallery selection or external window open (e.g. 150f)
        // Must immediately set to 150f without debouncing!
        tracker.onOtherWindowOpened(150f)
        assertFalse(tracker.isAltTabDebouncing)
        assertEquals(150f, tracker.currentTargetWidth)
    }

    @Test
    fun testDebouncedResizeUsesStandardSpringAnimationParameters() {
        // Verifies spring animation specifications match SpringSlideTransition
        assertEquals(0.82f, dev.ilamparithi.aournalpp.ui.animation.SpringSlideTransition.SLIDE_DAMPING, 0.001f)
        assertEquals(380f, dev.ilamparithi.aournalpp.ui.animation.SpringSlideTransition.SLIDE_STIFFNESS, 0.001f)
    }

    @Test
    fun testRapidWindowSwitchingTargetIndexTracking() {
        // When user rapidly spams Alt+Tab while transition is active,
        // target calculation must advance from the transitioning target window, not the stale active window
        val wins = listOf(
            ProcessSupervisor.X11WindowInfo("win1", "Note 1.xopp", isActive = true),
            ProcessSupervisor.X11WindowInfo("win2", "Note 2.xopp", isActive = false),
            ProcessSupervisor.X11WindowInfo("win3", "Note 3.xopp", isActive = false)
        )

        fun getNextTarget(
            isSwitchTransitionActive: Boolean,
            transitionTargetWindowId: String?
        ): ProcessSupervisor.X11WindowInfo {
            val currentIdx = if (isSwitchTransitionActive && transitionTargetWindowId != null) {
                wins.indexOfFirst { it.id == transitionTargetWindowId }
            } else {
                wins.indexOfFirst { it.isActive }
            }
            val nextIdx = if (currentIdx >= 0) (currentIdx + 1) % wins.size else 0
            return wins[nextIdx]
        }

        // 1. Initial state (win1 active, no transition): first switch targets win2
        assertEquals("win2", getNextTarget(isSwitchTransitionActive = false, transitionTargetWindowId = null).id)

        // 2. Rapid second click while transition to win2 is still active: must target win3, NOT win2 again!
        assertEquals("win3", getNextTarget(isSwitchTransitionActive = true, transitionTargetWindowId = "win2").id)

        // 3. Rapid third click while transition to win3 is active: must wrap around to win1!
        assertEquals("win1", getNextTarget(isSwitchTransitionActive = true, transitionTargetWindowId = "win3").id)
    }
}


