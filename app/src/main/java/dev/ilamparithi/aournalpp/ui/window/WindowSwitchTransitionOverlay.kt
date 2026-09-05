package dev.ilamparithi.aournalpp.ui.window

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay

/**
 * Transition overlay that displays the sliding spring animation between windows in CanvasActivity.
 *
 * Reuses the exact spring animation parameters from MainActivity:
 * - Slide: spring(dampingRatio = 0.82f, stiffness = 380f)
 * - Fade: spring(dampingRatio = 0.9f, stiffness = 400f)
 * - Offset fraction: (width / 3)
 */
@Composable
fun WindowSwitchTransitionOverlay(
    outgoingBitmap: Bitmap?,
    incomingBitmap: Bitmap?,
    isForward: Boolean,
    onTransitionFinished: () -> Unit
) {
    // Current step in the 2-state AnimatedContent transition: 0 = outgoing, 1 = incoming
    var animationState by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        // Kick off transition to state 1 on next frame
        animationState = 1
        // Allow spring to complete smoothly (~280ms) then notify finished
        delay(300)
        onTransitionFinished()
    }

    // Touch barrier to ensure gestures aren't delivered to LorieView mid-flight
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Consume all touch inputs during the brief transition
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        AnimatedContent(
            targetState = animationState,
            transitionSpec = {
                val enterOffset = if (isForward) 1 else -1
                val exitOffset = if (isForward) -1 else 1

                (slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 380f
                    ),
                    initialOffsetX = { (it / 3) * enterOffset }
                ) + fadeIn(
                    animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = 400f
                    )
                ))
                    .togetherWith(
                        slideOutHorizontally(
                            animationSpec = spring(
                                dampingRatio = 0.82f,
                                stiffness = 380f
                            ),
                            targetOffsetX = { -(it / 3) * exitOffset }
                        ) + fadeOut(
                            animationSpec = spring(
                                dampingRatio = 0.9f,
                                stiffness = 400f
                            )
                        )
                    )
            },
            label = "WindowSwitchSpringSlide"
        ) { targetStep ->
            val displayBitmap = if (targetStep == 0) outgoingBitmap else incomingBitmap
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (displayBitmap != null && !displayBitmap.isRecycled) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
