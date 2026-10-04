package dev.ilamparithi.aournalpp.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CrashLogStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storageDir: File
    private lateinit var storage: CrashLogStorage

    private val dummyMetadata = DeviceMetadata(
        manufacturer = "TestBrand",
        model = "TestModel",
        device = "testDevice",
        androidVersion = "14",
        sdkInt = 34,
        supportedAbis = listOf("arm64-v8a"),
        appVersionName = "1.0.0",
        appVersionCode = 1L
    )

    @Before
    fun setUp() {
        storageDir = tempFolder.newFolder("crashes")
        storage = CrashLogStorage(storageDir)
    }

    private fun createDummyReport(id: String, timestamp: Long): CrashReport {
        return CrashReport(
            id = id,
            timestamp = timestamp,
            type = CrashType.JVM_EXCEPTION,
            processName = "dev.ilamparithi.aournalpp",
            threadName = "main",
            errorClass = "java.lang.RuntimeException",
            errorMessage = "Crash $id",
            stackTrace = "StackTrace $id",
            deviceMetadata = dummyMetadata
        )
    }

    @Test
    fun testSaveAndRetrieveSingleCrashReport() {
        val report = createDummyReport("crash_test_1", 1000L)
        val file = storage.saveCrashReport(report)

        assertTrue(file.exists())
        assertEquals(1, storage.getCrashCount())

        val retrieved = storage.getCrashReport("crash_test_1")
        assertNotNull(retrieved)
        assertEquals("crash_test_1", retrieved?.id)
        assertEquals(1000L, retrieved?.timestamp)
        assertEquals("Crash crash_test_1", retrieved?.errorMessage)
    }

    @Test
    fun testGetAllReportsSortedDescending() {
        storage.saveCrashReport(createDummyReport("crash_old", 1000L))
        storage.saveCrashReport(createDummyReport("crash_new", 3000L))
        storage.saveCrashReport(createDummyReport("crash_mid", 2000L))

        val all = storage.getAllCrashReports()
        assertEquals(3, all.size)
        assertEquals("crash_new", all[0].id)
        assertEquals("crash_mid", all[1].id)
        assertEquals("crash_old", all[2].id)
    }

    @Test
    fun testDeleteCrashReport() {
        storage.saveCrashReport(createDummyReport("crash_to_del", 1000L))
        assertEquals(1, storage.getCrashCount())

        val deleted = storage.deleteCrashReport("crash_to_del")
        assertTrue(deleted)
        assertEquals(0, storage.getCrashCount())
        assertNull(storage.getCrashReport("crash_to_del"))
    }

    @Test
    fun testClearAllReports() {
        storage.saveCrashReport(createDummyReport("crash_1", 1000L))
        storage.saveCrashReport(createDummyReport("crash_2", 2000L))
        storage.saveCrashReport(createDummyReport("crash_3", 3000L))
        assertEquals(3, storage.getCrashCount())

        val clearedCount = storage.clearAllCrashReports()
        assertEquals(3, clearedCount)
        assertEquals(0, storage.getCrashCount())
        assertTrue(storage.getAllCrashReports().isEmpty())
    }

    @Test
    fun testFifoAutoRotationPrunesOldestWhenExceedingLimit() {
        // Save 25 reports with simulated timestamps & modified times
        for (i in 1..25) {
            val report = createDummyReport("crash_$i", i * 1000L)
            val file = storage.saveCrashReport(report)
            // Ensure unique lastModified times for deterministic sorting
            file.setLastModified(i * 1000L)
        }

        // Storage limit is MAX_CRASH_REPORTS = 20
        val count = storage.getCrashCount()
        assertTrue("Storage should not exceed 20 reports, actual: $count", count <= 20)
        assertEquals(20, count)

        // Reports 1..5 should have been pruned, report 25 should still exist
        assertNotNull(storage.getCrashReport("crash_25"))
        assertNotNull(storage.getCrashReport("crash_20"))
    }

    @Test
    fun testPathTraversalDefensePreventsEscapingStorageDir() {
        // Attempting to access parent path with traversal should not escape
        val report = storage.getCrashReport("../../secret")
        assertNull(report)

        val deleted = storage.deleteCrashReport("../escape")
        assertFalse(deleted)
    }
}
