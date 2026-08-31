package dev.ilamparithi.aournalpp.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileNameTemplateEngineTest {

    @Test
    fun `test default template generation with fallback`() {
        val evaluated = FileNameTemplateEngine.evaluate("", FileNameTemplateEngine.TemplateContext(fallbackName = "MyNote"))
        assertEquals("MyNote", evaluated)
    }

    @Test
    fun `test standard date and time placeholders`() {
        val now = Date()
        val template = "{year}_{month}_{day}_{hour}_{minute}_{second}"
        val evaluated = FileNameTemplateEngine.evaluate(template)

        val expectedYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(now)
        val expectedMonth = SimpleDateFormat("MM", Locale.getDefault()).format(now)
        val expectedDay = SimpleDateFormat("dd", Locale.getDefault()).format(now)
        assertTrue(evaluated.startsWith("${expectedYear}_${expectedMonth}_${expectedDay}_"))
    }

    @Test
    fun `test custom datetime pattern`() {
        val now = Date()
        val template = "Note_{datetime:yyyyMMdd_HHmm}"
        val evaluated = FileNameTemplateEngine.evaluate(template)
        val expected = "Note_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(now)
        assertEquals(expected, evaluated)
    }

    @Test
    fun `test file info placeholders`() {
        val dummyFile = File("/storage/emulated/0/Documents/Math101.xopp")
        val context = FileNameTemplateEngine.TemplateContext(existingFile = dummyFile)

        val evaluated = FileNameTemplateEngine.evaluate("{name}_exported.{ext}", context)
        assertEquals("Math101_exported.xopp", evaluated)

        val fullEvaluated = FileNameTemplateEngine.evaluate("Copy_of_{filename}", context)
        assertEquals("Copy_of_Math101.xopp", fullEvaluated)
    }

    @Test
    fun `test random alphanumeric placeholder`() {
        val evaluated4 = FileNameTemplateEngine.evaluate("Doc_{random}")
        assertTrue(evaluated4.startsWith("Doc_"))
        assertEquals(8, evaluated4.length) // "Doc_" (4) + 4 chars

        val evaluated8 = FileNameTemplateEngine.evaluate("Doc_{random:8}")
        assertTrue(evaluated8.startsWith("Doc_"))
        assertEquals(12, evaluated8.length) // "Doc_" (4) + 8 chars
    }

    @Test
    fun `test sanitize illegal characters`() {
        val raw = "Math / Science : Part * 1 ? <Final> | \"Draft\""
        val sanitized = FileNameTemplateEngine.sanitizeFileName(raw)
        assertEquals("Math _ Science _ Part _ 1 _ _Final_ _ _Draft_", sanitized)
    }
}
