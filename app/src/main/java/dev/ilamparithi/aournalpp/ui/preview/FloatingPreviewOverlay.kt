package dev.ilamparithi.aournalpp.ui.preview

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RuntimeShader
import android.os.Build
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
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ShaderBrush
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
import org.intellij.lang.annotations.Language
import java.io.File
import java.util.Random
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

@Language("AGSL")
private const val PERSISTENT_TURBULENCE_SHADER = """
    uniform float2 resolution;
    uniform float2 origin;
    uniform float time;
    uniform float progress;
    uniform float maxRadius;
    uniform float4 color;

    // High-frequency hash for crisp micro-sparkle grain (not cloudy blobs)
    float hash(float2 p) {
        return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
    }

    float grain(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float2 u = f * f * (3.0 - 2.0 * f);
        return mix(mix(hash(i + float2(0.0, 0.0)), hash(i + float2(1.0, 0.0)), u.x),
                   mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x), u.y);
    }

    half4 main(float2 fragCoord) {
        float dist = distance(fragCoord, origin);
        float currentRadius = maxRadius * progress;

        if (dist > currentRadius + 20.0) {
            return half4(0.0);
        }

        // High frequency fine sparkle noise (crisp pinpoints, zero cloudiness)
        float g1 = grain(fragCoord * 0.35 + float2(time * 0.8, time * 0.4));
        float g2 = grain(fragCoord * 0.70 - float2(time * 0.6, time * 0.7));
        float g = g1 * 0.6 + g2 * 0.4;

        // Sharp threshold for delicate, subtle pinpoint sparkles
        float sparkle = smoothstep(0.78, 0.94, g);
        float waveFront = 1.0 - clamp(abs(dist - currentRadius) / 60.0, 0.0, 1.0);
        float edgeFade = 1.0 - smoothstep(currentRadius - 10.0, currentRadius + 20.0, dist);

        float a = color.a * (sparkle * 0.75 + waveFront * 0.25) * edgeFade;
        return half4(color.rgb, clamp(a, 0.0, 1.0));
    }
"""

/**
 * Material 3 Standard / Emphasized Motion Easing.
 * Slow start with dynamic acceleration and gentle, soft-landing deceleration.
 */
