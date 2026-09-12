package dev.ilamparithi.aournalpp.ui

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.core.graphics.toColorInt
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
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
import dev.ilamparithi.aournalpp.ui.hub.NoteActionDropdown
import dev.ilamparithi.aournalpp.ui.hub.NoteCardCallbacks
import dev.ilamparithi.aournalpp.ui.hub.PinnedBadge
import dev.ilamparithi.aournalpp.ui.hub.SelectionCheckboxBadge
import dev.ilamparithi.aournalpp.ui.hub.rememberNoteAccessibilityActions
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.ui.preview.floatingPreviewLongPress
import dev.ilamparithi.aournalpp.utils.ThumbnailManager

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import dev.ilamparithi.aournalpp.utils.AccessibilityUtils
import dev.ilamparithi.aournalpp.utils.minTouchTarget
import dev.ilamparithi.aournalpp.ui.common.AppTooltipBox

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
    onShareExport: (() -> Unit)? = null,
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
    enableFloatingPreview: Boolean = true,
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
    val thumbnailFile = remember(thumbnailImage) { thumbnailManager.getCachedThumbnailFile(note.file, note.lastModifiedMs) }

    val folderAccentColor = remember(note.folderColorHex) {
        note.folderColorHex?.let {
            try { Color(it.toColorInt()) } catch (e: Exception) { null }
        }
    } ?: MaterialTheme.colorScheme.primary

    val hasActions = onTogglePin != null || onShareExport != null || onExportPdf != null || onSharePdf != null ||
            onShareXopp != null || onRename != null || onDuplicate != null || onDelete != null

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

    val a11yDescription = remember(note, isSelected, isSelectionMode) {
        AccessibilityUtils.buildNoteCardA11yDescription(
            title = note.title,
            fileType = note.fileType,
            folderName = note.folder,
            lastModified = note.fuzzyLastModified,
            isPinned = note.isPinned,
            isSelected = if (isSelectionMode) isSelected else null
        )
    }

    val baseModifier = modifier
        .shadow(elevation = 4.dp, shape = shape)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .border(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else folderAccentColor.copy(alpha = 0.25f),
            shape = shape
        )
        .semantics(mergeDescendants = true) {
            role = Role.Button
            this.contentDescription = a11yDescription
            customActions = customActionsList
        }

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
                contentDescription = null,
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
                PinnedBadge()
            }

            // Selection Checkbox Badge
            if (isSelectionMode) {
                SelectionCheckboxBadge(isSelected = isSelected)
            }

            // 3-dot Options Menu Button
            if (hasActions && !isSelectionMode && !isTrashMode) {
                Box {
                    val moreOptionsLabel = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_details)
                    AppTooltipBox(tooltipText = moreOptionsLabel) {
                        IconButton(
                            onClick = onOpenMenu,
                            modifier = Modifier.size(28.dp).minTouchTarget()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.45f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = moreOptionsLabel,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
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
                IconButton(
                    onClick = onRestore,
                    modifier = Modifier.size(28.dp).minTouchTarget()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.action_restore_autosave),
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
    externalTrigger: Any? = null,
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
            InteractiveMarqueeText(
                text = note.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                externalTrigger = externalTrigger,
                modifier = Modifier.fillMaxWidth()
            )

            // Bottom Row: Folder & Timestamp
            val defaultHomeFolder = androidx.compose.ui.res.stringResource(dev.ilamparithi.aournalpp.R.string.hub_root_folder_name)
            val folderDisplayName = if (note.folder.isBlank()) defaultHomeFolder else note.folder
            val isHome = note.folder.isBlank() || note.folder == "Notes Home" || note.folder == defaultHomeFolder
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

                InteractiveMarqueeText(
                    text = folderDisplayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = folderColor,
                    externalTrigger = externalTrigger,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                InteractiveMarqueeText(
                    text = note.fuzzyLastModified,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    externalTrigger = externalTrigger,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}


