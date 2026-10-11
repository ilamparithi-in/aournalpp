package dev.ilamparithi.aournalpp

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
        assertEquals(Icons.Default.PictureAsPdf, WindowTitleHelper.resolveWindowIcon("Export File"))
        assertEquals(Icons.Default.PictureAsPdf, WindowTitleHelper.resolveWindowIcon("Lecture.pdf"))
        assertEquals(Icons.Default.FolderOpen, WindowTitleHelper.resolveWindowIcon("Open Document"))
        assertEquals(Icons.Default.Save, WindowTitleHelper.resolveWindowIcon("Save As"))
        assertEquals(Icons.Default.Info, WindowTitleHelper.resolveWindowIcon("About Xournal++"))
        assertEquals(Icons.Default.Warning, WindowTitleHelper.resolveWindowIcon("Warning: Low Disk Space"))
        assertEquals(Icons.Default.Error, WindowTitleHelper.resolveWindowIcon("Error saving file"))
        assertEquals(Icons.AutoMirrored.Filled.Help, WindowTitleHelper.resolveWindowIcon("Question"))
        assertEquals(Icons.Default.Image, WindowTitleHelper.resolveWindowIcon("Choose image file"))
        assertEquals(Icons.Default.Image, WindowTitleHelper.resolveWindowIcon("MyNote - Choose image file"))
        assertEquals(Icons.Default.Print, WindowTitleHelper.resolveWindowIcon("Print"))
        assertEquals(Icons.Default.Print, WindowTitleHelper.resolveWindowIcon("MyNote - Print"))
        assertEquals(Icons.Default.EditNote, WindowTitleHelper.resolveWindowIcon("Annotate Pdf file"))
        assertEquals(Icons.Default.EditNote, WindowTitleHelper.resolveWindowIcon("MyNote - Annotate Pdf file"))
        assertEquals(Icons.Default.PictureAsPdf, WindowTitleHelper.resolveWindowIcon("MyNote - Export File"))
        assertEquals(Icons.Default.FontDownload, WindowTitleHelper.resolveWindowIcon("MyNote - Select Font"))
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

    @Test
    fun testNotesContainingDialogKeywordsRetainDescriptionIcon() {
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("13. U2 Question Clarification"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("*13. U2 Question Clarification"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("13. U2 Question Clarification.xopp"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("13 - U2 Question Clarification"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("Confirmation Bias"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("Error Analysis"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("Early Warning Signs"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("Printmaking 101"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("Color Theory"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("Favorite Fonts"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("Saving Money"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("Open Discussion"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("About History"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("Camera Settings"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("Historical Background"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("Image Processing"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("Plugin Architecture"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("Export Economics"))
    }

    @Test
    fun testNotesWithDialogPromptsResolvePromptIcon() {
        assertEquals(Icons.AutoMirrored.Filled.Help, WindowTitleHelper.resolveWindowIcon("13. U2 Question Clarification - Question"))
        assertEquals(Icons.Default.Save, WindowTitleHelper.resolveWindowIcon("13. U2 Question Clarification - Save As"))
        assertEquals(Icons.Default.FontDownload, WindowTitleHelper.resolveWindowIcon("13. U2 Question Clarification - Select Font"))
        assertEquals(Icons.Default.Print, WindowTitleHelper.resolveWindowIcon("13. U2 Question Clarification - Print"))
        assertEquals(Icons.Default.Image, WindowTitleHelper.resolveWindowIcon("13. U2 Question Clarification - Choose image file"))
    }

    @Test
    fun testExplicitPromptTitleParameter() {
        assertEquals(Icons.AutoMirrored.Filled.Help, WindowTitleHelper.resolveWindowIcon("13. U2 Question Clarification", "Question"))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("13. U2 Question Clarification", null))
        assertEquals(Icons.Default.Description, WindowTitleHelper.resolveWindowIcon("13. U2 Question Clarification", ""))
        assertEquals(Icons.Default.Save, WindowTitleHelper.resolveWindowIcon("13. U2 Question Clarification", "Save As"))
    }
}
