package dev.ilamparithi.aournalpp.ui.canvas

import dev.ilamparithi.aournalpp.ui.snap.SnapLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasViewModelTest {

    @Test
    fun `test initial snap and transition state`() {
        val viewModel = CanvasViewModel()

        assertEquals(SnapLayoutMode.SINGLE, viewModel.activeSnapMode.value)
        assertFalse(viewModel.isSnapMirrored.value)
        assertFalse(viewModel.showSnapAssistHost.value)
        assertNull(viewModel.activeConfiguringSlot.value)
        assertTrue(viewModel.snapSlotAssignments.value.isEmpty())
        assertTrue(viewModel.snapDividers.value.isEmpty())
        assertTrue(viewModel.snapGeometries.value.isEmpty())
        assertFalse(viewModel.showImageSourceDialog.value)
        assertFalse(viewModel.showWindowSwitcherGallery.value)
        assertTrue(viewModel.isTransitionForward.value)
        assertFalse(viewModel.isSwitchTransitionActive.value)
        assertEquals(0, viewModel.transitionSequence.intValue)
    }

    @Test
    fun `test snap mode mutations and mirror toggle`() {
        val viewModel = CanvasViewModel()

        viewModel.activeSnapMode.value = SnapLayoutMode.SPLIT_TWO
        assertEquals(SnapLayoutMode.SPLIT_TWO, viewModel.activeSnapMode.value)

        viewModel.isSnapMirrored.value = true
        assertTrue(viewModel.isSnapMirrored.value)

        viewModel.activeSnapMode.value = SnapLayoutMode.GRID_FOUR
        assertEquals(SnapLayoutMode.GRID_FOUR, viewModel.activeSnapMode.value)
    }

    @Test
    fun `test clearPreviews resets preview and transition bitmaps`() {
        val viewModel = CanvasViewModel()

        viewModel.clearPreviews()

        assertTrue(viewModel.windowPreviewCache.isEmpty())
        assertNull(viewModel.transitionOutgoingBitmap.value)
        assertNull(viewModel.transitionIncomingBitmap.value)
    }
}
