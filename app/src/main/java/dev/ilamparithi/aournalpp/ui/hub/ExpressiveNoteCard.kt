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
import dev.ilamparithi.aournalpp.ui.hub.NoteActionDropdown
import dev.ilamparithi.aournalpp.ui.hub.NoteCardCallbacks
import dev.ilamparithi.aournalpp.ui.hub.PinnedBadge
import dev.ilamparithi.aournalpp.ui.hub.SelectionCheckboxBadge
import dev.ilamparithi.aournalpp.ui.hub.rememberNoteAccessibilityActions
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
    onRestore: (() -> Unit)? = null,
    thumbnailManager: dev.ilamparithi.aournalpp.utils.IThumbnailManager = dev.ilamparithi.aournalpp.utils.LocalThumbnailManager.current
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val onOpenMenu = remember { { showMenu = true } }
    val onDismissMenu = remember { { showMenu = false } }

    val thumbnailImage by produceState<ImageBitmap?>(
        initialValue = thumbnailManager.getCachedThumbnail(note.file, note.lastModifiedMs),
        key1 = note.lastModifiedMs
    ) {
        value = thumbnailManager.getOrCreateThumbnailBitmap(context, note.file, pdfExportManager, note.lastModifiedMs)
    }

    val cardShape = RoundedCornerShape(16.dp)

    var cardInteractionTimestamp by remember { mutableStateOf(0L) }

    val callbacks = remember(
        onClick, onLongClick, onTogglePin, onShareExport, onExportPdf,
        onSharePdf, onShareXopp, onRename, onDuplicate, onDelete, onRestore
    ) {
        NoteCardCallbacks(
            onClick = onClick,
            onLongClick = onLongClick,
            onTogglePin = onTogglePin,
            onShareExport = onShareExport,
            onExportPdf = onExportPdf,
            onSharePdf = onSharePdf,
            onShareXopp = onShareXopp,
            onRename = onRename,
            onDuplicate = onDuplicate,
            onDelete = onDelete,
            onRestore = onRestore
        )
    }

    val customActionsList = rememberNoteAccessibilityActions(
        note = note,
        isTrashMode = isTrashMode,
        isSelectionMode = isSelectionMode,
        callbacks = callbacks
    )

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

                    // Pinned Badge Overlay (Top Right, when not selecting)                    // Pinned indicator badge
                    if (note.isPinned && !isSelectionMode) {
                        PinnedBadge(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            size = 26.dp
                        )
                    }

                    // Selection Checkbox Badge Overlay
                    if (isSelectionMode) {
                        SelectionCheckboxBadge(
                            isSelected = isSelected,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            size = 26.dp
                        )
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
                                NoteActionDropdown(
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
                    SelectionCheckboxBadge(
                        isSelected = isSelected,
                        size = 26.dp
                    )
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
                        NoteActionDropdown(
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


