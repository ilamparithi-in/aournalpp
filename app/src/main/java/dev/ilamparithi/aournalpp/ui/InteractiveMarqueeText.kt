package dev.ilamparithi.aournalpp.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val M3SlideBackEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

/**
 * Interactive Text component that displays single-line text with ellipsis by default.
 * When touched, hovered, tapped, or triggered externally (e.g. from thumbnail touches):
 * 1. Measures full unconstrained text width with infinite maxWidth constraint so NO glyphs are clipped.
 * 2. Smoothly runs forward at steady velocity for the complete text length.
 * 3. Pauses at the end so the entire name is easily readable.
 * 4. Animates BACK to the starting position using a quick Material 3 easing slide.
 * 5. Resets to default resting ellipsis.
 */
@Composable
fun InteractiveMarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    minWidth: Dp = Dp.Unspecified,
    maxWidth: Dp = Dp.Unspecified,
    externalTrigger: Any? = null
) {
    var isInteracted by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var animJob by remember { mutableStateOf<Job?>(null) }
    val density = LocalDensity.current

    val textMeasurer = rememberTextMeasurer()
    var actualTextWidthPx by remember(text, style, fontWeight) { mutableIntStateOf(-1) }

    var containerWidthPx by remember { mutableIntStateOf(0) }
    val animOffset = remember { Animatable(0f) }

    val startMarqueeSequence = {
        if (!isInteracted) {
            val textWidth = if (actualTextWidthPx >= 0) {
                actualTextWidthPx
            } else {
                val measured = textMeasurer.measure(
                    text = text,
                    style = if (fontWeight != null) style.copy(fontWeight = fontWeight) else style,
                    maxLines = 1,
                    softWrap = false
                ).size.width
                actualTextWidthPx = measured
                measured
            }

            val maxScrollPx = (textWidth - containerWidthPx).coerceAtLeast(0)
            if (maxScrollPx > 0) {
                isInteracted = true
                animJob?.cancel()
                animJob = coroutineScope.launch {
                    val scrollDistanceDp = with(density) { maxScrollPx.toDp().value }
                    val forwardDurationMs = ((scrollDistanceDp / 50f) * 1000f).toInt().coerceAtLeast(100)

                    // 1. Smooth forward scroll reveal at constant speed
                    animOffset.snapTo(0f)
                    animOffset.animateTo(
                        targetValue = maxScrollPx.toFloat(),
                        animationSpec = tween(durationMillis = forwardDurationMs, easing = LinearEasing)
                    )

                    // 2. Pause at the end for comfortable reading
                    delay(1500L)

                    // 3. Quick Material 3 easing slide back to starting position
                    animOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 420, easing = M3SlideBackEasing)
                    )

                    delay(100L)
                    isInteracted = false
                }
            }
        }
    }

    // Trigger on external event (e.g., thumbnail swipe or touch)
    LaunchedEffect(externalTrigger) {
        if (externalTrigger != null && externalTrigger != 0L && externalTrigger != false && !isInteracted) {
            startMarqueeSequence()
        }
    }

    val widthModifier = when {
        minWidth != Dp.Unspecified && maxWidth != Dp.Unspecified -> Modifier.widthIn(min = minWidth, max = maxWidth)
        minWidth != Dp.Unspecified -> Modifier.widthIn(min = minWidth)
        maxWidth != Dp.Unspecified -> Modifier.widthIn(max = maxWidth)
        else -> Modifier
    }

    Box(
        modifier = modifier
            .then(widthModifier)
            .clipToBounds()
            .onSizeChanged { size ->
                if (containerWidthPx != size.width) {
                    containerWidthPx = size.width
                }
            }
            .pointerInput(text) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (!isInteracted) {
                            val hasInitialTouch = event.changes.any { it.pressed && !it.previousPressed }
                            if (hasInitialTouch ||
                                event.type == PointerEventType.Enter ||
                                event.type == PointerEventType.Press
                            ) {
                                startMarqueeSequence()
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        if (isInteracted && (actualTextWidthPx > containerWidthPx)) {
            Text(
                text = text,
                style = style,
                fontWeight = fontWeight,
                color = color,
                textAlign = textAlign,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        constraints.copy(
                            minWidth = 0,
                            maxWidth = Constraints.Infinity
                        )
                    )
                    layout(constraints.maxWidth, placeable.height) {
                        placeable.placeRelative(-animOffset.value.toInt(), 0)
                    }
                }
            )
        } else {
            Text(
                text = text,
                style = style,
                fontWeight = fontWeight,
                color = color,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
        }
    }
}
