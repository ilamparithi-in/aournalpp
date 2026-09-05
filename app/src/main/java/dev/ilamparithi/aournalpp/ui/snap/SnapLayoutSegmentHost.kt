package dev.ilamparithi.aournalpp.ui.snap

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
            alpha = 0.5f,
            strength = 0.45f
        )

        val targetConfiguringSlot = activeConfiguringSlot ?: geometries.indices.firstOrNull { assignedSlotMap[it] == null } ?: 0

        for (geo in geometries) {
            val assignedWinId = assignedSlotMap[geo.slotIndex]
            val assignedWin = openWindows.find { it.id == assignedWinId }
            val isCurrentConfiguring = geo.slotIndex == targetConfiguringSlot

            val slotWidthDp = with(density) { geo.width.toDp() }
            val slotHeightDp = with(density) { geo.height.toDp() }

            val availableWindows = remember(openWindows, assignedSlotMap, geo.slotIndex) {
                openWindows.filter { win ->
                    assignedSlotMap.entries.none { it.key != geo.slotIndex && it.value == win.id }
                }
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(geo.x, geo.y) }
                    .size(slotWidthDp, slotHeightDp)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .border(
                        width = if (isCurrentConfiguring) 2.dp else 1.dp,
                        color = if (isCurrentConfiguring) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        onSlotClicked(geo.slotIndex)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentConfiguring) {
                    WindowGalleryPicker(
                        windows = if (availableWindows.isNotEmpty()) availableWindows else openWindows,
                        previewCache = previewCache,
                        isCompact = true,
                        headerTitle = "Select Note for Slot ${geo.slotIndex + 1}",
                        onSelectWindow = { win ->
                            onSelectWindowForSlot(geo.slotIndex, win)
                        },
                        onCloseWindow = null
                    )
                } else if (assignedWin != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "Slot ${geo.slotIndex + 1}: ${assignedWin.title}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        Text(
                            text = "Tap to change note",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Slot ${geo.slotIndex + 1} (Waiting)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
