package dev.ilamparithi.aournalpp.ui.hub

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.model.NoteFileType
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.ui.FileTypePill
import dev.ilamparithi.aournalpp.ui.InteractiveMarqueeText
import dev.ilamparithi.aournalpp.ui.StandardNoteActionDropdown
import dev.ilamparithi.aournalpp.ui.common.AppIconButton
import dev.ilamparithi.aournalpp.utils.AccessibilityUtils
import dev.ilamparithi.aournalpp.utils.ThumbnailManager

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveNoteCard(
    note: NoteDocument,
    isGridView: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isTrashMode: Boolean,
    pdfExportManager: PdfExportManager,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTogglePin: () -> Unit,
    onShareExport: (() -> Unit)? = null,
    onExportPdf: (() -> Unit)? = null,
    onSharePdf: (() -> Unit)? = null,
    onShareXopp: (() -> Unit)? = null,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onRestore: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val onOpenMenu = remember { { showMenu = true } }
    val onDismissMenu = remember { { showMenu = false } }

    val thumbnailImage by produceState<ImageBitmap?>(
        initialValue = ThumbnailManager.getCachedThumbnail(note.file, note.lastModifiedMs),
        key1 = note.lastModifiedMs
    ) {
        value = ThumbnailManager.getOrCreateThumbnailBitmap(context, note.file, pdfExportManager, note.lastModifiedMs)
    }

    val cardShape = RoundedCornerShape(16.dp)

    var cardInteractionTimestamp by remember { mutableStateOf(0L) }

    val openActionLabel = stringResource(R.string.action_open_note)
    val pinActionLabel = if (note.isPinned) stringResource(R.string.action_unpin_note) else stringResource(R.string.action_pin_note)
    val shareExportActionLabel = stringResource(R.string.action_share_export)
    val exportPdfActionLabel = stringResource(R.string.action_export_pdf)
    val shareActionLabel = stringResource(R.string.action_share_note)
    val renameActionLabel = stringResource(R.string.action_rename)
    val duplicateActionLabel = stringResource(R.string.action_keep_both)
    val deleteActionLabel = stringResource(R.string.action_delete)
    val restoreActionLabel = stringResource(R.string.action_restore_note)

    val customActionsList = remember(note, isTrashMode, isSelectionMode) {
        buildList {
            add(CustomAccessibilityAction(openActionLabel) { onClick(); true })
            if (!isSelectionMode) {
                add(CustomAccessibilityAction(pinActionLabel) { onTogglePin(); true })
                if (onShareExport != null) {
                    add(CustomAccessibilityAction(shareExportActionLabel) { onShareExport(); true })
                } else {
                    onExportPdf?.let { add(CustomAccessibilityAction(exportPdfActionLabel) { it(); true }) }
                    onShareXopp?.let { add(CustomAccessibilityAction(shareActionLabel) { it(); true }) }
                }
                add(CustomAccessibilityAction(renameActionLabel) { onRename(); true })
                add(CustomAccessibilityAction(duplicateActionLabel) { onDuplicate(); true })
                add(CustomAccessibilityAction(deleteActionLabel) { onDelete(); true })
            }
            if (isTrashMode && onRestore != null) {
                add(CustomAccessibilityAction(restoreActionLabel) { onRestore(); true })
            }
        }
    }

    val a11yNoteDescription = remember(note, isSelected, isSelectionMode) {
        AccessibilityUtils.buildNoteCardA11yDescription(
            title = note.title,
            fileType = note.fileType,
            folderName = note.folder,
            lastModified = note.fuzzyLastModified,
            isPinned = note.isPinned,
            isSelected = if (isSelectionMode) isSelected else null
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .pointerInput(note.path) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val hasTouch = event.changes.any { it.pressed || it.positionChanged() }
                        if (hasTouch || event.type == PointerEventType.Enter || event.type == PointerEventType.Move) {
                            cardInteractionTimestamp = System.currentTimeMillis()
                        }
                    }
                }
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                this.contentDescription = a11yNoteDescription
                customActions = customActionsList
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = if (isSelected) 2.5.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = cardShape
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                .compositeOver(MaterialTheme.colorScheme.surface)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                .compositeOver(MaterialTheme.colorScheme.surface)
        )
    ) {
        if (isGridView) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Thumbnail Preview Header (4:3 aspect ratio)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.35f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnailImage != null) {
                        Image(
                            bitmap = thumbnailImage!!,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = when (note.fileType) {
                                NoteFileType.PDF -> Icons.Default.PictureAsPdf
                                NoteFileType.XOJ -> Icons.Default.History
                                else -> Icons.Default.Edit
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Format Badge Overlay
                    FileTypePill(
                        fileType = note.fileType,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    )

                    // Pinned Badge Overlay (Top Right, when not selecting)
                    if (note.isPinned && !isSelectionMode) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned Note",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Selection Checkbox Badge Overlay
                    if (isSelectionMode) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(26.dp)
                                .border(
                                    width = if (isSelected) 0.dp else 1.5.dp,
                                    color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.padding(4.dp))
                            }
                        }
                    }
                    // Autosave Available Badge (Bottom of Thumbnail, above Info section)
                    if (note.autosaveInfo != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Autosave Available",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // Card Body
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InteractiveMarqueeText(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            externalTrigger = cardInteractionTimestamp,
                            modifier = Modifier.weight(1f)
                        )

                        if (!isSelectionMode && !isTrashMode) {
                            Box {
                                val moreOptionsLabel = stringResource(R.string.action_details)
                                AppIconButton(
                                    onClick = onOpenMenu,
                                    tooltip = moreOptionsLabel,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = moreOptionsLabel, modifier = Modifier.size(18.dp))
                                }
                                StandardNoteActionDropdown(
                                    expanded = showMenu,
                                    isPinned = note.isPinned,
                                    onDismiss = onDismissMenu,
                                    onTogglePin = onTogglePin,
                                    onShareExport = onShareExport,
                                    onExportPdf = onExportPdf,
                                    onSharePdf = onSharePdf,
                                    onShareXopp = onShareXopp,
                                    onRename = onRename,
                                    onDuplicate = onDuplicate,
                                    onDelete = onDelete
                                )
                            }
                        } else if (isTrashMode && onRestore != null) {
                            val restoreLabel = stringResource(R.string.action_restore)
                            AppIconButton(
                                onClick = onRestore,
                                tooltip = restoreLabel,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = restoreLabel, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InteractiveMarqueeText(
                            text = note.lastModifiedFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            externalTrigger = cardInteractionTimestamp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(note.sizeFormatted, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            // Horizontal List Mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isSelectionMode) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(26.dp)
                            .border(
                                width = if (isSelected) 0.dp else 1.5.dp,
                                color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.padding(4.dp))
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (note.fileType) {
                                    NoteFileType.PDF -> Icons.Default.PictureAsPdf
                                    NoteFileType.XOJ -> Icons.Default.History
                                    else -> Icons.Default.Edit
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InteractiveMarqueeText(
                            text = note.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            externalTrigger = cardInteractionTimestamp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (note.isPinned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        InteractiveMarqueeText(
                            text = note.lastModifiedFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            externalTrigger = cardInteractionTimestamp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(note.sizeFormatted, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (!isSelectionMode && !isTrashMode) {
                    Box {
                        val moreOptionsLabel = stringResource(R.string.action_details)
                        AppIconButton(
                            onClick = onOpenMenu,
                            tooltip = moreOptionsLabel
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = moreOptionsLabel)
                        }
                        StandardNoteActionDropdown(
                            expanded = showMenu,
                            isPinned = note.isPinned,
                            onDismiss = onDismissMenu,
                            onTogglePin = onTogglePin,
                            onShareExport = onShareExport,
                            onExportPdf = onExportPdf,
                            onSharePdf = onSharePdf,
                            onShareXopp = onShareXopp,
                            onRename = onRename,
                            onDuplicate = onDuplicate,
                            onDelete = onDelete
                        )
                    }
                } else if (isTrashMode && onRestore != null) {
                    val restoreLabel = stringResource(R.string.action_restore)
                    AppIconButton(
                        onClick = onRestore,
                        tooltip = restoreLabel
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = restoreLabel, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun NoteActionDropdown(
    expanded: Boolean,
    isPinned: Boolean = false,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onExportPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onShareXopp: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(if (isPinned) "Unpin from Home" else "Pin to Home") },
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
        DropdownMenuItem(
            text = { Text("Export to PDF") },
            leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
            onClick = { onDismiss(); onExportPdf() }
        )
        DropdownMenuItem(
            text = { Text("Share as PDF") },
            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
            onClick = { onDismiss(); onSharePdf() }
        )
        DropdownMenuItem(
            text = { Text("Share as Note") },
            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
            onClick = { onDismiss(); onShareXopp() }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Rename") },
            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
            onClick = { onDismiss(); onRename() }
        )
        DropdownMenuItem(
            text = { Text("Duplicate") },
            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
            onClick = { onDismiss(); onDuplicate() }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = { onDismiss(); onDelete() }
        )
    }
}
