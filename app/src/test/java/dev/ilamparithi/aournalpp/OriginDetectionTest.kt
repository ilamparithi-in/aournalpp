package dev.ilamparithi.aournalpp

import android.content.Context
import android.net.Uri
import dev.ilamparithi.aournalpp.utils.ExternalFileHandler
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class OriginDetectionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test resolveIfInNotesDirectory with file inside notes directory`() {
        val rootNotesDir = tempFolder.newFolder("Notes")
        val subFolder = File(rootNotesDir, "Subfolder").apply { mkdirs() }
        val noteFile = File(subFolder, "my_note.xopp").apply { writeText("dummy") }

        val mockContext = mockk<Context>(relaxed = true)
        val fileUri = mockk<Uri>()
        every { fileUri.scheme } returns "file"
        every { fileUri.path } returns noteFile.absolutePath

        val resolved = ExternalFileHandler.resolveIfInNotesDirectory(mockContext, fileUri, rootNotesDir)
        assertNotNull(resolved)
        assertEquals(noteFile.canonicalPath, resolved?.canonicalPath)
    }

    @Test
    fun `test resolveIfInNotesDirectory with file outside notes directory`() {
        val rootNotesDir = tempFolder.newFolder("Notes")
        val downloadsDir = tempFolder.newFolder("Downloads")
        val externalFile = File(downloadsDir, "external.xopp").apply { writeText("dummy") }

        val mockContext = mockk<Context>(relaxed = true)
        val fileUri = mockk<Uri>()
        every { fileUri.scheme } returns "file"
        every { fileUri.path } returns externalFile.absolutePath

        val resolved = ExternalFileHandler.resolveIfInNotesDirectory(mockContext, fileUri, rootNotesDir)
        assertNull(resolved)
    }

    @Test
    fun `test resolveIfInNotesDirectory with internal fileprovider URI`() {
        val rootNotesDir = tempFolder.newFolder("Notes")
        val noteFile = File(rootNotesDir, "test.xopp").apply { writeText("dummy") }

        val mockContext = mockk<Context>(relaxed = true)
        every { mockContext.packageName } returns "dev.ilamparithi.aournalpp"

        val providerUri = mockk<Uri>()
        every { providerUri.scheme } returns "content"
        every { providerUri.authority } returns "dev.ilamparithi.aournalpp.fileprovider"
        every { providerUri.path } returns "/internal_notes/test.xopp"

        val resolved = ExternalFileHandler.resolveIfInNotesDirectory(mockContext, providerUri, rootNotesDir)
        assertNotNull(resolved)
        assertEquals(noteFile.canonicalPath, resolved?.canonicalPath)
    }

    @Test
    fun `test resolveIfInNotesDirectory with decoded path in external provider URI`() {
        val rootNotesDir = tempFolder.newFolder("Notes")
        val noteFile = File(rootNotesDir, "embedded_path.xopp").apply { writeText("dummy") }

        val mockContext = mockk<Context>(relaxed = true)
        val providerUri = mockk<Uri>()
        every { providerUri.scheme } returns "content"
        every { providerUri.authority } returns "com.external.filemanager.provider"
        every { providerUri.toString() } returns "content://com.external.filemanager.provider/root${noteFile.absolutePath}"

        val resolved = ExternalFileHandler.resolveIfInNotesDirectory(mockContext, providerUri, rootNotesDir)
        assertNotNull(resolved)
        assertEquals(noteFile.canonicalPath, resolved?.canonicalPath)
    }
}
