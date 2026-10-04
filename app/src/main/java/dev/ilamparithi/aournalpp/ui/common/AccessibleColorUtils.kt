package dev.ilamparithi.aournalpp.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isSpecified
import kotlin.math.abs
import kotlin.math.pow

/**
 * Utility for WCAG 2.1 luminance calculation, contrast ratio evaluation,
 * and automatic collision detection to ensure accessible colors for icons and UI components across OEM themes.
 */
object AccessibleColorUtils {

    /**
     * Minimum contrast ratio recommended by WCAG 2.1 Level AA for UI components and graphical objects (3.0:1).
     */
    const val MIN_ICON_CONTRAST_RATIO: Float = 3.0f

    /**
     * Computes the WCAG 2.1 relative luminance of a Compose [Color].
     * Normalized between 0.0 (pure black) and 1.0 (pure white).
     */
    fun relativeLuminance(color: Color): Float {
        if (!color.isSpecified) return 0f
        fun toLinear(c: Float): Float {
            return if (c <= 0.04045f) {
                c / 12.92f
            } else {
                ((c + 0.055f) / 1.055f).pow(2.4f)
            }
        }
        return 0.2126f * toLinear(color.red) + 0.7152f * toLinear(color.green) + 0.0722f * toLinear(color.blue)
    }

    /**
     * Calculates the contrast ratio between foreground and background colors according to WCAG 2.1 specifications.
     * Returns a float value in the range [1.0, 21.0].
     */
    fun calculateContrastRatio(foreground: Color, background: Color): Float {
        val fg = if (foreground.isSpecified) foreground else Color.White
        val bg = if (background.isSpecified) background else Color.Black

        // If foreground has alpha, composite it over the background
        val effectiveFg = if (fg.alpha < 1f) fg.compositeOver(bg) else fg
        val l1 = relativeLuminance(effectiveFg)
        val l2 = relativeLuminance(bg)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    /**
     * Returns true if the contrast ratio between foreground and background is below the minimum threshold,
     * indicating a visual collision (insufficient contrast).
     */
    fun hasContrastCollision(
        foreground: Color,
        background: Color,
        minContrast: Float = MIN_ICON_CONTRAST_RATIO
    ): Boolean {
        if (!foreground.isSpecified || !background.isSpecified) return false
        return calculateContrastRatio(foreground, background) < minContrast
    }

    /**
     * Detects contrast collisions between an icon tint and its container background.
     * If a collision occurs (contrast < minContrast), resolves to an accessible tint color
     * that preserves the original hue and color character while guaranteeing >= minContrast.
     * If no adjusted hue variant reaches minContrast, falls back to high-contrast white or black.
     */
    fun resolveAccessibleIconColor(
        iconTint: Color,
        containerColor: Color,
        fallbackSurface: Color = Color.Black,
        minContrast: Float = MIN_ICON_CONTRAST_RATIO
    ): Color {
        if (!iconTint.isSpecified) return iconTint
        val effectiveBg = if (!containerColor.isSpecified) {
            fallbackSurface
        } else if (containerColor.alpha < 1f) {
            containerColor.compositeOver(fallbackSurface)
        } else {
            containerColor
        }

        val currentContrast = calculateContrastRatio(iconTint, effectiveBg)
        if (currentContrast >= minContrast) {
            return iconTint
        }

        // Contrast collision detected! Adjust lightness while preserving hue
        val (h, s, _) = colorToHsl(iconTint)
        val bgLum = relativeLuminance(effectiveBg)

        // Try adjusting lightness in the direction that offers the best contrast
        // If background is darker (lum < 0.25), prefer lighter tints; else prefer darker tints
        val testLighterFirst = bgLum < 0.25f

        val candidateTints = if (testLighterFirst) {
            val lightCandidates = listOf(0.70f, 0.78f, 0.85f, 0.92f, 0.96f).map { l ->
                hslToColor(h, s.coerceIn(0.25f, 0.70f), l, iconTint.alpha)
            }
            val darkCandidates = listOf(0.30f, 0.20f, 0.12f, 0.06f).map { l ->
                hslToColor(h, s.coerceIn(0.40f, 0.90f), l, iconTint.alpha)
            }
            lightCandidates + darkCandidates
        } else {
            val darkCandidates = listOf(0.30f, 0.20f, 0.12f, 0.06f).map { l ->
                hslToColor(h, s.coerceIn(0.40f, 0.90f), l, iconTint.alpha)
            }
            val lightCandidates = listOf(0.70f, 0.78f, 0.85f, 0.92f, 0.96f).map { l ->
                hslToColor(h, s.coerceIn(0.25f, 0.70f), l, iconTint.alpha)
            }
            darkCandidates + lightCandidates
        }

        // Return first candidate that satisfies minContrast
        for (candidate in candidateTints) {
            if (calculateContrastRatio(candidate, effectiveBg) >= minContrast) {
                return candidate
            }
        }

        // Ultimate fallback: high-contrast white or dark
        val whiteContrast = calculateContrastRatio(Color.White, effectiveBg)
        val blackContrast = calculateContrastRatio(Color(0xFF191C1D), effectiveBg)
        return if (whiteContrast >= blackContrast) Color.White else Color(0xFF191C1D)
    }

    fun colorToHsl(color: Color): FloatArray {
        val r = color.red
        val g = color.green
        val b = color.blue
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        var h = 0f
        var s = 0f
        val l = (max + min) / 2f

        if (delta != 0f) {
            s = if (l <= 0.5f) delta / (max + min) else delta / (2f - max - min)
            h = when (max) {
                r -> ((g - b) / delta + (if (g < b) 6f else 0f)) * 60f
                g -> ((b - r) / delta + 2f) * 60f
                else -> ((r - g) / delta + 4f) * 60f
            }
        }
        val normalizedH = ((h % 360f) + 360f) % 360f
        return floatArrayOf(normalizedH, s, l)
    }

    fun hslToColor(h: Float, s: Float, l: Float, alpha: Float = 1f): Color {
        val normalizedH = ((h % 360f) + 360f) % 360f
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((normalizedH / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (rPrime, gPrime, bPrime) = when {
            normalizedH < 60f -> Triple(c, x, 0f)
            normalizedH < 120f -> Triple(x, c, 0f)
            normalizedH < 180f -> Triple(0f, c, x)
            normalizedH < 240f -> Triple(0f, x, c)
            normalizedH < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(
            red = (rPrime + m).coerceIn(0f, 1f),
            green = (gPrime + m).coerceIn(0f, 1f),
            blue = (bPrime + m).coerceIn(0f, 1f),
            alpha = alpha
        )
    }
}
