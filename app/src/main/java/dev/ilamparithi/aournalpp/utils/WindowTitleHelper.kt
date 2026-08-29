package dev.ilamparithi.aournalpp.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

object WindowTitleHelper {

    /**
     * Resolves the appropriate Material 3 Vector icon according to the active window title or document state.
     */
    fun resolveWindowIcon(title: String?): ImageVector {
        if (title.isNullOrBlank()) return Icons.Default.Description
        val clean = title.removePrefix("*").removeSuffix("*").trim()
        val lower = clean.lowercase()

        return when {
            // Preferences & Settings
            lower.contains("preferences") || lower.contains("settings") || lower.contains("configuration") -> Icons.Default.Settings

            // Page Background / Layers
            lower.contains("page background") || lower.contains("set page background") || lower.contains("background") -> Icons.Default.Layers

            // Fonts & Typography
            lower.contains("font") || lower.contains("select font") || lower.contains("font selection") -> Icons.Default.FontDownload

            // Color Selection & Palette
            lower.contains("color") || lower.contains("select color") || lower.contains("color selection") || lower.contains("choose color") -> Icons.Default.Palette

            // Plugins & Extensions
            lower.contains("plugin") || lower.contains("manage plugins") || lower.contains("plugin manager") -> Icons.Default.Extension

            // PDF Documents & PDF Export
            lower.contains("export as pdf") || lower.contains("export pdf") || lower.endsWith(".pdf") -> Icons.Default.PictureAsPdf

            // File Open & Storage Browsing
            lower.contains("open document") || lower.contains("open file") || lower.contains("choose folder") || lower.contains("select folder") || lower.startsWith("open") -> Icons.Default.FolderOpen

            // Save / Save As
            lower.contains("save as") || lower.contains("save file") || lower.contains("save document") || lower.startsWith("save") -> Icons.Default.Save

            // Help & About
            lower.contains("about") || lower.contains("help") || lower.contains("information") -> Icons.Default.Info

            // Warnings & Alerts
            lower.contains("warning") || lower.contains("caution") || lower.contains("alert") -> Icons.Default.Warning

            // Errors & Failures
            lower.contains("error") -> Icons.Default.Error

            // Questions & Confirmations
            lower.contains("question") || lower.contains("confirm") -> Icons.AutoMirrored.Filled.Help

            // Standard Note Documents (*.xopp, New Note, Unsaved Document)
            else -> Icons.Default.Description
        }
    }
}
