package dev.ilamparithi.aournalpp.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class CanvasSessionServiceTest {

    @Test
    fun `formatNotificationText returns default text when title is null`() {
        val result = CanvasSessionService.formatNotificationText(null)
        assertEquals("Note session active", result)
    }

    @Test
    fun `formatNotificationText returns default text when title is blank`() {
        val result = CanvasSessionService.formatNotificationText("   ")
        assertEquals("Note session active", result)
    }

    @Test
    fun `formatNotificationText formats note title correctly`() {
        val result = CanvasSessionService.formatNotificationText("Meeting_Notes.xopp")
        assertEquals("Editing: Meeting_Notes.xopp", result)
    }

    @Test
    fun `formatNotificationText trims surrounding whitespace from title`() {
        val result = CanvasSessionService.formatNotificationText("  Calculus_Homework.xopp  ")
        assertEquals("Editing: Calculus_Homework.xopp", result)
    }
}
