package dev.ilamparithi.aournalpp.ui.animation

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * A wrapper around [InfiniteTransition] that respects "Reduce Animations".
 * When reduce motion is enabled, [animateFloat] and [animateColor] return a static [State]
 * with the initial value, eliminating continuous frame invalidation on low-end devices.
 */
class AppInfiniteTransition internal constructor(
    val reduceMotion: Boolean,
    private val underlying: InfiniteTransition?
) {
    @Composable
    fun animateFloat(
        initialValue: Float,
        targetValue: Float,
        animationSpec: InfiniteRepeatableSpec<Float>,
        label: String = "AppInfiniteFloat"
    ): State<Float> {
        return if (reduceMotion) {
            remember(initialValue) { mutableFloatStateOf(initialValue) }
        } else {
            underlying!!.animateFloat(
                initialValue = initialValue,
                targetValue = targetValue,
                animationSpec = animationSpec,
                label = label
            )
        }
    }

    @Composable
    fun animateColor(
        initialValue: Color,
        targetValue: Color,
        animationSpec: InfiniteRepeatableSpec<Color>,
        label: String = "AppInfiniteColor"
    ): State<Color> {
        return if (reduceMotion) {
            remember(initialValue) { mutableStateOf(initialValue) }
        } else {
            underlying!!.animateColor(
                initialValue = initialValue,
                targetValue = targetValue,
                animationSpec = animationSpec,
                label = label
            )
        }
    }
}

/**
 * Remembers an [AppInfiniteTransition].
 * When reduce motion is active, avoids creating or scheduling infinite frame loops.
 */
@Composable
fun rememberAppInfiniteTransition(label: String = "AppInfiniteTransition"): AppInfiniteTransition {
    val reduceMotion = LocalMotionPreferences.current.reduceAnimations
    val underlying = if (!reduceMotion) rememberInfiniteTransition(label = label) else null
    return remember(reduceMotion, underlying) {
        AppInfiniteTransition(reduceMotion, underlying)
    }
}
