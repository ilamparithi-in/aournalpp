package dev.ilamparithi.aournalpp.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.model.NoteFileType
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.ui.preview.floatingPreviewLongPress
import dev.ilamparithi.aournalpp.utils.ThumbnailManager

/**
 * Standardized Note Card Composable.
 * Unifies the visual design across Collage, Gallery, Recents Carousel, and Files Hub Grid:
 * - Full-card high-quality thumbnail preview with folder accent border & shadow
 * - Top-Left: Standardized [FileTypePill] (.xopp / .xoj / .pdf)
 * - Top-Right: Leveled Action Bar containing Pinned PushPin indicator, Selection Checkbox, and 3-dot Options button
 * - Bottom: Floating folder-palette details pill ([FloatingDetailsPill]) with folder icon/emoji, folder name, and modification timestamp
 * - Long press: Radial sweep & persistent turbulence floating preview
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StandardNoteCard(
    note: NoteDocument,
    modifier: Modifier = Modifier,
    pdfExportManager: PdfExportManager,
    shape: Shape = RoundedCornerShape(18.dp),
    initialCornerRadiusDp: Float = 18f,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    // Context Action Handlers
    onTogglePin: (() -> Unit)? = null,
    onExportPdf: (() -> Unit)? = null,
    onSharePdf: (() -> Unit)? = null,
    onShareXopp: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    // State Flags
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    isTrashMode: Boolean = false,
    enableFloatingPreview: Boolean = true
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val thumbnailImage by produceState<ImageBitmap?>(
        initialValue = ThumbnailManager.getCachedThumbnail(note.file),
        key1 = note.lastModifiedMs
    ) {
        value = ThumbnailManager.getOrCreateThumbnailBitmap(context, note.file, pdfExportManager)
    }
    val thumbnailFile = remember(thumbnailImage) { ThumbnailManager.getCachedThumbnailFile(note.file) }

    val folderAccentColor = note.folderColorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
    } ?: MaterialTheme.colorScheme.primary

    val hasActions = onTogglePin != null || onExportPdf != null || onSharePdf != null ||
            onShareXopp != null || onRename != null || onDuplicate != null || onDelete != null

    val baseModifier = modifier
        .shadow(elevation = 4.dp, shape = shape)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .border(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else folderAccentColor.copy(alpha = 0.25f),
            shape = shape
        )

    val interactiveModifier = if (enableFloatingPreview && !isSelectionMode && !isTrashMode) {
        baseModifier.floatingPreviewLongPress(
            note = note,
            thumbnailFile = thumbnailFile,
            folderColor = folderAccentColor,
            initialCornerRadiusDp = initialCornerRadiusDp,
            onClick = onClick,
            onLongPressFallback = onLongClick
        )
    } else {
        baseModifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    }

    Box(modifier = interactiveModifier) {
        // 1. Thumbnail Image or Placeholder
        if (thumbnailImage != null) {
            Image(
                bitmap = thumbnailImage!!,
                contentDescription = note.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(folderAccentColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (note.fileType) {
                        NoteFileType.PDF -> Icons.Default.PictureAsPdf
                        NoteFileType.XOJ -> Icons.Default.History
                        else -> Icons.Default.Description
                    },
                    contentDescription = null,
                    tint = folderAccentColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Selection overlay tint if selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            )
        }

        // 2. Top-Left: Standardized File Type Pill
        FileTypePill(
            fileType = note.fileType,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        )

        // 3. Top-Right: Leveled Action Bar (Pinned indicator, Selection badge, 3-dot options menu)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pinned indicator badge (leveled to the left of the 3-dot button)
            if (note.isPinned && !isSelectionMode) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 3.dp,
                    modifier = Modifier.size(28.dp)
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

            // Selection Checkbox Badge
            if (isSelectionMode) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(28.dp)
                        .border(
                            width = if (isSelected) 0.dp else 1.5.dp,
                            color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                }
            }

            // 3-dot Options Menu Button
            if (hasActions && !isSelectionMode && !isTrashMode) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    StandardNoteActionDropdown(
                        expanded = showMenu,
                        isPinned = note.isPinned,
                        onDismiss = { showMenu = false },
                        onTogglePin = onTogglePin,
                        onExportPdf = onExportPdf,
                        onSharePdf = onSharePdf,
                        onShareXopp = onShareXopp,
                        onRename = onRename,
                        onDuplicate = onDuplicate,
                        onDelete = onDelete
                    )
                }
            } else if (isTrashMode && onRestore != null) {
                IconButton(
                    onClick = onRestore,
                    modifier = Modifier.size(28.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = "Restore",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Bottom: Floating Folder-Palette Details Pill
        FloatingDetailsPill(
            note = note,
            folderColor = folderAccentColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp)
        )
    }
}

/**
 * Floating Details Pill using a palette variant of the folder color with color-coded details.
 */
@Composable
fun FloatingDetailsPill(
    note: NoteDocument,
    folderColor: Color,
    modifier: Modifier = Modifier
) {
    val tintedBgColor = folderColor.copy(alpha = 0.22f)
        .compositeOver(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f))

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = tintedBgColor,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, folderColor.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Title
            Text(
                text = note.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // Bottom Row: Folder & Timestamp
            val folderDisplayName = if (note.folder.isBlank()) "Notes Home" else note.folder
            val isHome = note.folder.isBlank() || note.folder == "Notes Home"
            val isEmergency = note.folderIconType == "emergency" || note.folder.equals("Emergency Saves", ignoreCase = true)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier.size(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!note.folderIconEmoji.isNullOrBlank()) {
                        Text(
                            text = note.folderIconEmoji,
                            fontSize = 10.sp,
                            lineHeight = 10.sp
                        )
                    } else {
                        Icon(
                            imageVector = when {
                                isHome -> Icons.Default.Home
                                isEmergency -> Icons.Default.Emergency
                                else -> Icons.Default.Folder
                            },
                            contentDescription = null,
                            tint = folderColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Text(
                    text = folderDisplayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = folderColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Text(
                    text = note.fuzzyLastModified,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Standard Context Actions Dropdown Menu for Notes.
 */
@Composable
fun StandardNoteActionDropdown(
    expanded: Boolean,
    isPinned: Boolean = false,
    onDismiss: () -> Unit,
    onTogglePin: (() -> Unit)? = null,
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
        }
        if (onExportPdf != null) {
            DropdownMenuItem(
                text = { Text("Export to PDF") },
                leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                onClick = { onDismiss(); onExportPdf() }
            )
        }
        if (onSharePdf != null) {
            DropdownMenuItem(
                text = { Text("Share as PDF") },
                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                onClick = { onDismiss(); onSharePdf() }
            )
        }
        if (onShareXopp != null) {
            DropdownMenuItem(
                text = { Text("Share as Note (.xopp)") },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                onClick = { onDismiss(); onShareXopp() }
            )
        }
        if (onRename != null) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                onClick = { onDismiss(); onRename() }
            )
        }
        if (onDuplicate != null) {
            DropdownMenuItem(
                text = { Text("Duplicate") },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                onClick = { onDismiss(); onDuplicate() }
            )
        }
        if (onDelete != null) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = { onDismiss(); onDelete() }
            )
        }
    }
}
