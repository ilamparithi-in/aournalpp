package dev.ilamparithi.aournalpp.ui.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard Material 3 Expressive icon badge container for settings items.
 * Enforces uniform size (40.dp) and rounded-square shape (RoundedCornerShape(12.dp))
 * across all settings sections.
 */
@Composable
fun SettingsIconBadge(
    icon: ImageVector? = null,
    imageVector: ImageVector? = null,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    badgeSize: Dp = 40.dp,
    iconSize: Dp = 22.dp
) {
    val vector = icon ?: imageVector ?: error("SettingsIconBadge requires either icon or imageVector")
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = modifier.size(badgeSize)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = vector,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Slot-based standard Material 3 Expressive icon badge container for settings items.
 */
@Composable
fun SettingsIconBadge(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    badgeSize: Dp = 40.dp,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = modifier.size(badgeSize)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
