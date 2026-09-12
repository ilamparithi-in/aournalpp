package dev.ilamparithi.aournalpp.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

/**
 * Standardized spring slide and fade animation matching MainActivity.
 *
 * Parameters:
 * - Slide: spring(dampingRatio = 0.82f, stiffness = 380f)
 * - Fade: spring(dampingRatio = 0.9f, stiffness = 400f)
 * - Offset fraction: width / 3
 * - Direction offsets: enterOffset = 1 / -1, exitOffset = -1 / 1
 */
object SpringSlideTransition {
    const val DEFAULT_OFFSET_FRACTION = 3
    const val SLIDE_DAMPING = 0.82f
    const val SLIDE_STIFFNESS = 380f
    const val FADE_DAMPING = 0.9f
    const val FADE_STIFFNESS = 400f

    fun calculateInitialOffsetX(width: Int, isForward: Boolean, offsetFraction: Int = DEFAULT_OFFSET_FRACTION): Int {
        val enterOffset = if (isForward) 1 else -1
        return (width / offsetFraction) * enterOffset
    }

    fun calculateTargetOffsetX(width: Int, isForward: Boolean, offsetFraction: Int = DEFAULT_OFFSET_FRACTION): Int {
        val exitOffset = if (isForward) -1 else 1
        return (width / offsetFraction) * exitOffset
    }

    fun <T> createSpec(
        isForward: Boolean,
        reduceAnimations: Boolean = false,
        isRapid: Boolean = false,
        offsetFraction: Int = DEFAULT_OFFSET_FRACTION
    ): AnimatedContentTransitionScope<T>.() -> ContentTransform = {
        if (reduceAnimations) {
            fadeIn(animationSpec = tween(120))
                .togetherWith(fadeOut(animationSpec = tween(100)))
        } else if (isRapid) {
            // Rapid switching fallback: Fast, crisp directional slide & fade without residual spring bounce
            (slideInHorizontally(
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                initialOffsetX = { calculateInitialOffsetX(it, isForward, offsetFraction) }
            ) + fadeIn(
                animationSpec = tween(durationMillis = 140)
            ))
                .togetherWith(
                    slideOutHorizontally(
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                        targetOffsetX = { calculateTargetOffsetX(it, isForward, offsetFraction) }
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 120)
                    )
                )
        } else {
            (slideInHorizontally(
                animationSpec = spring(
                    dampingRatio = SLIDE_DAMPING,
                    stiffness = SLIDE_STIFFNESS
                ),
                initialOffsetX = { calculateInitialOffsetX(it, isForward, offsetFraction) }
            ) + fadeIn(
                animationSpec = spring(
                    dampingRatio = FADE_DAMPING,
                    stiffness = FADE_STIFFNESS
                )
            ))
                .togetherWith(
                    slideOutHorizontally(
                        animationSpec = spring(
                            dampingRatio = SLIDE_DAMPING,
                            stiffness = SLIDE_STIFFNESS
                        ),
                        targetOffsetX = { calculateTargetOffsetX(it, isForward, offsetFraction) }
                    ) + fadeOut(
                        animationSpec = spring(
                            dampingRatio = FADE_DAMPING,
                            stiffness = FADE_STIFFNESS
                        )
                    )
                )
        }
    }
}
