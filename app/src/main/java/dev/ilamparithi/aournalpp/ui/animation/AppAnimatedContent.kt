package dev.ilamparithi.aournalpp.ui.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Standardized wrapper around [AnimatedContent] respecting [LocalMotionPreferences].
 * When reduce motion is enabled, replaces transitions with a subtle cross-fade
 * without jarring slide motions or jumps.
 */
@Composable
fun <S> AppAnimatedContent(
    targetState: S,
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
    },
    contentAlignment: Alignment = Alignment.TopStart,
    label: String = "AppAnimatedContent",
    contentKey: ((targetState: S) -> Any?) = { it },
    content: @Composable AnimatedContentScope.(targetState: S) -> Unit
) {
    val reduceMotion = LocalMotionPreferences.current.reduceAnimations
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(animationSpec = tween(120)) togetherWith fadeOut(animationSpec = tween(100))
            } else {
                transitionSpec(this)
            }
        },
        contentAlignment = contentAlignment,
        label = label,
        contentKey = contentKey,
        content = content
    )
}
