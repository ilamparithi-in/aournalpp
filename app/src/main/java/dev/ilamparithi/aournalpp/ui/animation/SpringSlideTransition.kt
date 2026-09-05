package dev.ilamparithi.aournalpp.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
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

    fun <T> createSpec(
        isForward: Boolean,
        reduceAnimations: Boolean = false,
        offsetFraction: Int = DEFAULT_OFFSET_FRACTION
    ): AnimatedContentTransitionScope<T>.() -> ContentTransform = {
        if (reduceAnimations) {
            fadeIn(animationSpec = tween(120))
                .togetherWith(fadeOut(animationSpec = tween(100)))
        } else {
            val enterOffset = if (isForward) 1 else -1
            val exitOffset = if (isForward) -1 else 1

            (slideInHorizontally(
                animationSpec = spring(
                    dampingRatio = SLIDE_DAMPING,
                    stiffness = SLIDE_STIFFNESS
                ),
                initialOffsetX = { (it / offsetFraction) * enterOffset }
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
                        targetOffsetX = { -(it / offsetFraction) * exitOffset }
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
