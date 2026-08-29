package dev.ilamparithi.aournalpp.ui

import android.graphics.Rect
import android.os.Build
import android.view.Surface
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat

/**
 * Screen safe area insets representing padding in density-independent pixels (dp).
 */
data class SafeAreaInsets(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) : java.io.Serializable

/**
 * Converts window-relative safe insets to physical chassis-relative insets
 * based on the display rotation when calibrated.
 */
fun windowToPhysicalInsets(windowInsets: SafeAreaInsets, rotation: Int): SafeAreaInsets {
    return when (rotation % 4) {
        Surface.ROTATION_90 -> SafeAreaInsets(
            top = windowInsets.left,
            right = windowInsets.top,
            bottom = windowInsets.right,
            left = windowInsets.bottom
        )
        Surface.ROTATION_180 -> SafeAreaInsets(
            top = windowInsets.bottom,
            right = windowInsets.left,
            bottom = windowInsets.top,
            left = windowInsets.right
        )
        Surface.ROTATION_270 -> SafeAreaInsets(
            top = windowInsets.right,
            right = windowInsets.bottom,
            bottom = windowInsets.left,
            left = windowInsets.top
        )
        else -> windowInsets // ROTATION_0
    }
}

/**
 * Converts physical chassis-relative insets to window-relative insets
 * for the current display rotation.
 */
fun physicalToWindowInsets(physicalInsets: SafeAreaInsets, rotation: Int): SafeAreaInsets {
    return when (rotation % 4) {
        Surface.ROTATION_90 -> SafeAreaInsets(
            top = physicalInsets.right,
            right = physicalInsets.bottom,
            bottom = physicalInsets.left,
            left = physicalInsets.top
        )
        Surface.ROTATION_180 -> SafeAreaInsets(
            top = physicalInsets.bottom,
            right = physicalInsets.left,
            bottom = physicalInsets.top,
            left = physicalInsets.right
        )
        Surface.ROTATION_270 -> SafeAreaInsets(
            top = physicalInsets.left,
            right = physicalInsets.top,
            bottom = physicalInsets.right,
            left = physicalInsets.bottom
        )
        else -> physicalInsets // ROTATION_0
    }
}

/**
 * Maps calibrated safe area insets to current display rotation so padding stays anchored
 * to the physical device chassis across all orientations (0°, 90°, 180°, 270°).
 */
fun getRotatedSafeAreaInsets(
    calibrated: SafeAreaInsets,
    refRotation: Int = Surface.ROTATION_0,
    currentRotation: Int = Surface.ROTATION_0
): SafeAreaInsets {
    val physical = windowToPhysicalInsets(calibrated, refRotation)
    return physicalToWindowInsets(physical, currentRotation)
}

/**
 * Lightweight rectangle model for display cutout geometry calculation without Android SDK stub dependencies.
 */
data class CutoutRect(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/**
 * Result of display cutout placement calculation for floating header positioning.
 */
data class CutoutPlacement(
    val topOffsetPx: Int = 0,
    val startOffsetPx: Int = 0,
    val endOffsetPx: Int = 0,
    val hasCenterCutout: Boolean = false,
    val hasLeftCutout: Boolean = false,
    val hasRightCutout: Boolean = false
)

/**
 * Classifies display cutout geometry and determines required offsets.
 * - Center cutout: topOffsetPx = bottom of cutout (floats below cutout).
 * - Left corner cutout: startOffsetPx = right of cutout, topOffsetPx = 0 (floats beside cutout).
 * - Right corner cutout: endOffsetPx = screenWidth - left of cutout, topOffsetPx = 0 (floats beside cutout).
 */
fun calculateCutoutPlacement(
    cutoutRects: List<CutoutRect>,
    screenWidthPx: Int,
    screenHeightPx: Int
): CutoutPlacement {
    if (cutoutRects.isEmpty() || screenWidthPx <= 0) {
        return CutoutPlacement()
    }

    var maxCenterCutoutBottom = 0
    var maxLeftCutoutRight = 0
    var minRightCutoutLeft = screenWidthPx
    var hasCenter = false
    var hasLeft = false
    var hasRight = false

    val topThreshold = if (screenHeightPx > 0) screenHeightPx * 0.25f else 300f

    for (rect in cutoutRects) {
        if (rect.top <= topThreshold) {
            val cx = rect.centerX
            val leftLimit = screenWidthPx * 0.33f
            val rightLimit = screenWidthPx * 0.67f

            if (cx < leftLimit) {
                // Left corner cutout
                hasLeft = true
                maxLeftCutoutRight = maxOf(maxLeftCutoutRight, rect.right)
            } else if (cx > rightLimit) {
                // Right corner cutout
                hasRight = true
                minRightCutoutLeft = minOf(minRightCutoutLeft, rect.left)
            } else {
                // Center cutout
                hasCenter = true
                maxCenterCutoutBottom = maxOf(maxCenterCutoutBottom, rect.bottom)
            }
        } else {
            // Cutout on the side (e.g. landscape mode)
            val cx = rect.centerX
            if (cx < screenWidthPx * 0.33f) {
                hasLeft = true
                maxLeftCutoutRight = maxOf(maxLeftCutoutRight, rect.right)
            } else if (cx > screenWidthPx * 0.67f) {
                hasRight = true
                minRightCutoutLeft = minOf(minRightCutoutLeft, rect.left)
            }
        }
    }

    val startOffset = if (hasLeft) maxLeftCutoutRight else 0
    val endOffset = if (hasRight) (screenWidthPx - minRightCutoutLeft) else 0

    return CutoutPlacement(
        topOffsetPx = if (hasCenter) maxCenterCutoutBottom else 0,
        startOffsetPx = startOffset,
        endOffsetPx = endOffset,
        hasCenterCutout = hasCenter,
        hasLeftCutout = hasLeft,
        hasRightCutout = hasRight
    )
}

/**
 * Composable that dynamically observes the window's display cutouts and calculates placement.
 */
@Composable
fun rememberCutoutPlacement(): CutoutPlacement {
    val view = LocalView.current
    var cutoutRects by remember { mutableStateOf<List<CutoutRect>>(emptyList()) }
    var screenWidthPx by remember { mutableIntStateOf(0) }
    var screenHeightPx by remember { mutableIntStateOf(0) }

    DisposableEffect(view) {
        val updateCutouts = {
            screenWidthPx = view.width
            screenHeightPx = view.height
            val rootInsets = ViewCompat.getRootWindowInsets(view)
            val androidRects = rootInsets?.displayCutout?.boundingRects ?: emptyList()
            cutoutRects = androidRects.map { CutoutRect(it.left, it.top, it.right, it.bottom) }
        }

        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            updateCutouts()
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        updateCutouts()

        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    return remember(cutoutRects, screenWidthPx, screenHeightPx) {
        calculateCutoutPlacement(cutoutRects, screenWidthPx, screenHeightPx)
    }
}
