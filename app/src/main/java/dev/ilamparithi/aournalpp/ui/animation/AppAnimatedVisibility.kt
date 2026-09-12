package dev.ilamparithi.aournalpp.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A wrapper around [AnimatedVisibility] that automatically respects the user's
 * "Reduce Animations" motion preference from [LocalMotionPreferences].
 *
 * When animations are reduced, all enter and exit transitions are replaced with
 * [EnterTransition.None] and [ExitTransition.None].
 */
@Composable
fun AppAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = shrinkOut() + fadeOut(),
    label: String = "AppAnimatedVisibility",
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val reduceMotion = LocalMotionPreferences.current.reduceAnimations
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (reduceMotion) EnterTransition.None else enter,
        exit = if (reduceMotion) ExitTransition.None else exit,
        label = label,
        content = content
    )
}

@Composable
fun AppAnimatedVisibility(
    visibleState: MutableTransitionState<Boolean>,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = shrinkOut() + fadeOut(),
    label: String = "AppAnimatedVisibility",
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val reduceMotion = LocalMotionPreferences.current.reduceAnimations
    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = if (reduceMotion) EnterTransition.None else enter,
        exit = if (reduceMotion) ExitTransition.None else exit,
        label = label,
        content = content
    )
}

@Composable
fun RowScope.AppAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandHorizontally(),
    exit: ExitTransition = shrinkHorizontally() + fadeOut(),
    label: String = "AppAnimatedVisibility",
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val reduceMotion = LocalMotionPreferences.current.reduceAnimations
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (reduceMotion) EnterTransition.None else enter,
        exit = if (reduceMotion) ExitTransition.None else exit,
        label = label,
        content = content
    )
}

@Composable
fun RowScope.AppAnimatedVisibility(
    visibleState: MutableTransitionState<Boolean>,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandHorizontally(),
    exit: ExitTransition = shrinkHorizontally() + fadeOut(),
    label: String = "AppAnimatedVisibility",
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val reduceMotion = LocalMotionPreferences.current.reduceAnimations
    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = if (reduceMotion) EnterTransition.None else enter,
        exit = if (reduceMotion) ExitTransition.None else exit,
        label = label,
        content = content
    )
}

@Composable
fun ColumnScope.AppAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandVertically(),
    exit: ExitTransition = shrinkVertically() + fadeOut(),
    label: String = "AppAnimatedVisibility",
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val reduceMotion = LocalMotionPreferences.current.reduceAnimations
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (reduceMotion) EnterTransition.None else enter,
        exit = if (reduceMotion) ExitTransition.None else exit,
        label = label,
        content = content
    )
}

@Composable
fun ColumnScope.AppAnimatedVisibility(
    visibleState: MutableTransitionState<Boolean>,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandVertically(),
    exit: ExitTransition = shrinkVertically() + fadeOut(),
    label: String = "AppAnimatedVisibility",
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val reduceMotion = LocalMotionPreferences.current.reduceAnimations
    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = if (reduceMotion) EnterTransition.None else enter,
        exit = if (reduceMotion) ExitTransition.None else exit,
        label = label,
        content = content
    )
}
