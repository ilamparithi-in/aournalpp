package dev.ilamparithi.aournalpp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.File

/**
 * Modular dialog for creating a new folder with name, emoji/icon picker, and color accent.
 *
 * @param parentFolder Optional parent directory (for display in title if desired).
 * @param title Custom title text. Defaults to "New Folder in <parent>" or "Create New Folder".
 * @param confirmButtonLabel Label for the primary button ("Create", "Create & Select", etc.).
 * @param initialColorHex Initial color accent hex.
 * @param initialEmoji Initial emoji symbol.
 * @param initialIconType Initial icon type (e.g. "folder").
 * @param isCreating Whether a creation request is currently in progress.
 * @param onDismiss Callback when the dialog is dismissed/cancelled.
 * @param onCreate Callback with folder parameters when confirmed.
 */
@Composable
fun CreateFolderDialog(
    parentFolder: File? = null,
    title: String = if (parentFolder != null) "New Folder in \"${parentFolder.name}\"" else "Create New Folder",
    confirmButtonLabel: String = "Create",
    initialColorHex: String = "#4CAF50",
    initialEmoji: String? = null,
    initialIconType: String? = "folder",
    isCreating: Boolean = false,
    onDismiss: () -> Unit,
    onCreate: (name: String, colorHex: String, iconEmoji: String?, iconType: String?) -> Unit
) {
    var newFolderName by remember { mutableStateOf("") }
    var selectedFolderColor by remember { mutableStateOf(initialColorHex) }
    var selectedFolderEmoji by remember { mutableStateOf(initialEmoji) }
    var selectedFolderIconType by remember { mutableStateOf(initialIconType) }

    Dialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        properties = AppDialogDefaults.Properties
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.promptWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.dialog_folder_name_hint)) },
                    placeholder = { Text("e.g. Physics, Sketches") },
                    singleLine = true,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_change_emoji), style = MaterialTheme.typography.labelMedium)
                    FolderIconPickerRow(
                        selectedEmoji = selectedFolderEmoji,
                        selectedIconType = selectedFolderIconType,
                        onIconSelected = { emoji, iconType ->
                            selectedFolderEmoji = emoji
                            selectedFolderIconType = iconType
                        }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_change_color), style = MaterialTheme.typography.labelMedium)
                    FolderColorPickerRow(
                        selectedColorHex = selectedFolderColor,
                        onColorSelected = { selectedFolderColor = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isCreating
                    ) {
                        Text(androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmed = newFolderName.trim()
                            if (trimmed.isNotBlank() && !isCreating) {
                                onCreate(trimmed, selectedFolderColor, selectedFolderEmoji, selectedFolderIconType)
                            }
                        },
                        enabled = newFolderName.isNotBlank() && !isCreating,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(confirmButtonLabel)
                    }
                }
            }
        }
    }
}
