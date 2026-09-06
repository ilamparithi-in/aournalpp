package dev.ilamparithi.aournalpp.ui.snap

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.ui.preview.DreamyStarsBackground
import dev.ilamparithi.aournalpp.ui.window.WindowGalleryPicker

@Composable
fun SnapLayoutSegmentHost(
    geometries: List<WindowSlotGeometry>,
    openWindows: List<ProcessSupervisor.X11WindowInfo>,
    previewCache: Map<String, Bitmap>,
    assignedSlotMap: Map<Int, String>,
    activeConfiguringSlot: Int?,
    onSelectWindowForSlot: (slotIndex: Int, window: ProcessSupervisor.X11WindowInfo) -> Unit,
    onSlotClicked: (slotIndex: Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (geometries.isEmpty() || openWindows.isEmpty()) return
    val density = LocalDensity.current

    var editingSlotIndex by remember { mutableStateOf<Int?>(null) }

    // All windows currently assigned to any slot
    val allAssignedWindowIds = remember(assignedSlotMap) {
        assignedSlotMap.values.filter { it.isNotBlank() }.toSet()
    }

    // Shared gallery of unassigned windows: open windows minus all currently selected windows
    val galleryWindows = remember(openWindows, allAssignedWindowIds) {
        openWindows.filter { it.id !in allAssignedWindowIds }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(150f)
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        DreamyStarsBackground(
            modifier = Modifier.fillMaxSize(),
            accentColor = MaterialTheme.colorScheme.primary,
            progress = 1f,
            alpha = 0.35f,
            strength = 0.35f
        )

        for (geo in geometries) {
            val assignedWinId = assignedSlotMap[geo.slotIndex]
            val assignedWin = openWindows.find { it.id == assignedWinId }
            val isEditingThisSlot = editingSlotIndex == geo.slotIndex

            val slotWidthDp = with(density) { geo.width.toDp() }
            val slotHeightDp = with(density) { geo.height.toDp() }

            Box(
                modifier = Modifier
                    .offset { IntOffset(geo.x, geo.y) }
                    .size(slotWidthDp, slotHeightDp)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .border(
                        width = if (assignedWin != null && !isEditingThisSlot) 2.dp else 1.dp,
                        color = if (assignedWin != null && !isEditingThisSlot) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (assignedWin != null && !isEditingThisSlot) {
                            editingSlotIndex = geo.slotIndex
                            onSlotClicked(geo.slotIndex)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Muted dreamy stars background animation inside each slot while configuring
                DreamyStarsBackground(
                    modifier = Modifier.fillMaxSize(),
                    accentColor = MaterialTheme.colorScheme.primary,
                    progress = 1f,
                    alpha = 0.65f,
                    strength = 0.65f
                )

                if (assignedWin != null && !isEditingThisSlot) {
                    // Display assigned note card with preview thumbnail, slot badge, and tap-to-change action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                            tonalElevation = 3.dp,
                            shadowElevation = 4.dp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Slot ${geo.slotIndex + 1}: ${assignedWin.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        val previewBmp = previewCache[assignedWin.id]
                        val previewCardW = (slotWidthDp * 0.65f).coerceIn(140.dp, 260.dp)
                        val previewCardH = (slotHeightDp * 0.45f).coerceIn(80.dp, 160.dp)

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                            modifier = Modifier
                                .size(previewCardW, previewCardH)
                                .clip(RoundedCornerShape(14.dp))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (previewBmp != null && !previewBmp.isRecycled) {
                                    Image(
                                        bitmap = previewBmp.asImageBitmap(),
                                        contentDescription = assignedWin.title,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = assignedWin.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.clickable {
                                editingSlotIndex = geo.slotIndex
                                onSlotClicked(geo.slotIndex)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Tap to change note",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Show unassigned gallery (or re-selecting gallery if editing this slot)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isEditingThisSlot && assignedWin != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .padding(top = 8.dp, bottom = 4.dp)
                                    .clickable { editingSlotIndex = null }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Keep: ${assignedWin.title}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (galleryWindows.isNotEmpty()) {
                            WindowGalleryPicker(
                                windows = galleryWindows,
                                previewCache = previewCache,
                                isCompact = true,
                                isVerticalGrid = false,
                                headerTitle = if (isEditingThisSlot) "Change Note for Slot ${geo.slotIndex + 1}"
                                else "Select Note for Slot ${geo.slotIndex + 1}",
                                onSelectWindow = { win ->
                                    editingSlotIndex = null
                                    onSelectWindowForSlot(geo.slotIndex, win)
                                },
                                onCloseWindow = null
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isEditingThisSlot) "No other open notes available"
                                    else "No open notes available for Slot ${geo.slotIndex + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
