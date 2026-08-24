package dev.ilamparithi.aournalpp.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.ilamparithi.aournalpp.model.FolderItem
import kotlinx.coroutines.launch
import java.io.File

/**
 * Unified, reusable Material 3 dialog for saving notes (Emergency Recovery, Autosaves, Duplicate/Save As).
 * Includes:
 * 1. Note name input field with clear button.
 * 2. Real-time folder selection dropdown with custom folder colors and icons/emojis.
 * 3. On-the-spot "+ New Folder" creation with integrated color and icon/emoji pickers.
 */
@Composable
fun SaveAsNoteDialog(
    title: String = "Save as Note",
    subtitle: String? = "Choose a name and destination folder for this note.",
    icon: ImageVector = Icons.Default.Description,
    initialName: String = "",
    initialFolder: File? = null,
    availableFolders: List<FolderItem>,
    rootFolder: File,
    onDismiss: () -> Unit,
    onSave: (name: String, targetFolder: File) -> Unit,
    onCreateFolder: (suspend (name: String, colorHex: String, iconEmoji: String?, iconType: String?) -> Result<File>)? = null
) {
    val cleanInitialName = remember(initialName) {
        if (initialName.endsWith(".xopp", ignoreCase = true)) {
            initialName.substring(0, initialName.length - 5)
        } else {
            initialName
        }
    }

    var noteNameInput by remember { mutableStateOf(cleanInitialName) }
    var selectedFolder by remember { mutableStateOf(initialFolder ?: rootFolder) }
    var isFolderDropdownExpanded by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    val currentFolders = remember(availableFolders) {
        mutableStateListOf<FolderItem>().apply { addAll(availableFolders) }
    }

    val coroutineScope = rememberCoroutineScope()

    val isRootSelected = remember(selectedFolder, rootFolder) {
        selectedFolder.absolutePath == rootFolder.absolutePath
    }

    val selectedFolderItem = remember(selectedFolder, currentFolders) {
        currentFolders.firstOrNull { it.file.absolutePath == selectedFolder.absolutePath }
    }

    val targetFolderDisplayName = when {
        isRootSelected -> "Notes Home (Root)"
        selectedFolderItem != null -> selectedFolderItem.name
        else -> selectedFolder.name
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 1. Note Name Input
                OutlinedTextField(
                    value = noteNameInput,
                    onValueChange = { noteNameInput = it },
                    label = { Text("Note Name") },
                    placeholder = { Text("e.g. Physics Lecture Notes") },
                    singleLine = true,
                    trailingIcon = {
                        if (noteNameInput.isNotEmpty()) {
                            IconButton(onClick = { noteNameInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear text")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // 2. Destination Folder Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Destination Folder",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    anchorWidthPx = coordinates.size.width
                                }
                                .clickable { isFolderDropdownExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Folder Leading Icon / Emoji
                                when {
                                    isRootSelected -> {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    selectedFolderItem?.iconEmoji?.isNotBlank() == true -> {
                                        Text(selectedFolderItem.iconEmoji, fontSize = 16.sp)
                                    }
                                    selectedFolderItem?.iconType == "emergency" || selectedFolderItem?.isEmergencyFolder == true -> {
                                        Icon(
                                            imageVector = Icons.Default.Emergency,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    else -> {
                                        val tintColor = parseHexColor(selectedFolderItem?.colorHex) ?: MaterialTheme.colorScheme.primary
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = tintColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = targetFolderDisplayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "Select Folder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isFolderDropdownExpanded,
                            onDismissRequest = { isFolderDropdownExpanded = false },
                            modifier = if (anchorWidthPx > 0) {
                                Modifier.width(with(density) { anchorWidthPx.toDp() })
                            } else {
                                Modifier.fillMaxWidth()
                            }
                        ) {
                            // Option: Create New Folder on the spot
                            if (onCreateFolder != null) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "+ New Folder",
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.CreateNewFolder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        isFolderDropdownExpanded = false
                                        showCreateFolderDialog = true
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }

                            // Option: Notes Home (Root)
                            DropdownMenuItem(
                                text = { Text("Notes Home (Root)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    selectedFolder = rootFolder
                                    isFolderDropdownExpanded = false
                                }
                            )

                            // List of all other folders
                            currentFolders.forEach { folder ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = folder.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingIcon = {
                                        when {
                                            !folder.iconEmoji.isNullOrBlank() -> {
                                                Text(folder.iconEmoji, fontSize = 16.sp)
                                            }
                                            folder.iconType == "emergency" || folder.isEmergencyFolder -> {
                                                Icon(
                                                    imageVector = Icons.Default.Emergency,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                            else -> {
                                                val folderColor = parseHexColor(folder.colorHex) ?: MaterialTheme.colorScheme.onSurfaceVariant
                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = null,
                                                    tint = folderColor
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedFolder = folder.file
                                        isFolderDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (noteNameInput.isNotBlank()) {
                        onSave(noteNameInput.trim(), selectedFolder)
                    }
                },
                enabled = noteNameInput.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Inline "Create New Folder" Dialog
    if (showCreateFolderDialog && onCreateFolder != null) {
        var newFolderName by remember { mutableStateOf("") }
        var selectedFolderColor by remember { mutableStateOf("#4CAF50") }
        var selectedFolderEmoji by remember { mutableStateOf<String?>(null) }
        var selectedFolderIconType by remember { mutableStateOf<String?>("folder") }
        var isCreating by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { if (!isCreating) showCreateFolderDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Create New Folder",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Folder Name") },
                        placeholder = { Text("e.g. Physics, Sketches") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Folder Icon / Emoji", style = MaterialTheme.typography.labelMedium)
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
                        Text("Folder Color Accent", style = MaterialTheme.typography.labelMedium)
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
                            onClick = { showCreateFolderDialog = false },
                            enabled = !isCreating
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newFolderName.isNotBlank() && !isCreating) {
                                    isCreating = true
                                    coroutineScope.launch {
                                        val result = onCreateFolder(
                                            newFolderName.trim(),
                                            selectedFolderColor,
                                            selectedFolderEmoji,
                                            selectedFolderIconType
                                        )
                                        if (result.isSuccess) {
                                            val createdDir = result.getOrThrow()
                                            val newFolderItem = FolderItem(
                                                file = createdDir,
                                                name = createdDir.name,
                                                colorHex = selectedFolderColor,
                                                iconEmoji = selectedFolderEmoji,
                                                iconType = selectedFolderIconType,
                                                isEmergencyFolder = false
                                            )
                                            currentFolders.add(newFolderItem)
                                            selectedFolder = createdDir
                                            showCreateFolderDialog = false
                                        }
                                        isCreating = false
                                    }
                                }
                            },
                            enabled = newFolderName.isNotBlank() && !isCreating
                        ) {
                            Text("Create & Select")
                        }
                    }
                }
            }
        }
    }
}

private fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        Color(AndroidColor.parseColor(hex))
    } catch (e: Exception) {
        null
    }
}

