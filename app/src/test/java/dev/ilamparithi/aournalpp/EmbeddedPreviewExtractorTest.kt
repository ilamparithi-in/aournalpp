package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.utils.ThumbnailManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream

class EmbeddedPreviewExtractorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test extractEmbeddedXoppPreviewBytes on file with embedded preview`() {
        val sampleFile = tempFolder.newFile("sample_with_preview.xopp")
        // Base64-encoded 1x1 PNG: iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==
        val base64Png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        val xmlContent = """
            <?xml version="1.0" standalone="no"?>
            <xournal creator="xournalpp 1.2.8" fileversion="4">
            <title>Test Note</title>
            <preview>$base64Png</preview>
            <page width="100" height="100"/>
            </xournal>
        """.trimIndent()

        GZIPOutputStream(FileOutputStream(sampleFile)).use { gzip ->
            gzip.write(xmlContent.toByteArray(Charsets.UTF_8))
        }

        val bytes = ThumbnailManager.extractEmbeddedXoppPreviewBytes(sampleFile)
        assertNotNull("Embedded preview bytes should be extracted", bytes)
        assertTrue("Preview bytes should not be empty", bytes!!.isNotEmpty())
        // Verify PNG magic signature: 89 50 4E 47
        assertEquals(0x89.toByte(), bytes[0])
        assertEquals(0x50.toByte(), bytes[1])
        assertEquals(0x4E.toByte(), bytes[2])
        assertEquals(0x47.toByte(), bytes[3])
    }

    @Test
    fun `test extractEmbeddedXoppPreviewBytes on file without preview returns null`() {
        val noPreviewFile = tempFolder.newFile("no_preview.xopp")
        GZIPOutputStream(FileOutputStream(noPreviewFile)).use { gzip ->
            gzip.write("<?xml version=\"1.0\"?>\n<xournal creator=\"xournalpp\">\n<page width=\"100\" height=\"100\"/>\n</xournal>".toByteArray(Charsets.UTF_8))
        }

        val bytes = ThumbnailManager.extractEmbeddedXoppPreviewBytes(noPreviewFile)
        assertNull(bytes)
    }

    @Test
    fun `test extractEmbeddedXoppPreviewBytes on empty file returns null`() {
        val emptyFile = tempFolder.newFile("empty.xopp")
        val bytes = ThumbnailManager.extractEmbeddedXoppPreviewBytes(emptyFile)
        assertNull(bytes)
    }
}
