package dev.ilamparithi.aournalpp.ui.snap

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
    onResetRatio: (String) -> Unit,
    onDragEnd: () -> Unit
) {
    if (dividers.isEmpty() || viewportWidth <= 0 || viewportHeight <= 0) return

    val density = LocalDensity.current
    val view = LocalView.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(50f)
    ) {
        for (div in dividers) {
            val interactionSource = remember(div.id) { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            var isDragging by remember(div.id) { mutableStateOf(false) }

            val isVisible = isHovered || isDragging
            val animatedAlpha by animateFloatAsState(
                targetValue = if (isDragging) 1.0f else if (isHovered) 0.85f else 0.0f,
                animationSpec = tween(durationMillis = 200),
                label = "handleAlpha_${div.id}"
            )

            val hitThicknessDp = 32.dp
            val hitThicknessPx = with(density) { hitThicknessDp.toPx() }

            if (div.orientation == DividerOrientation.VERTICAL) {
                val handleLeftPx = div.x - (hitThicknessPx / 2f)
                val handleHeightDp = with(density) { div.length.toDp() }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(handleLeftPx.toInt(), div.y) }
                        .width(hitThicknessDp)
                        .height(handleHeightDp)
                        .hoverable(interactionSource = interactionSource)
                        .pointerInput(div.id, viewportWidth) {
                            detectTapGestures(
                                onDoubleTap = {
                                    try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                                    onResetRatio(div.id)
                                    onDragEnd()
                                }
                            )
                        }
                        .pointerInput(div.id, viewportWidth) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    try { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) } catch (_: Exception) {}
                                },
                                onDragEnd = {
                                    isDragging = false
                                    try { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) } catch (_: Exception) {}
                                    onDragEnd()
                                },
                                onDragCancel = {
                                    isDragging = false
                                    onDragEnd()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val deltaRatio = dragAmount.x / viewportWidth.toFloat()
                                    val newRatio = (div.currentRatio + deltaRatio).coerceIn(div.minRatio, div.maxRatio)
                                    onUpdateRatio(div.id, newRatio)
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
                val handleTopPx = div.y - (hitThicknessPx / 2f)
                val handleWidthDp = with(density) { div.length.toDp() }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(div.x, handleTopPx.toInt()) }
                        .width(handleWidthDp)
                        .height(hitThicknessDp)
                        .hoverable(interactionSource = interactionSource)
                        .pointerInput(div.id, viewportHeight) {
                            detectTapGestures(
                                onDoubleTap = {
                                    try { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
                                    onResetRatio(div.id)
                                    onDragEnd()
                                }
                            )
                        }
                        .pointerInput(div.id, viewportHeight) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    try { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) } catch (_: Exception) {}
                                },
                                onDragEnd = {
                                    isDragging = false
                                    try { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) } catch (_: Exception) {}
                                    onDragEnd()
                                },
                                onDragCancel = {
                                    isDragging = false
                                    onDragEnd()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val deltaRatio = dragAmount.y / viewportHeight.toFloat()
                                    val newRatio = (div.currentRatio + deltaRatio).coerceIn(div.minRatio, div.maxRatio)
                                    onUpdateRatio(div.id, newRatio)
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
