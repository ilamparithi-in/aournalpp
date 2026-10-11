package dev.ilamparithi.aournalpp.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NoteOpenManagerTest {

    @Test
    fun `tier 1 emergency save takes precedence over all other conditions`() {
        val result = NoteOpenManager.resolveNoteOpenPrecedence(
            hasEmergencySave = true,
            hasActiveMatch = true,
            hasAutosave = true,
            defaultAction = NoteOpenAction.EDIT
        )
        assertEquals(NoteOpenPrecedenceResult.EMERGENCY_SAVE_DEFERRED, result)
    }

    @Test
    fun `tier 2 active session match takes precedence when no emergency save`() {
        val result = NoteOpenManager.resolveNoteOpenPrecedence(
            hasEmergencySave = false,
            hasActiveMatch = true,
            hasAutosave = true,
            defaultAction = NoteOpenAction.EDIT
        )
        assertEquals(NoteOpenPrecedenceResult.ACTIVE_SESSION_MATCH, result)
    }

    @Test
    fun `tier 3 autosave resolution takes precedence over default action preferences`() {
        val result = NoteOpenManager.resolveNoteOpenPrecedence(
            hasEmergencySave = false,
            hasActiveMatch = false,
            hasAutosave = true,
            defaultAction = NoteOpenAction.EDIT
        )
        assertEquals(NoteOpenPrecedenceResult.AUTOSAVE_FOUND, result)
    }

    @Test
    fun `tier 4 dispatches edit when default action is EDIT and no higher tier triggers`() {
        val result = NoteOpenManager.resolveNoteOpenPrecedence(
            hasEmergencySave = false,
            hasActiveMatch = false,
            hasAutosave = false,
            defaultAction = NoteOpenAction.EDIT
        )
        assertEquals(NoteOpenPrecedenceResult.ACTION_EDIT, result)
    }

    @Test
    fun `tier 4 dispatches view when default action is VIEW and no higher tier triggers`() {
        val result = NoteOpenManager.resolveNoteOpenPrecedence(
            hasEmergencySave = false,
            hasActiveMatch = false,
            hasAutosave = false,
            defaultAction = NoteOpenAction.VIEW
        )
        assertEquals(NoteOpenPrecedenceResult.ACTION_VIEW, result)
    }

    @Test
    fun `tier 4 prompts user when default action is ASK and no higher tier triggers`() {
        val result = NoteOpenManager.resolveNoteOpenPrecedence(
            hasEmergencySave = false,
            hasActiveMatch = false,
            hasAutosave = false,
            defaultAction = NoteOpenAction.ASK
        )
        assertEquals(NoteOpenPrecedenceResult.ACTION_ASK, result)
    }

    @Test
    fun `note open action parser returns expected enum values with safe fallbacks`() {
        assertEquals(NoteOpenAction.EDIT, NoteOpenAction.fromValue("edit"))
        assertEquals(NoteOpenAction.VIEW, NoteOpenAction.fromValue("view"))
        assertEquals(NoteOpenAction.ASK, NoteOpenAction.fromValue("ask"))
        assertEquals(NoteOpenAction.ASK, NoteOpenAction.fromValue("unknown"))
        assertEquals(NoteOpenAction.ASK, NoteOpenAction.fromValue(null))
    }

    @Test
    fun `pdf detection correctly recognizes pdf extensions case-insensitively`() {
        assertTrue(NoteOpenManager.isPdf(File("notes.pdf")))
        assertTrue(NoteOpenManager.isPdf(File("notes.PDF")))
        assertTrue(NoteOpenManager.isPdf("report.pdf"))
        assertTrue(NoteOpenManager.isPdf("report.PdF"))

        assertFalse(NoteOpenManager.isPdf(File("notes.xopp")))
        assertFalse(NoteOpenManager.isPdf(File("notes.xoj")))
        assertFalse(NoteOpenManager.isPdf("notes.xopp"))
    }
}
