package dev.ilamparithi.aournalpp.data

import android.content.Context
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.testutils.TestSharedPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DocumentRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var notesDir: File
    private lateinit var repository: DocumentRepository
    private lateinit var mockContext: Context
    private lateinit var prefs: TestSharedPreferences

    @Before
    fun setUp() {
        notesDir = tempFolder.newFolder("notes")
        val filesDir = tempFolder.newFolder("files")

        prefs = TestSharedPreferences()
        prefs.putString(LinuxEnvironment.PREF_KEY_NOTES_DIR, notesDir.absolutePath)

        mockContext = mockk<Context>(relaxed = true)
        every { mockContext.getSharedPreferences(any(), any()) } returns prefs
        every { mockContext.filesDir } returns filesDir
        every { mockContext.applicationContext } returns mockContext

        DocumentRepository.invalidateAllCaches()
        repository = DocumentRepository(mockContext)
    }

    @Test
    fun `test createBlankNote creates valid xopp file and handles naming`() = runBlocking {
        val result = repository.createBlankNote("Lecture Notes", notesDir)
        assertTrue(result.isSuccess)

        val noteFile = result.getOrThrow()
        assertTrue(noteFile.exists())
        assertEquals("Lecture Notes.xopp", noteFile.name)
        assertTrue(noteFile.length() > 0)
    }

    @Test
    fun `test createBlankNote sanitizes invalid filename characters`() = runBlocking {
        val result = repository.createBlankNote("Math / Physics : Part 1?", notesDir)
        assertTrue(result.isSuccess)

        val noteFile = result.getOrThrow()
        assertTrue(noteFile.exists())
        assertEquals("Math _ Physics _ Part 1_.xopp", noteFile.name)
    }

    @Test
    fun `test createBlankNote resolves unique filename when name already exists`() = runBlocking {
        val firstResult = repository.createBlankNote("Project", notesDir)
        val secondResult = repository.createBlankNote("Project", notesDir)
        val thirdResult = repository.createBlankNote("Project", notesDir)

        assertTrue(firstResult.isSuccess)
        assertTrue(secondResult.isSuccess)
        assertTrue(thirdResult.isSuccess)

        assertEquals("Project.xopp", firstResult.getOrThrow().name)
        assertEquals("Project_2.xopp", secondResult.getOrThrow().name)
        assertEquals("Project_3.xopp", thirdResult.getOrThrow().name)
    }

    @Test
    fun `test createFolder creates new directory and rejects duplicates`() {
        val result = repository.createFolder(notesDir, "Mathematics")
        assertTrue(result.isSuccess)
        val folder = result.getOrThrow()
        assertTrue(folder.exists())
        assertTrue(folder.isDirectory)
        assertEquals("Mathematics", folder.name)

        // Attempting to create duplicate folder
        val duplicateResult = repository.createFolder(notesDir, "Mathematics")
        assertTrue(duplicateResult.isFailure)
    }

    @Test
    fun `test renameFolder moves directory contents successfully`() = runBlocking {
        val folderResult = repository.createFolder(notesDir, "Drafts")
        val folder = folderResult.getOrThrow()
        val fileInside = File(folder, "draft1.xopp").apply { writeText("draft content") }

        val renameResult = repository.renameFolder(folder, "Archive")
        assertTrue(renameResult.isSuccess)

        val renamedFolder = renameResult.getOrThrow()
        assertEquals("Archive", renamedFolder.name)
        assertTrue(renamedFolder.exists())
        assertFalse(folder.exists())

        val movedFile = File(renamedFolder, "draft1.xopp")
        assertTrue(movedFile.exists())
        assertEquals("draft content", movedFile.readText())
    }

    @Test
    fun `test duplicateNote generates copy suffix correctly`() = runBlocking {
        val originalFile = File(notesDir, "Design.xopp").apply { writeText("original content") }
        val noteDoc = NoteDocument(
            file = originalFile,
            title = "Design.xopp",
            path = originalFile.absolutePath,
            lastModifiedMs = originalFile.lastModified(),
            sizeBytes = originalFile.length()
        )

        val copy1Result = repository.duplicateNote(noteDoc)
        assertTrue(copy1Result.isSuccess)
        val copy1File = copy1Result.getOrThrow()
        assertEquals("Design (Copy).xopp", copy1File.name)
        assertEquals("original content", copy1File.readText())

        val copy2Result = repository.duplicateNote(noteDoc)
        assertTrue(copy2Result.isSuccess)
        val copy2File = copy2Result.getOrThrow()
        assertEquals("Design (Copy 2).xopp", copy2File.name)
    }

    @Test
    fun `test note pinning lifecycle`() {
        val noteFile = File(notesDir, "PinnedNote.xopp").apply { writeText("data") }
        val path = noteFile.absolutePath

        assertFalse(repository.isNotePinned(path))

        repository.pinNote(path)
        assertTrue(repository.isNotePinned(path))
        assertTrue(repository.getPinnedNotePaths().contains(path))

        // Toggle unpins
        val toggledOff = repository.togglePinNote(path)
        assertFalse(toggledOff)
        assertFalse(repository.isNotePinned(path))

        // Toggle pins again
        val toggledOn = repository.togglePinNote(path)
        assertTrue(toggledOn)
        assertTrue(repository.isNotePinned(path))

        repository.unpinNote(path)
        assertFalse(repository.isNotePinned(path))
    }

    @Test
    fun `test folder pinning lifecycle`() {
        val folder = File(notesDir, "PinnedFolder").apply { mkdirs() }
        val path = folder.absolutePath

        assertFalse(repository.isFolderPinned(path))

        repository.pinFolder(path)
        assertTrue(repository.isFolderPinned(path))
        assertTrue(repository.getPinnedFolderPaths().contains(path))

        // Verify .aoppfolder was written
        val metaFile = File(folder, ".aoppfolder")
        assertTrue(metaFile.exists())
        val metaJson = org.json.JSONObject(metaFile.readText())
        assertTrue(metaJson.optBoolean("pinned"))

        repository.unpinFolder(path)
        assertFalse(repository.isFolderPinned(path))
        assertFalse(org.json.JSONObject(metaFile.readText()).optBoolean("pinned"))
    }

    @Test
    fun `test emergency saves unpinning persists to aoppfolder`() {
        val emergencyFolder = File(notesDir, "Emergency Saves").apply { mkdirs() }
        val folderItem = repository.getFolderItem(emergencyFolder)
        assertTrue(folderItem.isPinned)

        val willPin = repository.togglePinFolder(folderItem)
        assertFalse(willPin)
        assertFalse(repository.isFolderPinned(emergencyFolder.absolutePath))

        // Re-read item
        val updatedItem = repository.getFolderItem(emergencyFolder)
        assertFalse(updatedItem.isPinned)

        val metaFile = File(emergencyFolder, ".aoppfolder")
        assertTrue(metaFile.exists())
        assertFalse(org.json.JSONObject(metaFile.readText()).optBoolean("pinned"))
    }
}
