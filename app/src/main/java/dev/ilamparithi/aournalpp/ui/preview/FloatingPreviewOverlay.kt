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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ilamparithi.aournalpp.model.NoteDocument
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
    onTriggerAction: ((NoteDocument, DragActionTarget) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val controller = remember { FloatingPreviewController() }

    LaunchedEffect(onTriggerAction) {
        controller.registerActionCallback(onTriggerAction)
    }

    CompositionLocalProvider(LocalFloatingPreviewController provides controller) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()

            val previewData = controller.currentPreview.value
            if (previewData != null) {
                FloatingPreviewOverlay(
                    data = previewData,
                    controller = controller,
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
 * 4. Material 3 Drag Actions: Drag left/up to highlight "View as PDF", drag right/down for "Edit in Xournal++".
 * 5. Material 3 Motion physics "flying" card animation and dynamic space-pushing when clearance is constrained.
 * 6. Seamless fade-out on finger release without blocking subsequent touches on MainActivity.
 */
@Composable
private fun FloatingPreviewOverlay(
    data: FloatingPreviewData,
    controller: FloatingPreviewController,
    onDismissFinished: () -> Unit
) {
    val density = LocalDensity.current
    val morphProgress = remember { Animatable(0f) }
    val fadeAlpha = remember { Animatable(1f) }
    val rippleInteractionSource = remember { MutableInteractionSource() }
    var activePress by remember { mutableStateOf<PressInteraction.Press?>(null) }

    val isFingerDown = controller.isFingerDown.value
    val activeAction = controller.activeAction.value
    val dragDelta = controller.dragDelta.value

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
            // Concurrently drive morphProgress to 1f and fadeAlpha to 0f
            // so the card never freezes in a miniature intermediate state
            coroutineScope {
                launch {
                    morphProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 180,
                            easing = FastOutLinearInEasing
                        )
                    )
                }
                launch {
                    fadeAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = 180,
                            easing = FastOutLinearInEasing
                        )
                    )
                }
            }
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
        val isLandscape = screenWidthPx >= screenHeightPx

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

        // 3. Persistent AGSL Noise Turbulence
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

        // 5. Morphing Geometry Computation
        val aspectRatio = when {
            bitmap != null && bitmap.height > 0 -> bitmap.width.toFloat() / bitmap.height.toFloat()
            initBounds.height > 0 -> initBounds.width / initBounds.height
            else -> 0.75f
        }

        val maxTargetWidthPx = if (isLandscape) {
            screenWidthPx - with(density) { 80.dp.toPx() }
        } else {
            screenWidthPx - with(density) { 48.dp.toPx() }
        }

        val maxTargetHeightPx = if (isLandscape) {
            screenHeightPx - with(density) { 72.dp.toPx() }
        } else {
            screenHeightPx - with(density) { 140.dp.toPx() }
        }

        var targetWidthPx = maxTargetWidthPx
        var targetHeightPx = targetWidthPx / aspectRatio

        if (targetHeightPx > maxTargetHeightPx) {
            targetHeightPx = maxTargetHeightPx
            targetWidthPx = targetHeightPx * aspectRatio
        }

        val targetLeftPx = (screenWidthPx - targetWidthPx) / 2f
        val targetTopPx = (screenHeightPx - targetHeightPx) / 2f

        // Space & Pushing Calculation
        val actionBoxWidthPx = with(density) { 180.dp.toPx() }
        val actionBoxHeightPx = with(density) { 110.dp.toPx() }

        val availableSideSpacePx = targetLeftPx
        val requiredSideSpacePx = actionBoxWidthPx + with(density) { 20.dp.toPx() }
        val isLandscapeSpaceConstrained = availableSideSpacePx < requiredSideSpacePx
        val maxPushX = if (isLandscapeSpaceConstrained) (requiredSideSpacePx - availableSideSpacePx) else 0f

        val availableVerticalSpacePx = targetTopPx
        val requiredVerticalSpacePx = actionBoxHeightPx + with(density) { 20.dp.toPx() }
        val isPortraitSpaceConstrained = availableVerticalSpacePx < requiredVerticalSpacePx
        val maxPushY = if (isPortraitSpaceConstrained) (requiredVerticalSpacePx - availableVerticalSpacePx) else 0f

        val targetPushX = when {
            !isLandscape -> 0f
            activeAction == DragActionTarget.VIEW_PDF -> maxPushX // Drag left -> push card right
            activeAction == DragActionTarget.EDIT_CANVAS -> -maxPushX // Drag right -> push card left
            else -> 0f
        }

        val targetPushY = when {
            isLandscape -> 0f
            activeAction == DragActionTarget.VIEW_PDF -> maxPushY // Drag up -> push card down
            activeAction == DragActionTarget.EDIT_CANVAS -> -maxPushY // Drag down -> push card up
            else -> 0f
        }

        val animatedPushX by animateFloatAsState(
            targetValue = targetPushX,
            animationSpec = spring(dampingRatio = 0.80f, stiffness = 340f),
            label = "pushX"
        )
        val animatedPushY by animateFloatAsState(
            targetValue = targetPushY,
            animationSpec = spring(dampingRatio = 0.80f, stiffness = 340f),
            label = "pushY"
        )

        // Flying dynamic tilt & lag
        val flyingLagX = (dragDelta.x * 0.10f).coerceIn(-28f, 28f)
        val flyingLagY = (dragDelta.y * 0.10f).coerceIn(-28f, 28f)

        val targetTilt = when (activeAction) {
            DragActionTarget.VIEW_PDF -> if (isLandscape) -2.8f else -1.2f
            DragActionTarget.EDIT_CANVAS -> if (isLandscape) 2.8f else 1.2f
            DragActionTarget.NONE -> (dragDelta.x / screenWidthPx * 6f).coerceIn(-1.5f, 1.5f)
        }
        val animatedTilt by animateFloatAsState(
            targetValue = targetTilt,
            animationSpec = spring(dampingRatio = 0.80f, stiffness = 340f),
            label = "tilt"
        )

        val targetScale = if (activeAction != DragActionTarget.NONE) 0.98f else 1.0f
        val animatedScale by animateFloatAsState(
            targetValue = targetScale,
            animationSpec = spring(dampingRatio = 0.80f, stiffness = 340f),
            label = "scale"
        )

        val p = morphProgress.value
        val curLeftPx = initBounds.left + (targetLeftPx - initBounds.left) * p + animatedPushX * p + flyingLagX * p
        val curTopPx = initBounds.top + (targetTopPx - initBounds.top) * p + animatedPushY * p + flyingLagY * p
        val curWidthPx = initBounds.width + (targetWidthPx - initBounds.width) * p
        val curHeightPx = initBounds.height + (targetHeightPx - initBounds.height) * p

        val curCornerRadiusDp = data.initialCornerRadiusDp + (24f - data.initialCornerRadiusDp) * p
        val curElevationDp = 4f + (20f - 4f) * p

        val curWidthDp = with(density) { curWidthPx.toDp() }
        val curHeightDp = with(density) { curHeightPx.toDp() }

        // 6. Action Region Highlights ("View as PDF" & "Edit in Xournal++")
        if (isLandscape) {
            // LANDSCAPE: Left = View as PDF, Right = Edit in Xournal++
            val isViewPdfActive = activeAction == DragActionTarget.VIEW_PDF
            val viewPdfAlpha by animateFloatAsState(if (isViewPdfActive) 1f else 0f, spring(0.8f, 350f), label = "viewPdfAlpha")
            val viewPdfScale by animateFloatAsState(if (isViewPdfActive) 1f else 0.86f, spring(0.75f, 320f), label = "viewPdfScale")
            val viewPdfSlideX by animateDpAsState(if (isViewPdfActive) 0.dp else (-24).dp, spring(0.8f, 350f), label = "viewPdfSlideX")

            if (viewPdfAlpha > 0.005f) {
                ActionRegionCard(
                    icon = Icons.Default.PictureAsPdf,
                    title = "VIEW AS PDF",
                    subtitle = "Release finger to view",
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.94f * viewPdfAlpha),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f * viewPdfAlpha),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp, top = 32.dp, bottom = 32.dp)
                        .width(with(density) { actionBoxWidthPx.toDp() })
                        .height(curHeightDp.coerceAtLeast(160.dp))
                        .offset(x = viewPdfSlideX)
                        .graphicsLayer {
                            alpha = viewPdfAlpha * p
                            scaleX = viewPdfScale
                            scaleY = viewPdfScale
                        }
                )
            }

            val isEditCanvasActive = activeAction == DragActionTarget.EDIT_CANVAS
            val editCanvasAlpha by animateFloatAsState(if (isEditCanvasActive) 1f else 0f, spring(0.8f, 350f), label = "editCanvasAlpha")
            val editCanvasScale by animateFloatAsState(if (isEditCanvasActive) 1f else 0.86f, spring(0.75f, 320f), label = "editCanvasScale")
            val editCanvasSlideX by animateDpAsState(if (isEditCanvasActive) 0.dp else 24.dp, spring(0.8f, 350f), label = "editCanvasSlideX")

            if (editCanvasAlpha > 0.005f) {
                ActionRegionCard(
                    icon = Icons.Default.Edit,
                    title = "EDIT IN XOURNAL++",
                    subtitle = "Release finger to edit",
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f * editCanvasAlpha),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f * editCanvasAlpha),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp, top = 32.dp, bottom = 32.dp)
                        .width(with(density) { actionBoxWidthPx.toDp() })
                        .height(curHeightDp.coerceAtLeast(160.dp))
                        .offset(x = editCanvasSlideX)
                        .graphicsLayer {
                            alpha = editCanvasAlpha * p
                            scaleX = editCanvasScale
                            scaleY = editCanvasScale
                        }
                )
            }
        } else {
            // PORTRAIT: Top = View as PDF, Bottom = Edit in Xournal++
            val isViewPdfActive = activeAction == DragActionTarget.VIEW_PDF
            val viewPdfAlpha by animateFloatAsState(if (isViewPdfActive) 1f else 0f, spring(0.8f, 350f), label = "viewPdfAlphaPort")
            val viewPdfScale by animateFloatAsState(if (isViewPdfActive) 1f else 0.86f, spring(0.75f, 320f), label = "viewPdfScalePort")
            val viewPdfSlideY by animateDpAsState(if (isViewPdfActive) 0.dp else (-20).dp, spring(0.8f, 350f), label = "viewPdfSlideY")

            if (viewPdfAlpha > 0.005f) {
                ActionRegionCardHorizontal(
                    icon = Icons.Default.PictureAsPdf,
                    title = "VIEW AS PDF",
                    subtitle = "Release finger to view",
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.94f * viewPdfAlpha),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f * viewPdfAlpha),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .fillMaxWidth()
                        .height(with(density) { actionBoxHeightPx.toDp() })
                        .offset(y = viewPdfSlideY)
                        .graphicsLayer {
                            alpha = viewPdfAlpha * p
                            scaleX = viewPdfScale
                            scaleY = viewPdfScale
                        }
                )
            }

            val isEditCanvasActive = activeAction == DragActionTarget.EDIT_CANVAS
            val editCanvasAlpha by animateFloatAsState(if (isEditCanvasActive) 1f else 0f, spring(0.8f, 350f), label = "editCanvasAlphaPort")
            val editCanvasScale by animateFloatAsState(if (isEditCanvasActive) 1f else 0.86f, spring(0.75f, 320f), label = "editCanvasScalePort")
            val editCanvasSlideY by animateDpAsState(if (isEditCanvasActive) 0.dp else 20.dp, spring(0.8f, 350f), label = "editCanvasSlideY")

            if (editCanvasAlpha > 0.005f) {
                ActionRegionCardHorizontal(
                    icon = Icons.Default.Edit,
                    title = "EDIT IN XOURNAL++",
                    subtitle = "Release finger to edit",
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f * editCanvasAlpha),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f * editCanvasAlpha),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .fillMaxWidth()
                        .height(with(density) { actionBoxHeightPx.toDp() })
                        .offset(y = editCanvasSlideY)
                        .graphicsLayer {
                            alpha = editCanvasAlpha * p
                            scaleX = editCanvasScale
                            scaleY = editCanvasScale
                        }
                )
            }
        }

        // 7. Morphing & Flying Card
        Box(
            modifier = Modifier
                .offset { IntOffset(curLeftPx.toInt(), curTopPx.toInt()) }
                .size(curWidthDp, curHeightDp)
                .graphicsLayer {
                    rotationZ = animatedTilt * p
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
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

            // Bottom: Floating Full Details Pill
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
                border = BorderStroke(1.dp, data.folderColor.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = data.note.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().basicMarquee()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val folderDisplayName = if (data.note.folder.isBlank()) "Notes Home" else data.note.folder
                        val isHome = data.note.folder.isBlank() || data.note.folder == "Notes Home"
                        val isEmergency = data.note.folderIconType == "emergency" || data.note.folder.equals("Emergency Saves", ignoreCase = true)

                        Box(
                            modifier = Modifier.size(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!data.note.folderIconEmoji.isNullOrBlank()) {
                                Text(
                                    text = data.note.folderIconEmoji,
                                    fontSize = 10.sp,
                                    lineHeight = 10.sp
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
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Text(
                            text = folderDisplayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = data.folderColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        val timeText = if (data.note.fuzzyLastOpened != null) {
                            "Opened ${data.note.fuzzyLastOpened}"
                        } else {
                            "Modified ${data.note.fuzzyLastModified}"
                        }

                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false).basicMarquee()
                        )

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Text(
                            text = data.note.sizeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRegionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(2.dp, borderColor),
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.16f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = contentColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ActionRegionCardHorizontal(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(2.dp, borderColor),
        shadowElevation = 14.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.16f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
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
                r < 0.70f -> (0.80f + random.nextFloat() * 0.45f) * dpiScale
                r < 0.90f -> (1.25f + random.nextFloat() * 0.45f) * dpiScale
                else -> (1.70f + random.nextFloat() * 0.40f) * dpiScale
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

        val maxDist = hypot(
            max(origin.x, canvasWidth - origin.x),
            max(origin.y, canvasHeight - origin.y)
        ) * 1.12f

        val currentSweepRadius = maxDist * progress
        val waveBandWidth = 120f * density

        // Draw Sweeping Radial Scrim & Radiant Wavefront Glow
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

        // Draw Sweeping Particle Sparkle Ignition
        for (i in 0 until particleCount) {
            val p = particles[i]
            val x = p.xNorm * canvasWidth
            val y = p.yNorm * canvasHeight

            val dx = x - origin.x
            val dy = y - origin.y
            val dist = sqrt(dx * dx + dy * dy)

            if (dist > currentSweepRadius) continue

            val distFromWavefront = currentSweepRadius - dist
            val isNearWavefront = distFromWavefront < waveBandWidth

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
