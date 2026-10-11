package dev.ilamparithi.aournalpp.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

object WindowTitleHelper {

    /**
     * Resolves dialog/prompt vector icon if [text] corresponds to a known dialog or prompt type,
     * or returns null if [text] is not a recognized dialog.
     */
    private fun matchDialogIcon(text: String): ImageVector? {
        val trimmed = text.removePrefix("*").removeSuffix("*").trim()
        if (trimmed.isEmpty()) return null
        val lower = trimmed.lowercase()

        return when {
            // Preferences & Settings
            lower == "preferences" || lower == "xournal++ preferences" || lower == "preferences - xournal++" ||
                lower == "settings" || lower == "configuration" -> Icons.Default.Settings

            // Page Background / Paper Format
            lower == "page background" || lower == "set page background" || lower == "paper format" -> Icons.Default.Layers

            // Fonts & Typography
            lower == "select font" || lower == "font selection" || lower == "choose font" ||
                lower.startsWith("select font") || lower.startsWith("choose font") ||
                lower.startsWith("font selection") || lower == "font" -> Icons.Default.FontDownload

            // Image Selection / Chooser
            lower == "choose image file" || lower == "choose image" || lower == "select image" ||
                lower == "insert image" || lower.startsWith("choose image") || lower.startsWith("select image") ||
                lower.startsWith("insert image") || lower == "picture" || lower == "image" -> Icons.Default.Image

            // Print
            lower == "print" || lower == "page setup" || lower.startsWith("print ") || lower == "print..." -> Icons.Default.Print

            // Color Selection & Palette
            lower == "select color" || lower == "color selection" || lower == "choose color" ||
                lower == "custom color" || lower.startsWith("select color") || lower.startsWith("choose color") ||
                lower.startsWith("color selection") || lower == "color" -> Icons.Default.Palette

            // Plugins & Extensions
            lower == "plugin manager" || lower == "manage plugins" || lower == "plugins" -> Icons.Default.Extension

            // PDF Documents & PDF Export & Annotate
            lower == "export as pdf" || lower == "export pdf" || lower == "export file" ||
                lower == "export as..." || lower == "export as" || lower == "export" ||
                lower.startsWith("export as pdf") || lower.startsWith("export pdf") ||
                lower.startsWith("export file") -> Icons.Default.PictureAsPdf

            lower == "annotate pdf file" || lower == "annotate pdf" || lower == "annotate" ||
                lower.startsWith("annotate pdf") -> Icons.Default.EditNote

            // File Open & Storage Browsing
            lower == "open document" || lower == "open file" || lower == "open" ||
                lower.startsWith("choose folder") || lower.startsWith("select folder") ||
                lower.startsWith("select destination folder") || lower == "choose folder" ||
                lower == "select folder" -> Icons.Default.FolderOpen

            // Save / Save As
            lower == "save as" || lower == "save file" || lower == "save document" || lower == "save" -> Icons.Default.Save

            // Help & About
            lower == "about xournal++" || lower == "about" || lower == "information" ||
                lower == "help" || lower == "help..." -> Icons.Default.Info

            // Warnings & Alerts
            lower == "warning" || lower.startsWith("warning:") || lower.startsWith("warning -") ||
                lower == "caution" || lower.startsWith("caution:") || lower == "alert" || lower.startsWith("alert:") -> Icons.Default.Warning

            // Errors & Failures
            lower == "error" || lower.startsWith("error:") || lower.startsWith("error -") ||
                lower.startsWith("error saving") || lower.startsWith("error loading") -> Icons.Default.Error

            // Questions & Confirmations
            lower == "question" || lower == "confirm" || lower == "confirmation" ||
                lower.startsWith("confirm ") ||
                (lower.startsWith("save changes") && lower.endsWith("?")) ||
                (lower.startsWith("do you want to save") && lower.endsWith("?")) -> Icons.AutoMirrored.Filled.Help

            else -> null
        }
    }

    /**
     * Resolves the appropriate Material 3 Vector icon according to the active window title or document state.
     * When [promptTitle] is provided, dialog prompt icon takes precedence.
     */
    fun resolveWindowIcon(title: String?, promptTitle: String? = null): ImageVector {
        if (!promptTitle.isNullOrBlank()) {
            matchDialogIcon(promptTitle)?.let { return it }
        }

        if (title.isNullOrBlank()) return Icons.Default.Description
        val clean = title.removePrefix("*").removeSuffix("*").trim()
        if (clean.isBlank()) return Icons.Default.Description

        // If the title contains a prompt separated by " - ", check if the suffix matches a known dialog
        val lastDashIdx = clean.lastIndexOf(" - ")
        if (lastDashIdx != -1) {
            val suffix = clean.substring(lastDashIdx + 3).trim()
            matchDialogIcon(suffix)?.let { return it }
        }

        // Direct check for standalone dialogs
        matchDialogIcon(clean)?.let { return it }

        // Documents: PDF files show PictureAsPdf icon
        if (clean.endsWith(".pdf", ignoreCase = true)) {
            return Icons.Default.PictureAsPdf
        }

        // Standard Note Documents (*.xopp, *.xoj, New Note, Unsaved Document, arbitrary names)
        return Icons.Default.Description
    }
}
