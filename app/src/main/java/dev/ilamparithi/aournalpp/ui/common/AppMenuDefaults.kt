package dev.ilamparithi.aournalpp.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.PopupProperties

/**
 * Material 3 Expressive Menu Defaults & Components.
 * Standardizes rounded shapes (16dp container, 10dp items), surfaceContainer colors,
 * subtle border outline, and inset dividers across all dropdowns and menus in the app.
 */
object AppMenuDefaults {
    val MenuContainerShape = RoundedCornerShape(16.dp)
    val MenuItemShape = RoundedCornerShape(10.dp)
    val AnchorFieldShape = RoundedCornerShape(12.dp)
    val anchorFieldShape: Shape get() = AnchorFieldShape

    val ShadowElevation: Dp = 3.dp
    val TonalElevation: Dp = 2.dp

    val ItemHorizontalPadding = 6.dp
    val ItemVerticalPadding = 2.dp

    @Composable
    fun containerColor(): Color = MaterialTheme.colorScheme.surfaceContainer

    @Composable
    fun border(): BorderStroke = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )

    @Composable
    fun InsetDivider(modifier: Modifier = Modifier) {
        HorizontalDivider(
            modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

/**
 * Standard Material 3 Expressive DropdownMenu.
 */
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 4.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    shape: Shape = AppMenuDefaults.MenuContainerShape,
    containerColor: Color = AppMenuDefaults.containerColor(),
    tonalElevation: Dp = AppMenuDefaults.TonalElevation,
    shadowElevation: Dp = AppMenuDefaults.ShadowElevation,
    border: BorderStroke? = AppMenuDefaults.border(),
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        shape = shape,
        containerColor = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
        content = content
    )
}

/**
 * Standard Material 3 Expressive ExposedDropdownMenu (for form text fields).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuBoxScope.AppExposedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    matchAnchorWidth: Boolean = true,
    shape: Shape = AppMenuDefaults.MenuContainerShape,
    containerColor: Color = AppMenuDefaults.containerColor(),
    tonalElevation: Dp = AppMenuDefaults.TonalElevation,
    shadowElevation: Dp = AppMenuDefaults.ShadowElevation,
    border: BorderStroke? = AppMenuDefaults.border(),
    content: @Composable ColumnScope.() -> Unit
) {
    ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        scrollState = scrollState,
        matchAnchorWidth = matchAnchorWidth,
        shape = shape,
        containerColor = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
        content = content
    )
}

/**
 * Material 3 Expressive DropdownMenuItem with rounded shape, selection state, and proper margins.
 * Follows M3 Expressive (November 2025) vertical menu specifications:
 * - Selected items stand out with a vibrant container background (primaryContainer).
 * - Automatic checkmark indicator (Icons.Default.Check) for selected items.
 * - Rounded pill shape (10.dp) with vertical and horizontal internal spacing.
 */
@Composable
fun AppDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors? = null,
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding
) {
    val effectiveColors = colors ?: if (selected) {
        MenuDefaults.itemColors(
            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
            leadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            trailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    } else {
        MenuDefaults.itemColors(
            textColor = MaterialTheme.colorScheme.onSurface,
            leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    val effectiveLeadingIcon: (@Composable () -> Unit)? = when {
        selected && leadingIcon == null -> {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        else -> leadingIcon
    }

    val effectiveTrailingIcon: (@Composable () -> Unit)? = when {
        selected && leadingIcon != null && trailingIcon == null -> {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        else -> trailingIcon
    }

    val itemShape = AppMenuDefaults.MenuItemShape
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    DropdownMenuItem(
        text = {
            ProvideTextStyle(
                value = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                ),
                content = text
            )
        },
        onClick = onClick,
        modifier = modifier
            .padding(
                horizontal = AppMenuDefaults.ItemHorizontalPadding,
                vertical = AppMenuDefaults.ItemVerticalPadding
            )
            .clip(itemShape)
            .background(backgroundColor, itemShape),
        leadingIcon = effectiveLeadingIcon,
        trailingIcon = effectiveTrailingIcon,
        enabled = enabled,
        colors = effectiveColors,
        contentPadding = contentPadding
    )
}
