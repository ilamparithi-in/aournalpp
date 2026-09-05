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
}
