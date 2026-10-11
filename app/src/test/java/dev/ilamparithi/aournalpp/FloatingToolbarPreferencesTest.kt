package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.ui.canvas.FloatingToolbarUtils
import androidx.compose.ui.input.pointer.PointerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingToolbarPreferencesTest {

    @Test
    fun testToolbarPinButtonModeKey() {
        assertEquals("toolbarPinButtonMode", X11Preferences.KEY_TOOLBAR_PIN_BUTTON_MODE)
        assertEquals("toolbarAutoCollapseTimeoutMs", X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS)
        assertEquals("toolbarStylusHoverExpands", X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS)
        assertEquals("toolbarShowSync", X11Preferences.KEY_TOOLBAR_SHOW_SYNC)
    }

    @Test
    fun testStylusHoverCollapseCooldown() {
        val collapseTime = 10000L

        // Within cooldown (e.g. pen lifting immediately after tapping collapse button)
        assertFalse(FloatingToolbarUtils.canExpandOnHover(collapseTime + 50L, collapseTime))
        assertFalse(FloatingToolbarUtils.canExpandOnHover(collapseTime + 300L, collapseTime))
        assertFalse(FloatingToolbarUtils.canExpandOnHover(collapseTime + 600L, collapseTime))

        // After cooldown expires (user intentionally hovers pen over collapsed pill)
        assertTrue(FloatingToolbarUtils.canExpandOnHover(collapseTime + 601L, collapseTime))
        assertTrue(FloatingToolbarUtils.canExpandOnHover(collapseTime + 1500L, collapseTime))
    }

    @Test
    fun testHoverPointerTypes() {
        assertTrue(FloatingToolbarUtils.isHoverPointerType(PointerType.Stylus))
        assertTrue(FloatingToolbarUtils.isHoverPointerType(PointerType.Eraser))
        assertTrue(FloatingToolbarUtils.isHoverPointerType(PointerType.Mouse))
        assertFalse(FloatingToolbarUtils.isHoverPointerType(PointerType.Touch))
        assertFalse(FloatingToolbarUtils.isHoverPointerType(PointerType.Unknown))
    }

    @Test
    fun testShouldAutoCollapseInhibitedWhileHovered() {
        // While hovered, auto-collapse must NEVER trigger
        assertFalse(
            FloatingToolbarUtils.shouldAutoCollapse(
                pinButtonMode = true,
                isHeaderExpanded = true,
                isPinned = false,
                isSnapDropdownOpen = false,
                isHovered = true
            )
        )

        // When unhovered, auto-collapse is enabled for unpinned expanded toolbar in pin mode
        assertTrue(
            FloatingToolbarUtils.shouldAutoCollapse(
                pinButtonMode = true,
                isHeaderExpanded = true,
                isPinned = false,
                isSnapDropdownOpen = false,
                isHovered = false
            )
        )

        // When pinned, auto-collapse is disabled even if unhovered
        assertFalse(
            FloatingToolbarUtils.shouldAutoCollapse(
                pinButtonMode = true,
                isHeaderExpanded = true,
                isPinned = true,
                isSnapDropdownOpen = false,
                isHovered = false
            )
        )

        // When collapsed, auto-collapse does not apply
        assertFalse(
            FloatingToolbarUtils.shouldAutoCollapse(
                pinButtonMode = true,
                isHeaderExpanded = false,
                isPinned = false,
                isSnapDropdownOpen = false,
                isHovered = false
            )
        )

        // When pinButtonMode is false, auto-collapse is disabled
        assertFalse(
            FloatingToolbarUtils.shouldAutoCollapse(
                pinButtonMode = false,
                isHeaderExpanded = true,
                isPinned = false,
                isSnapDropdownOpen = false,
                isHovered = false
            )
        )

        // When snap dropdown is open, auto-collapse is disabled
        assertFalse(
            FloatingToolbarUtils.shouldAutoCollapse(
                pinButtonMode = true,
                isHeaderExpanded = true,
                isPinned = false,
                isSnapDropdownOpen = true,
                isHovered = false
            )
        )
    }

    @Test
    fun testPreferenceExtraSerializationMapping() {
        // Simulates serialize logic in notifyChanged and deserialize in CanvasCommandReceiver
        val simulatedMap = mapOf<String, Any>(
            X11Preferences.KEY_TOOLBAR_PIN_BUTTON_MODE to true,
            X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS to 4000,
            X11Preferences.KEY_TOOLBAR_POS_X_RATIO to 0.75f,
            X11Preferences.KEY_TOOLBAR_POSITION_PRESET to "top_right"
        )

        fun determineType(value: Any): String = when (value) {
            is Boolean -> "boolean"
            is Int -> "int"
            is Float -> "float"
            is Long -> "long"
            is String -> "string"
            else -> "unknown"
        }

        assertEquals("boolean", determineType(simulatedMap[X11Preferences.KEY_TOOLBAR_PIN_BUTTON_MODE]!!))
        assertEquals("int", determineType(simulatedMap[X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS]!!))
        assertEquals("float", determineType(simulatedMap[X11Preferences.KEY_TOOLBAR_POS_X_RATIO]!!))
        assertEquals("string", determineType(simulatedMap[X11Preferences.KEY_TOOLBAR_POSITION_PRESET]!!))
    }
}
