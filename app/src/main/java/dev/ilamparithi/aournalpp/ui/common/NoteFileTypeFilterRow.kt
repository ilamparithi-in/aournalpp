package dev.ilamparithi.aournalpp.ui.common

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.model.NoteFileType
import dev.ilamparithi.aournalpp.ui.settings.components.ConnectedButtonItem
import dev.ilamparithi.aournalpp.ui.settings.components.MultiSelectConnectedButtonGroup

/**
 * In-place file type filter row displaying the settings M3 Expressive Connected Button Group
 * for XOPP, XOJ, and PDF notes.
 * Enforces the invariant that at least 1 file type must remain enabled at all times.
 */
@Composable
fun NoteFileTypeFilterRow(
    enabledFileTypes: Set<NoteFileType>,
    onToggleFileType: (NoteFileType) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 32.dp
) {
    val items = remember {
        listOf(
            ConnectedButtonItem(value = NoteFileType.XOPP, label = NoteFileType.XOPP.displayName),
            ConnectedButtonItem(value = NoteFileType.XOJ, label = NoteFileType.XOJ.displayName),
            ConnectedButtonItem(value = NoteFileType.PDF, label = NoteFileType.PDF.displayName)
        )
    }

    MultiSelectConnectedButtonGroup(
        items = items,
        selectedItems = enabledFileTypes,
        onItemToggled = { type ->
            if (!enabledFileTypes.contains(type) || enabledFileTypes.size > 1) {
                onToggleFileType(type)
            }
        },
        modifier = modifier.widthIn(min = 180.dp, max = 220.dp),
        height = height,
        showCheckmark = true
    )
}
