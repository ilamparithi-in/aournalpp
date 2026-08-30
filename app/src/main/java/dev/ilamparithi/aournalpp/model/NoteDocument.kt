package dev.ilamparithi.aournalpp.model

import dev.ilamparithi.aournalpp.utils.FormatUtils
import java.io.File
import java.util.Date
import kotlin.math.abs

enum class NoteFileType(val extension: String, val displayName: String) {
    XOPP("xopp", ".xopp"),
    XOJ("xoj", ".xoj"),
    PDF("pdf", ".pdf")
}

data class FolderItem(
    val file: File,
    val name: String,
    val colorHex: String? = null,
    val iconEmoji: String? = null,
    val iconType: String? = null,
    val isEmergencyFolder: Boolean = false,
    val isPinned: Boolean = false,
    val isVirtuallyPinned: Boolean = false,
    val role: String? = null,
    val isExcludedFromRecents: Boolean = false,
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
        return FormatUtils.formatDateTimeMedium(ms)
    }

    private fun formatSize(bytes: Long): String {
        return FormatUtils.formatFileSize(bytes)
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

    val fuzzyLastModified: String
        get() = try {
            val now = System.currentTimeMillis()
            val diff = now - lastModifiedMs
            if (diff in 0L..59_999L) {
                "Just now"
            } else {
                android.text.format.DateUtils.getRelativeTimeSpanString(
                    lastModifiedMs,
                    now,
                    android.text.format.DateUtils.MINUTE_IN_MILLIS,
                    android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
                ).toString()
            }
        } catch (e: Exception) {
            lastModifiedFormatted
        }

    val fuzzyLastOpened: String?
        get() {
            val opened = lastOpenedMs ?: return null
            return try {
                val now = System.currentTimeMillis()
                val diff = now - opened
                if (diff in 0L..59_999L) {
                    "Just now"
                } else {
                    android.text.format.DateUtils.getRelativeTimeSpanString(
                        opened,
                        now,
                        android.text.format.DateUtils.MINUTE_IN_MILLIS,
                        android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
                    ).toString()
                }
            } catch (e: Exception) {
                null
            }
        }

    val fullFormattedDateTime: String
        get() = try {
            FormatUtils.formatDateTimeMedium(lastModifiedMs)
        } catch (e: Exception) {
            lastModifiedFormatted
        }

    val fullFormattedOpenedDateTime: String?
        get() {
            val opened = lastOpenedMs ?: return null
            return try {
                FormatUtils.formatDateTimeMedium(opened)
            } catch (e: Exception) {
                null
            }
        }
}
