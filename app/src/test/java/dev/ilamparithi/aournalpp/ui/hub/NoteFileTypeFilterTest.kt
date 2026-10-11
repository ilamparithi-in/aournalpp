package dev.ilamparithi.aournalpp.ui.hub

import android.app.Application
import android.content.Context
import dev.ilamparithi.aournalpp.data.AppPreferences
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.model.NoteFileType
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.testutils.TestSharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NoteFileTypeFilterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var prefs: TestSharedPreferences
    private lateinit var mockApp: Application
    private lateinit var mockContext: Context
    private lateinit var repository: DocumentRepository
    private lateinit var viewModel: DocumentHubViewModel

    @Before
    fun setUp() {
        val notesDir = tempFolder.newFolder("notes")
        val filesDir = tempFolder.newFolder("files")

        prefs = TestSharedPreferences()
        prefs.putString(LinuxEnvironment.PREF_KEY_NOTES_DIR, notesDir.absolutePath)

        val cacheDir = tempFolder.newFolder("cache")

        mockContext = mockk<Context>(relaxed = true)
        every { mockContext.getSharedPreferences(any(), any()) } returns prefs
        every { mockContext.filesDir } returns filesDir
        every { mockContext.cacheDir } returns cacheDir
        every { mockContext.applicationContext } returns mockContext

        mockApp = mockk<Application>(relaxed = true)
        every { mockApp.applicationContext } returns mockApp
        every { mockApp.getSharedPreferences(any(), any()) } returns prefs
        every { mockApp.filesDir } returns filesDir
        every { mockApp.cacheDir } returns cacheDir

        DocumentRepository.invalidateAllCaches()
        repository = DocumentRepository(mockContext)

        viewModel = DocumentHubViewModel(
            application = mockApp,
            repository = repository,
            prefs = prefs
        )
    }

    @Test
    fun `test default enabled file types has all three types enabled`() {
        val types = viewModel.enabledFileTypes.value
        assertEquals(3, types.size)
        assertTrue(types.contains(NoteFileType.XOPP))
        assertTrue(types.contains(NoteFileType.PDF))
        assertTrue(types.contains(NoteFileType.XOJ))
    }

    @Test
    fun `test toggling off one file type succeeds leaving two enabled`() {
        viewModel.toggleFileType(NoteFileType.PDF)
        val types = viewModel.enabledFileTypes.value
        assertEquals(2, types.size)
        assertTrue(types.contains(NoteFileType.XOPP))
        assertTrue(types.contains(NoteFileType.XOJ))
        assertFalse(types.contains(NoteFileType.PDF))
    }

    @Test
    fun `test toggling off two file types succeeds leaving one enabled`() {
        viewModel.toggleFileType(NoteFileType.PDF)
        viewModel.toggleFileType(NoteFileType.XOJ)
        val types = viewModel.enabledFileTypes.value
        assertEquals(1, types.size)
        assertTrue(types.contains(NoteFileType.XOPP))
    }

    @Test
    fun `test invariant - cannot uncheck the last remaining file type`() {
        viewModel.toggleFileType(NoteFileType.PDF)
        viewModel.toggleFileType(NoteFileType.XOJ)
        assertEquals(1, viewModel.enabledFileTypes.value.size)

        // Attempt to uncheck the final type (XOPP)
        viewModel.toggleFileType(NoteFileType.XOPP)

        // Invariant check: XOPP must still remain checked
        val types = viewModel.enabledFileTypes.value
        assertEquals(1, types.size)
        assertTrue(types.contains(NoteFileType.XOPP))
    }

    @Test
    fun `test re-enabling file types restores them`() {
        viewModel.toggleFileType(NoteFileType.PDF)
        assertFalse(viewModel.enabledFileTypes.value.contains(NoteFileType.PDF))

        viewModel.toggleFileType(NoteFileType.PDF)
        assertTrue(viewModel.enabledFileTypes.value.contains(NoteFileType.PDF))
        assertEquals(3, viewModel.enabledFileTypes.value.size)
    }

    @Test
    fun `test file types persist to preferences`() {
        viewModel.toggleFileType(NoteFileType.PDF)
        val saved = prefs.getStringSet(AppPreferences.PREF_VISIBLE_FILE_TYPES, mutableSetOf<String>())
        assertEquals(setOf("xopp", "xoj"), saved)
    }
}
