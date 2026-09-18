package dev.ilamparithi.aournalpp.ui.animation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.unit.Dp

/**
 * Standardized wrapper around [animateFloatAsState] respecting [LocalMotionPreferences].
 * Returns [snap] when [MotionPreferences.reduceAnimations] is active.
 */
@Composable
fun appAnimateFloatAsState(
    targetValue: Float,
    animationSpec: AnimationSpec<Float> = AppAnimationSpecs.springFloat(),
    visibilityThreshold: Float = 0.01f,
    label: String = "AppFloatAnimation",
    finishedListener: ((Float) -> Unit)? = null
): State<Float> {
    val reduceMotion = LocalMotionPreferences.current.reduceAnimations
    return animateFloatAsState(
        targetValue = targetValue,
        animationSpec = if (reduceMotion) snap() else animationSpec,
        visibilityThreshold = visibilityThreshold,
        label = label,
        finishedListener = finishedListener
    )
}

/**
 * Standardized wrapper around [animateDpAsState] respecting [LocalMotionPreferences].
 * Returns [snap] when [MotionPreferences.reduceAnimations] is active.
 */
@Composable
fun appAnimateDpAsState(
    targetValue: Dp,
    animationSpec: AnimationSpec<Dp> = AppAnimationSpecs.springDp(),
    label: String = "AppDpAnimation",
    finishedListener: ((Dp) -> Unit)? = null
): State<Dp> {
    val reduceMotion = LocalMotionPreferences.current.reduceAnimations
    return animateDpAsState(
        targetValue = targetValue,
        animationSpec = if (reduceMotion) snap() else animationSpec,
        label = label,
        finishedListener = finishedListener
    )
}
