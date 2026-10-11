package dev.ilamparithi.aournalpp.data

import android.content.Context
import dev.ilamparithi.aournalpp.testutils.TestSharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPreferencesTest {

    @Test
    fun `test PREFS_GENERAL is aournal_prefs`() {
        assertEquals("aournal_prefs", AppPreferences.PREFS_GENERAL)
    }

    @Test
    fun `test migrateIfNeeded migrates all legacy keys from aournalpp_general to aournal_prefs`() {
        val legacyPrefs = TestSharedPreferences()
        legacyPrefs.putString("pref_ui_scale", "1.5")
        legacyPrefs.putBoolean("pref_auto_show_ime_on_focus", false)
        legacyPrefs.putBoolean("pref_intelligent_emergency_recovery", false)
        legacyPrefs.putInt("test_int", 42)
        legacyPrefs.putFloat("test_float", 3.14f)
        legacyPrefs.putLong("test_long", 1000L)
        legacyPrefs.putStringSet("test_set", mutableSetOf("a", "b"))

        val targetPrefs = TestSharedPreferences()
        val mockContext = mockk<Context>()
        every { mockContext.getSharedPreferences("aournalpp_general", Context.MODE_PRIVATE) } returns legacyPrefs
        every { mockContext.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE) } returns targetPrefs

        AppPreferences.migrateIfNeeded(mockContext)

        // Verify values were migrated to aournal_prefs
        assertEquals("1.5", targetPrefs.getString("pref_ui_scale", null))
        assertFalse(targetPrefs.getBoolean("pref_auto_show_ime_on_focus", true))
        assertFalse(targetPrefs.getBoolean("pref_intelligent_emergency_recovery", true))
        assertEquals(42, targetPrefs.getInt("test_int", 0))
        assertEquals(3.14f, targetPrefs.getFloat("test_float", 0f), 0.001f)
        assertEquals(1000L, targetPrefs.getLong("test_long", 0L))
        assertEquals(setOf("a", "b"), targetPrefs.getStringSet("test_set", null))

        // Verify legacy was cleared
        assertTrue(legacyPrefs.all.isEmpty())
    }
}
