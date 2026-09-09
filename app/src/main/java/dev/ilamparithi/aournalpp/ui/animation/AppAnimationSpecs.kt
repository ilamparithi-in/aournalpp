package dev.ilamparithi.aournalpp.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

object AppAnimationSpecs {
    @Composable
    fun springDp(
        dampingRatio: Float = 0.82f,
        stiffness: Float = 380f,
        label: String = "dp"
    ): FiniteAnimationSpec<Dp> =
        if (LocalMotionPreferences.current.reduceAnimations) snap()
        else spring(dampingRatio = dampingRatio, stiffness = stiffness)

    @Composable
    fun springFloat(
        dampingRatio: Float = 0.78f,
        stiffness: Float = 340f,
        label: String = "float"
    ): FiniteAnimationSpec<Float> =
        if (LocalMotionPreferences.current.reduceAnimations) snap()
        else spring(dampingRatio = dampingRatio, stiffness = stiffness)

    @Composable
    fun springColor(
        stiffness: Float = Spring.StiffnessMedium
    ): FiniteAnimationSpec<Color> =
        if (LocalMotionPreferences.current.reduceAnimations) snap()
        else spring(stiffness = stiffness)

    @Composable
    fun fadeSpec(): FiniteAnimationSpec<Float> =
        if (LocalMotionPreferences.current.reduceAnimations) snap()
        else tween(durationMillis = 220)

    @Composable
    fun enterFade(): EnterTransition =
        if (LocalMotionPreferences.current.reduceAnimations) EnterTransition.None
        else fadeIn(animationSpec = tween(durationMillis = 220))

    @Composable
    fun exitFade(): ExitTransition =
        if (LocalMotionPreferences.current.reduceAnimations) ExitTransition.None
        else fadeOut(animationSpec = tween(durationMillis = 220))

    @Composable
    fun <T> contentTransformSpec(
        isForward: Boolean,
        offsetFraction: Int = SpringSlideTransition.DEFAULT_OFFSET_FRACTION
    ): AnimatedContentTransitionScope<T>.() -> ContentTransform =
        SpringSlideTransition.createSpec(
            isForward = isForward,
            reduceAnimations = LocalMotionPreferences.current.reduceAnimations,
            offsetFraction = offsetFraction
        )
}
