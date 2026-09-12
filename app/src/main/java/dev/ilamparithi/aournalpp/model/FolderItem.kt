package dev.ilamparithi.aournalpp.model

import java.io.File

data class FolderItem(
    val file: File,
    val name: String,
    val colorHex: String? = null,
    val iconEmoji: String? = null,
    val iconType: String? = null,
    val isEmergencyFolder: Boolean = false,
    val isPinned: Boolean = false,
    val role: String? = null,
    val isExcludedFromRecents: Boolean = false,
    val itemCount: Int = 0,
    val lastModifiedMs: Long = 0L,
    val isHidden: Boolean = false
)
