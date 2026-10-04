package dev.ilamparithi.aournalpp.data

import android.content.Context
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

class DocumentRepositoryRecursiveSearchTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var notesDir: File
    private lateinit var repository: DocumentRepository
    private lateinit var mockContext: Context
    private lateinit var prefs: TestSharedPreferences

    private lateinit var fakeDao: dev.ilamparithi.aournalpp.testutils.FakeNoteIndexDao

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
        fakeDao = dev.ilamparithi.aournalpp.testutils.FakeNoteIndexDao()
        repository = DocumentRepository(mockContext, fakeDao)
    }

    @Test
    fun `test non-recursive scan when query is blank only lists immediate folder contents`() = runBlocking {
        File(notesDir, "RootNote.xopp").writeText("note 1")
        val subDir = File(notesDir, "SubFolder").apply { mkdir() }
        File(subDir, "NestedNote.xopp").writeText("note 2")

        val (folders, notes) = repository.scanDirectory(notesDir, query = "", showHidden = false)

        assertEquals(1, folders.size)
        assertEquals("SubFolder", folders[0].name)

        assertEquals(1, notes.size)
        assertEquals("RootNote", notes[0].title)
    }

    @Test
    fun `test recursive search at root finds notes in nested subdirectories`() = runBlocking {
        File(notesDir, "AlphaNote.xopp").writeText("root")
        val projectsDir = File(notesDir, "Projects").apply { mkdir() }
        File(projectsDir, "BetaNote.xopp").writeText("sub")
        val deepDir = File(projectsDir, "Deep").apply { mkdir() }
        File(deepDir, "GammaNote.xopp").writeText("deep")

        val (folders, notes) = repository.scanDirectory(notesDir, query = "Note", showHidden = false)

        assertEquals(3, notes.size)
        val noteTitles = notes.map { it.title }.toSet()
        assertTrue(noteTitles.contains("AlphaNote"))
        assertTrue(noteTitles.contains("BetaNote"))
        assertTrue(noteTitles.contains("GammaNote"))

        // Verify folder relative paths for card badges
        val alpha = notes.first { it.title == "AlphaNote" }
        assertEquals("Notes Home", alpha.folder)

        val beta = notes.first { it.title == "BetaNote" }
        assertEquals("Projects", beta.folder)

        val gamma = notes.first { it.title == "GammaNote" }
        assertEquals("Projects${File.separator}Deep", gamma.folder)
    }

    @Test
    fun `test scoped recursive search in subfolder only finds notes within that subtree`() = runBlocking {
        File(notesDir, "RootItem.xopp").writeText("root")
        val mathDir = File(notesDir, "Math").apply { mkdir() }
        File(mathDir, "Calculus.xopp").writeText("math 1")
        val algebraDir = File(mathDir, "Algebra").apply { mkdir() }
        File(algebraDir, "Linear.xopp").writeText("math 2")

        val historyDir = File(notesDir, "History").apply { mkdir() }
        File(historyDir, "Rome.xopp").writeText("history")

        // Search specifically inside Math subfolder
        val (folders, notes) = repository.scanDirectory(mathDir, query = "l", showHidden = false)

        val titles = notes.map { it.title }.toSet()
        assertTrue(titles.contains("Calculus"))
        assertTrue(titles.contains("Linear"))
        assertFalse(titles.contains("RootItem"))
        assertFalse(titles.contains("Rome"))

        val linear = notes.first { it.title == "Linear" }
        assertEquals("Algebra", linear.folder)
    }

    @Test
    fun `test file type filtering excludes unchecked extensions`() = runBlocking {
        File(notesDir, "Doc1.xopp").writeText("xopp")
        File(notesDir, "Doc2.pdf").writeText("pdf")
        File(notesDir, "Doc3.xoj").writeText("xoj")

        // Only XOPP enabled
        val (_, notesXopp) = repository.scanDirectory(
            targetDir = notesDir,
            query = "",
            enabledExtensions = setOf("xopp")
        )
        assertEquals(1, notesXopp.size)
        assertEquals("Doc1", notesXopp[0].title)

        // XOPP and PDF enabled
        val (_, notesTwo) = repository.scanDirectory(
            targetDir = notesDir,
            query = "",
            enabledExtensions = setOf("xopp", "pdf")
        )
        assertEquals(2, notesTwo.size)
        val titles = notesTwo.map { it.title }.toSet()
        assertTrue(titles.contains("Doc1"))
        assertTrue(titles.contains("Doc2"))
        assertFalse(titles.contains("Doc3"))
    }

    @Test
    fun `test recursive search ignores trash directory`() = runBlocking {
        File(notesDir, "ActiveDoc.xopp").writeText("active")
        val trashDir = File(notesDir, ".trash").apply { mkdir() }
        File(trashDir, "TrashedDoc.xopp").writeText("trashed")

        val (_, notes) = repository.scanDirectory(notesDir, query = "Doc", showHidden = true)
        assertEquals(1, notes.size)
        assertEquals("ActiveDoc", notes[0].title)
    }
}
