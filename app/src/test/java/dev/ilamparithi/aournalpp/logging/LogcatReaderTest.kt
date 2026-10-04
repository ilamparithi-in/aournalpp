package dev.ilamparithi.aournalpp.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogcatReaderTest {

    @Test
    fun testParseTimeFormatLine() {
        val raw = "10-04 15:30:12.345 D/ProcessSupervisor( 1234): Starting WM with command: openbox"
        val entry = LogcatReader.parseLogcatLine(raw)

        assertEquals("10-04 15:30:12.345", entry.timestamp)
        assertEquals(LogLevel.DEBUG, entry.level)
        assertEquals("ProcessSupervisor", entry.tag)
        assertEquals("1234", entry.pid)
        assertEquals("Starting WM with command: openbox", entry.message)
    }

    @Test
    fun testParseThreadTimeFormatLine() {
        val raw = "10-04 15:30:12.345  1234  1240 E CrashHandler: Uncaught exception occurred"
        val entry = LogcatReader.parseLogcatLine(raw)

        assertEquals("10-04 15:30:12.345", entry.timestamp)
        assertEquals(LogLevel.ERROR, entry.level)
        assertEquals("CrashHandler", entry.tag)
        assertEquals("1234", entry.pid)
        assertEquals("1240", entry.tid)
        assertEquals("Uncaught exception occurred", entry.message)
    }

    @Test
    fun testParseSimpleFormatLine() {
        val raw = "W/CanvasActivity: Viewport resize event triggered"
        val entry = LogcatReader.parseLogcatLine(raw)

        assertEquals(LogLevel.WARN, entry.level)
        assertEquals("CanvasActivity", entry.tag)
        assertEquals("Viewport resize event triggered", entry.message)
    }

    @Test
    fun testFilterLogsByLevel() {
        val entries = listOf(
            LogEntry("1", LogLevel.VERBOSE, "", "", "Tag1", "Verbose msg", "V"),
            LogEntry("2", LogLevel.DEBUG, "", "", "Tag2", "Debug msg", "D"),
            LogEntry("3", LogLevel.INFO, "", "", "Tag3", "Info msg", "I"),
            LogEntry("4", LogLevel.WARN, "", "", "Tag4", "Warn msg", "W"),
            LogEntry("5", LogLevel.ERROR, "", "", "Tag5", "Error msg", "E")
        )

        // Filter WARN and above
        val warnAndAbove = LogcatReader.filterLogs(entries, minLevel = LogLevel.WARN)
        assertEquals(2, warnAndAbove.size)
        assertEquals(LogLevel.WARN, warnAndAbove[0].level)
        assertEquals(LogLevel.ERROR, warnAndAbove[1].level)

        // Filter INFO and above
        val infoAndAbove = LogcatReader.filterLogs(entries, minLevel = LogLevel.INFO)
        assertEquals(3, infoAndAbove.size)

        // No filter
        val all = LogcatReader.filterLogs(entries, minLevel = null)
        assertEquals(5, all.size)
    }

    @Test
    fun testFilterLogsBySearchQuery() {
        val entries = listOf(
            LogEntry("1", LogLevel.INFO, "", "", "ProcessSupervisor", "Starting WM openbox", ""),
            LogEntry("2", LogLevel.WARN, "", "", "CanvasActivity", "Touch rejected", ""),
            LogEntry("3", LogLevel.ERROR, "", "", "CrashHandler", "Uncaught exception in main", ""),
            LogEntry("4", LogLevel.DEBUG, "", "", "DocumentHub", "Loaded 12 notes", "")
        )

        val result1 = LogcatReader.filterLogs(entries, query = "touch")
        assertEquals(1, result1.size)
        assertEquals("CanvasActivity", result1[0].tag)

        val result2 = LogcatReader.filterLogs(entries, query = "processsupervisor")
        assertEquals(1, result2.size)
        assertEquals("ProcessSupervisor", result2[0].tag)

        val result3 = LogcatReader.filterLogs(entries, query = "nonexistent")
        assertTrue(result3.isEmpty())
    }
}
