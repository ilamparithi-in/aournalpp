package dev.ilamparithi.aournalpp.ui.preview

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ilamparithi.aournalpp.model.NoteFileType
import java.io.File
import java.util.Random
import kotlin.math.sin

/**
 * Material 3 Emphasized Decelerate Easing for expressive entrance and morph transitions.
 * Features an instantaneous start and an extended, gentle deceleration tail where the final
 * phase of motion becomes progressively slower and smoother.
 */
private val Material3EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

/**
 * Host component that wraps the app shell, providing [LocalFloatingPreviewController]
 * and rendering the high-fidelity floating preview overlay when activated.
 */
@Composable
fun FloatingPreviewHost(
    content: @Composable () -> Unit
) {
    val controller = remember { FloatingPreviewController() }

    CompositionLocalProvider(LocalFloatingPreviewController provides controller) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()

            val previewData = controller.currentPreview.value
            if (previewData != null) {
                FloatingPreviewOverlay(
                    data = previewData,
                    isFingerDown = controller.isFingerDown.value,
                    onDismissFinished = { controller.dismissImmediately() }
                )
            }
        }
    }
}

/**
 * Floating Preview Overlay featuring:
 * 1. Dark blurred background with sprinkled Google Pixel style white stardust micro-particles.
 * 2. Morphing image preview from original bounds to center-screen using Material 3 Standard Easing.
 * 3. Fade-out on finger release without blocking subsequent touches on MainActivity.
 */
