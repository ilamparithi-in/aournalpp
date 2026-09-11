package dev.ilamparithi.aournalpp.ui.canvas

import android.graphics.Bitmap
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor.X11WindowInfo
import dev.ilamparithi.aournalpp.ui.snap.DividerGeometry
import dev.ilamparithi.aournalpp.ui.snap.SnapLayoutMode
import dev.ilamparithi.aournalpp.ui.snap.WindowSlotGeometry

/**
 * ViewModel retaining Canvas UI state, window preview bitmaps, and snap layout selections
 * across configuration changes and UI recompositions.
 */
class CanvasViewModel : ViewModel() {
    val windowPreviewCache = mutableStateMapOf<String, Bitmap>()
    val transitionOutgoingBitmap = mutableStateOf<Bitmap?>(null)
    val transitionIncomingBitmap = mutableStateOf<Bitmap?>(null)
    val transitionTargetWindow = mutableStateOf<X11WindowInfo?>(null)
    val isTransitionForward = mutableStateOf(true)
    val isSwitchTransitionActive = mutableStateOf(false)
    val transitionSequence = mutableIntStateOf(0)

    val activeSnapMode = mutableStateOf(SnapLayoutMode.SINGLE)
    val isSnapMirrored = mutableStateOf(false)
    val showSnapAssistHost = mutableStateOf(false)
    val activeConfiguringSlot = mutableStateOf<Int?>(null)
    val snapSlotAssignments = mutableStateOf<Map<Int, String>>(emptyMap())
    val snapDividers = mutableStateOf<List<DividerGeometry>>(emptyList())
    val snapGeometries = mutableStateOf<List<WindowSlotGeometry>>(emptyList())

    val showImageSourceDialog = mutableStateOf(false)
    val showWindowSwitcherGallery = mutableStateOf(false)

    fun clearPreviews() {
        for (bmp in windowPreviewCache.values) {
            if (!bmp.isRecycled) {
                try {
                    bmp.recycle()
                } catch (_: Exception) {}
            }
        }
        windowPreviewCache.clear()
        transitionOutgoingBitmap.value = null
        transitionIncomingBitmap.value = null
    }

    override fun onCleared() {
        super.onCleared()
        clearPreviews()
    }
}
