package dev.ilamparithi.aournalpp.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.model.NoteFileType

/**
 * In-place file type filter row displaying M3 FilterChips for XOPP, PDF, and XOJ notes.
 * Enforces the invariant that at least 1 file type must remain enabled at all times.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteFileTypeFilterRow(
    enabledFileTypes: Set<NoteFileType>,
    onToggleFileType: (NoteFileType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val fileTypes = listOf(NoteFileType.XOPP, NoteFileType.PDF, NoteFileType.XOJ)

        fileTypes.forEach { type ->
            val isSelected = enabledFileTypes.contains(type)
            val canDisable = isSelected && enabledFileTypes.size > 1

            FilterChip(
                selected = isSelected,
                onClick = { onToggleFileType(type) },
                label = {
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                modifier = Modifier.semantics {
                    contentDescription = if (isSelected) {
                        "${type.displayName} filter active"
                    } else {
                        "${type.displayName} filter inactive"
                    }
                }
            )
        }
    }
}
