package dev.ilamparithi.aournalpp

import android.graphics.Rect
import android.view.Surface
import dev.ilamparithi.aournalpp.ui.SafeAreaInsets
import dev.ilamparithi.aournalpp.ui.calculateCutoutPlacement
import dev.ilamparithi.aournalpp.ui.getRotatedSafeAreaInsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayCutoutHelperTest {

    @Test
    fun testRotatedSafeAreaInsets_calibratedPortrait_rotatingAllOrientations() {
        val insets = SafeAreaInsets(left = 10, top = 20, right = 30, bottom = 40)
        
        // 0 -> 0: Identical
        val rot0 = getRotatedSafeAreaInsets(insets, Surface.ROTATION_0, Surface.ROTATION_0)
        assertEquals(10, rot0.left)
        assertEquals(20, rot0.top)
        assertEquals(30, rot0.right)
        assertEquals(40, rot0.bottom)

        // 0 -> 90: Top becomes Left, Right becomes Top, Bottom becomes Right, Left becomes Bottom
        val rot90 = getRotatedSafeAreaInsets(insets, Surface.ROTATION_0, Surface.ROTATION_90)
        assertEquals(20, rot90.left)
        assertEquals(30, rot90.top)
        assertEquals(40, rot90.right)
        assertEquals(10, rot90.bottom)

        // 0 -> 180: Upside down
        val rot180 = getRotatedSafeAreaInsets(insets, Surface.ROTATION_0, Surface.ROTATION_180)
        assertEquals(30, rot180.left)
        assertEquals(40, rot180.top)
        assertEquals(10, rot180.right)
        assertEquals(20, rot180.bottom)

        // 0 -> 270: Counter-clockwise 90
        val rot270 = getRotatedSafeAreaInsets(insets, Surface.ROTATION_0, Surface.ROTATION_270)
        assertEquals(40, rot270.left)
        assertEquals(10, rot270.top)
        assertEquals(20, rot270.right)
        assertEquals(30, rot270.bottom)
    }

    @Test
    fun testRotatedSafeAreaInsets_calibratedLandscape_rightPaddingMovesToBottomInPortrait() {
        // User scenario: Set Right padding = 25 dp on Landscape (ROTATION_90)
        val landscapeCalibrated = SafeAreaInsets(left = 0, top = 0, right = 25, bottom = 0)

        // When switching to Portrait (ROTATION_0), the physical edge (charging port) is at the bottom
        val portrait = getRotatedSafeAreaInsets(landscapeCalibrated, Surface.ROTATION_90, Surface.ROTATION_0)
        assertEquals(0, portrait.left)
        assertEquals(0, portrait.top)
        assertEquals(0, portrait.right)
        assertEquals(25, portrait.bottom)

        // When switching to Flipped Portrait (ROTATION_180), the physical bottom is at the top
        val flippedPortrait = getRotatedSafeAreaInsets(landscapeCalibrated, Surface.ROTATION_90, Surface.ROTATION_180)
        assertEquals(0, flippedPortrait.left)
        assertEquals(25, flippedPortrait.top)
        assertEquals(0, flippedPortrait.right)
        assertEquals(0, flippedPortrait.bottom)

        // When switching to Flipped Landscape (ROTATION_270), the physical bottom is at the left
        val flippedLandscape = getRotatedSafeAreaInsets(landscapeCalibrated, Surface.ROTATION_90, Surface.ROTATION_270)
        assertEquals(25, flippedLandscape.left)
        assertEquals(0, flippedLandscape.top)
        assertEquals(0, flippedLandscape.right)
        assertEquals(0, flippedLandscape.bottom)

        // When switching back to Landscape (ROTATION_90), right padding is exactly preserved
        val landscapeBack = getRotatedSafeAreaInsets(landscapeCalibrated, Surface.ROTATION_90, Surface.ROTATION_90)
        assertEquals(0, landscapeBack.left)
        assertEquals(0, landscapeBack.top)
        assertEquals(25, landscapeBack.right)
        assertEquals(0, landscapeBack.bottom)
    }

    @Test
    fun testCalculateCutoutPlacement_empty() {
        val placement = calculateCutoutPlacement(emptyList(), 1080, 2400)
        assertEquals(0, placement.topOffsetPx)
        assertEquals(0, placement.startOffsetPx)
        assertEquals(0, placement.endOffsetPx)
        assertFalse(placement.hasCenterCutout)
        assertFalse(placement.hasLeftCutout)
        assertFalse(placement.hasRightCutout)
    }

    @Test
    fun testCalculateCutoutPlacement_centerCutout() {
        // Camera punch-hole in the top center (screenWidth = 1080, cx = 540, bottom = 120)
        val centerRect = dev.ilamparithi.aournalpp.ui.CutoutRect(500, 0, 580, 120)
        val placement = calculateCutoutPlacement(listOf(centerRect), 1080, 2400)

        assertTrue(placement.hasCenterCutout)
        assertFalse(placement.hasLeftCutout)
        assertFalse(placement.hasRightCutout)
        assertEquals(120, placement.topOffsetPx)
        assertEquals(0, placement.startOffsetPx)
        assertEquals(0, placement.endOffsetPx)
    }

    @Test
    fun testCalculateCutoutPlacement_leftCornerCutout() {
        // Camera punch-hole in top-left corner (x: 20..100, bottom: 90)
        val leftRect = dev.ilamparithi.aournalpp.ui.CutoutRect(20, 0, 100, 90)
        val placement = calculateCutoutPlacement(listOf(leftRect), 1080, 2400)

        assertFalse(placement.hasCenterCutout)
        assertTrue(placement.hasLeftCutout)
        assertFalse(placement.hasRightCutout)
        assertEquals(0, placement.topOffsetPx) // topOffset remains 0 to float beside without vertical waste
        assertEquals(100, placement.startOffsetPx) // beside the cutout
        assertEquals(0, placement.endOffsetPx)
    }

    @Test
    fun testCalculateCutoutPlacement_rightCornerCutout() {
        // Camera punch-hole in top-right corner (x: 980..1060, bottom: 90)
        val rightRect = dev.ilamparithi.aournalpp.ui.CutoutRect(980, 0, 1060, 90)
        val placement = calculateCutoutPlacement(listOf(rightRect), 1080, 2400)

        assertFalse(placement.hasCenterCutout)
        assertFalse(placement.hasLeftCutout)
        assertTrue(placement.hasRightCutout)
        assertEquals(0, placement.topOffsetPx)
        assertEquals(0, placement.startOffsetPx)
        assertEquals(100, placement.endOffsetPx) // 1080 - 980 = 100
    }
}
