package dev.ilamparithi.aournalpp.data

import dev.ilamparithi.aournalpp.model.NoteDocument
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TrashRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var notesDir: File
    private lateinit var repository: TrashRepository
    private var cacheInvalidated = false

    @Before
    fun setUp() {
        notesDir = tempFolder.newFolder("notes")
        cacheInvalidated = false
        repository = TrashRepository(
            notesDirectoryProvider = { notesDir },
            findAssociatedFiles = { emptyList() },
            onRemoveOpenedNoteHistory = {},
            onInvalidateCaches = { cacheInvalidated = true },
            isOpenableFile = { it.extension in setOf("xopp", "xoj", "pdf") }
        )
    }

    @Test
    fun testMoveToTrashAndRestore() = runBlocking {
        val noteFile = File(notesDir, "test_note.xopp").apply { writeText("sample content") }
        val noteDoc = NoteDocument(
            file = noteFile,
            title = "test_note.xopp",
            path = noteFile.absolutePath,
            lastModifiedMs = noteFile.lastModified(),
            sizeBytes = noteFile.length()
        )

        // Move to trash
        val result = repository.moveToTrash(listOf(noteDoc))
        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertEquals(1, receipt.movedCount)
        assertEquals(1, receipt.trashFileNames.size)
        assertTrue(cacheInvalidated)
        assertFalse(noteFile.exists())

        // Scan trash
        val trashedItems = repository.scanTrash()
        assertEquals(1, trashedItems.size)
        assertEquals("test_note.xopp", trashedItems[0].title)

        // Restore from trash
        cacheInvalidated = false
        val restoreResult = repository.restoreTrashItems(receipt.trashFileNames)
        assertTrue(restoreResult.isSuccess)
        assertEquals(1, restoreResult.getOrThrow())
        assertTrue(noteFile.exists())
        assertTrue(cacheInvalidated)

        // Trash is now empty
        assertEquals(0, repository.scanTrash().size)
    }

    @Test
    fun testEmptyTrash() = runBlocking {
        val noteFile = File(notesDir, "trash_me.xopp").apply { writeText("to be emptied") }
        val noteDoc = NoteDocument(
            file = noteFile,
            title = "trash_me.xopp",
            path = noteFile.absolutePath,
            lastModifiedMs = noteFile.lastModified(),
            sizeBytes = noteFile.length()
        )

        repository.moveToTrash(listOf(noteDoc))
        assertEquals(1, repository.scanTrash().size)

        val emptyResult = repository.emptyTrash()
        assertTrue(emptyResult.isSuccess)
        assertEquals(0, repository.scanTrash().size)
    }

    @Test
    fun testMoveToTrashAndRestorePreservesTimestamp() = runBlocking {
        val noteFile = File(notesDir, "timestamp_test.xopp").apply {
            writeText("mtime test content")
        }
        val targetMtime = 1680000000000L // arbitrary fixed past timestamp
        noteFile.setLastModified(targetMtime)
        assertEquals(targetMtime, noteFile.lastModified())

        val noteDoc = NoteDocument(
            file = noteFile,
            title = "timestamp_test.xopp",
            path = noteFile.absolutePath,
            lastModifiedMs = noteFile.lastModified(),
            sizeBytes = noteFile.length()
        )

        // Move to trash
        val result = repository.moveToTrash(listOf(noteDoc))
        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()

        // Restore from trash
        val restoreResult = repository.restoreTrashItems(receipt.trashFileNames)
        assertTrue(restoreResult.isSuccess)
        assertEquals(1, restoreResult.getOrThrow())
        assertTrue(noteFile.exists())
        assertEquals("File lastModified must be preserved across trash & restore", targetMtime, noteFile.lastModified())
    }

    @Test
    fun testLegacyManifestCompatibility() = runBlocking {
        val trashDir = File(notesDir, ".Trash").apply { mkdirs() }
        val trashedFile = File(trashDir, "legacy_note.xopp").apply {
            writeText("legacy content")
        }
        val targetMtime = 1670000000000L
        trashedFile.setLastModified(targetMtime)

        val restoredDest = File(notesDir, "legacy_note.xopp")
        // Write legacy manifest with raw string path
        val legacyJson = """{"legacy_note.xopp": "${restoredDest.absolutePath}"}"""
        File(trashDir, ".trash_manifest.json").writeText(legacyJson)

        val restoreResult = repository.restoreTrashItems(listOf("legacy_note.xopp"))
        assertTrue(restoreResult.isSuccess)
        assertEquals(1, restoreResult.getOrThrow())
        assertTrue(restoredDest.exists())
        assertEquals(targetMtime, restoredDest.lastModified())
    }
}
