package dev.ilamparithi.aournalpp.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Spring motion tokens for Material 3 Expressive animations.
 */
object ExpressiveSprings {
    val Bouncy: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val Snappy: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val Gentle: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessVeryLow
    )

    val FastSpatial: AnimationSpec<Float> = spring(
        dampingRatio = 0.8f,
        stiffness = 380f
    )

    val SlowEffects: AnimationSpec<Float> = spring(
        dampingRatio = 0.9f,
        stiffness = 180f
    )
}

/**
 * M3 Expressive Arch Shape: A distinctive top-arched silhouette for cards and media items.
 */
class ArchShape(private val cornerRadiusRatio: Float = 0.5f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            val archRadius = (w * cornerRadiusRatio).coerceAtMost(h / 2f)
            val bottomCorner = (w * 0.15f).coerceAtMost(24f * density.density)

            moveTo(0f, archRadius)
            // Top Arch (two cubic beziers)
            cubicTo(0f, archRadius * 0.45f, w * 0.2f, 0f, w / 2f, 0f)
            cubicTo(w * 0.8f, 0f, w, archRadius * 0.45f, w, archRadius)

            // Right side down to bottom-right corner
            lineTo(w, h - bottomCorner)
            quadraticTo(w, h, w - bottomCorner, h)

            // Bottom edge to bottom-left corner
            lineTo(bottomCorner, h)
            quadraticTo(0f, h, 0f, h - bottomCorner)

            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * M3 Expressive Asymmetric Card Shape: Diagonal curvature (e.g. 32dp and 12dp).
 */
fun AsymmetricCardShape(
    topStart: Dp = 32.dp,
    topEnd: Dp = 12.dp,
    bottomEnd: Dp = 32.dp,
    bottomStart: Dp = 12.dp
): RoundedCornerShape = RoundedCornerShape(
    topStart = CornerSize(topStart),
    topEnd = CornerSize(topEnd),
    bottomEnd = CornerSize(bottomEnd),
    bottomStart = CornerSize(bottomStart)
)

/**
 * M3 Expressive Scallop / Flower Shape: Decorative multi-lobed organic outline.
 */
class ScallopShape(private val lobes: Int = 8, private val depth: Float = 0.08f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseR = (size.width.coerceAtMost(size.height) / 2f) * 0.95f
        val amplitude = baseR * depth

        val points = 72
        for (i in 0..points) {
            val theta = (i.toFloat() / points) * 2f * PI.toFloat()
            val r = baseR + amplitude * cos(lobes * theta)
            val x = cx + r * cos(theta)
            val y = cy + r * sin(theta)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * M3 Expressive Sunny / Soft Polygon Shape: Smooth multi-corner polygon.
 */
class SunnyShape(private val vertices: Int = 8, private val roundness: Float = 0.25f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = (size.width.coerceAtMost(size.height) / 2f) * 0.95f

        val step = (2f * PI / vertices).toFloat()
        val cornerOffset = step * roundness

        for (i in 0 until vertices) {
            val angle = i * step - (PI / 2f).toFloat()
            val p1Angle = angle - cornerOffset
            val p2Angle = angle + cornerOffset

            val x1 = cx + r * cos(p1Angle)
            val y1 = cy + r * sin(p1Angle)
            val xc = cx + (r * 1.05f) * cos(angle)
            val yc = cy + (r * 1.05f) * sin(angle)
            val x2 = cx + r * cos(p2Angle)
            val y2 = cy + r * sin(p2Angle)

            if (i == 0) {
                path.moveTo(x1, y1)
            } else {
                path.lineTo(x1, y1)
            }
            path.quadraticTo(xc, yc, x2, y2)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * M3 Expressive Clover Shape: 4-lobed playful curved shape.
 */
class CloverShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val w = size.width
        val h = size.height

        path.moveTo(w * 0.5f, 0f)
        path.cubicTo(w * 0.8f, 0f, w, h * 0.2f, w, h * 0.5f)
        path.cubicTo(w, h * 0.8f, w * 0.8f, h, w * 0.5f, h)
        path.cubicTo(w * 0.2f, h, 0f, h * 0.8f, 0f, h * 0.5f)
        path.cubicTo(0f, h * 0.2f, w * 0.2f, 0f, w * 0.5f, 0f)
        path.close()
        return Outline.Generic(path)
    }
}
