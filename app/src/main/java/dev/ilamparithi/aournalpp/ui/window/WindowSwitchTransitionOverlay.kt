package dev.ilamparithi.aournalpp.ui.window

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import dev.ilamparithi.aournalpp.ui.animation.SpringSlideTransition
import kotlinx.coroutines.delay

/**
 * Transition overlay that displays the sliding spring animation between screens in CanvasActivity.
 * Reuses the standardized spring slide and fade animation matching MainActivity.
 * The transition slides the full old screen out and the full new screen in, edge-to-edge.
 * The background is rendered with the desktop background according to settings.
 */
@Composable
fun WindowSwitchTransitionOverlay(
    outgoingBitmap: Bitmap?,
    incomingBitmap: Bitmap?,
    targetTitle: String,
    wallpaperBitmap: ImageBitmap,
    isForward: Boolean,
    onStarted: () -> Unit,
    onTransitionFinished: () -> Unit,
    targetIcon: ImageVector? = null
) {
    // Current step in the 2-state transition: 0 = outgoing, 1 = incoming
    var animationState by remember { mutableStateOf(0) }
    val transition = updateTransition(targetState = animationState, label = "WindowSwitchSpringSlide")

    val context = androidx.compose.ui.platform.LocalContext.current
    var incomingFallbackThumbnail by remember(targetTitle) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(targetTitle, incomingBitmap) {
        if (incomingBitmap == null) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val repo = dev.ilamparithi.aournalpp.data.DocumentRepository(context)
                    val doc = repo.findNoteDocumentByTitle(targetTitle)
                    if (doc != null) {
                        val thumb = dev.ilamparithi.aournalpp.utils.ThumbnailManager.getOrCreateThumbnailBitmap(context, doc.file, null, doc.lastModifiedMs)
                        if (thumb != null) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                incomingFallbackThumbnail = thumb
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(Unit) {
        // Allow the overlay and wallpaper backdrop to render on screen first, safely covering LorieView
        delay(32)
        onStarted()
        animationState = 1
    }

    // Await natural completion of the spring animation (including full rebound and settling)
    LaunchedEffect(transition.currentState, transition.targetState) {
        if (transition.currentState == 1 && transition.targetState == 1) {
            delay(50)
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
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (targetStep == 1 && incomingFallbackThumbnail != null) {
                    Image(
                        bitmap = incomingFallbackThumbnail!!,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

