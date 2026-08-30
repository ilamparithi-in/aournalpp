package dev.ilamparithi.aournalpp.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class FormatUtilsTest {

    @Test
    fun `test formatFileSize formats byte boundaries with correct units`() {
        assertEquals("0 B", FormatUtils.formatFileSize(0L, Locale.US))
        assertEquals("0 B", FormatUtils.formatFileSize(-50L, Locale.US))
        assertEquals("500 B", FormatUtils.formatFileSize(500L, Locale.US))
        assertEquals("1 KB", FormatUtils.formatFileSize(1024L, Locale.US))
        assertEquals("1.5 KB", FormatUtils.formatFileSize(1536L, Locale.US))
        assertEquals("1 MB", FormatUtils.formatFileSize(1024L * 1024L, Locale.US))
        assertEquals("2.5 MB", FormatUtils.formatFileSize((2.5 * 1024 * 1024).toLong(), Locale.US))
        assertEquals("1 GB", FormatUtils.formatFileSize(1024L * 1024L * 1024L, Locale.US))
        assertEquals("2 TB", FormatUtils.formatFileSize(2L * 1024L * 1024L * 1024L * 1024L, Locale.US))
    }

    @Test
    fun `test formatFileSize respects locale decimal separators`() {
        val bytes = (1.5 * 1024 * 1024).toLong()
        val usFormatted = FormatUtils.formatFileSize(bytes, Locale.US)
        val deFormatted = FormatUtils.formatFileSize(bytes, Locale.GERMANY)

        assertEquals("1.5 MB", usFormatted)
        assertEquals("1,5 MB", deFormatted)
    }

    @Test
    fun `test formatPercentage formatting and precision`() {
        val usPercent = FormatUtils.formatPercentage(45.678, decimals = 1, locale = Locale.US)
        val dePercent = FormatUtils.formatPercentage(45.678, decimals = 1, locale = Locale.GERMANY)

        assertEquals("45.7%", usPercent)
        assertEquals("45,7%", dePercent)
    }

    @Test
    fun `test formatNumber formatting`() {
        val usNumber = FormatUtils.formatNumber(1234.56, maxDecimals = 2, minDecimals = 2, locale = Locale.US)
        val deNumber = FormatUtils.formatNumber(1234.56, maxDecimals = 2, minDecimals = 2, locale = Locale.GERMANY)

        assertEquals("1,234.56", usNumber)
        assertEquals("1.234,56", deNumber)
    }

    @Test
    fun `test formatDateMedium and formatDateTimeMedium output non-empty strings`() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.AUGUST, 30, 12, 0, 0)
        }
        val ms = cal.timeInMillis

        val usDate = FormatUtils.formatDateMedium(ms, Locale.US)
        val usDateTime = FormatUtils.formatDateTimeMedium(ms, Locale.US)

        assertTrue(usDate.isNotEmpty())
        assertTrue(usDateTime.isNotEmpty())
        assertTrue("Expected 2026 in date", usDate.contains("2026"))

        val invalidDate = FormatUtils.formatDateMedium(0L, Locale.US)
        assertEquals("", invalidDate)
    }

    @Test
    fun `test formatTime generates valid time string`() {
        val usTime = FormatUtils.formatTime(14, 30, Locale.US)
        val deTime = FormatUtils.formatTime(14, 30, Locale.GERMANY)

        assertTrue("US time should contain 2:30 or PM", usTime.contains("2:30") || usTime.contains("PM"))
        assertTrue("German time should contain 14:30", deTime.contains("14:30") || deTime.contains("14"))
    }
}
