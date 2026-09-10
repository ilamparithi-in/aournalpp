package dev.ilamparithi.aournalpp.ui.hub.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.ui.AppDialogDefaults
import dev.ilamparithi.aournalpp.ui.promptWidth

@Composable
fun DeleteNoteDialog(
    note: NoteDocument,
    isViewingTrash: Boolean,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = AppDialogDefaults.Properties,
        modifier = Modifier.promptWidth(),
        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = {
            Text(
                if (isViewingTrash) stringResource(R.string.dialog_delete_permanent_title)
                else stringResource(R.string.dialog_delete_note_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                if (isViewingTrash) "Permanently delete \"${note.title}\"? This action cannot be undone."
                else stringResource(R.string.dialog_delete_note_body, note.title)
            )
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = onConfirmDelete
            ) {
                Text(
                    if (isViewingTrash) stringResource(R.string.action_delete_permanent)
                    else stringResource(R.string.action_delete)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun BatchDeletePermanentDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = AppDialogDefaults.Properties,
        modifier = Modifier.promptWidth(),
        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.dialog_delete_permanent_title), fontWeight = FontWeight.Bold) },
        text = {
            Text(
                pluralStringResource(
                    R.plurals.dialog_delete_multi_body,
                    selectedCount,
                    selectedCount
                )
            )
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = onConfirmDelete
            ) {
                Text(stringResource(R.string.action_delete_permanent))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun EmptyTrashConfirmDialog(
    onDismiss: () -> Unit,
    onConfirmEmpty: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = AppDialogDefaults.Properties,
        modifier = Modifier.promptWidth(),
        icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.dialog_empty_trash_title), fontWeight = FontWeight.Bold) },
        text = {
            Text(stringResource(R.string.dialog_empty_trash_body))
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = onConfirmEmpty
            ) {
                Text(stringResource(R.string.hub_menu_empty_trash))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun RenameNoteDialog(
    note: NoteDocument,
    onDismiss: () -> Unit,
    onRename: (newTitle: String) -> Unit
) {
    var renameInputText by remember(note) { mutableStateOf(note.title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = AppDialogDefaults.Properties,
        modifier = Modifier.promptWidth(),
        title = { Text(stringResource(R.string.dialog_rename_title), fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = renameInputText,
                onValueChange = { renameInputText = it },
                singleLine = true,
                label = { Text(stringResource(R.string.dialog_note_name_hint)) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                val trimmed = renameInputText.trim()
                if (trimmed.isNotBlank()) {
                    onRename(trimmed)
                }
            }) {
                Text(stringResource(R.string.action_rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
