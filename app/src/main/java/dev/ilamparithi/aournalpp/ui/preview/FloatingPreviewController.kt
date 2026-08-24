package dev.ilamparithi.aournalpp.ui.preview

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.model.NoteDocument
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Supported drag action target regions when dragging during floating preview.
 */
enum class DragActionTarget {
    NONE,
    VIEW_PDF,
    EDIT_CANVAS
}

/**
 * Data describing the source preview to morph and float.
 */
data class FloatingPreviewData(
    val note: NoteDocument,
    val initialBounds: Rect,
    val initialCornerRadiusDp: Float = 16f,
    val thumbnailFile: File? = null,
    val folderColor: Color = Color(0xFF6750A4),
    val touchPositionInWindow: Offset = Offset.Unspecified
)

/**
 * Controller managing the lifecycle, visibility, drag offsets,
 * and action selection of the floating preview overlay.
 */
class FloatingPreviewController {
    private val _currentPreview = mutableStateOf<FloatingPreviewData?>(null)
    val currentPreview: State<FloatingPreviewData?> = _currentPreview

    private val _isFingerDown = mutableStateOf(false)
    val isFingerDown: State<Boolean> = _isFingerDown

    private val _dragDelta = mutableStateOf(Offset.Zero)
    val dragDelta: State<Offset> = _dragDelta

    private val _activeAction = mutableStateOf(DragActionTarget.NONE)
    val activeAction: State<DragActionTarget> = _activeAction

    private var onActionTriggeredCallback: ((NoteDocument, DragActionTarget) -> Unit)? = null

    fun registerActionCallback(callback: ((NoteDocument, DragActionTarget) -> Unit)?) {
        onActionTriggeredCallback = callback
    }

    fun showPreview(data: FloatingPreviewData) {
        _currentPreview.value = data
        _isFingerDown.value = true
        _dragDelta.value = Offset.Zero
        _activeAction.value = DragActionTarget.NONE
    }

    fun updateDrag(delta: Offset, isLandscape: Boolean, screenWidthPx: Float, screenHeightPx: Float) {
        if (!_isFingerDown.value) return
        _dragDelta.value = delta

        val threshold = if (isLandscape) {
            (screenWidthPx * 0.08f).coerceIn(48f, 150f)
        } else {
            (screenHeightPx * 0.06f).coerceIn(48f, 150f)
        }

        val newAction = if (isLandscape) {
            when {
                delta.x < -threshold -> DragActionTarget.VIEW_PDF
                delta.x > threshold -> DragActionTarget.EDIT_CANVAS
                else -> DragActionTarget.NONE
            }
        } else {
            when {
                delta.y < -threshold -> DragActionTarget.VIEW_PDF
                delta.y > threshold -> DragActionTarget.EDIT_CANVAS
                else -> DragActionTarget.NONE
            }
        }

        _activeAction.value = newAction
    }

    fun onFingerReleased(): DragActionTarget {
        _isFingerDown.value = false
        val finalAction = _activeAction.value
        val note = _currentPreview.value?.note
        if (finalAction != DragActionTarget.NONE && note != null) {
            onActionTriggeredCallback?.invoke(note, finalAction)
        }
        return finalAction
    }

    fun dismissImmediately() {
        _currentPreview.value = null
        _isFingerDown.value = false
        _dragDelta.value = Offset.Zero
        _activeAction.value = DragActionTarget.NONE
    }
}

val LocalFloatingPreviewController = compositionLocalOf<FloatingPreviewController> {
    error("No FloatingPreviewController provided. Wrap UI in FloatingPreviewHost.")
}

private enum class GesturePhase {
    TAP,
    CANCEL,
    LONG_PRESS
}

/**
 * Modifier to attach long-press floating preview behavior and drag-to-action gesture tracking.
 * - Tap: invokes [onClick].
 * - Long press: captures current window bounds, triggers haptic feedback,
 *   and holds the floating preview open.
 * - Drag while held in preview: tracks directional displacement to highlight action regions ("View as PDF" / "Edit in Xournal++").
 * - Release: executes chosen action if threshold is reached, or dismisses preview if neutral.
 */
fun Modifier.floatingPreviewLongPress(
    note: NoteDocument,
    thumbnailFile: File?,
    folderColor: Color = Color(0xFF6750A4),
    initialCornerRadiusDp: Float = 16f,
    onClick: () -> Unit,
    onLongPressFallback: (() -> Unit)? = null
): Modifier = composed {
    val controller = LocalFloatingPreviewController.current
    val haptics = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    var boundsInWindow by remember { mutableStateOf(Rect.Zero) }

    val screenWidthPx = remember(configuration.screenWidthDp, density) {
        with(density) { configuration.screenWidthDp.dp.toPx() }
    }
    val screenHeightPx = remember(configuration.screenHeightDp, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }
    val isLandscape = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        configuration.screenWidthDp >= configuration.screenHeightDp
    }

    this
        .onGloballyPositioned { coordinates ->
            if (coordinates.isAttached) {
                boundsInWindow = coordinates.boundsInWindow()
            }
        }
        .pointerInput(note.path, thumbnailFile?.lastModified()) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val downPos = down.position
                val touchSlop = viewConfiguration.touchSlop
                var previousAction = DragActionTarget.NONE

                // Detect long press timeout or cancellation from early movement / child consumption
                val gesturePhase = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            if (change != null && change.isConsumed) {
                                // Child composable (e.g. 3-dot options IconButton, pin, or checkbox) consumed the click
                                return@withTimeoutOrNull GesturePhase.CANCEL
                            }
                            return@withTimeoutOrNull GesturePhase.TAP
                        }
                        if (change.isConsumed) {
                            return@withTimeoutOrNull GesturePhase.CANCEL
                        }
                        val dist = (change.position - downPos).getDistance()
                        if (dist > touchSlop) {
                            return@withTimeoutOrNull GesturePhase.CANCEL
                        }
                    }
                } ?: GesturePhase.LONG_PRESS

                when (gesturePhase) {
                    GesturePhase.TAP -> {
                        onClick()
                    }
                    GesturePhase.CANCEL -> {
                        // User swiped or moved beyond touch slop before long press - exit gesture
                        // without activating preview or drag actions.
                    }
                    GesturePhase.LONG_PRESS -> {
                        // Long press activated!
                        try {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        } catch (_: Exception) {}

                        val globalTouch = if (boundsInWindow != Rect.Zero) {
                            Offset(boundsInWindow.left + downPos.x, boundsInWindow.top + downPos.y)
                        } else {
                            downPos
                        }

                        controller.showPreview(
                            FloatingPreviewData(
                                note = note,
                                initialBounds = boundsInWindow,
                                initialCornerRadiusDp = initialCornerRadiusDp,
                                thumbnailFile = thumbnailFile,
                                folderColor = folderColor,
                                touchPositionInWindow = globalTouch
                            )
                        )
                        onLongPressFallback?.invoke()

                        // Continuously track drag movement until finger is released
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                // Finger released
                                val finalAction = controller.onFingerReleased()
                                if (finalAction != DragActionTarget.NONE) {
                                    try {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } catch (_: Exception) {}
                                }
                                break
                            }

                            val delta = change.position - downPos
                            controller.updateDrag(delta, isLandscape, screenWidthPx, screenHeightPx)

                            val currentAction = controller.activeAction.value
                            if (currentAction != previousAction) {
                                if (currentAction != DragActionTarget.NONE) {
                                    try {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } catch (_: Exception) {}
                                }
                                previousAction = currentAction
                            }

                            change.consume()
                        }
                    }
                }
            }
        }
}
