package dev.ilamparithi.aournalpp.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportTest {

    @Test
    fun testDeviceMetadataSerializationRoundtrip() {
        val metadata = DeviceMetadata(
            manufacturer = "Google",
            model = "Pixel Tablet",
            device = "tangorpro",
            androidVersion = "14",
            sdkInt = 34,
            supportedAbis = listOf("arm64-v8a"),
            appVersionName = "1.0.0-beta",
            appVersionCode = 42L,
            availableRamMb = 3500L,
            totalRamMb = 7800L
        )

        val json = metadata.toJson()
        val deserialized = DeviceMetadata.fromJson(json)

        assertEquals("Google", deserialized.manufacturer)
        assertEquals("Pixel Tablet", deserialized.model)
        assertEquals("tangorpro", deserialized.device)
        assertEquals("14", deserialized.androidVersion)
        assertEquals(34, deserialized.sdkInt)
        assertEquals(listOf("arm64-v8a"), deserialized.supportedAbis)
        assertEquals("1.0.0-beta", deserialized.appVersionName)
        assertEquals(42L, deserialized.appVersionCode)
        assertEquals(3500L, deserialized.availableRamMb)
        assertEquals(7800L, deserialized.totalRamMb)
    }

    @Test
    fun testJvmCrashReportSerializationRoundtrip() {
        val metadata = DeviceMetadata(
            manufacturer = "Samsung",
            model = "Galaxy Tab S9",
            device = "gts9",
            androidVersion = "13",
            sdkInt = 33,
            supportedAbis = listOf("arm64-v8a"),
            appVersionName = "1.2.0",
            appVersionCode = 120L
        )

        val report = CrashReport(
            id = "crash_20261004_120000_main",
            timestamp = 1728043200000L,
            type = CrashType.JVM_EXCEPTION,
            processName = "dev.ilamparithi.aournalpp",
            threadName = "main",
            errorClass = "java.lang.NullPointerException",
            errorMessage = "Attempt to read from null object",
            stackTrace = "java.lang.NullPointerException: Attempt to read from null object\n\tat com.example.Foo.bar(Foo.kt:42)",
            deviceMetadata = metadata,
            recentLogs = listOf("Line 1", "Line 2"),
            exitCode = null
        )

        val json = report.toJson()
        val deserialized = CrashReport.fromJson(json)

        assertEquals(report.id, deserialized.id)
        assertEquals(report.timestamp, deserialized.timestamp)
        assertEquals(CrashType.JVM_EXCEPTION, deserialized.type)
        assertEquals("dev.ilamparithi.aournalpp", deserialized.processName)
        assertEquals("main", deserialized.threadName)
        assertEquals("java.lang.NullPointerException", deserialized.errorClass)
        assertEquals("Attempt to read from null object", deserialized.errorMessage)
        assertEquals(report.stackTrace, deserialized.stackTrace)
        assertEquals(2, deserialized.recentLogs.size)
        assertEquals("Line 1", deserialized.recentLogs[0])
        assertEquals("Line 2", deserialized.recentLogs[1])
        assertNull(deserialized.exitCode)
    }

    @Test
    fun testNativeCrashReportSerializationRoundtrip() {
        val metadata = DeviceMetadata(
            manufacturer = "Lenovo",
            model = "Tab P12",
            device = "p12",
            androidVersion = "13",
            sdkInt = 33,
            supportedAbis = listOf("arm64-v8a"),
            appVersionName = "1.0.0",
            appVersionCode = 100L
        )

        val report = CrashReport(
            id = "crash_native_20261004_120500_xournalpp",
            timestamp = 1728043500000L,
            type = CrashType.NATIVE_PROCESS,
            processName = "dev.ilamparithi.aournalpp:canvas",
            threadName = null,
            errorClass = "NativeProcessCrash (xournalpp)",
            errorMessage = "Native binary 'xournalpp' terminated abnormally with exit code 139",
            stackTrace = "Segmentation fault (core dumped)",
            deviceMetadata = metadata,
            recentLogs = listOf("[NativeProcess:Xournalpp] starting...", "[NativeProcess:Xournalpp] SEGV"),
            exitCode = 139
        )

        val json = report.toJson()
        val deserialized = CrashReport.fromJson(json)

        assertEquals(report.id, deserialized.id)
        assertEquals(CrashType.NATIVE_PROCESS, deserialized.type)
        assertEquals(Integer.valueOf(139), deserialized.exitCode)
        assertEquals("NativeProcessCrash (xournalpp)", deserialized.errorClass)
        assertEquals("Segmentation fault (core dumped)", deserialized.stackTrace)
    }

    @Test
    fun testMarkdownFormattingContainsAllCriticalSections() {
        val metadata = DeviceMetadata(
            manufacturer = "Google",
            model = "Pixel Tablet",
            device = "tangorpro",
            androidVersion = "14",
            sdkInt = 34,
            supportedAbis = listOf("arm64-v8a"),
            appVersionName = "1.5.0",
            appVersionCode = 150L,
            availableRamMb = 2048L,
            totalRamMb = 8192L
        )

        val report = CrashReport(
            id = "crash_test_markdown",
            timestamp = 1728043200000L,
            type = CrashType.JVM_EXCEPTION,
            processName = "dev.ilamparithi.aournalpp",
            threadName = "DefaultDispatcher-worker-1",
            errorClass = "java.lang.IllegalStateException",
            errorMessage = "Corrupt workspace",
            stackTrace = "java.lang.IllegalStateException: Corrupt workspace\n\tat App.kt:10",
            deviceMetadata = metadata,
            recentLogs = listOf("Logcat line 1", "Logcat line 2")
        )

        val markdown = report.toMarkdown()

        assertTrue(markdown.contains("# Aournal++ Crash Report"))
        assertTrue(markdown.contains("`crash_test_markdown`"))
        assertTrue(markdown.contains("`dev.ilamparithi.aournalpp`"))
        assertTrue(markdown.contains("DefaultDispatcher-worker-1"))
        assertTrue(markdown.contains("Pixel Tablet"))
        assertTrue(markdown.contains("1.5.0 (150)"))
        assertTrue(markdown.contains("java.lang.IllegalStateException: Corrupt workspace"))
        assertTrue(markdown.contains("App.kt:10"))
        assertTrue(markdown.contains("Logcat line 1"))
        assertTrue(markdown.contains("Logcat line 2"))
    }
}
