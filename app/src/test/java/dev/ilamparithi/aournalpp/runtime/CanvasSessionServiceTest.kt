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

    @Test
    fun `formatNotificationText formats multiple active notes with plus count`() {
        val single = CanvasSessionService.formatNotificationText("Meeting_Notes.xopp", 1)
        assertEquals("Editing: Meeting_Notes.xopp", single)

        val twoNotes = CanvasSessionService.formatNotificationText("Meeting_Notes.xopp", 2)
        assertEquals("Editing: Meeting_Notes.xopp +1", twoNotes)

        val threeNotes = CanvasSessionService.formatNotificationText("Meeting_Notes.xopp", 3)
        assertEquals("Editing: Meeting_Notes.xopp +2", threeNotes)

        val blankWithTwoNotes = CanvasSessionService.formatNotificationText(null, 2)
        assertEquals("Note session active +1", blankWithTwoNotes)
    }

    @Test
    fun `formatSyncSubText returns null when total is zero or negative`() {
        assertEquals(null, CanvasSessionService.formatSyncSubText("Nextcloud", 0, 0))
        assertEquals(null, CanvasSessionService.formatSyncSubText(null, -1, -1))
    }

    @Test
    fun `formatSyncSubText formats service name and progress numbers correctly`() {
        val withService = CanvasSessionService.formatSyncSubText("Nextcloud", 2, 5)
        assertEquals("Syncing Nextcloud (2/5)", withService)

        val withoutService = CanvasSessionService.formatSyncSubText(null, 2, 5)
        assertEquals("Syncing (2/5)", withoutService)

        val withBlankService = CanvasSessionService.formatSyncSubText("   ", 3, 10)
        assertEquals("Syncing (3/10)", withBlankService)
    }
}
