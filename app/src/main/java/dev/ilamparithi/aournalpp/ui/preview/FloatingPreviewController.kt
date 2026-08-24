package dev.ilamparithi.aournalpp.ui.preview

import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.ilamparithi.aournalpp.model.NoteDocument
import java.io.File

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
 * Controller managing the lifecycle and visibility of the floating preview overlay.
 */
class FloatingPreviewController {
    private val _currentPreview = mutableStateOf<FloatingPreviewData?>(null)
    val currentPreview: State<FloatingPreviewData?> = _currentPreview

    private val _isFingerDown = mutableStateOf(false)
    val isFingerDown: State<Boolean> = _isFingerDown

    fun showPreview(data: FloatingPreviewData) {
        _currentPreview.value = data
        _isFingerDown.value = true
    }

    fun onFingerReleased() {
        _isFingerDown.value = false
    }

    fun dismissImmediately() {
        _currentPreview.value = null
        _isFingerDown.value = false
    }
}

val LocalFloatingPreviewController = compositionLocalOf<FloatingPreviewController> {
    error("No FloatingPreviewController provided. Wrap UI in FloatingPreviewHost.")
}

/**
 * Modifier to attach long-press floating preview behavior to any preview component.
 * - Tap: invokes [onClick].
 * - Long press: captures current window bounds, triggers haptic feedback,
 *   and holds the floating preview open until finger release.
 * - Scroll/Drag: gracefully yields to parent scroll containers.
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
    var boundsInWindow by remember { mutableStateOf(Rect.Zero) }

    this
        .onGloballyPositioned { coordinates ->
            if (coordinates.isAttached) {
                boundsInWindow = coordinates.boundsInWindow()
            }
        }
        .pointerInput(note.path, thumbnailFile?.lastModified()) {
            detectTapGestures(
                onPress = {
                    try {
                        tryAwaitRelease()
                    } finally {
                        controller.onFingerReleased()
                    }
                },
                onLongPress = { localOffset ->
                    try {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (e: Exception) {
                        // ignore if haptics unavailable
                    }
                    val globalTouch = if (boundsInWindow != Rect.Zero) {
                        Offset(boundsInWindow.left + localOffset.x, boundsInWindow.top + localOffset.y)
                    } else {
                        Offset.Unspecified
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
                },
                onTap = {
                    onClick()
                }
            )
        }
}
