package dev.ilamparithi.aournalpp.ui.snap

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun SnapDividerOverlay(
    viewportWidth: Int,
    viewportHeight: Int,
    dividers: List<DividerGeometry>,
    modifier: Modifier = Modifier,
    onUpdateRatio: (String, Float) -> Unit,
    onResetRatio: (String) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    if (dividers.isEmpty() || viewportWidth <= 0 || viewportHeight <= 0) return

    val density = LocalDensity.current
    val view = LocalView.current

    val currentOnUpdateRatio by rememberUpdatedState(onUpdateRatio)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(50f)
    ) {
        for (div in dividers) {
            val currentDiv by rememberUpdatedState(div)
            val interactionSource = remember(div.id) { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            var isDragging by remember(div.id) { mutableStateOf(false) }
            var currentLiveRatio by remember(div.id) { mutableFloatStateOf(div.currentRatio) }

            LaunchedEffect(div.currentRatio) {
                if (!isDragging) {
                    currentLiveRatio = div.currentRatio
                }
            }

            val animatedAlpha by animateFloatAsState(
                targetValue = if (isDragging) 1.0f else if (isHovered) 0.85f else 0.0f,
                animationSpec = tween(durationMillis = 200),
                label = "handleAlpha_${div.id}"
            )

            val hitThicknessDp = 36.dp
            val hitThicknessPx = with(density) { hitThicknessDp.toPx() }

            if (div.orientation == DividerOrientation.VERTICAL) {
                val handleHeightDp = with(density) { div.length.toDp() }

                Box(
                    modifier = Modifier
                        .offset {
                            val effRatio = if (isDragging) currentLiveRatio else currentDiv.currentRatio
                            val handleX = Math.round(effRatio * viewportWidth).toInt()
                            val handleLeftPx = handleX - (hitThicknessPx / 2f).toInt()
                            IntOffset(handleLeftPx, currentDiv.y)
                        }
                        .width(hitThicknessDp)
                        .height(handleHeightDp)
                        .hoverable(interactionSource = interactionSource)
                        .pointerInput(div.id, viewportWidth) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    currentLiveRatio = currentDiv.currentRatio
                                    try { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) } catch (_: Exception) {}
                                },
                                onDragEnd = {
                                    isDragging = false
                                    try { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) } catch (_: Exception) {}
                                    currentOnDragEnd()
                                },
                                onDragCancel = {
                                    isDragging = false
                                    currentOnDragEnd()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val deltaRatio = dragAmount.x / viewportWidth.toFloat()
                                    val nextRatio = (currentLiveRatio + deltaRatio).coerceIn(currentDiv.minRatio, currentDiv.maxRatio)
                                    currentLiveRatio = nextRatio
                                    currentOnUpdateRatio(currentDiv.id, nextRatio)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Visible pill handle
                    if (animatedAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(48.dp)
                                .alpha(animatedAlpha)
                                .shadow(4.dp, RoundedCornerShape(3.dp))
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            } else {
                val handleWidthDp = with(density) { div.length.toDp() }

                Box(
                    modifier = Modifier
                        .offset {
                            val effRatio = if (isDragging) currentLiveRatio else currentDiv.currentRatio
                            val handleY = Math.round(effRatio * viewportHeight).toInt()
                            val handleTopPx = handleY - (hitThicknessPx / 2f).toInt()
                            IntOffset(currentDiv.x, handleTopPx)
                        }
                        .width(handleWidthDp)
                        .height(hitThicknessDp)
                        .hoverable(interactionSource = interactionSource)
                        .pointerInput(div.id, viewportHeight) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    currentLiveRatio = currentDiv.currentRatio
                                    try { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) } catch (_: Exception) {}
                                },
                                onDragEnd = {
                                    isDragging = false
                                    try { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) } catch (_: Exception) {}
                                    currentOnDragEnd()
                                },
                                onDragCancel = {
                                    isDragging = false
                                    currentOnDragEnd()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val deltaRatio = dragAmount.y / viewportHeight.toFloat()
                                    val nextRatio = (currentLiveRatio + deltaRatio).coerceIn(currentDiv.minRatio, currentDiv.maxRatio)
                                    currentLiveRatio = nextRatio
                                    currentOnUpdateRatio(currentDiv.id, nextRatio)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Visible pill handle
                    if (animatedAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .height(5.dp)
                                .width(48.dp)
                                .alpha(animatedAlpha)
                                .shadow(4.dp, RoundedCornerShape(3.dp))
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}
