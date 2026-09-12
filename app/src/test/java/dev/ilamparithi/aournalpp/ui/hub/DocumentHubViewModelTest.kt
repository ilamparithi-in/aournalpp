package dev.ilamparithi.aournalpp.ui.hub

import android.app.Application
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.model.FolderItem
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.testutils.TestSharedPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentHubViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application
    private lateinit var repository: DocumentRepository
    private lateinit var env: LinuxEnvironment
    private lateinit var supervisor: ProcessSupervisor
    private lateinit var pdfExportManager: PdfExportManager
    private lateinit var prefs: TestSharedPreferences
    private lateinit var rootDir: File

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        rootDir = tempFolder.newFolder("notes")
        app = mockk<Application>(relaxed = true)
        repository = mockk<DocumentRepository>(relaxed = true)
        env = mockk<LinuxEnvironment>(relaxed = true)
        supervisor = mockk<ProcessSupervisor>(relaxed = true)
        pdfExportManager = mockk<PdfExportManager>(relaxed = true)
        prefs = TestSharedPreferences()

        every { repository.getRootNotesDirectory() } returns rootDir
        every { repository.getCachedDirectory(any(), any(), any()) } returns null
        every { repository.getCachedRecentNotes(any()) } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): DocumentHubViewModel {
        return DocumentHubViewModel(
            application = app,
            repository = repository,
            env = env,
            supervisor = supervisor,
            pdfExportManager = pdfExportManager,
            prefs = prefs,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `test initial state reflects defaults and repository root`() {
        val viewModel = createViewModel()

        assertEquals(rootDir, viewModel.currentDirectory.value)
        assertTrue(viewModel.folders.value.isEmpty())
        assertTrue(viewModel.notes.value.isEmpty())
        assertFalse(viewModel.isViewingTrash.value)
        assertTrue(viewModel.isGridView.value)
        assertFalse(viewModel.showHiddenFiles.value)
        assertEquals("", viewModel.searchQuery.value)
        assertFalse(viewModel.isSearchActive.value)
        assertFalse(viewModel.isSelectionMode.value)
        assertTrue(viewModel.selectedNotePaths.value.isEmpty())
    }

    @Test
    fun `test setCurrentDirectory updates state from cache`() {
        val subDir = File(rootDir, "Algebra")
        val sampleNote = NoteDocument(
            file = File(subDir, "linear.xopp"),
            title = "linear.xopp",
            path = File(subDir, "linear.xopp").absolutePath,
            lastModifiedMs = 1000L,
            sizeBytes = 200L
        )
        val sampleFolder = FolderItem(
            file = File(subDir, "Chapter1"),
            name = "Chapter1",
            itemCount = 2
        )

        every {
            repository.getCachedDirectory(subDir, "", false)
        } returns Pair(listOf(sampleFolder), listOf(sampleNote))

        val viewModel = createViewModel()
        viewModel.setCurrentDirectory(subDir)

        assertEquals(subDir, viewModel.currentDirectory.value)
        assertEquals(1, viewModel.folders.value.size)
        assertEquals("Chapter1", viewModel.folders.value[0].name)
        assertEquals(1, viewModel.notes.value.size)
        assertEquals("linear.xopp", viewModel.notes.value[0].title)
    }

    @Test
    fun `test search query and active state mutators`() {
        val viewModel = createViewModel()

        viewModel.setSearchQuery("Matrix")
        assertEquals("Matrix", viewModel.searchQuery.value)

        viewModel.setSearchActive(true)
        assertTrue(viewModel.isSearchActive.value)

        viewModel.setSearchActive(false)
        assertFalse(viewModel.isSearchActive.value)
    }

    @Test
    fun `test selection mode toggling and clearing`() {
        val viewModel = createViewModel()

        viewModel.setSelectionMode(true)
        assertTrue(viewModel.isSelectionMode.value)

        val paths = setOf("/path/1.xopp", "/path/2.xopp")
        viewModel.setSelectedNotePaths(paths)
        assertEquals(2, viewModel.selectedNotePaths.value.size)

        // Disabling selection mode clears selected paths
        viewModel.setSelectionMode(false)
        assertFalse(viewModel.isSelectionMode.value)
        assertTrue(viewModel.selectedNotePaths.value.isEmpty())
        assertNull(viewModel.lastSelectedNotePath.value)
    }

    @Test
    fun `test trash viewing mode toggling`() {
        val viewModel = createViewModel()

        assertFalse(viewModel.isViewingTrash.value)
        viewModel.setViewingTrash(true)
        assertTrue(viewModel.isViewingTrash.value)

        viewModel.setViewingTrash(false)
        assertFalse(viewModel.isViewingTrash.value)
    }

    @Test
    fun `test grid view and hidden files preferences persistence`() {
        val viewModel = createViewModel()

        viewModel.toggleGridView()
        assertFalse(viewModel.isGridView.value)
        assertFalse(prefs.getBoolean("pref_is_grid_view", true))

        viewModel.toggleShowHiddenFiles()
        assertTrue(viewModel.showHiddenFiles.value)
        assertTrue(prefs.getBoolean("pref_show_hidden_files", false))
    }

    @Test
    fun `test emergency dialog dismiss and quarantine clearing`() {
        val viewModel = createViewModel()

        viewModel.dismissEmergencyDialog()
        assertFalse(viewModel.showEmergencyDialog.value)

        viewModel.clearQuarantinedEmergencySave()
        assertNull(viewModel.quarantinedEmergencySave.value)
    }
}
