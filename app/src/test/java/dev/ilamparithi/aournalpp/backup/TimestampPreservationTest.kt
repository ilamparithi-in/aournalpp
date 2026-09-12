package dev.ilamparithi.aournalpp.backup

import android.content.Context
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import dev.ilamparithi.aournalpp.backup.provider.WebDavStorageProvider
import dev.ilamparithi.aournalpp.data.DocumentRepository
import dev.ilamparithi.aournalpp.model.AutosaveInfo
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.testutils.TestSharedPreferences
import dev.ilamparithi.aournalpp.utils.ExternalFileHandler
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TimestampPreservationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0
        every { Log.w(any(), any<String>(), any<Throwable>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun testWebDavUploadSendsXOcMtimeHeader() = runBlocking {
        var interceptedMtimeHeader: String? = null
        val interceptor = Interceptor { chain ->
            val request = chain.request()
            interceptedMtimeHeader = request.header("X-OC-Mtime")
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(204)
                .message("No Content")
                .body("".toResponseBody("text/plain".toMediaTypeOrNull()))
                .build()
        }

        val testClient = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val config = ServiceConfig(
            id = "nc-test",
            name = "Nextcloud Test",
            providerType = StorageProviderType.NEXTCLOUD,
            serverUrl = "https://cloud.example.org",
            username = "testuser",
            passwordOrSecret = "pwd"
        )
        val provider = WebDavStorageProvider(config, httpClient = testClient)

        val localFile = File(tempFolder.root, "test_upload.xopp").apply {
            writeText("dummy content")
        }
        val targetMtime = 1690000000000L // epoch ms
        localFile.setLastModified(targetMtime)

        val result = provider.uploadFile(localFile, "test_upload.xopp") { _, _ -> }
        assertTrue(result.isSuccess)
        assertNotNull(interceptedMtimeHeader)
        val expectedMtimeSec = (targetMtime / 1000L).toString()
        assertEquals("X-OC-Mtime header must match file modification time in epoch seconds", expectedMtimeSec, interceptedMtimeHeader)
    }

    @Test
    fun testWebDavDownloadSetsDestinationLastModified() = runBlocking {
        // HTTP-date format: Sun, 06 Nov 1994 08:49:37 GMT -> 784111777000L ms
        val httpDateStr = "Sun, 06 Nov 1994 08:49:37 GMT"
        val expectedEpochMs = 784111777000L

        val interceptor = Interceptor { chain ->
            val request = chain.request()
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("Last-Modified", httpDateStr)
                .body("downloaded note content".toResponseBody("application/octet-stream".toMediaTypeOrNull()))
                .build()
        }

        val testClient = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val config = ServiceConfig(
            id = "nc-test",
            name = "Nextcloud Test",
            providerType = StorageProviderType.NEXTCLOUD,
            serverUrl = "https://cloud.example.org",
            username = "testuser",
            passwordOrSecret = "pwd"
        )
        val provider = WebDavStorageProvider(config, httpClient = testClient)

        val destFile = File(tempFolder.root, "downloaded.xopp")
        val result = provider.downloadFile("downloaded.xopp", destFile) { _, _ -> }
        assertTrue(result.isSuccess)
        assertTrue(destFile.exists())
        assertEquals("Downloaded file lastModified must match remote Last-Modified HTTP header", expectedEpochMs, destFile.lastModified())
    }

    @Test
    fun testDuplicateNotePreservesTimestamp() = runBlocking {
        val notesDir = tempFolder.newFolder("notes")
        val filesDir = tempFolder.newFolder("files")

        val prefs = TestSharedPreferences()
        prefs.putString(LinuxEnvironment.PREF_KEY_NOTES_DIR, notesDir.absolutePath)

        val mockContext = mockk<Context>(relaxed = true)
        every { mockContext.getSharedPreferences(any(), any()) } returns prefs
        every { mockContext.filesDir } returns filesDir
        every { mockContext.applicationContext } returns mockContext

        DocumentRepository.invalidateAllCaches()
        val repo = DocumentRepository(mockContext)

        val originalFile = File(notesDir, "my_lecture.xopp").apply {
            writeText("Lecture note content")
        }
        val targetMtime = 1685000000000L
        originalFile.setLastModified(targetMtime)

        val doc = NoteDocument(
            file = originalFile,
            title = "my_lecture.xopp",
            path = originalFile.absolutePath,
            lastModifiedMs = originalFile.lastModified(),
            sizeBytes = originalFile.length()
        )

        val result = repo.duplicateNote(doc)
        assertTrue(result.isSuccess)
        val duplicateFile = result.getOrThrow()
        assertTrue(duplicateFile.exists())
        assertEquals("Duplicate note file must preserve original file lastModified", targetMtime, duplicateFile.lastModified())
    }

    @Test
    fun testAutosaveReplacementAndKeepBothPreserveTimestamp() {
        val notesDir = tempFolder.newFolder("notes_auto")
        val filesDir = tempFolder.newFolder("files_auto")

        val prefs = TestSharedPreferences()
        prefs.putString(LinuxEnvironment.PREF_KEY_NOTES_DIR, notesDir.absolutePath)

        val mockContext = mockk<Context>(relaxed = true)
        every { mockContext.getSharedPreferences(any(), any()) } returns prefs
        every { mockContext.filesDir } returns filesDir
        every { mockContext.applicationContext } returns mockContext

        DocumentRepository.invalidateAllCaches()
        val repo = DocumentRepository(mockContext)

        val noteFile = File(notesDir, "active_note.xopp").apply {
            writeText("old content")
        }
        noteFile.setLastModified(1600000000000L)

        val autosaveMtime = 1705000000000L
        val autoFile = File(notesDir, "active_note.xopp.autosave.xopp").apply {
            writeText("new autosave content")
        }
        autoFile.setLastModified(autosaveMtime)

        val autoInfo = AutosaveInfo(
            autosaveFile = autoFile,
            mainFile = noteFile,
            mainLastModifiedMs = noteFile.lastModified(),
            autosaveLastModifiedMs = autosaveMtime,
            mainSizeBytes = noteFile.length(),
            autosaveSizeBytes = autoFile.length()
        )

        val noteDoc = NoteDocument(
            file = noteFile,
            title = "active_note.xopp",
            path = noteFile.absolutePath,
            lastModifiedMs = noteFile.lastModified(),
            sizeBytes = noteFile.length(),
            autosaveInfo = autoInfo
        )

        val replaced = repo.replaceWithAutosave(noteDoc)
        assertEquals("active_note.xopp", replaced.name)
        assertEquals("Replaced note must retain autosave timestamp", autosaveMtime, replaced.lastModified())
    }

    @Test
    fun testExternalFileImportPreservesTimestamp() = runBlocking {
        val importedDir = tempFolder.newFolder("imported")
        val env = mockk<LinuxEnvironment>(relaxed = true)
        every { env.getImportedDirectory() } returns importedDir

        val sourceDir = tempFolder.newFolder("source")
        val sourceFile = File(sourceDir, "imported_doc.xopp").apply {
            writeText("External document content")
        }
        val targetMtime = 1695000000000L
        sourceFile.setLastModified(targetMtime)

        val result = ExternalFileHandler.importToImportedDir(sourceFile, env)
        assertTrue(result.isSuccess)
        val importedFile = result.getOrThrow()
        assertTrue(importedFile.exists())
        assertEquals("Imported note must preserve source file lastModified timestamp", targetMtime, importedFile.lastModified())
    }

    @Test
    fun testGoogleDriveUploadPreservesModifiedTime() = runBlocking {
        var capturedRequestBody = ""
        val interceptor = Interceptor { chain ->
            val request = chain.request()
            val url = request.url.toString()
            if (url.contains("/files?") && url.contains("uploadType=multipart")) {
                val buffer = okio.Buffer()
                request.body?.writeTo(buffer)
                capturedRequestBody = buffer.readUtf8()
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"id": "file123"}""".toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            } else {
                // files search
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"files": []}""".toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            }
        }

        val testClient = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val config = ServiceConfig(
            id = "gdrive-test",
            name = "Google Drive Test",
            providerType = StorageProviderType.GOOGLE_DRIVE,
            authToken = "mock_token"
        )
        val provider = dev.ilamparithi.aournalpp.backup.provider.GoogleDriveProvider(config, httpClient = testClient)

        val localFile = File(tempFolder.root, "lecture.xopp").apply {
            writeText("Lecture notes")
        }
        val targetMtime = 1710000000000L
        localFile.setLastModified(targetMtime)

        val result = provider.uploadFile(localFile, "lecture.xopp") { _, _ -> }
        assertTrue(result.isSuccess)
        assertTrue("Multipart upload must include modifiedTime in metadata JSON", capturedRequestBody.contains("modifiedTime"))
    }

    @Test
    fun testGoogleDriveDownloadPreservesModifiedTime() = runBlocking {
        val targetEpochMs = 1715000000000L // 2024-05-06T12:53:20.000Z
        val isoString = "2024-05-06T12:53:20.000Z"

        val interceptor = Interceptor { chain ->
            val request = chain.request()
            val url = request.url.toString()
            if (url.contains("alt=media")) {
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("Google drive downloaded content".toResponseBody("application/octet-stream".toMediaTypeOrNull()))
                    .build()
            } else {
                // findFileInfoInFolder
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"files": [{"id": "gdrive_file_1", "modifiedTime": "$isoString"}]}""".toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            }
        }

        val testClient = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val config = ServiceConfig(
            id = "gdrive-test",
            name = "Google Drive Test",
            providerType = StorageProviderType.GOOGLE_DRIVE,
            authToken = "mock_token"
        )
        val provider = dev.ilamparithi.aournalpp.backup.provider.GoogleDriveProvider(config, httpClient = testClient)

        val destFile = File(tempFolder.root, "restored_gdrive.xopp")
        val result = provider.downloadFile("restored_gdrive.xopp", destFile) { _, _ -> }
        assertTrue(result.isSuccess)
        assertTrue(destFile.exists())
        assertEquals("Downloaded file lastModified must match remote Google Drive modifiedTime", targetEpochMs, destFile.lastModified())
    }
}
