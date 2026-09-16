package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.utils.FileTypeDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream

class FileTypeDetectorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test detect by extension`() {
        val pdfFile = tempFolder.newFile("sample.pdf")
        val xoppFile = tempFolder.newFile("sample.xopp")
        val xojFile = tempFolder.newFile("sample.xoj")

        val pdfResult = FileTypeDetector.detect(pdfFile)
        assertTrue(pdfResult.isSupportedNote)
        assertEquals("application/pdf", pdfResult.detectedMimeType)
        assertEquals(FileTypeDetector.NoteType.PDF, pdfResult.noteType)

        val xoppResult = FileTypeDetector.detect(xoppFile)
        assertTrue(xoppResult.isSupportedNote)
        assertEquals("application/x-xopp", xoppResult.detectedMimeType)
        assertEquals(FileTypeDetector.NoteType.XOPP, xoppResult.noteType)

        val xojResult = FileTypeDetector.detect(xojFile)
        assertTrue(xojResult.isSupportedNote)
        assertEquals("application/x-xoj", xojResult.detectedMimeType)
        assertEquals(FileTypeDetector.NoteType.XOJ, xojResult.noteType)
    }

    @Test
    fun `test detect PDF by magic bytes without extension`() {
        val unknownPdf = tempFolder.newFile("unknown_binary_file")
        FileOutputStream(unknownPdf).use { out ->
            out.write("%PDF-1.7\n".toByteArray(Charsets.US_ASCII))
            out.write("trailer << /Root ... >>".toByteArray(Charsets.US_ASCII))
        }

        val result = FileTypeDetector.detect(unknownPdf)
        assertTrue(result.isSupportedNote)
        assertEquals("application/pdf", result.detectedMimeType)
        assertEquals(FileTypeDetector.NoteType.PDF, result.noteType)
    }

    @Test
    fun `test detect XOPP by gzip content containing xournal tag`() {
        val xoppWithoutExt = tempFolder.newFile("xopp_without_extension")
        GZIPOutputStream(FileOutputStream(xoppWithoutExt)).use { gzip ->
            gzip.write("<?xml version=\"1.0\"?>\n<xournal creator=\"xournalpp\">\n</xournal>".toByteArray(Charsets.UTF_8))
        }

        val result = FileTypeDetector.detect(xoppWithoutExt)
        assertTrue(result.isSupportedNote)
        assertEquals("application/x-xopp", result.detectedMimeType)
        assertEquals(FileTypeDetector.NoteType.XOPP, result.noteType)
    }

    @Test
    fun `test detect arbitrary gzip archive as unsupported`() {
        val genericGzip = tempFolder.newFile("archive.tar.gz")
        GZIPOutputStream(FileOutputStream(genericGzip)).use { gzip ->
            gzip.write("some random tar archive data that does not have xournal XML".toByteArray(Charsets.UTF_8))
        }

        val result = FileTypeDetector.detect(genericGzip)
        assertFalse(result.isSupportedNote)
        assertTrue(result.detectedMimeType == "application/x-gzip" || result.detectedMimeType == "application/gzip")
        assertNull(result.noteType)
    }

    @Test
    fun `test detect PNG image as unsupported with proper image MIME`() {
        val pngFile = tempFolder.newFile("photo.png")
        FileOutputStream(pngFile).use { out ->
            // PNG signature: 89 50 4E 47 0D 0A 1A 0A
            out.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            out.write(ByteArray(32))
        }

        val result = FileTypeDetector.detect(pngFile)
        assertFalse(result.isSupportedNote)
        assertEquals("image/png", result.detectedMimeType)
        assertNull(result.noteType)
    }

    @Test
    fun `test detect full xopp note in tempFolder`() {
        val sample = tempFolder.newFile("sample_note.xopp")
        GZIPOutputStream(FileOutputStream(sample)).use { gzip ->
            gzip.write("<?xml version=\"1.0\" standalone=\"no\"?>\n<xournal creator=\"xournalpp 1.2.8\" fileversion=\"4\">\n<title>Sample</title>\n</xournal>".toByteArray(Charsets.UTF_8))
        }

        val result = FileTypeDetector.detect(sample)
        assertTrue(result.isSupportedNote)
        assertEquals("application/x-xopp", result.detectedMimeType)
        assertEquals(FileTypeDetector.NoteType.XOPP, result.noteType)
    }
}
