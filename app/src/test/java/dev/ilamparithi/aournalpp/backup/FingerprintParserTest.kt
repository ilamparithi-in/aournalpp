package dev.ilamparithi.aournalpp.backup

import dev.ilamparithi.aournalpp.backup.engine.FingerprintParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class FingerprintParserTest {

    @Test
    fun testParseCreatorDesktopDefault() {
        val (app, device) = FingerprintParser.parseCreatorString("xournalpp 1.3.7")
        assertEquals("Desktop Xournal++ 1.3.7", app)
        assertEquals("Desktop PC", device)
    }

    @Test
    fun testParseCreatorDesktopLegacy() {
        val (app, device) = FingerprintParser.parseCreatorString("Xournal++ 1.2.4")
        assertEquals("Desktop Xournal++ 1.2.4", app)
        assertEquals("Desktop PC", device)
    }

    @Test
    fun testParseCreatorDesktopWithCustomHostname() {
        val (app, device) = FingerprintParser.parseCreatorString("Xournal++ 1.3.7 (Device: ArchLinux-Workstation)")
        assertEquals("Desktop Xournal++ 1.3.7", app)
        assertEquals("ArchLinux-Workstation", device)
    }

    @Test
    fun testParseCreatorAournalWithDevice() {
        val (app, device) = FingerprintParser.parseCreatorString("Aournal++ 1.3.7 (Google Pixel Tablet)")
        assertEquals("Aournal++ 1.3.7", app)
        assertEquals("Google Pixel Tablet", device)
    }

    @Test
    fun testExtractFingerprintFromGzipBytes() {
        val xml = """
            <?xml version="1.0" standalone="no"?>
            <xournal creator="Xournal++ 1.3.7 (Device: Ubuntu-Laptop)" fileversion="4">
              <title>Document</title>
              <page width="612.00000000" height="792.00000000">
                <layer/>
              </page>
            </xournal>
        """.trimIndent()

        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { gos ->
            gos.write(xml.toByteArray(Charsets.UTF_8))
        }
        val fullCompressed = baos.toByteArray()

        // Pass only the first 256 bytes (simulating HTTP Range: bytes=0-2047)
        val slice = fullCompressed.take(256).toByteArray()
        val (app, device) = FingerprintParser.extractFingerprintFromGzipBytes(slice)

        assertNotNull(app)
        assertEquals("Desktop Xournal++ 1.3.7", app)
        assertEquals("Ubuntu-Laptop", device)
    }

    @Test
    fun testExtractFingerprintFromInvalidBytesReturnsNull() {
        val invalidBytes = byteArrayOf(0, 1, 2, 3)
        val (app, device) = FingerprintParser.extractFingerprintFromGzipBytes(invalidBytes)
        assertNull(app)
        assertNull(device)
    }
}
