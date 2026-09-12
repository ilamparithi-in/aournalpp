package dev.ilamparithi.aournalpp.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FloatingToolbarLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testFloatingToolbarLayoutRendersBothMainAndTrailingContents() {
        var undoClicked = false
        var closeClicked = false

        composeTestRule.setContent {
            FloatingToolbarLayout(
                mainContent = {
                    Button(onClick = { undoClicked = true }) {
                        Text("Undo")
                    }
                    Button(onClick = {}) {
                        Text("Redo")
                    }
                },
                trailingContent = {
                    Button(onClick = { closeClicked = true }) {
                        Text("Close")
                    }
                }
            )
        }

        composeTestRule.onNodeWithText("Undo")
            .assertIsDisplayed()
            .performClick()
        assertTrue(undoClicked)

        composeTestRule.onNodeWithText("Redo")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Close")
            .assertIsDisplayed()
            .performClick()
        assertTrue(closeClicked)
    }
}
