package dev.ilamparithi.aournalpp.utils

import dev.ilamparithi.aournalpp.utils.xopp.XoppParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayInputStream

class XoppParserTest {

    @Test
    fun testParseCoordinates() {
        val raw = "10.5 20.25 30.75 -40.5 50.0 60 70.125"
        val result = XoppParser.parseCoordinates(raw)

        assertEquals(7, result.size)
        assertEquals(10.5f, result[0], 0.001f)
        assertEquals(20.25f, result[1], 0.001f)
        assertEquals(30.75f, result[2], 0.001f)
        assertEquals(-40.5f, result[3], 0.001f)
        assertEquals(50.0f, result[4], 0.001f)
        assertEquals(60.0f, result[5], 0.001f)
        assertEquals(70.125f, result[6], 0.001f)
    }

    @Test
    fun testParseColor() {
        // Hex 8-digit RGBA -> ARGB
        val color8 = XoppParser.parseXoppColor("#FF0000FF", 0)
        assertEquals(0xFFFF0000.toInt(), color8)

        // Hex 6-digit RGB -> ARGB
        val color6 = XoppParser.parseXoppColor("#00FF00", 0)
        assertEquals(0xFF00FF00.toInt(), color6)

        // Named color
        val colorNamed = XoppParser.parseXoppColor("blue", 0)
        assertEquals(0xFF3333CC.toInt(), colorNamed)
    }

    @Test
    fun testParseStrokeWidth() {
        assertEquals(1.41f, XoppParser.parseStrokeWidth(null), 0.001f)
        assertEquals(1.41f, XoppParser.parseStrokeWidth(""), 0.001f)
        assertEquals(2.5f, XoppParser.parseStrokeWidth("2.5"), 0.001f)
        assertEquals(5.0f, XoppParser.parseStrokeWidth("5.0 2.0 1.0"), 0.001f)
        assertEquals(80f, XoppParser.parseStrokeWidth("150.0"), 0.001f) // Clamped to 80f
        assertEquals(0.2f, XoppParser.parseStrokeWidth("0.01"), 0.001f) // Clamped to 0.2f
    }
}
