package dev.ilamparithi.aournalpp.data

import android.content.Context
import dev.ilamparithi.aournalpp.testutils.TestSharedPreferences
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NoteHistoryTrackerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var notesDir: File
    private lateinit var prefs: TestSharedPreferences
    private lateinit var cache: DocumentCache
    private lateinit var tracker: NoteHistoryTracker
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Before
    fun setUp() {
        notesDir = tempFolder.newFolder("notes")
        prefs = TestSharedPreferences()
        cache = DocumentCache()
        val mockContext = mockk<Context>(relaxed = true)

        tracker = NoteHistoryTracker(
            context = mockContext,
            prefs = prefs,
            cache = cache,
            scope = scope,
            isExcludedFromRecents = { it.path.contains("Excluded") },
            isWithinRootDirectory = { it.absolutePath.startsWith(notesDir.absolutePath) }
        )
    }

    @Test
    fun `test recordNoteOpened adds to head and tracks timestamp`() {
        val note1 = File(notesDir, "note1.xopp").apply { writeText("1") }.absolutePath
        val note2 = File(notesDir, "note2.xopp").apply { writeText("2") }.absolutePath

        tracker.recordNoteOpened(note1)
        tracker.recordNoteOpened(note2)

        val paths = tracker.getRecentlyOpenedPaths(strictlyWithinRoot = true)
        assertEquals(2, paths.size)
        assertEquals(note2, paths[0])
        assertEquals(note1, paths[1])

        val timestamps = tracker.getOpenedNotesTimestamps()
        assertTrue(timestamps.containsKey(note1))
        assertTrue(timestamps.containsKey(note2))
        assertTrue(timestamps[note2]!! >= timestamps[note1]!!)
    }

    @Test
    fun `test recordNoteOpened moves existing note to head without duplicates`() {
        val note1 = File(notesDir, "note1.xopp").apply { writeText("1") }.absolutePath
        val note2 = File(notesDir, "note2.xopp").apply { writeText("2") }.absolutePath

        tracker.recordNoteOpened(note1)
        tracker.recordNoteOpened(note2)
        // Re-open note1
        tracker.recordNoteOpened(note1)

        val paths = tracker.getRecentlyOpenedPaths(strictlyWithinRoot = true)
        assertEquals(2, paths.size)
        assertEquals(note1, paths[0])
        assertEquals(note2, paths[1])
    }

    @Test
    fun `test recordNoteOpened caps history to 50 items`() {
        for (i in 1..60) {
            val note = File(notesDir, "note_$i.xopp").apply { writeText("$i") }.absolutePath
            tracker.recordNoteOpened(note)
        }

        val paths = tracker.getRecentlyOpenedPaths(strictlyWithinRoot = true)
        assertEquals(50, paths.size)
        assertEquals(File(notesDir, "note_60.xopp").absolutePath, paths[0])
    }

    @Test
    fun `test updateOpenedNotePath updates path on file rename`() {
        val oldPath = File(notesDir, "old_name.xopp").apply { writeText("data") }.absolutePath
        val newPath = File(notesDir, "new_name.xopp").apply { writeText("data") }.absolutePath

        tracker.recordNoteOpened(oldPath)
        assertTrue(tracker.getRecentlyOpenedPaths().contains(oldPath))

        tracker.updateOpenedNotePath(oldPath, newPath)
        val paths = tracker.getRecentlyOpenedPaths()
        assertFalse(paths.contains(oldPath))
        assertTrue(paths.contains(newPath))
        assertEquals(newPath, paths[0])
    }

    @Test
    fun `test removeOpenedNoteHistory deletes note from history`() {
        val note1 = File(notesDir, "note1.xopp").apply { writeText("1") }.absolutePath
        val note2 = File(notesDir, "note2.xopp").apply { writeText("2") }.absolutePath

        tracker.recordNoteOpened(note1)
        tracker.recordNoteOpened(note2)

        tracker.removeOpenedNoteHistory(note1)
        val paths = tracker.getRecentlyOpenedPaths()
        assertEquals(1, paths.size)
        assertEquals(note2, paths[0])
        assertFalse(paths.contains(note1))
    }

    @Test
    fun `test ignored paths are not recorded in recents`() {
        // Blank path
        tracker.recordNoteOpened("")
        // Staged imports
        tracker.recordNoteOpened("/storage/emulated/0/staged_imports/test.xopp")
        // Cache directory
        tracker.recordNoteOpened("/data/data/dev.ilamparithi.aournalpp/cache/temp.xopp")
        // Trash directory
        tracker.recordNoteOpened("${notesDir.absolutePath}/.Trash/deleted.xopp")
        // Excluded folder
        tracker.recordNoteOpened("${notesDir.absolutePath}/Excluded_Folder/note.xopp")

        val paths = tracker.getRecentlyOpenedPaths(strictlyWithinRoot = false)
        assertTrue(paths.isEmpty())
    }
}
