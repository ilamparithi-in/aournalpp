package dev.ilamparithi.aournalpp.ui.util

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.ilamparithi.aournalpp.model.NoteFileType

/**
 * Common Accessibility (a11y) utilities and Compose modifier extensions for Aournal++.
 */
object AccessibilityUtils {

    /**
     * WCAG minimum recommended touch target dimension (48x48 dp).
     */
    val MinimumTouchTargetDp = 48.dp

    /**
     * Builds a cohesive, localized TalkBack screen reader description for composite note cards.
     */
    fun buildNoteCardA11yDescription(
        title: String,
        fileType: NoteFileType,
        folderName: String,
        lastModified: String,
        isPinned: Boolean = false,
        isSelected: Boolean? = null
    ): String {
        val typeLabel = when (fileType) {
            NoteFileType.PDF -> "PDF document"
            NoteFileType.XOJ -> "Legacy XOJ note"
            NoteFileType.XOPP -> "Xournal++ note"
        }
        val folderLabel = if (folderName.isBlank() || folderName == "Notes Home") "Notes Home" else folderName
        val parts = mutableListOf<String>()

        if (isSelected == true) {
            parts.add("Selected")
        }
        if (isPinned) {
            parts.add("Pinned")
        }
        parts.add(title)
        parts.add(typeLabel)
        parts.add("in $folderLabel")
        if (lastModified.isNotBlank()) {
            parts.add("modified $lastModified")
        }

        return parts.joinToString(", ")
    }

    /**
     * Builds a cohesive TalkBack description for folder cards.
     */
    fun buildFolderCardA11yDescription(
        folderName: String,
        noteCount: Int,
        isPinned: Boolean = false,
        isExcludedFromRecents: Boolean = false,
        role: String? = null
    ): String {
        val parts = mutableListOf<String>()
        if (isPinned) {
            parts.add("Pinned")
        }
        if (isExcludedFromRecents) {
            parts.add("Excluded from recents")
        }
        val typeLabel = when (role) {
            "emergency" -> "Emergency saves folder"
            "import", "imported" -> "Imported folder"
            "audio" -> "Audio folder"
            else -> "Folder"
        }
        parts.add("$folderName, $typeLabel")
        parts.add(if (noteCount == 1) "1 note" else "$noteCount notes")
        return parts.joinToString(", ")
    }

    /**
     * Builds a cohesive TalkBack description for conflict resolution version items.
     */
    fun buildConflictVersionA11yDescription(
        sourceName: String,
        dateFormatted: String,
        sizeFormatted: String,
        isPrimary: Boolean,
        isAlongside: Boolean
    ): String {
        val status = when {
            isPrimary -> "Primary copy"
            isAlongside -> "Saved alongside"
            else -> "Unselected"
        }
        return "$sourceName ($status), modified $dateFormatted, size $sizeFormatted"
    }
}

/**
 * Marks this composable as an accessibility section heading for TalkBack navigation.
 */
fun Modifier.a11yHeading(): Modifier = this.semantics { heading() }

/**
 * Ensures this interactive element meets or exceeds the WCAG 48x48 dp minimum touch target size.
 */
fun Modifier.minTouchTarget(): Modifier = this.sizeIn(
    minWidth = AccessibilityUtils.MinimumTouchTargetDp,
    minHeight = AccessibilityUtils.MinimumTouchTargetDp
)
