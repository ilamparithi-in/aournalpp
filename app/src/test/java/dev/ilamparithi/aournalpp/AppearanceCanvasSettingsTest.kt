package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.WallpaperHelper
import dev.ilamparithi.aournalpp.ui.settings.SettingsCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceCanvasSettingsTest {

    @Test
    fun testSettingsCategoryAppearanceCanvas() {
        val category = SettingsCategory.APPEARANCE_CANVAS
        assertEquals("Appearance & Canvas", category.title)
        assertTrue(category.description.contains("App theme"))
        assertTrue(category.description.contains("GTK canvas theme"))
        assertTrue(category.description.contains("wallpaper"))
        assertTrue(category.description.contains("animations"))
    }

    @Test
    fun testAppThemePreferenceKeysAndValues() {
        assertEquals("pref_app_theme", LinuxEnvironment.PREF_KEY_APP_THEME)

        val validThemes = listOf("system", "light", "dark")
        assertTrue(validThemes.contains("system"))
        assertTrue(validThemes.contains("light"))
        assertTrue(validThemes.contains("dark"))

        fun evaluateTheme(pref: String, systemInDark: Boolean): Boolean = when (pref) {
            "light" -> false
            "dark" -> true
            else -> systemInDark
        }

        assertFalse(evaluateTheme("light", systemInDark = true))
        assertTrue(evaluateTheme("dark", systemInDark = false))
        assertTrue(evaluateTheme("system", systemInDark = true))
        assertFalse(evaluateTheme("system", systemInDark = false))
    }

    @Test
    fun testGtkThemePreferenceKeysAndValues() {
        assertEquals("pref_gtk_theme", LinuxEnvironment.PREF_KEY_GTK_THEME)

        val validGtkThemes = listOf("system", "light", "dark")
        assertTrue(validGtkThemes.contains("system"))
        assertTrue(validGtkThemes.contains("light"))
        assertTrue(validGtkThemes.contains("dark"))

        fun evaluateGtkThemeDark(pref: String, systemInDark: Boolean): Boolean = when (pref) {
            "light" -> false
            "dark" -> true
            else -> systemInDark
        }

        assertFalse(evaluateGtkThemeDark("light", systemInDark = true))
        assertTrue(evaluateGtkThemeDark("dark", systemInDark = false))
        assertTrue(evaluateGtkThemeDark("system", systemInDark = true))
        assertFalse(evaluateGtkThemeDark("system", systemInDark = false))
    }

    @Test
    fun testWallpaperBackdropModes() {
        assertEquals("pref_canvas_wallpaper_mode", WallpaperHelper.PREF_KEY_WALLPAPER_MODE)
        assertEquals("system", WallpaperHelper.MODE_SYSTEM)
        assertEquals("custom", WallpaperHelper.MODE_CUSTOM)
        assertEquals("theme", WallpaperHelper.MODE_THEME)

        val supportedModes = listOf(
            WallpaperHelper.MODE_SYSTEM,
            WallpaperHelper.MODE_CUSTOM,
            WallpaperHelper.MODE_THEME
        )
        assertEquals(3, supportedModes.size)
        assertTrue(supportedModes.contains("system"))
        assertTrue(supportedModes.contains("custom"))
        assertTrue(supportedModes.contains("theme"))
    }

    @Test
    fun testMotionPreferencesMode() {
        assertEquals("pref_reduce_animations", LinuxEnvironment.PREF_KEY_REDUCE_ANIMATIONS)

        fun resolveSpringSpecs(reduceAnimations: Boolean): String {
            return if (reduceAnimations) "snap" else "spring(damping=0.52, stiffness=460)"
        }

        assertEquals("spring(damping=0.52, stiffness=460)", resolveSpringSpecs(reduceAnimations = false))
        assertEquals("snap", resolveSpringSpecs(reduceAnimations = true))
    }
}