private val M3MotionEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

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
 * 1. Native Android 12+ (API 31+) Sparkle / Turbulence Ripple via [Modifier.indication] and [ripple].
 * 2. Radial sweep expanding from the long-press coordinate across the entire backdrop.
 * 3. Morphing image preview with full 1200p+ HD rendering and crisp FilterQuality.High.
 * 4. Seamless fade-out on finger release without blocking subsequent touches on MainActivity.
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
    val rippleInteractionSource = remember { MutableInteractionSource() }
    var activePress by remember { mutableStateOf<PressInteraction.Press?>(null) }

    // Trigger morph-in animation and emit native Sparkle Ripple press at touch origin
    LaunchedEffect(data) {
        morphProgress.snapTo(0f)
        fadeAlpha.snapTo(1f)

        val origin = if (data.touchPositionInWindow != Offset.Unspecified) {
            data.touchPositionInWindow
        } else {
            data.initialBounds.center
        }
        val press = PressInteraction.Press(origin)
        activePress = press
        rippleInteractionSource.emit(press)

        morphProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 360,
                easing = M3MotionEasing
            )
        )
    }

    // Trigger fade-out animation and release ripple when finger is released
    LaunchedEffect(isFingerDown) {
        if (!isFingerDown) {
            activePress?.let { press ->
                rippleInteractionSource.emit(PressInteraction.Release(press))
                activePress = null
            }
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

    val bitmap = remember(data.thumbnailFile, data.note.file) {
        data.thumbnailFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                try {
                    val opts = BitmapFactory.Options().apply {
                        inScaled = false
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    BitmapFactory.decodeFile(file.absolutePath, opts)
                } catch (e: Exception) { null }
            } else null
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = currentAlpha
            }
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        // 1. Initial Bounds Geometry
        val initBounds = if (data.initialBounds != Rect.Zero && data.initialBounds.width > 0 && data.initialBounds.height > 0) {
            data.initialBounds
        } else {
            val defW = with(density) { 160.dp.toPx() }
            val defH = with(density) { 200.dp.toPx() }
            Rect(
                left = (screenWidthPx - defW) / 2f,
                top = (screenHeightPx - defH) / 2f,
                right = (screenWidthPx + defW) / 2f,
                bottom = (screenHeightPx + defH) / 2f
            )
        }

        // Long press origin for radial sweep and native sparkle ripple
        val origin = if (data.touchPositionInWindow != Offset.Unspecified &&
            data.touchPositionInWindow.x in 0f..screenWidthPx &&
            data.touchPositionInWindow.y in 0f..screenHeightPx
        ) {
            data.touchPositionInWindow
        } else {
            initBounds.center
        }

        // 2. Dark Blur Scrim with Native Android 12+ Sparkle / Noise Ripple
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0C10).copy(alpha = 0.70f * morphProgress.value))
                .blur(radius = (16.dp * morphProgress.value).coerceAtLeast(0.1.dp))
                .indication(
                    interactionSource = rippleInteractionSource,
                    indication = ripple(
                        bounded = true,
                        color = Color.White
                    )
                )
        )

        // 3. Persistent AGSL Noise Turbulence (keeps the shimmer active after ripple sweep)
        PersistentTurbulenceOverlay(
            origin = origin,
            progress = morphProgress.value,
            alpha = currentAlpha,
            modifier = Modifier.fillMaxSize()
        )

        // 4. Sweeping Particle Sparkle Constellation
        PixelParticleField(
            origin = origin,
            accentColor = data.folderColor,
            progress = morphProgress.value,
            alpha = currentAlpha,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Compute Morphing Geometry from initial bounds to centered target bounds
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

        // 4. Morphing Floating Card
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
            // Note Full High-Quality Preview Image
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = data.note.title,
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (data.note.fileType) {
                                NoteFileType.PDF -> Icons.Default.PictureAsPdf
                                else -> Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = data.folderColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = data.note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Top-Left: Standardized File Type Pill
            dev.ilamparithi.aournalpp.ui.FileTypePill(
                fileType = data.note.fileType,
                fontSize = 10f,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .graphicsLayer { alpha = p }
            )

            // Top-Right: Pinned Pushpin Badge (if pinned)
            if (data.note.isPinned) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(28.dp)
                        .graphicsLayer { alpha = p }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned Note",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Bottom: Floating Full Details Pill (Shows complete title, folder, date, time & size without ellipsis)
            val pillBgColor = data.folderColor.copy(alpha = 0.22f)
                .compositeOver(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .graphicsLayer { alpha = p },
                shape = RoundedCornerShape(14.dp),
                color = pillBgColor,
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, data.folderColor.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Title (Full text, wraps naturally without ellipses)
                    Text(
                        text = data.note.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Bottom Row: Folder, Full Date, Time & Size
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val folderDisplayName = if (data.note.folder.isBlank()) "Notes Home" else data.note.folder
                        val isHome = data.note.folder.isBlank() || data.note.folder == "Notes Home"
                        val isEmergency = data.note.folderIconType == "emergency" || data.note.folder.equals("Emergency Saves", ignoreCase = true)

                        Box(
                            modifier = Modifier.size(15.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!data.note.folderIconEmoji.isNullOrBlank()) {
                                Text(
                                    text = data.note.folderIconEmoji,
                                    fontSize = 11.sp,
                                    lineHeight = 11.sp
                                )
                            } else {
                                Icon(
                                    imageVector = when {
                                        isHome -> Icons.Default.Home
                                        isEmergency -> Icons.Default.Emergency
                                        else -> Icons.Default.Folder
                                    },
                                    contentDescription = null,
                                    tint = data.folderColor,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Text(
                            text = folderDisplayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = data.folderColor
                        )

                        if (data.note.fuzzyLastOpened != null) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Opened ${data.note.fuzzyLastOpened}",
                                style = MaterialTheme.typography.labelSmall,
                                color = data.folderColor
                            )
                        }

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Text(
                            text = "Modified ${data.note.fuzzyLastModified}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Text(
                            text = data.note.sizeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
 * High-performance Canvas rendering of radial sweep with sparkling stardust micro-particles.
 * - Sweeps radially outwards from the long-press [origin] to cover the entire background.
 * - Ignites particles with an energetic sparkle flash as the expanding wavefront reaches each point.
 * - Settles into an ambient twinkling starfield constellation over a dark luminous backdrop.
 */
@Composable
private fun PixelParticleField(
    origin: Offset,
    accentColor: Color,
    progress: Float,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val particleCount = 520

    val particles = remember(density) {
        val random = Random(1337L)
        val dpiScale = (density / 2.75f).coerceIn(0.85f, 1.25f)

        List(particleCount) {
            val r = random.nextFloat()
            val radius = when {
                r < 0.70f -> (0.80f + random.nextFloat() * 0.45f) * dpiScale // Ultra-fine crisp micro-speck (0.80 - 1.25px)
                r < 0.90f -> (1.25f + random.nextFloat() * 0.45f) * dpiScale // Sparkling star speck (1.25 - 1.70px)
                else -> (1.70f + random.nextFloat() * 0.40f) * dpiScale      // Bright pinpoint grain (1.70 - 2.10px)
            }
            PixelParticle(
                xNorm = random.nextFloat(),
                yNorm = random.nextFloat(),
                baseRadiusPx = radius,
                baseAlpha = 0.35f + random.nextFloat() * 0.45f,
                shimmerSpeed = 1.2f + random.nextFloat() * 2.2f,
                phase = random.nextFloat() * (2f * Math.PI.toFloat())
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particleShimmer")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleTime"
    )

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val totalAlphaMultiplier = alpha

        if (totalAlphaMultiplier <= 0.01f || progress <= 0.001f) return@Canvas

        // Calculate maximum sweep distance from long press origin to the farthest screen corner
        val maxDist = hypot(
            max(origin.x, canvasWidth - origin.x),
            max(origin.y, canvasHeight - origin.y)
        ) * 1.12f

        val currentSweepRadius = maxDist * progress
        val waveBandWidth = 120f * density

        // 1. Draw Sweeping Radial Scrim & Radiant Wavefront Glow
        if (currentSweepRadius > 1f) {
            val gradientRadius = (currentSweepRadius + 35f * density).coerceAtLeast(10f)
            val waveEdgeFraction = (currentSweepRadius / gradientRadius).coerceIn(0.1f, 0.95f)

            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF0A0C10).copy(alpha = 0.74f * progress * totalAlphaMultiplier),
                        (waveEdgeFraction * 0.85f).coerceIn(0f, 1f) to Color(0xFF0A0C10).copy(alpha = 0.70f * progress * totalAlphaMultiplier),
                        waveEdgeFraction to accentColor.copy(alpha = 0.25f * (1f - progress * 0.45f) * totalAlphaMultiplier),
                        1.0f to Color.Transparent
                    ),
                    center = origin,
                    radius = gradientRadius
                ),
                radius = gradientRadius,
                center = origin
            )
        }

        // 2. Draw Sweeping Particle Sparkle Ignition
        for (i in 0 until particleCount) {
            val p = particles[i]
            val x = p.xNorm * canvasWidth
            val y = p.yNorm * canvasHeight

            val dx = x - origin.x
            val dy = y - origin.y
            val dist = sqrt(dx * dx + dy * dy)

            // If the radial wave hasn't reached this particle yet, skip rendering
            if (dist > currentSweepRadius) continue

            val distFromWavefront = currentSweepRadius - dist
            val isNearWavefront = distFromWavefront < waveBandWidth

            // Flash energy boost as the wavefront sweeps across the particle
            val flash = if (isNearWavefront) {
                (1f - (distFromWavefront / waveBandWidth)).coerceIn(0f, 1f)
            } else {
                0f
            }

            val shimmer = (sin(time * p.shimmerSpeed + p.phase) * 0.40f + 0.60f)
            val dynamicAlpha = ((p.baseAlpha * shimmer + flash * 0.65f) * totalAlphaMultiplier).coerceIn(0f, 1f)
            val dynamicRadius = p.baseRadiusPx * (1f + flash * 0.50f)

            drawCircle(
                color = Color.White.copy(alpha = dynamicAlpha),
                radius = dynamicRadius,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * Persistent AGSL runtime noise turbulence layer.
 * Replicates the platform's sparkle/turbulence ripple shader and keeps the animated noise active
 * indefinitely over the preview backdrop on Android 13+ (API 33+).
 */
@Composable
private fun PersistentTurbulenceOverlay(
    origin: Offset,
    progress: Float,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    if (alpha <= 0.01f || progress <= 0.001f) return

    val infiniteTransition = rememberInfiniteTransition(label = "agslTurbulence")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "agslTime"
    )

    val shader = remember {
        try {
            RuntimeShader(PERSISTENT_TURBULENCE_SHADER)
        } catch (e: Throwable) {
            null
        }
    }

    if (shader != null) {
        Box(
            modifier = modifier.drawWithCache {
                try {
                    val maxDist = hypot(
                        max(origin.x, size.width - origin.x),
                        max(origin.y, size.height - origin.y)
                    ) * 1.15f

                    shader.setFloatUniform("resolution", size.width, size.height)
                    shader.setFloatUniform("origin", origin.x, origin.y)
                    shader.setFloatUniform("time", time)
                    shader.setFloatUniform("progress", progress)
                    shader.setFloatUniform("maxRadius", maxDist)
                    shader.setFloatUniform("color", 1.0f, 1.0f, 1.0f, 0.35f * alpha)
                    val brush = ShaderBrush(shader)

                    onDrawWithContent {
                        drawContent()
                        drawRect(brush = brush)
                    }
                } catch (e: Throwable) {
                    onDrawWithContent {
                        drawContent()
                    }
                }
            }
        )
    }
}

