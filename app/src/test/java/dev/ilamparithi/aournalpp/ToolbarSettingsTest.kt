package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.data.X11Preferences
import dev.ilamparithi.aournalpp.ui.STANDARD_TOOLBAR_PRESETS
import dev.ilamparithi.aournalpp.ui.settings.SettingsCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class ToolbarSettingsTest {

    @Test
    fun testSettingsCategoryToolbar() {
        val category = SettingsCategory.TOOLBAR
        assertEquals("Floating Toolbar", category.title)
        assertTrue(category.description.contains("Placement"))
        assertTrue(category.description.contains("collapse"))
        assertTrue(category.description.contains("toolbar controls"))
    }

    @Test
    fun testStandardToolbarPresetsCoordinates() {
        val topCenter = STANDARD_TOOLBAR_PRESETS.firstOrNull { it.id == "top_center" }
        assertNotNull(topCenter)
        assertEquals(0.5f, topCenter!!.normX, 0.001f)
        assertEquals(0.0f, topCenter.normY, 0.001f)

        val topLeft = STANDARD_TOOLBAR_PRESETS.firstOrNull { it.id == "top_left" }
        assertNotNull(topLeft)
        assertEquals(0.0f, topLeft!!.normX, 0.001f)
        assertEquals(0.0f, topLeft.normY, 0.001f)

        val topRight = STANDARD_TOOLBAR_PRESETS.firstOrNull { it.id == "top_right" }
        assertNotNull(topRight)
        assertEquals(1.0f, topRight!!.normX, 0.001f)
        assertEquals(0.0f, topRight.normY, 0.001f)

        val bottomCenter = STANDARD_TOOLBAR_PRESETS.firstOrNull { it.id == "bottom_center" }
        assertNotNull(bottomCenter)
        assertEquals(0.5f, bottomCenter!!.normX, 0.001f)
        assertEquals(1.0f, bottomCenter.normY, 0.001f)

        val bottomLeft = STANDARD_TOOLBAR_PRESETS.firstOrNull { it.id == "bottom_left" }
        assertNotNull(bottomLeft)
        assertEquals(0.0f, bottomLeft!!.normX, 0.001f)
        assertEquals(1.0f, bottomLeft.normY, 0.001f)

        val bottomRight = STANDARD_TOOLBAR_PRESETS.firstOrNull { it.id == "bottom_right" }
        assertNotNull(bottomRight)
        assertEquals(1.0f, bottomRight!!.normX, 0.001f)
        assertEquals(1.0f, bottomRight.normY, 0.001f)
    }

    @Test
    fun testCustomCoordinatePercentageFormatting() {
        val normX = 0.25f
        val normY = 0.75f
        val xPercent = (normX * 100).roundToInt()
        val yPercent = (normY * 100).roundToInt()
        assertEquals(25, xPercent)
        assertEquals(75, yPercent)
    }

    @Test
    fun testAutoCollapseTimeoutValidation() {
        fun isValidTimeout(input: String): Boolean {
            val parsed = input.toIntOrNull() ?: return false
            return parsed in 500..60000
        }

        assertTrue(isValidTimeout("500"))
        assertTrue(isValidTimeout("5000"))
        assertTrue(isValidTimeout("60000"))
        assertFalse(isValidTimeout("499"))
        assertFalse(isValidTimeout("60001"))
        assertFalse(isValidTimeout("invalid"))
        assertFalse(isValidTimeout("-100"))
    }

    @Test
    fun testAutoCollapseSliderConversion() {
        val ms = 5000
        val seconds = (ms / 1000f).coerceIn(1f, 30f)
        assertEquals(5f, seconds, 0.001f)

        val newSec = 7f
        val calculatedMs = (newSec * 1000).roundToInt()
        assertEquals(7000, calculatedMs)
    }

    @Test
    fun testCloseButtonBehaviors() {
        assertEquals("foreground", X11Preferences.CLOSE_BEHAVIOR_FOREGROUND)
        assertEquals("all_sequential", X11Preferences.CLOSE_BEHAVIOR_ALL_SEQUENTIAL)
    }

    @Test
    fun testAllToolbarItemPreferenceKeysExist() {
        val keys = listOf(
            X11Preferences.KEY_TOOLBAR_SHOW_TITLE,
            X11Preferences.KEY_TOOLBAR_SHOW_BACK,
            X11Preferences.KEY_TOOLBAR_SHOW_CLOSE,
            X11Preferences.KEY_TOOLBAR_SHOW_WINDOW_SWITCHER,
            X11Preferences.KEY_TOOLBAR_SHOW_SNAP_LAYOUTS,
            X11Preferences.KEY_TOOLBAR_SHOW_KEYBOARD,
            X11Preferences.KEY_TOOLBAR_SHOW_DRAG_HANDLE,
            X11Preferences.KEY_SHOW_STYLUS_CLICK_OVERRIDE,
            X11Preferences.KEY_TOOLBAR_SHOW_TOUCH_STYLUS,
            X11Preferences.KEY_DISABLE_TOUCH_STYLUS_ON_STYLUS_HOVER,
            X11Preferences.KEY_REMEMBER_FINGER_AS_STYLUS_STATE,
            X11Preferences.KEY_TOOLBAR_SHOW_CUT,
            X11Preferences.KEY_TOOLBAR_SHOW_COPY,
            X11Preferences.KEY_TOOLBAR_SHOW_PASTE,
            X11Preferences.KEY_TOOLBAR_SHOW_IMAGE,
            X11Preferences.KEY_TOP_BAR_CENTER_WITHIN_BOUNDS,
            X11Preferences.KEY_TOOLBAR_START_COLLAPSED,
            X11Preferences.KEY_TOOLBAR_ALWAYS_SHOW_FILE_NAME,
            X11Preferences.KEY_TOOLBAR_PIN_BUTTON_MODE,
            X11Preferences.KEY_TOOLBAR_AUTO_COLLAPSE_TIMEOUT_MS,
            X11Preferences.KEY_TOOLBAR_STYLUS_HOVER_EXPANDS
        )
        for (key in keys) {
            assertTrue("Preference key should not be blank: $key", key.isNotBlank())
        }
    }
}
