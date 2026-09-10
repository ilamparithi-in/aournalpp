package dev.ilamparithi.aournalpp.ui.hub.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.model.FolderItem
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.ui.AppDialogDefaults
import dev.ilamparithi.aournalpp.ui.promptWidth
import dev.ilamparithi.aournalpp.ui.FolderColorPickerRow
import dev.ilamparithi.aournalpp.ui.FolderIconPickerRow

@Composable
fun EditFolderAppearanceDialog(
    folder: FolderItem,
    onDismiss: () -> Unit,
    onSave: (colorHex: String?, iconEmoji: String?, iconType: String?) -> Unit
) {
    var selectedColor by remember(folder) {
        mutableStateOf(folder.colorHex ?: (if (folder.isEmergencyFolder) DocumentRepository.EMERGENCY_SAVES_DEFAULT_COLOR else null))
    }
    var selectedEmoji by remember(folder) { mutableStateOf(folder.iconEmoji) }
    var selectedIconType by remember(folder) {
        mutableStateOf<String?>(folder.iconType ?: (if (folder.isEmergencyFolder) "emergency" else (folder.role ?: "folder")))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = AppDialogDefaults.Properties,
        modifier = Modifier.promptWidth(),
        icon = { Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Customize \"${folder.name}\"", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.action_change_emoji), style = MaterialTheme.typography.labelMedium)
                    FolderIconPickerRow(
                        selectedEmoji = selectedEmoji,
                        selectedIconType = selectedIconType,
                        defaultRoleIconType = folder.role ?: (if (folder.isEmergencyFolder) "emergency" else null),
                        onIconSelected = { emoji, iconType ->
                            selectedEmoji = emoji
                            selectedIconType = iconType
                        }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.action_change_color), style = MaterialTheme.typography.labelMedium)
                    FolderColorPickerRow(
                        selectedColorHex = selectedColor,
                        onColorSelected = { selectedColor = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedColor, selectedEmoji, selectedIconType) }) {
                Text(stringResource(R.string.action_save))
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
fun RenameFolderDialog(
    folder: FolderItem,
    onDismiss: () -> Unit,
    onRename: (newName: String) -> Unit
) {
    var renameFolderNameInput by remember(folder) { mutableStateOf(folder.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = AppDialogDefaults.Properties,
        modifier = Modifier.promptWidth(),
        icon = {
            Icon(
                imageVector = Icons.Default.DriveFileRenameOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text(stringResource(R.string.dialog_rename_folder_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = renameFolderNameInput,
                    onValueChange = { renameFolderNameInput = it },
                    label = { Text(stringResource(R.string.dialog_folder_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val trimmed = renameFolderNameInput.trim()
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

@Composable
fun MoveToFolderDialog(
    selectedDocs: List<NoteDocument>,
    availableFolders: List<FolderItem>,
    onDismiss: () -> Unit,
    onMoveToRoot: () -> Unit,
    onMoveToFolder: (destFolder: FolderItem) -> Unit,
    onCreateInlineFolderAndMove: (name: String, colorHex: String?) -> Unit
) {
    var isCreatingInlineFolder by remember { mutableStateOf(false) }
    var inlineFolderName by remember { mutableStateOf("") }
    var inlineFolderColor by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = AppDialogDefaults.Properties,
        modifier = Modifier.promptWidth(),
        icon = {
            Icon(
                Icons.AutoMirrored.Filled.DriveFileMove,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(stringResource(R.string.dialog_move_notes_title, selectedDocs.size), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.dialog_move_destination_header),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isCreatingInlineFolder) {
                    OutlinedTextField(
                        value = inlineFolderName,
                        onValueChange = { inlineFolderName = it },
                        label = { Text(stringResource(R.string.dialog_new_folder_title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FolderColorPickerRow(
                        selectedColorHex = inlineFolderColor,
                        onColorSelected = { inlineFolderColor = it }
                    )
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { isCreatingInlineFolder = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(onClick = {
                            if (inlineFolderName.isNotBlank()) {
                                onCreateInlineFolderAndMove(inlineFolderName.trim(), inlineFolderColor)
                            }
                        }) {
                            Text(stringResource(R.string.action_create_and_move))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMoveToRoot() }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(stringResource(R.string.hub_root_folder_name), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        items(availableFolders, key = { it.file.absolutePath }) { folder ->
                            val fColor = folder.colorHex?.let {
                                try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
                            } ?: MaterialTheme.colorScheme.primary

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = fColor.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMoveToFolder(folder) }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (!folder.iconEmoji.isNullOrBlank()) {
                                        Text(folder.iconEmoji, fontSize = 18.sp)
                                    } else {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = fColor, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(folder.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text(
                                        pluralStringResource(
                                            R.plurals.hub_folder_notes_count,
                                            folder.itemCount,
                                            folder.itemCount
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        item {
                            OutlinedButton(
                                onClick = { isCreatingInlineFolder = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.hub_create_folder))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
