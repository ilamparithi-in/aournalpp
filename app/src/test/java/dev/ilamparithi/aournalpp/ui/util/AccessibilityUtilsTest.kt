package dev.ilamparithi.aournalpp.ui.util

import dev.ilamparithi.aournalpp.model.NoteFileType
import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityUtilsTest {

    @Test
    fun testBuildNoteCardA11yDescription_standard() {
        val desc = AccessibilityUtils.buildNoteCardA11yDescription(
            title = "Physics 101",
            fileType = NoteFileType.XOPP,
            folderName = "University",
            lastModified = "Aug 30, 2026",
            isPinned = false,
            isSelected = null
        )
        assertEquals("Physics 101, Xournal++ note, in University, modified Aug 30, 2026", desc)
    }

    @Test
    fun testBuildNoteCardA11yDescription_pinnedAndSelected() {
        val desc = AccessibilityUtils.buildNoteCardA11yDescription(
            title = "Research Paper",
            fileType = NoteFileType.PDF,
            folderName = "Notes Home",
            lastModified = "10:30 AM",
            isPinned = true,
            isSelected = true
        )
        assertEquals("Selected, Pinned, Research Paper, PDF document, in Notes Home, modified 10:30 AM", desc)
    }

    @Test
    fun testBuildNoteCardA11yDescription_legacyXoj() {
        val desc = AccessibilityUtils.buildNoteCardA11yDescription(
            title = "Old Notes",
            fileType = NoteFileType.XOJ,
            folderName = "",
            lastModified = "",
            isPinned = false,
            isSelected = false
        )
        assertEquals("Old Notes, Legacy XOJ note, in Notes Home", desc)
    }

    @Test
    fun testBuildFolderCardA11yDescription_standard() {
        val desc = AccessibilityUtils.buildFolderCardA11yDescription(
            folderName = "Sketches",
            noteCount = 5,
            isPinned = false,
            isExcludedFromRecents = false,
            role = null
        )
        assertEquals("Sketches, Folder, 5 notes", desc)
    }

    @Test
    fun testBuildFolderCardA11yDescription_singleNote() {
        val desc = AccessibilityUtils.buildFolderCardA11yDescription(
            folderName = "Drafts",
            noteCount = 1,
            isPinned = false,
            isExcludedFromRecents = false,
            role = null
        )
        assertEquals("Drafts, Folder, 1 note", desc)
    }

    @Test
    fun testBuildFolderCardA11yDescription_specialRolesAndFlags() {
        val desc = AccessibilityUtils.buildFolderCardA11yDescription(
            folderName = "Autosaves",
            noteCount = 2,
            isPinned = true,
            isExcludedFromRecents = true,
            role = "emergency"
        )
        assertEquals("Pinned, Excluded from recents, Autosaves, Emergency saves folder, 2 notes", desc)
    }

    @Test
    fun testBuildFolderCardA11yDescription_importAndAudioRoles() {
        val importDesc = AccessibilityUtils.buildFolderCardA11yDescription(
            folderName = "Imported",
            noteCount = 0,
            role = "import"
        )
        assertEquals("Imported, Imported folder, 0 notes", importDesc)

        val audioDesc = AccessibilityUtils.buildFolderCardA11yDescription(
            folderName = "Voice Memos",
            noteCount = 3,
            role = "audio"
        )
        assertEquals("Voice Memos, Audio folder, 3 notes", audioDesc)
    }

    @Test
    fun testBuildConflictVersionA11yDescription_primary() {
        val desc = AccessibilityUtils.buildConflictVersionA11yDescription(
            sourceName = "Google Drive",
            dateFormatted = "Aug 30, 2026, 4:15 PM",
            sizeFormatted = "1.2 MB",
            isPrimary = true,
            isAlongside = false
        )
        assertEquals("Google Drive (Primary copy), modified Aug 30, 2026, 4:15 PM, size 1.2 MB", desc)
    }

    @Test
    fun testBuildConflictVersionA11yDescription_alongside() {
        val desc = AccessibilityUtils.buildConflictVersionA11yDescription(
            sourceName = "Local Device",
            dateFormatted = "Aug 30, 2026, 3:00 PM",
            sizeFormatted = "950 KB",
            isPrimary = false,
            isAlongside = true
        )
        assertEquals("Local Device (Saved alongside), modified Aug 30, 2026, 3:00 PM, size 950 KB", desc)
    }

    @Test
    fun testMinimumTouchTargetIs48Dp() {
        assertEquals(48.0f, AccessibilityUtils.MinimumTouchTargetDp.value, 0.001f)
    }
}
