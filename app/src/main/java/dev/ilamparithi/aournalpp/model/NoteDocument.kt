package dev.ilamparithi.aournalpp.model

import dev.ilamparithi.aournalpp.utils.FormatUtils
import java.io.File
import java.util.Date
import kotlin.math.abs

enum class NoteFileType(val extension: String, val displayName: String, val a11yLabel: String) {
    XOPP("xopp", ".xopp", "Xournal++ note"),
    XOJ("xoj", ".xoj", "Legacy XOJ note"),
    PDF("pdf", ".pdf", "PDF document")
}

/**
 * Domain model representing a note document in the Document Hub.
 */
data class NoteDocument(
    val file: File,
    val title: String = file.nameWithoutExtension,
    val path: String = file.absolutePath,
    val lastModifiedMs: Long = file.lastModified(),
    val sizeBytes: Long = file.length(),
    val lastModifiedFormatted: String = FormatUtils.formatDateTimeMedium(lastModifiedMs),
    val sizeFormatted: String = FormatUtils.formatFileSize(sizeBytes),
    val autosaveInfo: AutosaveInfo? = null,
    val isHidden: Boolean = false,
    val isEmergencyRecovery: Boolean = false,
    val isPinned: Boolean = false,
    val folder: String = "",
    val folderColorHex: String? = null,
    val folderIconEmoji: String? = null,
    val folderIconType: String? = null,
    val tags: List<String> = emptyList(),
    val lastOpenedMs: Long? = null
) {
    val fileType: NoteFileType
        get() = when (file.extension.lowercase()) {
            "xoj" -> NoteFileType.XOJ
            "pdf" -> NoteFileType.PDF
            else -> NoteFileType.XOPP
        }

    val fuzzyLastModified: String = formatFuzzyTime(lastModifiedMs, lastModifiedFormatted)
    val fuzzyLastOpened: String? = lastOpenedMs?.let { formatFuzzyTime(it, null) }

    val fullFormattedDateTime: String = try {
        FormatUtils.formatDateTimeMedium(lastModifiedMs)
    } catch (_: Exception) {
        lastModifiedFormatted
    }

    val fullFormattedOpenedDateTime: String? = lastOpenedMs?.let { opened ->
        try {
            FormatUtils.formatDateTimeMedium(opened)
        } catch (_: Exception) {
            null
        }
    }
}

private fun formatFuzzyTime(timestampMs: Long, fallback: String?): String {
    return try {
        val now = System.currentTimeMillis()
        val diff = now - timestampMs
        if (diff in 0L..59_999L) {
            "Just now"
        } else {
            android.text.format.DateUtils.getRelativeTimeSpanString(
                timestampMs,
                now,
                android.text.format.DateUtils.MINUTE_IN_MILLIS,
                android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        }
    } catch (_: Exception) {
        fallback ?: ""
    }
}

