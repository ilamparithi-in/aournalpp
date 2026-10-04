package dev.ilamparithi.aournalpp.ui.common

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibleColorUtilsTest {

    @Test
    fun testRelativeLuminanceBlackAndWhite() {
        val blackLum = AccessibleColorUtils.relativeLuminance(Color.Black)
        val whiteLum = AccessibleColorUtils.relativeLuminance(Color.White)
        assertEquals(0f, blackLum, 0.001f)
        assertEquals(1f, whiteLum, 0.001f)
    }

    @Test
    fun testContrastRatioBlackOnWhite() {
        val ratio = AccessibleColorUtils.calculateContrastRatio(Color.Black, Color.White)
        assertEquals(21.0f, ratio, 0.1f)
    }

    @Test
    fun testContrastRatioIdenticalColors() {
        val color = Color(0xFF6750A4)
        val ratio = AccessibleColorUtils.calculateContrastRatio(color, color)
        assertEquals(1.0f, ratio, 0.01f)
        assertTrue(AccessibleColorUtils.hasContrastCollision(color, color))
    }

    @Test
    fun testHasContrastCollisionLowContrastPairings() {
        // Dark brown-red background with dark muted red foreground (common OEM dark theme issue)
        val darkBg = Color(0xFF381E1C)
        val mutedFg = Color(0xFF55322F)

        val ratio = AccessibleColorUtils.calculateContrastRatio(mutedFg, darkBg)
        assertTrue("Expected ratio < 3.0f but got $ratio", ratio < 3.0f)
        assertTrue(AccessibleColorUtils.hasContrastCollision(mutedFg, darkBg))
    }

    @Test
    fun testResolveAccessibleIconColorResolvesCollisionOnDarkBackground() {
        // Dark background where low contrast icon tint is passed
        val darkBg = Color(0xFF2B1715)
        val lowContrastTint = Color(0xFF4A2522)

        assertTrue(AccessibleColorUtils.hasContrastCollision(lowContrastTint, darkBg))

        val resolved = AccessibleColorUtils.resolveAccessibleIconColor(
            iconTint = lowContrastTint,
            containerColor = darkBg
        )

        val resolvedRatio = AccessibleColorUtils.calculateContrastRatio(resolved, darkBg)
        assertTrue(
            "Resolved contrast ratio ($resolvedRatio) must be >= 3.0f",
            resolvedRatio >= 3.0f
        )
        assertFalse(AccessibleColorUtils.hasContrastCollision(resolved, darkBg))
    }

    @Test
    fun testResolveAccessibleIconColorResolvesCollisionOnLightBackground() {
        // Light pastel background with light pastel foreground
        val lightBg = Color(0xFFFDE8E5)
        val lightFg = Color(0xFFE8C2BD)

        assertTrue(AccessibleColorUtils.hasContrastCollision(lightFg, lightBg))

        val resolved = AccessibleColorUtils.resolveAccessibleIconColor(
            iconTint = lightFg,
            containerColor = lightBg
        )

        val resolvedRatio = AccessibleColorUtils.calculateContrastRatio(resolved, lightBg)
        assertTrue(
            "Resolved contrast ratio ($resolvedRatio) must be >= 3.0f",
            resolvedRatio >= 3.0f
        )
        assertFalse(AccessibleColorUtils.hasContrastCollision(resolved, lightBg))
    }

    @Test
    fun testResolveAccessibleIconColorKeepsSufficientContrastUnchanged() {
        // High contrast pairing: pure white on dark container
        val darkBg = Color(0xFF1E1F22)
        val highContrastTint = Color.White

        val resolved = AccessibleColorUtils.resolveAccessibleIconColor(
            iconTint = highContrastTint,
            containerColor = darkBg
        )

        assertEquals(highContrastTint, resolved)
    }

    @Test
    fun testColorToHslAndBackPreservesColor() {
        val original = Color(0xFF336699)
        val hsl = AccessibleColorUtils.colorToHsl(original)
        val reconstructed = AccessibleColorUtils.hslToColor(hsl[0], hsl[1], hsl[2])

        assertEquals(original.red, reconstructed.red, 0.02f)
        assertEquals(original.green, reconstructed.green, 0.02f)
        assertEquals(original.blue, reconstructed.blue, 0.02f)
    }
}
