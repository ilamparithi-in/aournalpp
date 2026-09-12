package dev.ilamparithi.aournalpp.ui.hub

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.ilamparithi.aournalpp.R

/**
 * Standard context action dropdown menu for Note cards and items.
 */
@Composable
fun NoteActionDropdown(
    expanded: Boolean,
    isPinned: Boolean = false,
    onDismiss: () -> Unit,
    onTogglePin: (() -> Unit)? = null,
    onShareExport: (() -> Unit)? = null,
    onExportPdf: (() -> Unit)? = null,
    onSharePdf: (() -> Unit)? = null,
    onShareXopp: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (onTogglePin != null) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (isPinned) stringResource(R.string.action_unpin_note)
                        else stringResource(R.string.action_pin_note)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isPinned) Icons.Outlined.PushPin else Icons.Default.PushPin,
                        contentDescription = null,
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = { onDismiss(); onTogglePin() }
            )
            HorizontalDivider()
        }
        if (onShareExport != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_share_export)) },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                onClick = { onDismiss(); onShareExport() }
            )
        } else {
            if (onExportPdf != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_export_pdf)) },
                    leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                    onClick = { onDismiss(); onExportPdf() }
                )
            }
            if (onSharePdf != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_share)) },
                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                    onClick = { onDismiss(); onSharePdf() }
                )
            }
            if (onShareXopp != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_share_note)) },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                    onClick = { onDismiss(); onShareXopp() }
                )
            }
        }
        if (onRename != null) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_rename)) },
                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                onClick = { onDismiss(); onRename() }
            )
        }
        if (onDuplicate != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_keep_both)) },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                onClick = { onDismiss(); onDuplicate() }
            )
        }
        if (onDelete != null) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = { onDismiss(); onDelete() }
            )
        }
    }
}

/**
 * Backwards compatibility alias for NoteActionDropdown.
 */
@Composable
fun StandardNoteActionDropdown(
    expanded: Boolean,
    isPinned: Boolean = false,
    onDismiss: () -> Unit,
    onTogglePin: (() -> Unit)? = null,
    onShareExport: (() -> Unit)? = null,
    onExportPdf: (() -> Unit)? = null,
    onSharePdf: (() -> Unit)? = null,
    onShareXopp: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) = NoteActionDropdown(
    expanded = expanded,
    isPinned = isPinned,
    onDismiss = onDismiss,
    onTogglePin = onTogglePin,
    onShareExport = onShareExport,
    onExportPdf = onExportPdf,
    onSharePdf = onSharePdf,
    onShareXopp = onShareXopp,
    onRename = onRename,
    onDuplicate = onDuplicate,
    onDelete = onDelete
)
