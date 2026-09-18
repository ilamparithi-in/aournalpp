package dev.ilamparithi.aournalpp.model

import dev.ilamparithi.aournalpp.utils.FormatUtils
import java.io.File
import java.util.Date
import kotlin.math.abs

enum class NoteFileType(val extension: String, val displayName: String, val a11yLabel: String) {
    XOPP("xopp", "XOPP", "Xournal++ note"),
    XOJ("xoj", "XOJ", "Legacy XOJ note"),
    PDF("pdf", "PDF", "PDF document")
}

/**
 * Domain model representing a note document in the Document Hub.
 */
data class NoteDocument(
    val file: File,
    val title: String = file.nameWithoutExtension,
    val path: String = file.absolutePath,
    val lastModifiedMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val autosaveInfo: AutosaveInfo? = null,
    val isHidden: Boolean = false,
    val isEmergencyRecovery: Boolean = false,
    val isPinned: Boolean = false,
    val folder: String = "",
    val folderColorHex: String? = null,
    val folderIconEmoji: String? = null,
    val folderIconType: String? = null,
    val tags: List<String> = emptyList(),
    val lastOpenedMs: Long? = null,
    private val explicitLastModifiedFormatted: String? = null,
    private val explicitSizeFormatted: String? = null
) {
    constructor(
        file: File,
        title: String = file.nameWithoutExtension,
        path: String = file.absolutePath,
        lastModifiedMs: Long = 0L,
        sizeBytes: Long = 0L,
        lastModifiedFormatted: String,
        sizeFormatted: String,
        autosaveInfo: AutosaveInfo? = null,
        isHidden: Boolean = false,
        isEmergencyRecovery: Boolean = false,
        isPinned: Boolean = false,
        folder: String = "",
        folderColorHex: String? = null,
        folderIconEmoji: String? = null,
        folderIconType: String? = null,
        tags: List<String> = emptyList(),
        lastOpenedMs: Long? = null
    ) : this(
        file = file,
        title = title,
        path = path,
        lastModifiedMs = lastModifiedMs,
        sizeBytes = sizeBytes,
        autosaveInfo = autosaveInfo,
        isHidden = isHidden,
        isEmergencyRecovery = isEmergencyRecovery,
        isPinned = isPinned,
        folder = folder,
        folderColorHex = folderColorHex,
        folderIconEmoji = folderIconEmoji,
        folderIconType = folderIconType,
        tags = tags,
        lastOpenedMs = lastOpenedMs,
        explicitLastModifiedFormatted = lastModifiedFormatted,
        explicitSizeFormatted = sizeFormatted
    )

    val lastModifiedFormatted: String
        get() = explicitLastModifiedFormatted ?: if (lastModifiedMs > 0L) FormatUtils.formatDateTimeMedium(lastModifiedMs) else ""

    val sizeFormatted: String
        get() = explicitSizeFormatted ?: if (sizeBytes > 0L) FormatUtils.formatFileSize(sizeBytes) else ""

    val fileType: NoteFileType
        get() = when (file.extension.lowercase()) {
            "xoj" -> NoteFileType.XOJ
            "pdf" -> NoteFileType.PDF
            else -> NoteFileType.XOPP
        }

    val fuzzyLastModified: String
        get() = if (lastModifiedMs > 0L) formatFuzzyTime(lastModifiedMs, lastModifiedFormatted) else ""

    val fuzzyLastOpened: String?
        get() = lastOpenedMs?.let { formatFuzzyTime(it, null) }

    val fullFormattedDateTime: String
        get() = if (lastModifiedMs > 0L) {
            try {
                FormatUtils.formatDateTimeMedium(lastModifiedMs)
            } catch (_: Exception) {
                lastModifiedFormatted
            }
        } else {
            ""
        }

    val fullFormattedOpenedDateTime: String?
        get() = lastOpenedMs?.let { opened ->
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

