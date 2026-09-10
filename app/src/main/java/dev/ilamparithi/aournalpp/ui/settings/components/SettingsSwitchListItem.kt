package dev.ilamparithi.aournalpp.ui.settings.components

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight

@Composable
fun SettingsSwitchListItem(
    headline: String,
    supporting: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        leadingContent = leadingContent,
        headlineContent = {
            Text(headline, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = supporting?.let {
            { Text(it, style = MaterialTheme.typography.bodySmall) }
        },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null
            )
        },
        modifier = modifier
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            ),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
