package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.data.X11Preferences
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
    }

    @Test
    fun testStylusHoverCollapseCooldown() {
        val cooldownMs = 600L
        val collapseTime = 10000L

        fun canExpandOnStylusHover(currentTime: Long, lastCollapseTime: Long): Boolean {
            return (currentTime - lastCollapseTime) > cooldownMs
        }

        // Within cooldown (e.g. pen lifting immediately after tapping collapse button)
        assertFalse(canExpandOnStylusHover(collapseTime + 50L, collapseTime))
        assertFalse(canExpandOnStylusHover(collapseTime + 300L, collapseTime))
        assertFalse(canExpandOnStylusHover(collapseTime + 600L, collapseTime))

        // After cooldown expires (user intentionally hovers pen over collapsed pill)
        assertTrue(canExpandOnStylusHover(collapseTime + 601L, collapseTime))
        assertTrue(canExpandOnStylusHover(collapseTime + 1500L, collapseTime))
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
