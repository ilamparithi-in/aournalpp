package dev.ilamparithi.aournalpp.ui.canvas

import androidx.compose.ui.input.pointer.PointerType
import java.io.File

/**
 * Pure utility and policy algorithms for the floating canvas toolbar.
 *
 * Extracted according to ADR 0018 (Pure Extraction Pattern) for deterministic
 * host JVM unit testing without Android platform mocks.
 */
object FloatingToolbarUtils {
    /**
     * Cooldown in milliseconds after an intentional toolbar collapse, preventing
     * immediate re-expansion while the stylus or cursor is lifting away.
     */
    const val HOVER_EXPAND_COOLDOWN_MS = 600L

    /**
     * Determines whether the collapsed toolbar can expand upon pointer hover,
     * enforcing a cooldown after an intentional collapse to prevent jarring
     * immediate re-expansion when lifting the stylus/cursor.
     */
    fun canExpandOnHover(currentTime: Long, lastCollapseTime: Long): Boolean {
        return (currentTime - lastCollapseTime) > HOVER_EXPAND_COOLDOWN_MS
    }

    /**
     * Determines if a pointer type is capable of hovering (Stylus, Eraser, or Mouse).
     * Touch pointers do not hover.
     */
    fun isHoverPointerType(pointerType: PointerType): Boolean {
        return pointerType == PointerType.Stylus ||
                pointerType == PointerType.Eraser ||
                pointerType == PointerType.Mouse
    }

    /**
     * Determines whether the floating toolbar should auto-collapse due to inactivity.
     * When hovered by a stylus or cursor, auto-collapse is inhibited to keep the
     * toolbar expanded throughout the hover duration.
     */
    fun shouldAutoCollapse(
        pinButtonMode: Boolean,
        isHeaderExpanded: Boolean,
        isPinned: Boolean,
        isSnapDropdownOpen: Boolean,
        isHovered: Boolean
    ): Boolean {
        return pinButtonMode && isHeaderExpanded && !isPinned && !isSnapDropdownOpen && !isHovered
    }

    /**
     * Pure evaluator for determining the destination folder when creating a new note window.
     * Preserves the active note's folder when contained within the root notes directory.
     * Falls back to rootNotesDir when the current note is null or outside the sandbox.
     */
    fun resolveTargetFolderForNewNote(currentNoteFile: File?, rootNotesDir: File): File {
        if (currentNoteFile == null) return rootNotesDir
        val parent = currentNoteFile.parentFile ?: return rootNotesDir
        return try {
            val rootCanonical = rootNotesDir.canonicalPath
            val parentCanonical = parent.canonicalPath
            if (parentCanonical.startsWith(rootCanonical)) {
                parent
            } else {
                rootNotesDir
            }
        } catch (_: Exception) {
            if (parent.absolutePath.startsWith(rootNotesDir.absolutePath)) {
                parent
            } else {
                rootNotesDir
            }
        }
    }

    /**
     * Pure evaluator for determining whether the Open New Window toolbar button should be visible.
     */
    fun isNewWindowButtonVisible(configEnabled: Boolean): Boolean {
        return configEnabled
    }

    /**
     * Guard evaluator preventing concurrent new window spawns or spawning while a modal dialog is open.
     */
    fun canTriggerNewWindow(isSpawning: Boolean, isModalOrDialogOpen: Boolean): Boolean {
        return !isSpawning && !isModalOrDialogOpen
    }
}
