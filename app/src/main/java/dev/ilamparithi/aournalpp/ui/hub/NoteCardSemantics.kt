package dev.ilamparithi.aournalpp.ui.hub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.model.NoteDocument

/**
 * Standard callback action set for Note card interactions.
 */
data class NoteCardCallbacks(
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
    val onTogglePin: (() -> Unit)? = null,
    val onShareExport: (() -> Unit)? = null,
    val onExportPdf: (() -> Unit)? = null,
    val onSharePdf: (() -> Unit)? = null,
    val onShareXopp: (() -> Unit)? = null,
    val onRename: (() -> Unit)? = null,
    val onDuplicate: (() -> Unit)? = null,
    val onDelete: (() -> Unit)? = null,
    val onRestore: (() -> Unit)? = null
)

/**
 * Builds reusable custom accessibility actions for TalkBack and assistive technologies.
 */
@Composable
fun rememberNoteAccessibilityActions(
    note: NoteDocument,
    isTrashMode: Boolean,
    isSelectionMode: Boolean,
    callbacks: NoteCardCallbacks
): List<CustomAccessibilityAction> {
    val openActionLabel = stringResource(R.string.action_open_note)
    val pinActionLabel = if (note.isPinned) stringResource(R.string.action_unpin_note) else stringResource(R.string.action_pin_note)
    val shareExportActionLabel = stringResource(R.string.action_share_export)
    val exportPdfActionLabel = stringResource(R.string.action_export_pdf)
    val shareActionLabel = stringResource(R.string.action_share_note)
    val renameActionLabel = stringResource(R.string.action_rename)
    val duplicateActionLabel = stringResource(R.string.action_keep_both)
    val deleteActionLabel = stringResource(R.string.action_delete)
    val restoreActionLabel = stringResource(R.string.action_restore_note)

    return remember(
        note, isTrashMode, isSelectionMode,
        callbacks.onClick, callbacks.onTogglePin, callbacks.onShareExport,
        callbacks.onExportPdf, callbacks.onSharePdf, callbacks.onShareXopp,
        callbacks.onRename, callbacks.onDuplicate, callbacks.onDelete, callbacks.onRestore
    ) {
        buildList {
            add(CustomAccessibilityAction(openActionLabel) { callbacks.onClick(); true })
            if (!isSelectionMode) {
                callbacks.onTogglePin?.let { pin ->
                    add(CustomAccessibilityAction(pinActionLabel) { pin(); true })
                }
                if (callbacks.onShareExport != null) {
                    add(CustomAccessibilityAction(shareExportActionLabel) { callbacks.onShareExport.invoke(); true })
                } else {
                    callbacks.onExportPdf?.let { exportPdf ->
                        add(CustomAccessibilityAction(exportPdfActionLabel) { exportPdf(); true })
                    }
                    callbacks.onShareXopp?.let { shareXopp ->
                        add(CustomAccessibilityAction(shareActionLabel) { shareXopp(); true })
                    }
                }
                callbacks.onRename?.let { rename ->
                    add(CustomAccessibilityAction(renameActionLabel) { rename(); true })
                }
                callbacks.onDuplicate?.let { duplicate ->
                    add(CustomAccessibilityAction(duplicateActionLabel) { duplicate(); true })
                }
                callbacks.onDelete?.let { delete ->
                    add(CustomAccessibilityAction(deleteActionLabel) { delete(); true })
                }
            }
            if (isTrashMode && callbacks.onRestore != null) {
                add(CustomAccessibilityAction(restoreActionLabel) { callbacks.onRestore.invoke(); true })
            }
        }
    }
}
