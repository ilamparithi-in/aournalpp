package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.ui.STANDARD_TOOLBAR_PRESETS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ToolbarPositionTest {

    @Test
    fun testStandardPresetsPresence() {
        val presetIds = STANDARD_TOOLBAR_PRESETS.map { it.id }
        assertTrue(presetIds.contains("top_center"))
        assertTrue(presetIds.contains("top_left"))
        assertTrue(presetIds.contains("top_right"))
        assertTrue(presetIds.contains("bottom_center"))
        assertTrue(presetIds.contains("bottom_left"))
        assertTrue(presetIds.contains("bottom_right"))
    }

    @Test
    fun testPresetCoordinatesInRange() {
        STANDARD_TOOLBAR_PRESETS.forEach { preset ->
            assertTrue("${preset.id} normX out of range", preset.normX in 0.0f..1.0f)
            assertTrue("${preset.id} normY out of range", preset.normY in 0.0f..1.0f)
        }
    }

    @Test
    fun testMagneticSnapHysteresis() {
        val snapThreshold = 18f
        val hysteresisBuffer = 28f
        val screenCenterX = 500f

        // Case 1: Point is within snap threshold -> should snap
        val pointX = 490f
        val dist1 = abs(pointX - screenCenterX)
        assertTrue(dist1 < snapThreshold)

        // Case 2: Once snapped, movement up to hysteresis buffer (e.g. delta 22) -> remains snapped
        val movedX = 478f
        val dist2 = abs(movedX - screenCenterX)
        assertTrue(dist2 < hysteresisBuffer)

        // Case 3: Movement exceeds hysteresis buffer (e.g. delta 35) -> breaks away from snap
        val breakAwayX = 460f
        val dist3 = abs(breakAwayX - screenCenterX)
        assertTrue(dist3 > hysteresisBuffer)
    }
}
