package dev.ilamparithi.aournalpp.runtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CrossProcessLockTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test acquire and release lock`() {
        val lockFile = File(tempFolder.root, "test.lock")

        assertFalse(CrossProcessLock.isLocked(lockFile))

        val lock = CrossProcessLock.tryAcquire(lockFile)
        assertNotNull(lock)
        assertTrue(CrossProcessLock.isLocked(lockFile))

        lock?.release()
        assertFalse(CrossProcessLock.isLocked(lockFile))
    }

    @Test
    fun `test mutual exclusion prevents double acquisition`() {
        val lockFile = File(tempFolder.root, "exclusive.lock")

        val firstLock = CrossProcessLock.tryAcquire(lockFile)
        assertNotNull(firstLock)

        // Second acquisition attempt should fail immediately
        val secondLock = CrossProcessLock.tryAcquire(lockFile)
        assertNull("Second lock acquisition should return null while held", secondLock)

        firstLock?.close()

        // Now acquisition should succeed again
        val thirdLock = CrossProcessLock.tryAcquire(lockFile)
        assertNotNull("Lock acquisition should succeed after release", thirdLock)
        thirdLock?.close()
    }
}
