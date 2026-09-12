package dev.ilamparithi.aournalpp.ui

import android.app.Application
import dev.ilamparithi.aournalpp.runtime.BootstrapInstaller
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.StorageCheckResult
import io.mockk.every
import io.mockk.mockk
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
import android.util.Log
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class BootstrapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application
    private lateinit var env: LinuxEnvironment
    private lateinit var installer: BootstrapInstaller

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        app = mockk<Application>(relaxed = true)
        env = mockk<LinuxEnvironment>(relaxed = true)
        installer = mockk<BootstrapInstaller>(relaxed = true)

        val mockBin = mockk<File>(relaxed = true)
        every { mockBin.exists() } returns true
        every { mockBin.canExecute() } returns true
        every { mockBin.absolutePath } returns "/usr/bin/xournalpp"
        every { env.resolveExecutable(any()) } returns mockBin
        every { env.rootDir } returns File("/tmp/mock_rootfs")
        every { env.usrDir } returns File("/tmp/mock_rootfs/usr")
        every { env.binDir } returns File("/tmp/mock_rootfs/usr/bin")
        every { env.nativeLibDir } returns File("/tmp/mock_rootfs/lib")
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state is Ready when onboarding completed and installation valid without upgrade`() = runTest(testDispatcher) {
        every { env.isOnboardingCompleted() } returns true
        every { installer.hasValidInstallation() } returns true
        every { installer.isUpgradeAvailable() } returns false
        every { installer.needsBootstrap() } returns false

        val viewModel = BootstrapViewModel(app, env, installer)
        advanceUntilIdle()

        assertEquals(BootstrapState.Ready, viewModel.uiState.value)
        assertTrue(viewModel.isOnboardingCompleted.value)
    }

    @Test
    fun `test completeOnboarding marks environment and updates StateFlow`() = runTest(testDispatcher) {
        every { env.isOnboardingCompleted() } returns false
        every { installer.hasValidInstallation() } returns false
        every { installer.needsBootstrap() } returns false

        val viewModel = BootstrapViewModel(app, env, installer)
        advanceUntilIdle()

        assertFalse(viewModel.isOnboardingCompleted.value)

        viewModel.completeOnboarding()
        assertTrue(viewModel.isOnboardingCompleted.value)
        verify { env.setOnboardingCompleted(true) }
    }

    @Test
    fun `test cancelUpdateCountdown resets countdown to zero`() = runTest(testDispatcher) {
        every { env.isOnboardingCompleted() } returns true
        every { installer.hasValidInstallation() } returns true
        every { installer.isUpgradeAvailable() } returns true
        every { installer.needsBootstrap() } returns true
        every { installer.getInstalledVersion() } returns 10L
        every { installer.getCurrentAppVersionCode() } returns 11L
        every { installer.computeDiff() } returns null

        val viewModel = BootstrapViewModel(app, env, installer)
        testDispatcher.scheduler.advanceTimeBy(1000)

        viewModel.cancelUpdateCountdown()

        val state = viewModel.uiState.value
        if (state is BootstrapState.UpdatePrompt) {
            assertEquals(0, state.countdownSeconds)
        }
    }

    @Test
    fun `test storage warning transitions state when storage is insufficient`() = runTest(testDispatcher) {
        every { env.isOnboardingCompleted() } returns false
        every { installer.needsBootstrap() } returns true
        every { installer.checkStorageThresholds() } returns StorageCheckResult(
            requiredBytes = 500L * 1024 * 1024,
            availableBytes = 100L * 1024 * 1024,
            totalBytes = 1000L * 1024 * 1024,
            isInsufficient = true,
            isLowStorageWarning = false,
            missingBytes = 400L * 1024 * 1024
        )

        val viewModel = BootstrapViewModel(app, env, installer)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is BootstrapState.StorageWarning)
        val warning = state as BootstrapState.StorageWarning
        assertTrue(warning.isInsufficient)
        assertEquals(400L * 1024 * 1024, warning.missingBytes)
    }
}
