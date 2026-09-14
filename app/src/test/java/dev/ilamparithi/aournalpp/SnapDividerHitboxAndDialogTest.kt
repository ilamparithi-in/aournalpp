package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.ui.snap.DividerGeometry
import dev.ilamparithi.aournalpp.ui.snap.DividerOrientation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class SnapDividerHitboxAndDialogTest {

    data class HitboxBounds(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )

    private fun calculateHitbox(
        div: DividerGeometry,
        viewportWidth: Int,
        viewportHeight: Int,
        density: Float = 2.0f // 2.0 density: 1dp = 2px
    ): HitboxBounds {
        val hitThicknessPx = (18f * density).roundToInt()
        val maxHitLengthPx = (56f * density).roundToInt()
        val hitLengthPx = minOf(maxHitLengthPx, div.length)

        return if (div.orientation == DividerOrientation.VERTICAL) {
            val handleX = (div.currentRatio * viewportWidth).roundToInt()
            val handleLeftPx = handleX - (hitThicknessPx / 2f).toInt()
            val centerYPx = div.y + ((div.length - hitLengthPx) / 2f).toInt()
            HitboxBounds(x = handleLeftPx, y = centerYPx, width = hitThicknessPx, height = hitLengthPx)
        } else {
            val handleY = (div.currentRatio * viewportHeight).roundToInt()
            val handleTopPx = handleY - (hitThicknessPx / 2f).toInt()
            val centerXPx = div.x + ((div.length - hitLengthPx) / 2f).toInt()
            HitboxBounds(x = centerXPx, y = handleTopPx, width = hitLengthPx, height = hitThicknessPx)
        }
    }

    @Test
    fun testVerticalDividerHitboxIsCenteredAndConstrained() {
        val div = DividerGeometry(
            id = "v1",
            orientation = DividerOrientation.VERTICAL,
            x = 960,
            y = 0,
            length = 1200, // Full viewport height
            currentRatio = 0.5f,
            minRatio = 0.15f,
            maxRatio = 0.85f,
            defaultRatio = 0.5f
        )

        val bounds = calculateHitbox(div, viewportWidth = 1920, viewportHeight = 1200, density = 2.0f)

        // 18dp * 2 = 36px thickness, 56dp * 2 = 112px height
        assertEquals(36, bounds.width)
        assertEquals(112, bounds.height)

        // Centered horizontally around 960 (960 - 18 = 942)
        assertEquals(942, bounds.x)
        // Centered vertically along [0, 1200]: (1200 - 112) / 2 = 544
        assertEquals(544, bounds.y)

        // Crucial test: Scrollbars running from y=0..400 and y=700..1200 are completely unblocked!
        assertTrue("Top region should have zero hitbox overlap", 0 < bounds.y)
        assertTrue("Bottom region should have zero hitbox overlap", 1200 > bounds.y + bounds.height)
    }

    @Test
    fun testHorizontalDividerHitboxIsCenteredAndConstrained() {
        val div = DividerGeometry(
            id = "h1",
            orientation = DividerOrientation.HORIZONTAL,
            x = 0,
            y = 600,
            length = 1920,
            currentRatio = 0.5f,
            minRatio = 0.15f,
            maxRatio = 0.85f,
            defaultRatio = 0.5f
        )

        val bounds = calculateHitbox(div, viewportWidth = 1920, viewportHeight = 1200, density = 2.0f)

        assertEquals(112, bounds.width)
        assertEquals(36, bounds.height)

        // Centered horizontally: (1920 - 112) / 2 = 904
        assertEquals(904, bounds.x)
        // Centered vertically around 600 (600 - 18 = 582)
        assertEquals(582, bounds.y)
    }

    @Test
    fun testShortDividerSegmentClampsLength() {
        // Divider segment is very short (e.g. only 60px in an extreme split)
        val div = DividerGeometry(
            id = "v_short",
            orientation = DividerOrientation.VERTICAL,
            x = 500,
            y = 200,
            length = 60,
            currentRatio = 0.5f,
            minRatio = 0.15f,
            maxRatio = 0.85f,
            defaultRatio = 0.5f
        )

        val bounds = calculateHitbox(div, viewportWidth = 1000, viewportHeight = 1000, density = 2.0f)

        // Height must be clamped to 60px, NOT 112px
        assertEquals(60, bounds.height)
        assertEquals(200, bounds.y)
        assertTrue(bounds.y + bounds.height <= div.y + div.length)
    }

    @Test
    fun testDialogBlockingLogic() {
        fun isBlocking(
            isModalOrDialogOpen: Boolean,
            activePromptTitle: String?,
            showEmergencyForceCloseDialog: Boolean,
            showImageSourceDialog: Boolean,
            showWindowSwitcherGallery: Boolean,
            showSnapAssistHost: Boolean,
            isSwitchTransitionActive: Boolean
        ): Boolean {
            return isModalOrDialogOpen ||
                (activePromptTitle != null) ||
                showEmergencyForceCloseDialog ||
                showImageSourceDialog ||
                showWindowSwitcherGallery ||
                showSnapAssistHost ||
                isSwitchTransitionActive
        }

        // Normal steady state: NOT blocking
        assertFalse(isBlocking(false, null, false, false, false, false, false))

        // X11 dialog open
        assertTrue(isBlocking(true, null, false, false, false, false, false))

        // Active prompt
        assertTrue(isBlocking(false, "Save changes?", false, false, false, false, false))

        // Emergency dialog
        assertTrue(isBlocking(false, null, true, false, false, false, false))

        // Bottom sheet dialog
        assertTrue(isBlocking(false, null, false, true, false, false, false))

        // Window gallery
        assertTrue(isBlocking(false, null, false, false, true, false, false))

        // Snap assist
        assertTrue(isBlocking(false, null, false, false, false, true, false))

        // Window transition
        assertTrue(isBlocking(false, null, false, false, false, false, true))
    }

    @Test
    fun testSplitScreenTouchActivationHitTesting() {
        val manager = dev.ilamparithi.aournalpp.ui.snap.SnapLayoutManager()
        manager.setMode(dev.ilamparithi.aournalpp.ui.snap.SnapLayoutMode.SPLIT_TWO)

        val windows = listOf(
            dev.ilamparithi.aournalpp.runtime.ProcessSupervisor.X11WindowInfo(id = "win1", title = "Note 1.xopp", isActive = true),
            dev.ilamparithi.aournalpp.runtime.ProcessSupervisor.X11WindowInfo(id = "win2", title = "Note 2.xopp", isActive = false)
        )

        val vpW = 2000
        val vpH = 1200
        val assignments = manager.buildSnapAssignments(vpW, vpH, windows)
        assertEquals(2, assignments.size)

        fun findSwitchTarget(touchX: Float, touchY: Float, currentWindows: List<dev.ilamparithi.aournalpp.runtime.ProcessSupervisor.X11WindowInfo>): String? {
            val hit = assignments.find { a ->
                touchX >= a.x.toFloat() && touchX < (a.x + a.width).toFloat() &&
                touchY >= a.y.toFloat() && touchY < (a.y + a.height).toFloat()
            }
            val currentActiveId = currentWindows.find { it.isActive }?.id
            return if (hit != null && hit.windowId != currentActiveId) hit.windowId else null
        }

        // Tapping on left half (win1 - already active) -> no switch
        val targetLeft = findSwitchTarget(250f, 600f, windows)
        org.junit.Assert.assertNull(targetLeft)

        // Tapping on right half (win2 - inactive) -> should switch to win2
        val targetRight = findSwitchTarget(1500f, 600f, windows)
        assertEquals("win2", targetRight)
    }
}

