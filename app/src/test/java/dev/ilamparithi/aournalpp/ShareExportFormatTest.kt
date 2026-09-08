package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.data.DocumentRepository.ShareExportFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ShareExportFormatTest {

    @Test
    fun `test share export format enum values`() {
        assertEquals(3, ShareExportFormat.values().size)
        assertTrue(ShareExportFormat.values().contains(ShareExportFormat.PDF))
        assertTrue(ShareExportFormat.values().contains(ShareExportFormat.XOPP))
        assertTrue(ShareExportFormat.values().contains(ShareExportFormat.ORIGINAL))
    }

    @Test
    fun `test single note format feasibility rules`() {
        // Rule: PDF files cannot be exported as XOPP
        val pdfFile = File("/path/to/document.pdf")
        val isPdf = pdfFile.extension.equals("pdf", ignoreCase = true)
        val isPdfXoj = pdfFile.extension.equals("xoj", ignoreCase = true)
        val canPdfExportAsXopp = !isPdf && !isPdfXoj
        assertFalse(canPdfExportAsXopp)

        // Rule: XOJ files cannot be converted to XOPP headlessly, and XOJ is never an export format
        val xojFile = File("/path/to/legacy.xoj")
        val isXoj = xojFile.extension.equals("xoj", ignoreCase = true)
        val canXojExportAsXopp = !xojFile.extension.equals("pdf", ignoreCase = true) && !isXoj
        assertFalse(canXojExportAsXopp)

        // Rule: XOPP files can be exported as PDF or XOPP
        val xoppFile = File("/path/to/note.xopp")
        val canXoppExportAsXopp = !xoppFile.extension.equals("pdf", ignoreCase = true) &&
                !xoppFile.extension.equals("xoj", ignoreCase = true)
        assertTrue(canXoppExportAsXopp)
    }

    @Test
    fun `test batch note format rules`() {
        val pureXopp = listOf(File("1.xopp"), File("2.xopp"), File("3.xopp"))
        val allXoppPure = pureXopp.all { it.extension.equals("xopp", ignoreCase = true) }
        assertTrue(allXoppPure)

        val mixedWithPdf = listOf(File("1.xopp"), File("2.pdf"))
        val allXoppMixedPdf = mixedWithPdf.all { it.extension.equals("xopp", ignoreCase = true) }
        assertFalse(allXoppMixedPdf)

        val mixedWithXoj = listOf(File("1.xopp"), File("2.xoj"))
        val allXoppMixedXoj = mixedWithXoj.all { it.extension.equals("xopp", ignoreCase = true) }
        assertFalse(allXoppMixedXoj)

        // Mixed formats detection
        val distinctExts = listOf("xopp", "pdf").distinct()
        assertTrue(distinctExts.size > 1)
    }

    @Test
    fun `test dynamic extension resolution`() {
        fun resolveExtension(format: ShareExportFormat, sourceExt: String): String {
            return when (format) {
                ShareExportFormat.PDF -> ".pdf"
                ShareExportFormat.XOPP -> ".xopp"
                ShareExportFormat.ORIGINAL -> if (sourceExt.startsWith(".")) sourceExt else ".$sourceExt"
            }
        }

        assertEquals(".pdf", resolveExtension(ShareExportFormat.PDF, "xopp"))
        assertEquals(".pdf", resolveExtension(ShareExportFormat.PDF, "pdf"))
        assertEquals(".xopp", resolveExtension(ShareExportFormat.XOPP, "xopp"))
        assertEquals(".xoj", resolveExtension(ShareExportFormat.ORIGINAL, "xoj"))
        assertEquals(".pdf", resolveExtension(ShareExportFormat.ORIGINAL, "pdf"))
    }
}
