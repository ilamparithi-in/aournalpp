package dev.ilamparithi.aournalpp.ui.settings.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight

/**
 * Standard Material 3 Expressive Accessible Switch List Item.
 * Conforms to M3 Switch Accessibility standards:
 * - Thumb contains an affirmative checkmark icon when selected for non-color-only state indication.
 * - Entire container is a unified accessible toggleable target.
 * - Minimum 48dp touch target and clear state description for assistive technologies.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsSwitchListItem(
    headline: String,
    supporting: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.ui.graphics.RectangleShape
) {
    ListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier.semantics {
            role = Role.Switch
            stateDescription = if (checked) "On" else "Off"
        },
        leadingContent = leadingContent,
        supportingContent = supporting?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null,
                thumbContent = if (checked) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                } else null
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        shapes = ListItemDefaults.shapes(
            shape = shape,
            selectedShape = shape,
            pressedShape = shape,
            focusedShape = shape,
            hoveredShape = shape,
            draggedShape = shape
        )
    ) {
        Text(
            text = headline,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