@Composable
private fun FloatingPreviewOverlay(
    data: FloatingPreviewData,
    isFingerDown: Boolean,
    onDismissFinished: () -> Unit
) {
    val density = LocalDensity.current
    val morphProgress = remember { Animatable(0f) }
    val fadeAlpha = remember { Animatable(1f) }

    // Trigger morph-in animation when preview is opened
    LaunchedEffect(data) {
        morphProgress.snapTo(0f)
        fadeAlpha.snapTo(1f)
        morphProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 380,
                easing = Material3EmphasizedDecelerate
            )
        )
    }

    // Trigger fade-out animation when finger is released
    LaunchedEffect(isFingerDown) {
        if (!isFingerDown) {
            fadeAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 180,
                    easing = FastOutLinearInEasing
                )
            )
            onDismissFinished()
        }
    }

    val currentAlpha = fadeAlpha.value
    if (currentAlpha <= 0.001f) return

    val bitmap = remember(data.thumbnailFile) {
        data.thumbnailFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                try { BitmapFactory.decodeFile(file.absolutePath) } catch (e: Exception) { null }
            } else null
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // When finger is released, do NOT intercept pointer events so MainActivity is instantly interactive
            .graphicsLayer {
                alpha = currentAlpha
            }
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        // 1. Dark Blur Scrim & Google Pixel Stardust Particle Field
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0C10).copy(alpha = 0.70f * morphProgress.value))
                .blur(radius = (16.dp * morphProgress.value).coerceAtLeast(0.1.dp))
        )

        // Pixel-Style White Micro-Particle Constellation
        PixelParticleField(
            progress = morphProgress.value,
            alpha = currentAlpha,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Compute Morphing Geometry from initial bounds to centered target bounds
        val initBounds = if (data.initialBounds != Rect.Zero && data.initialBounds.width > 0 && data.initialBounds.height > 0) {
            data.initialBounds
        } else {
            // Fallback starting bounds from center
            val defW = with(density) { 160.dp.toPx() }
            val defH = with(density) { 200.dp.toPx() }
            Rect(
                left = (screenWidthPx - defW) / 2f,
                top = (screenHeightPx - defH) / 2f,
                right = (screenWidthPx + defW) / 2f,
                bottom = (screenHeightPx + defH) / 2f
            )
        }

        // Determine target aspect ratio based on bitmap or initial shape (default 3:4 portrait)
        val aspectRatio = when {
            bitmap != null && bitmap.height > 0 -> bitmap.width.toFloat() / bitmap.height.toFloat()
            initBounds.height > 0 -> initBounds.width / initBounds.height
            else -> 0.75f
        }

        val maxTargetWidthPx = screenWidthPx - with(density) { 56.dp.toPx() }
        val maxTargetHeightPx = screenHeightPx - with(density) { 120.dp.toPx() }

        var targetWidthPx = maxTargetWidthPx
        var targetHeightPx = targetWidthPx / aspectRatio

        if (targetHeightPx > maxTargetHeightPx) {
            targetHeightPx = maxTargetHeightPx
            targetWidthPx = targetHeightPx * aspectRatio
        }

        val targetLeftPx = (screenWidthPx - targetWidthPx) / 2f
        val targetTopPx = (screenHeightPx - targetHeightPx) / 2f

        val p = morphProgress.value
        val curLeftPx = initBounds.left + (targetLeftPx - initBounds.left) * p
        val curTopPx = initBounds.top + (targetTopPx - initBounds.top) * p
        val curWidthPx = initBounds.width + (targetWidthPx - initBounds.width) * p
        val curHeightPx = initBounds.height + (targetHeightPx - initBounds.height) * p

        val curCornerRadiusDp = data.initialCornerRadiusDp + (24f - data.initialCornerRadiusDp) * p
        val curElevationDp = 4f + (20f - 4f) * p

        val curWidthDp = with(density) { curWidthPx.toDp() }
        val curHeightDp = with(density) { curHeightPx.toDp() }

        // 3. Morphing Floating Card
        Box(
            modifier = Modifier
                .offset { IntOffset(curLeftPx.toInt(), curTopPx.toInt()) }
                .size(curWidthDp, curHeightDp)
                .shadow(
                    elevation = curElevationDp.dp,
                    shape = RoundedCornerShape(curCornerRadiusDp.dp)
                )
                .clip(RoundedCornerShape(curCornerRadiusDp.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 1.5.dp,
                    color = data.folderColor.copy(alpha = 0.35f + 0.35f * p),
                    shape = RoundedCornerShape(curCornerRadiusDp.dp)
                )
        ) {
            // Note Full Preview Image
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = data.note.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(data.folderColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (data.note.fileType) {
                            NoteFileType.PDF -> Icons.Default.PictureAsPdf
                            else -> Icons.Default.Description
                        },
                        contentDescription = null,
                        tint = data.folderColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            // Top Floating Header Pill (Fades in gracefully during morph)
            if (p > 0.3f) {
                val headerAlpha = ((p - 0.3f) / 0.7f).coerceIn(0f, 1f)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                        .graphicsLayer { alpha = headerAlpha }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        data.note.folder.takeIf { it.isNotBlank() }?.let { _ ->
                            Surface(
                                shape = CircleShape,
                                color = data.folderColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = data.folderColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = data.note.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = data.folderColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = ".${data.note.file.extension}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = data.folderColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }

                        if (data.note.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Data class representing an individual stardust speck for the Google Pixel style blur sprinkle.
 */
private data class PixelParticle(
    val xNorm: Float,
    val yNorm: Float,
    val baseRadiusPx: Float,
    val baseAlpha: Float,
    val shimmerSpeed: Float,
    val phase: Float
)

/**
 * High-performance Canvas rendering of sprinkled white micro-particles (Google Pixel fingerprint unlock style).
 * Sized ultra-fine to visually look like crisp pinpoint pixel dust across all display densities.
 */
@Composable
private fun PixelParticleField(
    progress: Float,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val particleCount = 750

    val particles = remember(density) {
        val random = Random(1337L)
        // Normalize physical dot radius so particles appear as ultra-fine micro-specks across all DPIs
        val dpiScale = (density / 2.75f).coerceIn(0.85f, 1.25f)

        List(particleCount) {
            val r = random.nextFloat()
            val radius = when {
                r < 0.80f -> (0.65f + random.nextFloat() * 0.50f) * dpiScale // Ultra-fine micro-speck (0.65 - 1.15px)
                r < 0.95f -> (1.15f + random.nextFloat() * 0.40f) * dpiScale // Fine sparkling speck (1.15 - 1.55px)
                else -> (1.55f + random.nextFloat() * 0.30f) * dpiScale      // Occasional crisp star grain (1.55 - 1.85px)
            }
            PixelParticle(
                xNorm = random.nextFloat(),
                yNorm = random.nextFloat(),
                baseRadiusPx = radius,
                baseAlpha = 0.15f + random.nextFloat() * 0.75f,
                shimmerSpeed = 0.8f + random.nextFloat() * 2.2f,
                phase = random.nextFloat() * (2f * Math.PI.toFloat())
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particleShimmer")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleTime"
    )

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val totalAlphaMultiplier = progress * alpha

        if (totalAlphaMultiplier <= 0.01f) return@Canvas

        for (i in 0 until particleCount) {
            val p = particles[i]
            val x = p.xNorm * canvasWidth
            val y = p.yNorm * canvasHeight

            val shimmer = (sin(time * p.shimmerSpeed + p.phase) * 0.35f + 0.65f)
            val dynamicAlpha = (p.baseAlpha * shimmer * totalAlphaMultiplier).coerceIn(0f, 1f)

            // Draw crisp, ultra-fine micro-dot
            drawCircle(
                color = Color.White.copy(alpha = dynamicAlpha),
                radius = p.baseRadiusPx,
                center = Offset(x, y)
            )
        }
    }
}
