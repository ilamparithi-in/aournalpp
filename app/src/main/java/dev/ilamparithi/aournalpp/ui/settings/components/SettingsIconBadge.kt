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

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import dev.ilamparithi.aournalpp.ui.common.AccessibleColorUtils

/**
 * Standard Material 3 Expressive icon badge container for settings items.
 * Enforces uniform size (40.dp) and rounded-square shape (RoundedCornerShape(12.dp))
 * across all settings sections.
 *
 * Includes built-in contrast collision detection: if the passed [iconTint] has insufficient
 * contrast against [containerColor] (e.g., due to OEM dynamic color extraction or matching accents),
 * it automatically calculates and applies an accessible, high-contrast hue-preserving color.
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
    val surfaceColor = MaterialTheme.colorScheme.surface
    val resolvedIconTint = remember(iconTint, containerColor, surfaceColor) {
        AccessibleColorUtils.resolveAccessibleIconColor(
            iconTint = iconTint,
            containerColor = containerColor,
            fallbackSurface = surfaceColor
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = modifier.size(badgeSize)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = vector,
                contentDescription = contentDescription,
                tint = resolvedIconTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Slot-based standard Material 3 Expressive icon badge container for settings items.
 * Includes built-in contrast collision detection and provides an accessible [LocalContentColor]
 * to slot content.
 */
@Composable
fun SettingsIconBadge(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    badgeSize: Dp = 40.dp,
    content: @Composable () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val defaultTint = MaterialTheme.colorScheme.onPrimaryContainer
    val resolvedTint = remember(defaultTint, containerColor, surfaceColor) {
        AccessibleColorUtils.resolveAccessibleIconColor(
            iconTint = defaultTint,
            containerColor = containerColor,
            fallbackSurface = surfaceColor
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = modifier.size(badgeSize)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides resolvedTint) {
                content()
            }
        }
    }
}
