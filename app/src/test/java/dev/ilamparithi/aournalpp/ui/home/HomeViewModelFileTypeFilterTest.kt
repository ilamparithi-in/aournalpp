package dev.ilamparithi.aournalpp.ui.home

import android.app.Application
import android.content.Context
import dev.ilamparithi.aournalpp.data.AppPreferences
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.model.NoteFileType
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import dev.ilamparithi.aournalpp.testutils.TestSharedPreferences
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import dev.ilamparithi.aournalpp.utils.ThumbnailManager
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelFileTypeFilterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var prefs: TestSharedPreferences
    private lateinit var mockApp: Application
    private lateinit var repository: DocumentRepository
    private lateinit var env: LinuxEnvironment
    private lateinit var supervisor: ProcessSupervisor
    private lateinit var pdfExportManager: PdfExportManager

    private lateinit var xoppFile: File
    private lateinit var xojFile: File
    private lateinit var pdfFile: File

    private lateinit var xoppDoc: NoteDocument
    private lateinit var xojDoc: NoteDocument
    private lateinit var pdfDoc: NoteDocument

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkObject(ThumbnailManager)
        every { ThumbnailManager.prefetchThumbnails(any(), any(), any(), any()) } returns Unit

        prefs = TestSharedPreferences()
        mockApp = mockk<Application>(relaxed = true)
        repository = mockk<DocumentRepository>(relaxed = true)
        env = mockk<LinuxEnvironment>(relaxed = true)
        supervisor = mockk<ProcessSupervisor>(relaxed = true)
        pdfExportManager = mockk<PdfExportManager>(relaxed = true)

        xoppFile = tempFolder.newFile("sample.xopp")
        xojFile = tempFolder.newFile("sample.xoj")
        pdfFile = tempFolder.newFile("sample.pdf")

        xoppDoc = NoteDocument(file = xoppFile, title = "XoppDoc")
        xojDoc = NoteDocument(file = xojFile, title = "XojDoc")
        pdfDoc = NoteDocument(file = pdfFile, title = "PdfDoc")

        val notesList = listOf(xoppDoc, xojDoc, pdfDoc)
        every { repository.getCachedHomeNotes(any()) } returns notesList
        every { repository.getCachedContinueNote() } returns xoppDoc
        every { repository.getCachedTotalNotesCount() } returns 3
        every { repository.getCachedTotalFoldersCount() } returns 0

        coEvery { repository.getHomeData(any()) } returns DocumentRepository.HomeDataResult(
            notes = notesList,
            continueNote = xoppDoc,
            totalNotesCount = 3,
            totalFoldersCount = 0
        )
    }

    @After
    fun tearDown() {
        unmockkObject(ThumbnailManager)
        Dispatchers.resetMain()
    }

    @Test
    fun `test default enabled file types has all three types enabled and includes all recent notes`() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(
            application = mockApp,
            repository = repository,
            env = env,
            supervisor = supervisor,
            pdfExportManager = pdfExportManager,
            prefs = prefs,
            ioDispatcher = testDispatcher
        )

        val state = viewModel.uiState.value
        assertEquals(3, state.enabledFileTypes.size)
        assertTrue(state.enabledFileTypes.contains(NoteFileType.XOPP))
        assertTrue(state.enabledFileTypes.contains(NoteFileType.XOJ))
        assertTrue(state.enabledFileTypes.contains(NoteFileType.PDF))
        assertEquals(3, state.recentNotes.size)
    }

    @Test
    fun `test toggling off PDF excludes PDF from recent notes and persists to preferences`() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(
            application = mockApp,
            repository = repository,
            env = env,
            supervisor = supervisor,
            pdfExportManager = pdfExportManager,
            prefs = prefs,
            ioDispatcher = testDispatcher
        )

        viewModel.toggleFileType(NoteFileType.PDF)

        val state = viewModel.uiState.value
        assertEquals(2, state.enabledFileTypes.size)
        assertFalse(state.enabledFileTypes.contains(NoteFileType.PDF))
        assertTrue(state.enabledFileTypes.contains(NoteFileType.XOPP))
        assertTrue(state.enabledFileTypes.contains(NoteFileType.XOJ))

        assertEquals(2, state.recentNotes.size)
        assertTrue(state.recentNotes.any { it.fileType == NoteFileType.XOPP })
        assertTrue(state.recentNotes.any { it.fileType == NoteFileType.XOJ })
        assertFalse(state.recentNotes.any { it.fileType == NoteFileType.PDF })

        val saved = prefs.getStringSet(AppPreferences.PREF_VISIBLE_FILE_TYPES, mutableSetOf<String>())
        assertEquals(setOf("xopp", "xoj"), saved)
    }

    @Test
    fun `test invariant - cannot uncheck the last remaining file type`() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(
            application = mockApp,
            repository = repository,
            env = env,
            supervisor = supervisor,
            pdfExportManager = pdfExportManager,
            prefs = prefs,
            ioDispatcher = testDispatcher
        )

        viewModel.toggleFileType(NoteFileType.PDF)
        viewModel.toggleFileType(NoteFileType.XOJ)
        assertEquals(1, viewModel.uiState.value.enabledFileTypes.size)
        assertTrue(viewModel.uiState.value.enabledFileTypes.contains(NoteFileType.XOPP))

        // Attempt to disable last remaining type
        viewModel.toggleFileType(NoteFileType.XOPP)

        // Must still remain enabled
        assertEquals(1, viewModel.uiState.value.enabledFileTypes.size)
        assertTrue(viewModel.uiState.value.enabledFileTypes.contains(NoteFileType.XOPP))
        assertEquals(1, viewModel.uiState.value.recentNotes.size)
        assertEquals(NoteFileType.XOPP, viewModel.uiState.value.recentNotes.first().fileType)
    }

    @Test
    fun `test onAppResume syncs changes made externally to preferences`() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(
            application = mockApp,
            repository = repository,
            env = env,
            supervisor = supervisor,
            pdfExportManager = pdfExportManager,
            prefs = prefs,
            ioDispatcher = testDispatcher
        )

        assertEquals(3, viewModel.uiState.value.recentNotes.size)

        // External update (e.g. DocumentHub or Settings)
        AppPreferences.saveEnabledFileTypes(prefs, setOf(NoteFileType.XOPP))

        viewModel.onAppResume(force = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(setOf(NoteFileType.XOPP), state.enabledFileTypes)
        assertEquals(1, state.recentNotes.size)
        assertEquals(NoteFileType.XOPP, state.recentNotes.first().fileType)
    }
}
