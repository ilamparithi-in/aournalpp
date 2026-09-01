package dev.ilamparithi.aournalpp.ui.predictive

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * State holder for tracking and animating Android Predictive Back gestures.
 */
@Stable
class PredictiveBackState(
    val reduceAnimations: Boolean = false,
    val maxScaleReduction: Float = 0.10f,
    val maxCornerRadius: Dp = 28.dp,
    val maxTranslationX: Dp = 36.dp
) {
    var isGestureActive by mutableStateOf(false)
        internal set

    var rawProgress by mutableFloatStateOf(0f)
        internal set

    var swipeEdge by mutableIntStateOf(BackEventCompat.EDGE_LEFT)
        internal set

    var touchX by mutableFloatStateOf(0f)
        internal set

    var touchY by mutableFloatStateOf(0f)
        internal set

    val progressAnimatable = Animatable(0f)

    /**
     * Eased progress value (0.0f to 1.0f) applying standard deceleration curve.
     */
    val easedProgress: Float
        get() = if (reduceAnimations) {
            progressAnimatable.value
        } else {
            FastOutSlowInEasing.transform(progressAnimatable.value)
        }

    /**
     * Scaling factor for surface (e.g. 1.0 down to 0.90).
     */
    val scale: Float
        get() = if (reduceAnimations) 1f else (1f - (easedProgress * maxScaleReduction))

    /**
     * Dynamic corner radius to give sheet/card detachment look during swipe.
     */
    val cornerRadius: Dp
        get() = if (reduceAnimations) 0.dp else (maxCornerRadius * easedProgress)

    /**
     * Directional horizontal shift along the swipe direction.
     */
    fun calculateTranslationX(density: Float): Float {
        if (reduceAnimations) return 0f
        val direction = if (swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
        val maxPx = maxTranslationX.value * density
        return direction * maxPx * easedProgress
    }
}

/**
 * Creates and remembers a [PredictiveBackState].
 */
@Composable
fun rememberPredictiveBackState(
    reduceAnimations: Boolean = false,
    maxScaleReduction: Float = 0.10f,
    maxCornerRadius: Dp = 28.dp,
    maxTranslationX: Dp = 36.dp
): PredictiveBackState {
    return remember(reduceAnimations, maxScaleReduction, maxCornerRadius, maxTranslationX) {
        PredictiveBackState(
            reduceAnimations = reduceAnimations,
            maxScaleReduction = maxScaleReduction,
            maxCornerRadius = maxCornerRadius,
            maxTranslationX = maxTranslationX
        )
    }
}

/**
 * A container that handles Predictive Back gestures according to Android 14/15+ design specifications.
 *
 * As the user performs a back swipe from either edge:
 * - Foreground content smoothly scales down (up to 90%), translates in the direction of the swipe,
 *   and rounds its corners.
 * - Optional [backgroundContent] is revealed directly beneath the scaling surface.
 * - If the gesture is committed, [onBack] is invoked.
 * - If the gesture is cancelled, the surface springs back to full-screen resting state.
 */
@Composable
fun PredictiveBackLayout(
    enabled: Boolean = true,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    reduceAnimations: Boolean = false,
    maxScaleReduction: Float = 0.10f,
    maxCornerRadius: Dp = 28.dp,
    maxTranslationX: Dp = 36.dp,
    scrimColor: Color = Color.Black.copy(alpha = 0.25f),
    backgroundContent: (@Composable () -> Unit)? = null,
    content: @Composable (isGestureActive: Boolean, progress: Float) -> Unit
) {
    val state = rememberPredictiveBackState(
        reduceAnimations = reduceAnimations,
        maxScaleReduction = maxScaleReduction,
        maxCornerRadius = maxCornerRadius,
        maxTranslationX = maxTranslationX
    )

    val density = LocalDensity.current.density
    val coroutineScope = rememberCoroutineScope()

    PredictiveBackHandler(enabled = enabled) { backEvents: Flow<BackEventCompat> ->
        try {
            state.isGestureActive = true
            backEvents.collect { event ->
                state.rawProgress = event.progress
                state.swipeEdge = event.swipeEdge
                state.touchX = event.touchX
                state.touchY = event.touchY
                state.progressAnimatable.snapTo(event.progress)
            }
            // User released / committed gesture: finish slide to 100%
            if (!reduceAnimations) {
                state.progressAnimatable.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                )
            }
            onBack()
        } catch (e: CancellationException) {
            // User cancelled / swiped back to edge: spring back to 0%
            try {
                state.progressAnimatable.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            } catch (_: Throwable) {}
        } finally {
            state.progressAnimatable.snapTo(0f)
            state.rawProgress = 0f
            state.isGestureActive = false
        }
    }

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val progress = state.progressAnimatable.value
        val showProgress = state.isGestureActive || progress > 0f

        // Destination screen underneath (slides in from left with parallax)
        if (backgroundContent != null && showProgress && enabled) {
            val bgTranslationX = if (reduceAnimations) 0f else -(1f - progress) * (widthPx * 0.28f)
            val bgAlpha = if (reduceAnimations) 1f else (0.85f + 0.15f * progress).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = bgTranslationX
                        alpha = bgAlpha
                    }
            ) {
                backgroundContent()

                val scrimAlpha = ((1f - progress) * 0.25f).coerceIn(0f, 0.4f)
                if (scrimAlpha > 0f && !reduceAnimations) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = scrimAlpha))
                    )
                }
            }
        }

        // Current screen in foreground (slides out to right)
        val fgTranslationX = if (reduceAnimations) 0f else progress * widthPx
        val fgScale = if (reduceAnimations) 1f else 1f - (progress * 0.05f)
        val fgCornerRadius = if (reduceAnimations || !showProgress) 0.dp else (progress * 20.dp.value).dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = fgTranslationX
                    scaleX = fgScale
                    scaleY = fgScale
                    if (fgCornerRadius > 0.dp) {
                        shadowElevation = 16f * progress
                        shape = RoundedCornerShape(fgCornerRadius)
                        clip = true
                    }
                }
        ) {
            content(state.isGestureActive, progress)
        }
    }
}
