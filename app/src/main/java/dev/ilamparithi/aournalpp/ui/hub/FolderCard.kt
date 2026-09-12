package dev.ilamparithi.aournalpp.ui.hub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.model.FolderItem
import dev.ilamparithi.aournalpp.ui.common.AppIconButton
import dev.ilamparithi.aournalpp.utils.AccessibilityUtils

@Composable
fun FolderCard(
    folder: FolderItem,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleExcludeRecents: () -> Unit,
    onRename: () -> Unit,
    onMapToCloud: () -> Unit,
    onCustomize: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFolderMenu by remember { mutableStateOf(false) }
    var folderInteractionTimestamp by remember { mutableStateOf(0L) }
    val accentColor = folder.colorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
    } ?: MaterialTheme.colorScheme.primary

    val a11yFolderDescription = remember(folder) {
        AccessibilityUtils.buildFolderCardA11yDescription(
            folderName = folder.name,
            noteCount = folder.itemCount,
            isPinned = folder.isPinned,
            isExcludedFromRecents = folder.isExcludedFromRecents,
            role = folder.role ?: folder.iconType
        )
    }

    val openFolderActionLabel = stringResource(R.string.action_open_folder)
    val pinFolderActionLabel = if (folder.isPinned) {
        stringResource(R.string.action_unpin_folder)
    } else {
        stringResource(R.string.action_pin_folder)
    }
    val renameFolderActionLabel = stringResource(R.string.action_rename_folder)
    val customizeFolderActionLabel = stringResource(R.string.action_customize_folder)
    val deleteFolderActionLabel = stringResource(R.string.action_delete_folder)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(folder.file.absolutePath) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val hasTouch = event.changes.any { it.pressed || it.positionChanged() }
                        if (hasTouch || event.type == PointerEventType.Enter || event.type == PointerEventType.Move) {
                            folderInteractionTimestamp = System.currentTimeMillis()
                        }
                    }
                }
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                this.contentDescription = a11yFolderDescription
                customActions = listOf(
                    CustomAccessibilityAction(openFolderActionLabel) {
                        onClick()
                        true
                    },
                    CustomAccessibilityAction(pinFolderActionLabel) {
                        onTogglePin()
                        true
                    },
                    CustomAccessibilityAction(renameFolderActionLabel) {
                        onRename()
                        true
                    },
                    CustomAccessibilityAction(customizeFolderActionLabel) {
                        onCustomize()
                        true
                    },
                    CustomAccessibilityAction(deleteFolderActionLabel) {
                        onDelete()
                        true
                    }
                )
            }
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!folder.iconEmoji.isNullOrBlank()) {
                Text(
                    text = folder.iconEmoji,
                    fontSize = 28.sp
                )
            } else if (folder.iconType == "emergency" || folder.isEmergencyFolder) {
                Icon(
                    imageVector = Icons.Default.Emergency,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (folder.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = pluralStringResource(
                        R.plurals.hub_folder_notes_count,
                        folder.itemCount,
                        folder.itemCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                val moreFolderOptionsLabel = stringResource(R.string.action_details)
                AppIconButton(
                    onClick = { showFolderMenu = true },
                    tooltip = moreFolderOptionsLabel
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = moreFolderOptionsLabel,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showFolderMenu,
                    onDismissRequest = { showFolderMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (folder.isPinned) "Unpin Folder" else "Pin Folder") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = null,
                                tint = if (folder.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            showFolderMenu = false
                            onTogglePin()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (folder.isExcludedFromRecents) "Include in Recents" else "Exclude from Recents") },
                        leadingIcon = {
                            Icon(
                                if (folder.isExcludedFromRecents) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (folder.isExcludedFromRecents) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            showFolderMenu = false
                            onToggleExcludeRecents()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename Folder") },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                        onClick = {
                            showFolderMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Map to Cloud...") },
                        leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            showFolderMenu = false
                            onMapToCloud()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Customize Icon & Color") },
                        leadingIcon = { Icon(Icons.Default.ColorLens, contentDescription = null) },
                        onClick = {
                            showFolderMenu = false
                            onCustomize()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete Folder", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showFolderMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
