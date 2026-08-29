package dev.ilamparithi.aournalpp.ui

import java.text.BreakIterator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.sp

import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.FileDownload

val DEFAULT_PRESET_FOLDER_EMOJIS = listOf(
    "📝", "📚", "🎨", "💡", "🔬", "📐", "💼", "🏠", "⭐", "🚀", "🧪", "📓", "🏷️", "🎯", "🌿", "💻", "☕"
)

/**
 * An expressive folder icon & emoji selector with:
 * 1. Default Role Icon (if folder has a special role, e.g. Audio, Import, Emergency)
 * 2. Default Folder Icon (Icons.Default.Folder)
 * 3. Curated emoji presets
 * 4. Custom selected emoji (if active)
 * 5. Plus (+) button for custom emoji input
 */
@Composable
fun FolderIconPickerRow(
    selectedEmoji: String?,
    selectedIconType: String?, // "folder", "emergency", "import", "audio", or null
    onIconSelected: (emoji: String?, iconType: String?) -> Unit,
    defaultRoleIconType: String? = null,
    presetEmojis: List<String> = DEFAULT_PRESET_FOLDER_EMOJIS,
    modifier: Modifier = Modifier
) {
    var showCustomEmojiDialog by remember { mutableStateOf(false) }

    val effectiveRoleIcon = defaultRoleIconType?.lowercase()
    val isRoleIconSelected = selectedEmoji.isNullOrBlank() && selectedIconType == effectiveRoleIcon && effectiveRoleIcon != null
    val isDefaultFolderSelected = selectedEmoji.isNullOrBlank() && (selectedIconType == "folder" || (selectedIconType == null && effectiveRoleIcon == null))

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // 1. Role-specific Default Icon Option (First Choice if present)
        if (effectiveRoleIcon != null) {
            item {
                val roleVector = when (effectiveRoleIcon) {
                    "emergency" -> Icons.Default.Emergency
                    "import", "imported" -> Icons.Default.FileDownload
                    "audio" -> Icons.Default.AudioFile
                    else -> Icons.Default.Folder
                }
                val roleContainerColor = if (isRoleIconSelected) {
                    if (effectiveRoleIcon == "emergency") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
                val roleBorder = if (isRoleIconSelected) {
                    if (effectiveRoleIcon == "emergency") BorderStroke(2.dp, MaterialTheme.colorScheme.error) else BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else null

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = roleContainerColor,
                    border = roleBorder,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onIconSelected(null, effectiveRoleIcon) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = roleVector,
                            contentDescription = "Role Default Icon ($effectiveRoleIcon)",
                            tint = if (isRoleIconSelected) {
                                if (effectiveRoleIcon == "emergency") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Standard Default Folder Icon Option
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDefaultFolderSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = if (isDefaultFolderSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onIconSelected(null, "folder") }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Default Folder Icon",
                        tint = if (isDefaultFolderSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 3. Preset Emojis
        items(presetEmojis) { emoji ->
            val isSelected = emoji == selectedEmoji
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onIconSelected(emoji, null) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = emoji, fontSize = 20.sp)
                }
            }
        }


        // 4. Custom Selected Emoji (if selected and not in preset list)
        if (!selectedEmoji.isNullOrBlank() && !presetEmojis.contains(selectedEmoji)) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { showCustomEmojiDialog = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = selectedEmoji, fontSize = 20.sp)
                    }
                }
            }
        }

        // 5. "+" Option to input/type any custom emoji
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(40.dp)
                    .clickable { showCustomEmojiDialog = true }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Choose Custom Emoji",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showCustomEmojiDialog) {
        CustomEmojiInputDialog(
            currentEmoji = selectedEmoji,
            onDismiss = { showCustomEmojiDialog = false },
            onEmojiSelected = { chosenEmoji ->
                if (chosenEmoji != null) {
                    onIconSelected(chosenEmoji, null)
                }
            }
        )
    }
}

/**
 * Backward-compatible FolderEmojiPickerRow delegating to FolderIconPickerRow.
 */
@Composable
fun FolderEmojiPickerRow(
    selectedEmoji: String?,
    onEmojiSelected: (String?) -> Unit,
    presetEmojis: List<String> = DEFAULT_PRESET_FOLDER_EMOJIS,
    modifier: Modifier = Modifier
) {
    FolderIconPickerRow(
        selectedEmoji = selectedEmoji,
        selectedIconType = if (selectedEmoji.isNullOrBlank()) "folder" else null,
        onIconSelected = { emoji, _ -> onEmojiSelected(emoji) },
        presetEmojis = presetEmojis,
        modifier = modifier
    )
}

private fun getSingleEmojiOrChar(text: String): String {
    if (text.isEmpty()) return ""
    val it = BreakIterator.getCharacterInstance()
    it.setText(text)
    val firstEnd = it.next()
    return if (firstEnd != BreakIterator.DONE && firstEnd > 0) text.substring(0, firstEnd) else ""
}

@Composable
fun CustomEmojiInputDialog(
    currentEmoji: String?,
    onDismiss: () -> Unit,
    onEmojiSelected: (String?) -> Unit
) {
    var input by remember { mutableStateOf(currentEmoji ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Any Emoji", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Type or pick a single emoji using your keyboard:",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { raw ->
                        input = getSingleEmojiOrChar(raw)
                    },
                    label = { Text("Emoji (1 Character)") },
                    placeholder = { Text("e.g. 🐼") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onEmojiSelected(input.trim().takeIf { it.isNotBlank() })
                onDismiss()
            }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
