package dev.ilamparithi.aournalpp.ui.snap

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Composable
fun SnapLayoutToolbarButton(
    activeMode: SnapLayoutMode,
    isMirrored: Boolean,
    openWindowCount: Int,
    onSelectMode: (SnapLayoutMode, Boolean) -> Unit,
    onToggleMirror: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (activeMode != SnapLayoutMode.SINGLE && activeMode != SnapLayoutMode.UNLOCKED)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            else
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = true }
                )
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                when (activeMode) {
                    SnapLayoutMode.SPLIT_TWO -> TwoSplitIcon(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    SnapLayoutMode.SPLIT_THREE -> ThreeSplitIcon(
                        isMirrored = isMirrored,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    SnapLayoutMode.GRID_FOUR -> GridFourIcon(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    SnapLayoutMode.UNLOCKED -> Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Snap Layouts",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    else -> Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Snap Layouts",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        SnapLayoutDropdownMenu(
            expanded = expanded,
            openWindowCount = openWindowCount,
            currentMode = activeMode,
            isMirrored = isMirrored,
            onDismissRequest = { expanded = false },
            onSelectMode = { mode, mirrored ->
                expanded = false
                onSelectMode(mode, mirrored)
            },
            onToggleMirror = onToggleMirror
        )
    }
}

@Composable
fun SnapLayoutDropdownMenu(
    expanded: Boolean,
    openWindowCount: Int,
    currentMode: SnapLayoutMode,
    isMirrored: Boolean,
    onDismissRequest: () -> Unit,
    onSelectMode: (SnapLayoutMode, Boolean) -> Unit,
    onToggleMirror: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(0.dp, 8.dp),
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .width(260.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Snap Layouts",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // 2-Split Option (Shown only if openWindowCount >= 2)
            if (openWindowCount >= 2) {
                SnapLayoutMenuItem(
                    title = "Two Windows",
                    subtitle = "Side-by-side or stacked",
                    isSelected = currentMode == SnapLayoutMode.SPLIT_TWO,
                    icon = {
                        TwoSplitIcon(
                            color = if (currentMode == SnapLayoutMode.SPLIT_TWO)
                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        onSelectMode(SnapLayoutMode.SPLIT_TWO, isMirrored)
                        onDismissRequest()
                    }
                )
            }

            // 3-Split Option (Shown only if openWindowCount >= 3)
            if (openWindowCount >= 3) {
                SnapLayoutMenuItem(
                    title = "Three Windows",
                    subtitle = if (isMirrored) "1 right, 2 left" else "1 left, 2 right",
                    isSelected = currentMode == SnapLayoutMode.SPLIT_THREE,
                    trailingAction = {
                        IconButton(
                            onClick = onToggleMirror,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flip,
                                contentDescription = "Flip layout",
                                modifier = Modifier.size(16.dp),
                                tint = if (isMirrored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    icon = {
                        ThreeSplitIcon(
                            isMirrored = isMirrored,
                            color = if (currentMode == SnapLayoutMode.SPLIT_THREE)
                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        onSelectMode(SnapLayoutMode.SPLIT_THREE, isMirrored)
                        onDismissRequest()
                    }
                )
            }

            // 4-Split Grid Option (Shown only if openWindowCount >= 4)
            if (openWindowCount >= 4) {
                SnapLayoutMenuItem(
                    title = "Four Windows",
                    subtitle = "2x2 grid",
                    isSelected = currentMode == SnapLayoutMode.GRID_FOUR,
                    icon = {
                        GridFourIcon(
                            color = if (currentMode == SnapLayoutMode.GRID_FOUR)
                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        onSelectMode(SnapLayoutMode.GRID_FOUR, false)
                        onDismissRequest()
                    }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Unlock (Desktop Mode) Option
            SnapLayoutMenuItem(
                title = "Unlock Windows",
                subtitle = "Free-floating desktop mode",
                isSelected = currentMode == SnapLayoutMode.UNLOCKED,
                icon = {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (currentMode == SnapLayoutMode.UNLOCKED)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = {
                    onSelectMode(SnapLayoutMode.UNLOCKED, false)
                    onDismissRequest()
                }
            )

            // Single / Maximize Option
            SnapLayoutMenuItem(
                title = "Single Note",
                subtitle = "Standard maximized window",
                isSelected = currentMode == SnapLayoutMode.SINGLE,
                icon = {
                    Icon(
                        imageVector = Icons.Default.CropSquare,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (currentMode == SnapLayoutMode.SINGLE)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = {
                    onSelectMode(SnapLayoutMode.SINGLE, false)
                    onDismissRequest()
                }
            )
        }
    }
}

@Composable
private fun SnapLayoutMenuItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    icon: @Composable () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            if (trailingAction != null) {
                trailingAction()
            }
        }
    }
}

@Composable
fun TwoSplitIcon(color: Color, modifier: Modifier = Modifier.size(22.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val gap = 2f
        val stroke = 1.8f
        val halfW = (w - gap) / 2f

        drawRoundRect(
            color = color,
            topLeft = Offset(0f, 0f),
            size = Size(halfW, h),
            cornerRadius = CornerRadius(3f, 3f),
            style = Stroke(width = stroke)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(halfW + gap, 0f),
            size = Size(halfW, h),
            cornerRadius = CornerRadius(3f, 3f),
            style = Stroke(width = stroke)
        )
    }
}

@Composable
fun ThreeSplitIcon(isMirrored: Boolean, color: Color, modifier: Modifier = Modifier.size(22.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val gap = 2f
        val stroke = 1.8f
        val halfW = (w - gap) / 2f
        val halfH = (h - gap) / 2f

        if (!isMirrored) {
            // Main left, two right
            drawRoundRect(
                color = color,
                topLeft = Offset(0f, 0f),
                size = Size(halfW, h),
                cornerRadius = CornerRadius(3f, 3f),
                style = Stroke(width = stroke)
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(halfW + gap, 0f),
                size = Size(halfW, halfH),
                cornerRadius = CornerRadius(3f, 3f),
                style = Stroke(width = stroke)
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(halfW + gap, halfH + gap),
                size = Size(halfW, halfH),
                cornerRadius = CornerRadius(3f, 3f),
                style = Stroke(width = stroke)
            )
        } else {
            // Two left, main right
            drawRoundRect(
                color = color,
                topLeft = Offset(0f, 0f),
                size = Size(halfW, halfH),
                cornerRadius = CornerRadius(3f, 3f),
                style = Stroke(width = stroke)
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(0f, halfH + gap),
                size = Size(halfW, halfH),
                cornerRadius = CornerRadius(3f, 3f),
                style = Stroke(width = stroke)
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(halfW + gap, 0f),
                size = Size(halfW, h),
                cornerRadius = CornerRadius(3f, 3f),
                style = Stroke(width = stroke)
            )
        }
    }
}

@Composable
fun GridFourIcon(color: Color, modifier: Modifier = Modifier.size(22.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val gap = 2f
        val stroke = 1.8f
        val halfW = (w - gap) / 2f
        val halfH = (h - gap) / 2f

        drawRoundRect(
            color = color,
            topLeft = Offset(0f, 0f),
            size = Size(halfW, halfH),
            cornerRadius = CornerRadius(3f, 3f),
            style = Stroke(width = stroke)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(halfW + gap, 0f),
            size = Size(halfW, halfH),
            cornerRadius = CornerRadius(3f, 3f),
            style = Stroke(width = stroke)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, halfH + gap),
            size = Size(halfW, halfH),
            cornerRadius = CornerRadius(3f, 3f),
            style = Stroke(width = stroke)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(halfW + gap, halfH + gap),
            size = Size(halfW, halfH),
            cornerRadius = CornerRadius(3f, 3f),
            style = Stroke(width = stroke)
        )
    }
}
