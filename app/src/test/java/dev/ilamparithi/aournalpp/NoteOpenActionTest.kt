package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.utils.NoteOpenAction
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteOpenActionTest {

    @Test
    fun `test default note open action parsing from raw strings`() {
        assertEquals(NoteOpenAction.ASK, NoteOpenAction.fromValue("ask"))
        assertEquals(NoteOpenAction.EDIT, NoteOpenAction.fromValue("edit"))
        assertEquals(NoteOpenAction.VIEW, NoteOpenAction.fromValue("view"))
    }

    @Test
    fun `test fallback on unknown or null action string`() {
        assertEquals(NoteOpenAction.ASK, NoteOpenAction.fromValue(null))
        assertEquals(NoteOpenAction.ASK, NoteOpenAction.fromValue(""))
        assertEquals(NoteOpenAction.ASK, NoteOpenAction.fromValue("unknown_action"))
    }

    @Test
    fun `test action values and display names`() {
        assertEquals("ask", NoteOpenAction.ASK.value)
        assertEquals("Ask every time", NoteOpenAction.ASK.displayName)

        assertEquals("edit", NoteOpenAction.EDIT.value)
        assertEquals("Edit in Xournal++", NoteOpenAction.EDIT.displayName)

        assertEquals("view", NoteOpenAction.VIEW.value)
        assertEquals("View as PDF", NoteOpenAction.VIEW.displayName)
    }
}
