package dev.ilamparithi.aournalpp.logging

import java.io.BufferedReader
import java.io.InputStreamReader

enum class LogLevel(val letter: Char) {
    VERBOSE('V'),
    DEBUG('D'),
    INFO('I'),
    WARN('W'),
    ERROR('E'),
    ASSERT('A');

    companion object {
        fun fromChar(c: Char): LogLevel = when (c.uppercaseChar()) {
            'V' -> VERBOSE
            'D' -> DEBUG
            'I' -> INFO
            'W' -> WARN
            'E' -> ERROR
            'A', 'F' -> ASSERT
            else -> DEBUG
        }
    }
}

data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val pid: String,
    val tid: String,
    val tag: String,
    val message: String,
    val raw: String,
    val isNew: Boolean = false
)

/**
 * Utility for reading, parsing, and filtering Android Logcat entries.
 */
object LogcatReader {

    // Regex 1: -v time format: "04-10 15:30:12.345 D/MyTag( 1234): Message text"
    private val TIME_FORMAT_REGEX = Regex(
        """^(\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3})\s+([VDIWEAF])/([^(]+)\(\s*(\d+)\):\s*(.*)$"""
    )

    // Regex 2: -v threadtime format: "04-10 15:30:12.345  1234  1240 D MyTag   : Message text"
    private val THREADTIME_REGEX = Regex(
        """^(\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEAF])\s+([^:]+):\s*(.*)$"""
    )

    // Regex 3: Simple level/tag format: "D/MyTag: Message text"
    private val SIMPLE_FORMAT_REGEX = Regex(
        """^([VDIWEAF])/([^:]+):\s*(.*)$"""
    )

    fun parseLogcatLine(line: String): LogEntry {
        val trimmed = line.trimEnd()
        val timeMatch = TIME_FORMAT_REGEX.matchEntire(trimmed)
        if (timeMatch != null) {
            val (time, lvlChar, tag, pid, msg) = timeMatch.destructured
            return LogEntry(
                timestamp = time,
                level = LogLevel.fromChar(lvlChar[0]),
                pid = pid,
                tid = "",
                tag = tag.trim(),
                message = msg,
                raw = trimmed
            )
        }

        val threadTimeMatch = THREADTIME_REGEX.matchEntire(trimmed)
        if (threadTimeMatch != null) {
            val (time, pid, tid, lvlChar, tag, msg) = threadTimeMatch.destructured
            return LogEntry(
                timestamp = time,
                level = LogLevel.fromChar(lvlChar[0]),
                pid = pid,
                tid = tid,
                tag = tag.trim(),
                message = msg,
                raw = trimmed
            )
        }

        val simpleMatch = SIMPLE_FORMAT_REGEX.matchEntire(trimmed)
        if (simpleMatch != null) {
            val (lvlChar, tag, msg) = simpleMatch.destructured
            return LogEntry(
                timestamp = "",
                level = LogLevel.fromChar(lvlChar[0]),
                pid = "",
                tid = "",
                tag = tag.trim(),
                message = msg,
                raw = trimmed
            )
        }

        return LogEntry(
            timestamp = "",
            level = LogLevel.DEBUG,
            pid = "",
            tid = "",
            tag = "Log",
            message = trimmed,
            raw = trimmed
        )
    }

    /**
     * Executes `logcat -d -v time` and returns the most recent [maxLines] parsed entries.
     */
    fun readLogcat(maxLines: Int = 1000): List<LogEntry> {
        val entries = mutableListOf<LogEntry>()
        try {
            val process = ProcessBuilder("logcat", "-d", "-v", "time")
                .redirectErrorStream(true)
                .start()

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.isNotBlank() && !line.startsWith("--------- beginning of")) {
                        entries.add(parseLogcatLine(line))
                    }
                    line = reader.readLine()
                }
            }
            process.waitFor()
        } catch (_: Exception) {
            // Execution failure or permission fallback
        }

        return if (entries.size > maxLines) {
            entries.subList(entries.size - maxLines, entries.size)
        } else {
            entries
        }
    }

    /**
     * Clears the logcat circular buffer by running `logcat -c`.
     */
    fun clearLogcat() {
        try {
            val process = ProcessBuilder("logcat", "-c").start()
            process.waitFor()
        } catch (_: Exception) {
            // Execution failure or permission fallback
        }
    }

    /**
     * Filters log entries by minimum level and optional case-insensitive search query.
     */
    fun filterLogs(
        entries: List<LogEntry>,
        minLevel: LogLevel? = null,
        query: String? = null
    ): List<LogEntry> {
        val cleanQuery = query?.trim()?.lowercase() ?: ""
        return entries.filter { entry ->
            val matchesLevel = minLevel == null || entry.level.ordinal >= minLevel.ordinal
            val matchesQuery = cleanQuery.isEmpty() ||
                    entry.tag.lowercase().contains(cleanQuery) ||
                    entry.message.lowercase().contains(cleanQuery) ||
                    entry.raw.lowercase().contains(cleanQuery)
            matchesLevel && matchesQuery
        }
    }

    /**
     * Filters log entries by allowed set of levels and optional search query.
     */
    fun filterLogsByLevels(
        entries: List<LogEntry>,
        levels: Set<LogLevel>,
        query: String? = null
    ): List<LogEntry> {
        val cleanQuery = query?.trim()?.lowercase() ?: ""
        return entries.filter { entry ->
            val matchesLevel = when (entry.level) {
                LogLevel.ASSERT -> LogLevel.ERROR in levels
                LogLevel.VERBOSE -> LogLevel.VERBOSE in levels || LogLevel.DEBUG in levels
                else -> entry.level in levels
            }
            val matchesQuery = cleanQuery.isEmpty() ||
                    entry.tag.lowercase().contains(cleanQuery) ||
                    entry.message.lowercase().contains(cleanQuery) ||
                    entry.raw.lowercase().contains(cleanQuery)
            matchesLevel && matchesQuery
        }
    }
}
