package dev.ilamparithi.aournalpp.runtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowOrientationAdvisorTest {

    @Test
    fun `test canFitFullscreen respects UI scaling factors`() {
        // Base Preferences requisition is 1024x740
        // At 1.0x scale:
        assertTrue(WindowOrientationAdvisor.canFitPreferencesFullscreen(1024, 740, 1.0f))
        assertFalse(WindowOrientationAdvisor.canFitPreferencesFullscreen(1023, 740, 1.0f))
        assertFalse(WindowOrientationAdvisor.canFitPreferencesFullscreen(1024, 739, 1.0f))

        // At 1.25x scale: required is ceil(1024 * 1.25) = 1280, ceil(740 * 1.25) = 925
        assertTrue(WindowOrientationAdvisor.canFitPreferencesFullscreen(1280, 925, 1.25f))
        assertFalse(WindowOrientationAdvisor.canFitPreferencesFullscreen(1200, 925, 1.25f))

        // At 1.5x scale: required is ceil(1024 * 1.5) = 1536, ceil(740 * 1.5) = 1110
        assertTrue(WindowOrientationAdvisor.canFitPreferencesFullscreen(1600, 1200, 1.5f))
        assertFalse(WindowOrientationAdvisor.canFitPreferencesFullscreen(1500, 1200, 1.5f))

        // At 2.0x scale: required is 2048x1480
        assertFalse(WindowOrientationAdvisor.canFitPreferencesFullscreen(1920, 1200, 2.0f))
    }

    @Test
    fun `test shouldRecommendLandscape returns true when window overflows portrait but fits in landscape`() {
        // Standard Android tablet (1200x1920 portrait display):
        // Current portrait viewport: 1200w x 1920h
        // At 1.25x scale, required is 1280x925.
        // Current viewport width (1200) < required (1280) -> does NOT fit in portrait.
        // In landscape (1920w x 1200h): width (1920 >= 1280) and height (1200 >= 925) -> FITS!
        val recommend = WindowOrientationAdvisor.shouldRecommendLandscape(
            windowTitle = "Xournal++ Preferences",
            currentWidth = 1200,
            currentHeight = 1920,
            displayWidth = 1200,
            displayHeight = 1920,
            uiScale = 1.25f
        )
        assertTrue(recommend)
    }

    @Test
    fun `test shouldRecommendLandscape returns false when landscape also cannot fit the window`() {
        // Phone screen (720x1600):
        // At 1.0x scale, Preferences requires 1024x740.
        // In landscape (1600x720): height (720) < required (740) -> CANNOT fit even in landscape!
        // We must NOT mislead the user!
        val recommend = WindowOrientationAdvisor.shouldRecommendLandscape(
            windowTitle = "Preferences",
            currentWidth = 720,
            currentHeight = 1600,
            displayWidth = 720,
            displayHeight = 1600,
            uiScale = 1.0f
        )
        assertFalse(recommend)
    }

    @Test
    fun `test shouldRecommendLandscape returns false when current orientation already fits`() {
        // Tablet already in landscape (1920x1200) with 1.0x scale:
        // Already fits fullscreen (1920 >= 1024 && 1200 >= 740).
        val recommend = WindowOrientationAdvisor.shouldRecommendLandscape(
            windowTitle = "Preferences",
            currentWidth = 1920,
            currentHeight = 1200,
            displayWidth = 1920,
            displayHeight = 1200,
            uiScale = 1.0f
        )
        assertFalse(recommend)
    }

    @Test
    fun `test shouldRecommendLandscape works with localized titles and matchedTitles`() {
        // German title "Xournal++ Einstellungen"
        val recommend = WindowOrientationAdvisor.shouldRecommendLandscape(
            windowTitle = "Xournal++ Einstellungen",
            currentWidth = 800,
            currentHeight = 1280,
            displayWidth = 800,
            displayHeight = 1280,
            uiScale = 1.0f,
            matchedTitles = setOf("Xournal++ Einstellungen", "Einstellungen")
        )
        // Portrait (800x1280): 800 < 1024 -> overflows.
        // Landscape (1280x800): 1280 >= 1024 && 800 >= 740 -> fits!
        assertTrue(recommend)
    }

    @Test
    fun `test shouldRecommendLandscape works with standard dialogs like Export`() {
        // Export requires 640x480
        // Screen with height 400 in portrait (e.g. split window 600w x 400h)
        // Current: 600w x 400h -> does NOT fit height (400 < 480).
        // Landscape display (800w x 600h): fits (800 >= 640 && 600 >= 480).
        val recommend = WindowOrientationAdvisor.shouldRecommendLandscape(
            windowTitle = "Export as PDF",
            currentWidth = 600,
            currentHeight = 400,
            displayWidth = 600,
            displayHeight = 800,
            uiScale = 1.0f
        )
        assertTrue(recommend)
    }

    @Test
    fun `test shouldRecommendLandscape returns false for null or blank title`() {
        assertFalse(
            WindowOrientationAdvisor.shouldRecommendLandscape(
                windowTitle = null,
                currentWidth = 800,
                currentHeight = 1200,
                displayWidth = 800,
                displayHeight = 1200,
                uiScale = 1.0f
            )
        )
        assertFalse(
            WindowOrientationAdvisor.shouldRecommendLandscape(
                windowTitle = "   ",
                currentWidth = 800,
                currentHeight = 1200,
                displayWidth = 800,
                displayHeight = 1200,
                uiScale = 1.0f
            )
        )
    }
}
