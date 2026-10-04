package dev.ilamparithi.aournalpp.ui.settings.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.ui.animation.LocalMotionPreferences
import kotlin.math.roundToInt

/**
 * Material 3 Expressive Standard Slider (Official Specification).
 *
 * Implements the official Material 3 Expressive standard slider:
 * - 16dp height track.
 * - Flat inner ends with 2dp corner radius facing the thumb gap.
 * - Semicircular 8dp outer ends.
 * - 44dp height vertical pill thumb handle (4dp idle width, expanding to 6dp on drag).
 * - Symmetrical 6dp gap between track ends and thumb handle.
 * - 4dp stop indicator dot on the right side of the inactive track.
 * - Tactile spring physics during press/drag.
 * - Full accessibility compliance (ProgressBarRangeInfo, setProgress, 48dp minimum touch target).
 */
@Composable
fun ExpressiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    trackHeight: Dp = 16.dp,
    thumbTrackGap: Dp = 6.dp,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val reduceMotion = LocalMotionPreferences.current.reduceAnimations
    val updatedOnValueChange by rememberUpdatedState(onValueChange)
    val updatedOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

    var isDragging by remember { mutableStateOf(false) }
    val thumbScale = remember { Animatable(1.0f) }

    LaunchedEffect(isDragging) {
        if (!reduceMotion) {
            if (isDragging) {
                thumbScale.animateTo(1.4f, spring(dampingRatio = 0.55f, stiffness = 500f))
            } else {
                thumbScale.animateTo(1.0f, spring(dampingRatio = 0.65f, stiffness = 450f))
            }
        }
    }

    val rangeSpan = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val currentFraction = ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)

    val activeTrackColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val inactiveTrackColor = if (enabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val thumbColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val stopDotColor = if (enabled) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    val activePath = remember { Path() }
    val inactivePath = remember { Path() }

    fun calculateNewValue(touchX: Float, width: Float, horizontalInset: Float): Float {
        val usableWidth = (width - 2 * horizontalInset).coerceAtLeast(1f)
        val rawFraction = ((touchX - horizontalInset) / usableWidth).coerceIn(0f, 1f)
        val steppedFraction = if (steps > 0) {
            val stepSize = 1f / (steps + 1)
            (rawFraction / stepSize).roundToInt() * stepSize
        } else {
            rawFraction
        }
        return (valueRange.start + steppedFraction * rangeSpan).coerceIn(valueRange.start, valueRange.endInclusive)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = value,
                    range = valueRange
                )
                if (enabled) {
                    setProgress { targetValue ->
                        val coerced = targetValue.coerceIn(valueRange.start, valueRange.endInclusive)
                        updatedOnValueChange(coerced)
                        updatedOnValueChangeFinished?.invoke()
                        true
                    }
                }
            }
            .pointerInput(enabled, valueRange, steps, trackHeight) {
                if (!enabled) return@pointerInput
                val horizontalInset = (trackHeight / 2f).toPx()
                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true
                        val newValue = calculateNewValue(offset.x, size.width.toFloat(), horizontalInset)
                        updatedOnValueChange(newValue)
                        tryAwaitRelease()
                        isDragging = false
                        updatedOnValueChangeFinished?.invoke()
                    }
                )
            }
            .pointerInput(enabled, valueRange, steps, trackHeight) {
                if (!enabled) return@pointerInput
                val horizontalInset = (trackHeight / 2f).toPx()
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val newValue = calculateNewValue(offset.x, size.width.toFloat(), horizontalInset)
                        updatedOnValueChange(newValue)
                    },
                    onDragEnd = {
                        isDragging = false
                        updatedOnValueChangeFinished?.invoke()
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val newValue = calculateNewValue(change.position.x, size.width.toFloat(), horizontalInset)
                        updatedOnValueChange(newValue)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val centerY = size.height / 2f
            val trackH = trackHeight.toPx()
            val outsideCornerRadius = trackH / 2f
            val insideCornerRadius = 2.dp.toPx()
            val gapPx = thumbTrackGap.toPx()

            val thumbIdleWidth = 4.dp.toPx()
            val thumbIdleHeight = 44.dp.toPx()
            val thumbDragWidth = 6.dp.toPx()
            val thumbDragHeight = 44.dp.toPx()

            val effectiveThumbWidth = if (isDragging && !reduceMotion) thumbDragWidth else thumbIdleWidth
            val effectiveThumbHeight = if (isDragging && !reduceMotion) thumbDragHeight else thumbIdleHeight
            val thumbHalfWidth = effectiveThumbWidth / 2f

            val usableWidth = (size.width - 2 * outsideCornerRadius).coerceAtLeast(1f)
            val thumbX = outsideCornerRadius + currentFraction * usableWidth

            val trackLeft = 0f
            val trackRight = size.width

            // 1. Active Track (Left: 8dp rounded outer corner; Right: 2dp flat inner corner)
            val activeTrackEnd = (thumbX - thumbHalfWidth - gapPx).coerceIn(trackLeft, trackRight)
            if (activeTrackEnd > trackLeft) {
                val activeLength = activeTrackEnd - trackLeft
                val outerR = outsideCornerRadius.coerceAtMost(activeLength / 2f)
                val innerR = insideCornerRadius.coerceAtMost(activeLength / 2f)

                activePath.rewind()
                activePath.addRoundRect(
                    RoundRect(
                        rect = Rect(
                            left = trackLeft,
                            top = centerY - trackH / 2f,
                            right = activeTrackEnd,
                            bottom = centerY + trackH / 2f
                        ),
                        topLeft = CornerRadius(outerR, outerR),
                        bottomLeft = CornerRadius(outerR, outerR),
                        topRight = CornerRadius(innerR, innerR),
                        bottomRight = CornerRadius(innerR, innerR)
                    )
                )
                drawPath(path = activePath, color = activeTrackColor)
            }

            // 2. Inactive Track (Left: 2dp flat inner corner; Right: 8dp rounded outer corner)
            val inactiveTrackStart = (thumbX + thumbHalfWidth + gapPx).coerceIn(trackLeft, trackRight)
            if (inactiveTrackStart < trackRight) {
                val inactiveLength = trackRight - inactiveTrackStart
                val innerR = insideCornerRadius.coerceAtMost(inactiveLength / 2f)
                val outerR = outsideCornerRadius.coerceAtMost(inactiveLength / 2f)

                inactivePath.rewind()
                inactivePath.addRoundRect(
                    RoundRect(
                        rect = Rect(
                            left = inactiveTrackStart,
                            top = centerY - trackH / 2f,
                            right = trackRight,
                            bottom = centerY + trackH / 2f
                        ),
                        topLeft = CornerRadius(innerR, innerR),
                        bottomLeft = CornerRadius(innerR, innerR),
                        topRight = CornerRadius(outerR, outerR),
                        bottomRight = CornerRadius(outerR, outerR)
                    )
                )
                drawPath(path = inactivePath, color = inactiveTrackColor)

                // 3. Stop indicator dot near the right end of the inactive track
                val stopDotRadius = 2.dp.toPx()
                val stopDotX = trackRight - outsideCornerRadius
                if (inactiveTrackStart < stopDotX - stopDotRadius - 2.dp.toPx()) {
                    drawCircle(
                        color = stopDotColor,
                        radius = stopDotRadius,
                        center = Offset(stopDotX, centerY)
                    )
                }
            }

            // 4. Thumb Handle (Vertical rounded line/pill 44dp height)
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(thumbX - thumbHalfWidth, centerY - effectiveThumbHeight / 2f),
                size = Size(effectiveThumbWidth, effectiveThumbHeight),
                cornerRadius = CornerRadius(thumbHalfWidth, thumbHalfWidth)
            )
        }
    }
}
