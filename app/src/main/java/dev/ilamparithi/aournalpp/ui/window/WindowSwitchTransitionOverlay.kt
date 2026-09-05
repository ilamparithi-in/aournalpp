package dev.ilamparithi.aournalpp.ui.window

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import dev.ilamparithi.aournalpp.ui.animation.SpringSlideTransition
import kotlinx.coroutines.delay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Transition overlay that displays the sliding spring animation between windows in CanvasActivity.
 * Reuses the standardized spring slide and fade animation matching MainActivity.
 * The background is rendered with the desktop background according to settings, hiding
 * the Openbox-side window switch occurring on the live surface underneath.
 */
@Composable
fun WindowSwitchTransitionOverlay(
    outgoingBitmap: Bitmap?,
    incomingBitmap: Bitmap?,
    targetTitle: String,
    targetIcon: ImageVector,
    wallpaperBitmap: ImageBitmap,
    isForward: Boolean,
    onStarted: () -> Unit,
    onTransitionFinished: () -> Unit
) {
    // Current step in the 2-state transition: 0 = outgoing, 1 = incoming
    var animationState by remember { mutableStateOf(0) }
    val transition = updateTransition(targetState = animationState, label = "WindowSwitchSpringSlide")

    LaunchedEffect(Unit) {
        // Allow the overlay and wallpaper backdrop to render on screen first, safely covering LorieView
        delay(32)
        onStarted()
        animationState = 1
    }

    // Await natural completion of the spring animation (including full rebound and settling)
    LaunchedEffect(transition.currentState, transition.targetState) {
        if (transition.currentState == 1 && transition.targetState == 1) {
            onTransitionFinished()
        }
    }

    // Fallback safety timeout in case of interrupted lifecycle
    LaunchedEffect(Unit) {
        delay(900)
        onTransitionFinished()
    }

    // Touch barrier to ensure gestures aren't delivered to LorieView mid-flight
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f)
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
        // Desktop background according to settings
        Image(
            bitmap = wallpaperBitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        transition.AnimatedContent(
            transitionSpec = SpringSlideTransition.createSpec(isForward = isForward),
            contentKey = { it },
            modifier = Modifier.fillMaxSize()
        ) { targetStep ->
            val displayBitmap = if (targetStep == 0) outgoingBitmap else incomingBitmap
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (displayBitmap != null && !displayBitmap.isRecycled) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (targetStep == 1) {
                    // Fallback preview card if target window hasn't been cached yet
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = targetIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = targetTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

