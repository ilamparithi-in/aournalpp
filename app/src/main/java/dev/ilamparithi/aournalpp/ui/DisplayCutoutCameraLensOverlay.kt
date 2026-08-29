package dev.ilamparithi.aournalpp.ui

import android.os.Build
import android.view.ViewTreeObserver
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat

/**
 * Renders a high-precision Material 3 Camera Lens within the physical display cutout boundaries.
 * Accurately models the outer chassis bezel, anti-reflective optical multi-coating, dark optical glass,
 * aperture iris, and realistic specular glint highlights.
 */
@Composable
fun DisplayCutoutCameraLensOverlay(
    modifier: Modifier = Modifier
) {
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

    if (cutoutRects.isEmpty()) return

    Canvas(modifier = modifier.fillMaxSize()) {
        for (rect in cutoutRects) {
            drawMaterial3CameraLens(rect)
        }
    }
}

/**
 * Draws the layered Material 3 Camera Lens inside the specified cutout rectangle.
 */
fun DrawScope.drawMaterial3CameraLens(rect: CutoutRect) {
    val cx = rect.centerX
    val cy = rect.centerY
    val rectW = (rect.right - rect.left).toFloat()
    val rectH = (rect.bottom - rect.top).toFloat()
    val radius = minOf(rectW, rectH) / 2f
    if (radius <= 1f) return

    val center = Offset(cx, cy)
    val isPillCutout = (rectW / rectH > 1.4f) || (rectH / rectW > 1.4f)

    if (isPillCutout) {
        // Pill-shaped / Dynamic Island cutout
        val cornerRadius = radius
        val topLeft = Offset(rect.left.toFloat(), rect.top.toFloat())
        val size = Size(rectW, rectH)

        // 1. Dark chassis border
        drawRoundRect(
            color = Color(0xFF0F0F14),
            topLeft = topLeft,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
            style = Fill
        )

        // 2. Specular outer rim stroke
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF32323A), Color(0xFF141418), Color(0xFF282830)),
                start = topLeft,
                end = Offset(rect.right.toFloat(), rect.bottom.toFloat())
            ),
            topLeft = topLeft,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // 3. Multi-lens circles inside the pill
        val lensRadius = cornerRadius * 0.78f
        val lens1Center = Offset(rect.left.toFloat() + cornerRadius, cy)
        val lens2Center = Offset(rect.right.toFloat() - cornerRadius, cy)

        drawSingleLensElement(lens1Center, lensRadius)
        if (rectW > cornerRadius * 2.8f) {
            drawSingleLensElement(lens2Center, lensRadius)
        }
    } else {
        // Circular punch-hole camera
        drawSingleLensElement(center, radius)
    }
}

private fun DrawScope.drawSingleLensElement(center: Offset, radius: Float) {
    val cx = center.x
    val cy = center.y

    // 1. Outer dark bezel ring
    drawCircle(
        color = Color(0xFF0C0D12),
        radius = radius,
        center = center
    )

    // 2. Bezel metal chamfer highlight ring
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                Color(0xFF383842),
                Color(0xFF18181E),
                Color(0xFF4A4A56),
                Color(0xFF18181E),
                Color(0xFF383842)
            ),
            center = center
        ),
        radius = radius - 0.5.dp.toPx(),
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )

    // 3. Inner dark optical glass body
    val glassRadius = radius * 0.88f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF08080C), Color(0xFF020204)),
            center = center,
            radius = glassRadius
        ),
        radius = glassRadius,
        center = center
    )

    // 4. Anti-Reflective (AR) optical coating tint (sapphire cyan / magenta iridescent ring)
    val coatingRadius = glassRadius * 0.72f
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                Color(0x351976D2), // Deep blue
                Color(0x2800B0FF), // Cyan
                Color(0x309C27B0), // Purple magenta
                Color(0x2000E676), // Emerald
                Color(0x351976D2)
            ),
            center = center
        ),
        radius = coatingRadius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )

    // 5. Central aperture sensor element
    val sensorRadius = glassRadius * 0.42f
    drawCircle(
        color = Color(0xFF05060A),
        radius = sensorRadius,
        center = center
    )

    // 6. Aperture pupil iris ring
    drawCircle(
        color = Color(0x3000E5FF),
        radius = sensorRadius * 0.65f,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )

    // 7. Specular highlight glint (top-left pinpoint light reflection)
    val glintCenter = Offset(cx - radius * 0.32f, cy - radius * 0.32f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0f)),
            center = glintCenter,
            radius = radius * 0.22f
        ),
        radius = radius * 0.22f,
        center = glintCenter
    )

    // Secondary subtle micro-glint
    val microGlintCenter = Offset(cx + radius * 0.26f, cy + radius * 0.26f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x6000E5FF), Color(0x0000E5FF)),
            center = microGlintCenter,
            radius = radius * 0.14f
        ),
        radius = radius * 0.14f,
        center = microGlintCenter
    )
}
