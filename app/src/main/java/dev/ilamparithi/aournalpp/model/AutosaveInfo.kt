package dev.ilamparithi.aournalpp.model

import dev.ilamparithi.aournalpp.utils.FormatUtils
import java.io.File
import kotlin.math.abs

/**
 * Encapsulates metadata and comparative metrics for an autosave file associated with a note.
 */
data class AutosaveInfo(
    val autosaveFile: File,
    val mainFile: File,
    val mainLastModifiedMs: Long,
    val autosaveLastModifiedMs: Long,
    val mainSizeBytes: Long,
    val autosaveSizeBytes: Long
) {
    val isAutosaveNewer: Boolean
        get() = autosaveLastModifiedMs > mainLastModifiedMs

    val mainModifiedFormatted: String
        get() = formatDate(mainLastModifiedMs)

    val autosaveModifiedFormatted: String
        get() = formatDate(autosaveLastModifiedMs)

    val mainSizeFormatted: String
        get() = formatSize(mainSizeBytes)

    val autosaveSizeFormatted: String
        get() = formatSize(autosaveSizeBytes)

    val timeDiffFormatted: String
        get() {
            val diffMs = abs(autosaveLastModifiedMs - mainLastModifiedMs)
            val diffSec = diffMs / 1000
            val diffMin = diffSec / 60
            val diffHours = diffMin / 60
            val diffDays = diffHours / 24

            val timeStr = when {
                diffDays > 0 -> "$diffDays day${if (diffDays > 1L) "s" else ""}"
                diffHours > 0 -> "$diffHours hr${if (diffHours > 1L) "s" else ""}"
                diffMin > 0 -> "$diffMin min"
                else -> "$diffSec sec"
            }

            return if (isAutosaveNewer) {
                "Autosave is $timeStr newer than saved note"
            } else {
                "Autosave is $timeStr older than saved note"
            }
        }

    private fun formatDate(ms: Long): String {
        return FormatUtils.formatDateTimeMedium(ms)
    }

    private fun formatSize(bytes: Long): String {
        return FormatUtils.formatFileSize(bytes)
    }
}
