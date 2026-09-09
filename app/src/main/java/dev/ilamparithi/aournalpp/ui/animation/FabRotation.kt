package dev.ilamparithi.aournalpp.ui.animation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * Reusable spring-animated FAB rotation for speed-dial FABs, respecting reduce-motion settings.
 */
@Composable
fun rememberFabRotation(
    isExpanded: Boolean,
    reduceAnimations: Boolean = LocalMotionPreferences.current.reduceAnimations
): State<Float> {
    return animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f,
        animationSpec = if (reduceAnimations) snap() else spring(dampingRatio = 0.65f, stiffness = 300f),
        label = "fabRotation"
    )
}
