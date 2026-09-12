package dev.ilamparithi.aournalpp.ui.hub

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.R

/**
 * Pinned indicator badge for note and folder cards.
 */
@Composable
fun PinnedBadge(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    iconSize: Dp = 15.dp,
    contentDescription: String = stringResource(R.string.action_pin_note)
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 3.dp,
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Selection checkbox badge indicator for multi-selection mode on note cards.
 */
@Composable
fun SelectionCheckboxBadge(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    onClick: (() -> Unit)? = null,
    contentDescription: String = stringResource(R.string.action_confirm)
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Surface(
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        modifier = clickableModifier
            .size(size)
            .border(
                width = if (isSelected) 0.dp else 1.5.dp,
                color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
    ) {
        if (isSelected) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier.padding((size * 0.18f).coerceAtLeast(3.dp))
                )
            }
        }
    }
}
