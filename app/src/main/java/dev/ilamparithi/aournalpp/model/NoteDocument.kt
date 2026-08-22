package dev.ilamparithi.aournalpp.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class NoteFileType(val extension: String, val displayName: String) {
    XOPP("xopp", "XOPP Note"),
    XOJ("xoj", "Legacy XOJ"),
    PDF("pdf", "PDF Document")
}

data class FolderItem(
    val file: File,
    val name: String,
    val colorHex: String? = null,
    val itemCount: Int = 0,
    val lastModifiedMs: Long = 0L,
    val isHidden: Boolean = false
)

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
        val sdf = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
        return sdf.format(Date(ms))
    }

    private fun formatSize(bytes: Long): String {
        val sizeKb = (bytes + 1023) / 1024
        return if (sizeKb >= 1024) {
            String.format(Locale.getDefault(), "%.1f MB", sizeKb / 1024.0)
        } else {
            "$sizeKb KB"
        }
    }
}

/**
 * Domain model representing a note document in the Document Hub.
 */
data class NoteDocument(
    val file: File,
    val title: String,
    val path: String,
    val lastModifiedMs: Long,
    val sizeBytes: Long,
    val lastModifiedFormatted: String,
    val sizeFormatted: String,
    val autosaveInfo: AutosaveInfo? = null,
    val isHidden: Boolean = false,
    val isEmergencyRecovery: Boolean = false,
    val folder: String = "",
    val tags: List<String> = emptyList()
) {
    val fileType: NoteFileType
        get() = when (file.extension.lowercase()) {
            "xoj" -> NoteFileType.XOJ
            "pdf" -> NoteFileType.PDF
            else -> NoteFileType.XOPP
        }
}
