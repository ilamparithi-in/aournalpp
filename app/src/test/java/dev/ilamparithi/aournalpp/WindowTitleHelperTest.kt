package dev.ilamparithi.aournalpp

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
import dev.ilamparithi.aournalpp.utils.WindowTitleHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class WindowTitleHelperTest {

    @Test
    fun testPreferencesWindowIcon() {
        assertEquals(Icons.Default.Settings, WindowTitleHelper.resolveWindowIcon("Preferences"))
        assertEquals(Icons.Default.Settings, WindowTitleHelper.resolveWindowIcon("Xournal++ Preferences"))
        assertEquals(Icons.Default.Settings, WindowTitleHelper.resolveWindowIcon("Settings"))
    }

    @Test
    fun testDialogWindowIcons() {
        assertEquals(Icons.Default.Layers, WindowTitleHelper.resolveWindowIcon("Page Background"))
        assertEquals(Icons.Default.FontDownload, WindowTitleHelper.resolveWindowIcon("Select Font"))
        assertEquals(Icons.Default.Palette, WindowTitleHelper.resolveWindowIcon("Select Color"))
        assertEquals(Icons.Default.Extension, WindowTitleHelper.resolveWindowIcon("Plugin Manager"))
        assertEquals(Icons.Default.PictureAsPdf, WindowTitleHelper.resolveWindowIcon("Export as PDF"))
        assertEquals(Icons.Default.PictureAsPdf, WindowTitleHelper.resolveWindowIcon("Lecture.pdf"))
        assertEquals(Icons.Default.FolderOpen, WindowTitleHelper.resolveWindowIcon("Open Document"))
        assertEquals(Icons.Default.Save, WindowTitleHelper.resolveWindowIcon("Save As"))
        assertEquals(Icons.Default.Info, WindowTitleHelper.resolveWindowIcon("About Xournal++"))
        assertEquals(Icons.Default.Warning, WindowTitleHelper.resolveWindowIcon("Warning: Low Disk Space"))
        assertEquals(Icons.Default.Error, WindowTitleHelper.resolveWindowIcon("Error saving file"))
        assertEquals(Icons.AutoMirrored.Filled.Help, WindowTitleHelper.resolveWindowIcon("Question"))
    }

    @Test
    fun testNoteDocumentWindowIcons() {
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("MyNotes.xopp"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("*MyNotes.xopp"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("New Note"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("Unsaved Document"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon(null))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon(""))
    }
}
