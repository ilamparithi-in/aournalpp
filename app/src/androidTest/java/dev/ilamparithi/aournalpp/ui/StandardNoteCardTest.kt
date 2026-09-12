package dev.ilamparithi.aournalpp.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.runtime.ProcessSupervisor
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class StandardNoteCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var pdfExportManager: PdfExportManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val env = LinuxEnvironment(context)
        val supervisor = ProcessSupervisor(env)
        pdfExportManager = PdfExportManager(env, supervisor)
    }

    @Test
    fun testStandardNoteCardDisplaysTitleAndHandlesClick() {
        val note = NoteDocument(
            file = File("/test/Differential_Equations.xopp"),
            title = "Differential_Equations.xopp",
            path = "/test/Differential_Equations.xopp",
            lastModifiedMs = System.currentTimeMillis(),
            sizeBytes = 2048L,
            folder = "Math"
        )

        var clicked = false

        composeTestRule.setContent {
            StandardNoteCard(
                note = note,
                pdfExportManager = pdfExportManager,
                onClick = { clicked = true },
                enableFloatingPreview = false
            )
        }

        // Verify title is rendered
        composeTestRule.onNodeWithText("Differential_Equations.xopp")
            .assertIsDisplayed()
            .performClick()

        assertTrue("Note card click callback should be triggered", clicked)
    }

    @Test
    fun testStandardNoteCardRendersFolderBadge() {
        val note = NoteDocument(
            file = File("/test/Lecture1.xopp"),
            title = "Lecture1.xopp",
            path = "/test/Lecture1.xopp",
            lastModifiedMs = System.currentTimeMillis(),
            sizeBytes = 4096L,
            folder = "Physics"
        )

        composeTestRule.setContent {
            StandardNoteCard(
                note = note,
                pdfExportManager = pdfExportManager,
                onClick = {},
                enableFloatingPreview = false
            )
        }

        // Folder name should be visible in details pill
        composeTestRule.onNodeWithText("Physics")
            .assertIsDisplayed()
    }
}
