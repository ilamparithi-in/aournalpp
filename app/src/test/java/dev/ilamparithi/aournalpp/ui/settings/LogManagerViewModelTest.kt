package dev.ilamparithi.aournalpp.ui.settings

import android.app.Application
import android.util.Log
import dev.ilamparithi.aournalpp.logging.CrashLogStorage
import dev.ilamparithi.aournalpp.logging.CrashReport
import dev.ilamparithi.aournalpp.logging.CrashType
import dev.ilamparithi.aournalpp.logging.DeviceMetadata
import dev.ilamparithi.aournalpp.logging.LogEntry
import dev.ilamparithi.aournalpp.logging.LogLevel
import dev.ilamparithi.aournalpp.ui.settings.viewmodel.LogManagerViewModel
import dev.ilamparithi.aournalpp.ui.settings.viewmodel.LogTab
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogManagerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application
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

    private val testCrashes = listOf(
        CrashReport(
            id = "crash_1",
            timestamp = 1000L,
            type = CrashType.JVM_EXCEPTION,
            processName = "dev.ilamparithi.aournalpp",
            threadName = "main",
            errorClass = "NullPointerException",
            errorMessage = "NPE",
            stackTrace = "trace1",
            deviceMetadata = dummyMetadata
        ),
        CrashReport(
            id = "crash_2",
            timestamp = 2000L,
            type = CrashType.NATIVE_PROCESS,
            processName = "dev.ilamparithi.aournalpp:canvas",
            threadName = null,
            errorClass = "NativeProcessCrash",
            errorMessage = "Signal 11",
            stackTrace = "trace2",
            deviceMetadata = dummyMetadata,
            exitCode = 139
        )
    )

    private val testLogs = listOf(
        LogEntry("10:00", LogLevel.INFO, "100", "", "Main", "App launched", "I/Main: App launched"),
        LogEntry("10:01", LogLevel.WARN, "100", "", "Canvas", "Touch warning", "W/Canvas: Touch warning"),
        LogEntry("10:02", LogLevel.ERROR, "100", "", "CrashHandler", "Crash detected", "E/CrashHandler: Crash detected")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        app = mockk<Application>(relaxed = true)
        storage = mockk<CrashLogStorage>(relaxed = true)
        every { storage.getAllCrashReports() } returns testCrashes
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    private fun createViewModel(): LogManagerViewModel {
        return LogManagerViewModel(
            application = app,
            storage = storage,
            ioDispatcher = testDispatcher,
            defaultDispatcher = testDispatcher,
            logReader = { testLogs }
        )
    }

    @Test
    fun testInitializationLoadsCrashesAndLogs() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(LogTab.CRASHES, state.activeTab)
        assertEquals(2, state.crashReports.size)
        assertEquals(3, state.liveLogs.size)
        assertEquals(3, state.filteredLogs.size)
    }

    @Test
    fun testSwitchTab() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.switchTab(LogTab.LIVE_LOGS)
        assertEquals(LogTab.LIVE_LOGS, vm.uiState.value.activeTab)

        vm.switchTab(LogTab.CRASHES)
        assertEquals(LogTab.CRASHES, vm.uiState.value.activeTab)
    }

    @Test
    fun testLogLevelFiltering() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        // Filter ERROR only
        vm.setLogLevel(LogLevel.ERROR)
        assertEquals(LogLevel.ERROR, vm.uiState.value.selectedLogLevel)
        assertEquals(1, vm.uiState.value.filteredLogs.size)
        assertEquals(LogLevel.ERROR, vm.uiState.value.filteredLogs[0].level)

        // Toggle same level off (returns to null)
        vm.setLogLevel(LogLevel.ERROR)
        assertNull(vm.uiState.value.selectedLogLevel)
        assertEquals(3, vm.uiState.value.filteredLogs.size)
    }

    @Test
    fun testSearchQueryFiltering() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.setSearchQuery("touch")
        assertEquals(1, vm.uiState.value.filteredLogs.size)
        assertEquals("Canvas", vm.uiState.value.filteredLogs[0].tag)

        vm.setSearchQuery("nonexistent")
        assertTrue(vm.uiState.value.filteredLogs.isEmpty())

        vm.setSearchQuery("")
        assertEquals(3, vm.uiState.value.filteredLogs.size)
    }

    @Test
    fun testSelectCrashReport() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        assertNull(vm.uiState.value.selectedCrashReport)
        vm.selectCrashReport(testCrashes[0])
        assertEquals("crash_1", vm.uiState.value.selectedCrashReport?.id)

        vm.selectCrashReport(null)
        assertNull(vm.uiState.value.selectedCrashReport)
    }

    @Test
    fun testDeleteCrashReport() = runTest(testDispatcher) {
        every { storage.deleteCrashReport("crash_1") } returns true
        every { storage.getAllCrashReports() } returns listOf(testCrashes[1])

        val vm = createViewModel()
        advanceUntilIdle()

        vm.deleteCrashReport("crash_1")
        advanceUntilIdle()

        verify { storage.deleteCrashReport("crash_1") }
        assertEquals(1, vm.uiState.value.crashReports.size)
        assertEquals("crash_2", vm.uiState.value.crashReports[0].id)
    }

    @Test
    fun testClearAllCrashReports() = runTest(testDispatcher) {
        every { storage.clearAllCrashReports() } returns 2
        every { storage.getAllCrashReports() } returns emptyList()

        val vm = createViewModel()
        advanceUntilIdle()

        vm.clearAllCrashReports()
        advanceUntilIdle()

        verify { storage.clearAllCrashReports() }
        assertTrue(vm.uiState.value.crashReports.isEmpty())
        assertNull(vm.uiState.value.selectedCrashReport)
    }

    @Test
    fun testToggleLogLevelMultiSelect() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        // All 4 default levels enabled initially
        assertEquals(3, vm.uiState.value.filteredLogs.size)

        // Toggle INFO off
        vm.toggleLogLevel(LogLevel.INFO)
        assertFalse(LogLevel.INFO in vm.uiState.value.selectedLogLevels)
        assertEquals(2, vm.uiState.value.filteredLogs.size)

        // Toggle WARN off
        vm.toggleLogLevel(LogLevel.WARN)
        assertEquals(1, vm.uiState.value.filteredLogs.size)
        assertEquals(LogLevel.ERROR, vm.uiState.value.filteredLogs[0].level)

        // Toggle INFO back on
        vm.toggleLogLevel(LogLevel.INFO)
        assertTrue(LogLevel.INFO in vm.uiState.value.selectedLogLevels)
        assertEquals(2, vm.uiState.value.filteredLogs.size)
    }

    @Test
    fun testToggleAutoRefresh() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isAutoRefreshEnabled)
        vm.toggleAutoRefresh()
        assertFalse(vm.uiState.value.isAutoRefreshEnabled)
        vm.toggleAutoRefresh()
        assertTrue(vm.uiState.value.isAutoRefreshEnabled)
    }

    @Test
    fun testClearLiveLogs() = runTest(testDispatcher) {
        var clearCalled = false
        val vm = LogManagerViewModel(
            application = app,
            storage = storage,
            ioDispatcher = testDispatcher,
            defaultDispatcher = testDispatcher,
            logReader = { testLogs },
            logClearer = { clearCalled = true }
        )
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.liveLogs.size)
        assertEquals(3, vm.uiState.value.filteredLogs.size)

        vm.clearLiveLogs()
        advanceUntilIdle()

        assertTrue(clearCalled)
        assertTrue(vm.uiState.value.liveLogs.isEmpty())
        assertTrue(vm.uiState.value.filteredLogs.isEmpty())
    }
}
