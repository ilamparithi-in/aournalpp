package dev.ilamparithi.aournalpp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencySaveRecoveryTest {

    private class RapidTapDetector(
        private val thresholdCount: Int = 3,
        private val windowMs: Long = 2000L,
        private val isEnabled: () -> Boolean = { true }
    ) {
        val timestamps = mutableListOf<Long>()

        fun registerTap(now: Long): Boolean {
            timestamps.add(now)
            timestamps.removeAll { now - it > windowMs }
            if (isEnabled() && timestamps.size >= thresholdCount) {
                timestamps.clear()
                return true
            }
            return false
        }
    }

    @Test
    fun `test rapid tap triggers force close on 3 taps within 2000ms`() {
        val detector = RapidTapDetector()
        val t0 = 10000L
        assertFalse(detector.registerTap(t0))
        assertFalse(detector.registerTap(t0 + 500))
        assertTrue(detector.registerTap(t0 + 1200))
        assertTrue(detector.timestamps.isEmpty())
    }

    @Test
    fun `test rapid tap does not trigger if taps exceed 2000ms window`() {
        val detector = RapidTapDetector()
        val t0 = 10000L
        assertFalse(detector.registerTap(t0))
        // Next tap after window expired (2100ms later) -> first tap is purged
        assertFalse(detector.registerTap(t0 + 2100))
        // Third tap 1000ms after second -> count is only 2
        assertFalse(detector.registerTap(t0 + 3100))
    }

    @Test
    fun `test rapid tap ignored when triple tap preference is disabled`() {
        val detector = RapidTapDetector(isEnabled = { false })
        val t0 = 10000L
        assertFalse(detector.registerTap(t0))
        assertFalse(detector.registerTap(t0 + 200))
        assertFalse(detector.registerTap(t0 + 400))
    }

    @Test
    fun `test deferred note open sealed class equality and instances`() {
        val file1 = java.io.File("/path/to/test.xopp")
        val deferred1 = MainActivityEmergencyManager.DeferredNoteOpen.NoteFile(file1, isImport = false)
        val deferred2 = MainActivityEmergencyManager.DeferredNoteOpen.NoteFile(file1, isImport = false)
        val deferredImport = MainActivityEmergencyManager.DeferredNoteOpen.NoteFile(file1, isImport = true)

        org.junit.Assert.assertEquals(deferred1, deferred2)
        org.junit.Assert.assertNotEquals(deferred1, deferredImport)
    }
}
