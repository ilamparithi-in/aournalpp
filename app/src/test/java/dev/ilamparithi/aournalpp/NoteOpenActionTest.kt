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

    @Test
    fun `test isPdf detection logic`() {
        assertEquals(true, dev.ilamparithi.aournalpp.utils.NoteOpenManager.isPdf(java.io.File("document.pdf")))
        assertEquals(true, dev.ilamparithi.aournalpp.utils.NoteOpenManager.isPdf(java.io.File("NOTE.PDF")))
        assertEquals(true, dev.ilamparithi.aournalpp.utils.NoteOpenManager.isPdf("archive.pdf"))
        assertEquals(false, dev.ilamparithi.aournalpp.utils.NoteOpenManager.isPdf(java.io.File("note.xopp")))
        assertEquals(false, dev.ilamparithi.aournalpp.utils.NoteOpenManager.isPdf(java.io.File("legacy.xoj")))
        assertEquals(false, dev.ilamparithi.aournalpp.utils.NoteOpenManager.isPdf(java.io.File("plain_text.txt")))
        assertEquals(false, dev.ilamparithi.aournalpp.utils.NoteOpenManager.isPdf(java.io.File("no_extension")))
    }

    @Test
    fun `test separate preference keys and defaults`() {
        assertEquals("pref_default_open_action_xopp", dev.ilamparithi.aournalpp.utils.NoteOpenManager.PREF_KEY_DEFAULT_OPEN_ACTION_XOPP)
        assertEquals("pref_default_open_action_pdf", dev.ilamparithi.aournalpp.utils.NoteOpenManager.PREF_KEY_DEFAULT_OPEN_ACTION_PDF)
        assertEquals("pref_default_open_action", dev.ilamparithi.aournalpp.utils.NoteOpenManager.PREF_KEY_DEFAULT_OPEN_ACTION)
    }

    @Test
    fun `test split preference resolution and isolation between PDF and XOPP`() {
        val mockContext = io.mockk.mockk<android.content.Context>(relaxed = true)
        val inMemoryPrefs = mutableMapOf<String, String>()

        val mockEditor = io.mockk.mockk<android.content.SharedPreferences.Editor>(relaxed = true)
        io.mockk.every { mockEditor.putString(any(), any()) } answers {
            val key = firstArg<String>()
            val value = secondArg<String>()
            inMemoryPrefs[key] = value
            mockEditor
        }
        io.mockk.every { mockEditor.apply() } returns Unit

        val mockPrefs = io.mockk.mockk<android.content.SharedPreferences>(relaxed = true)
        io.mockk.every { mockPrefs.getString(any(), any()) } answers {
            val key = firstArg<String>()
            val default = secondArg<String?>()
            inMemoryPrefs[key] ?: default
        }
        io.mockk.every { mockPrefs.edit() } returns mockEditor

        io.mockk.mockkObject(dev.ilamparithi.aournalpp.data.AppPreferences)
        io.mockk.every { dev.ilamparithi.aournalpp.data.AppPreferences.getGeneral(mockContext) } returns mockPrefs

        try {
            // Initial state: both should be ASK
            assertEquals(NoteOpenAction.ASK, dev.ilamparithi.aournalpp.utils.NoteOpenManager.getDefaultAction(mockContext, isPdf = false))
            assertEquals(NoteOpenAction.ASK, dev.ilamparithi.aournalpp.utils.NoteOpenManager.getDefaultAction(mockContext, isPdf = true))

            // Set XOPP to EDIT
            dev.ilamparithi.aournalpp.utils.NoteOpenManager.setDefaultAction(mockContext, isPdf = false, NoteOpenAction.EDIT)
            assertEquals(NoteOpenAction.EDIT, dev.ilamparithi.aournalpp.utils.NoteOpenManager.getDefaultAction(mockContext, isPdf = false))
            // PDF must remain unaffected (still ASK)
            assertEquals(NoteOpenAction.ASK, dev.ilamparithi.aournalpp.utils.NoteOpenManager.getDefaultAction(mockContext, isPdf = true))

            // Set PDF to VIEW
            dev.ilamparithi.aournalpp.utils.NoteOpenManager.setDefaultAction(mockContext, isPdf = true, NoteOpenAction.VIEW)
            assertEquals(NoteOpenAction.VIEW, dev.ilamparithi.aournalpp.utils.NoteOpenManager.getDefaultAction(mockContext, isPdf = true))
            // XOPP remains EDIT
            assertEquals(NoteOpenAction.EDIT, dev.ilamparithi.aournalpp.utils.NoteOpenManager.getDefaultAction(mockContext, isPdf = false))

            // File-based dispatch check
            val xoppFile = java.io.File("/tmp/math.xopp")
            val xojFile = java.io.File("/tmp/notes.xoj")
            val pdfFile = java.io.File("/tmp/manual.pdf")
            assertEquals(NoteOpenAction.EDIT, dev.ilamparithi.aournalpp.utils.NoteOpenManager.getDefaultAction(mockContext, xoppFile))
            assertEquals(NoteOpenAction.EDIT, dev.ilamparithi.aournalpp.utils.NoteOpenManager.getDefaultAction(mockContext, xojFile))
            assertEquals(NoteOpenAction.VIEW, dev.ilamparithi.aournalpp.utils.NoteOpenManager.getDefaultAction(mockContext, pdfFile))

            // Clear in-memory prefs and set legacy pref
            inMemoryPrefs.clear()
            inMemoryPrefs[dev.ilamparithi.aournalpp.utils.NoteOpenManager.PREF_KEY_DEFAULT_OPEN_ACTION] = "edit"

            // XOPP should fall back to legacy pref ("edit")
            assertEquals(NoteOpenAction.EDIT, dev.ilamparithi.aournalpp.utils.NoteOpenManager.getDefaultAction(mockContext, isPdf = false))
            // PDF must NOT inherit legacy pref (remains ASK)
            assertEquals(NoteOpenAction.ASK, dev.ilamparithi.aournalpp.utils.NoteOpenManager.getDefaultAction(mockContext, isPdf = true))
        } finally {
            io.mockk.unmockkObject(dev.ilamparithi.aournalpp.data.AppPreferences)
        }
    }
}
