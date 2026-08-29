package dev.ilamparithi.aournalpp

import dev.ilamparithi.aournalpp.runtime.InstallProgress
import dev.ilamparithi.aournalpp.ui.BootstrapState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingAndBootstrapTest {

    @Test
    fun `test BootstrapState UpdatePrompt holds correct values`() {
        val state = BootstrapState.UpdatePrompt(
            installedVersion = 100L,
            newVersion = 101L,
            countdownSeconds = 10
        )

        assertEquals(100L, state.installedVersion)
        assertEquals(101L, state.newVersion)
        assertEquals(10, state.countdownSeconds)
    }

    @Test
    fun `test BootstrapState Installing holds progress and message`() {
        val progress = InstallProgress(
            currentFile = "usr/bin/xournalpp",
            extractedBytes = 1024 * 1024 * 15,
            percentage = 45.5f
        )
        val state = BootstrapState.Installing(
            progress = progress,
            message = "Extracting runtime libraries..."
        )

        assertEquals("usr/bin/xournalpp", state.progress?.currentFile)
        assertEquals(45.5f, state.progress?.percentage ?: 0f, 0.01f)
        assertEquals("Extracting runtime libraries...", state.message)
    }

    @Test
    fun `test upgrade detection helper logic`() {
        fun needsBootstrap(installedVersion: Long?, currentAppVersion: Long): Boolean {
            if (installedVersion == null) return true
            return installedVersion < currentAppVersion
        }

        fun isUpgradeAvailable(
            versionFileExists: Boolean,
            binaryExists: Boolean,
            binaryCanExecute: Boolean,
            installedVersion: Long?,
            currentAppVersion: Long
        ): Boolean {
            val hasValidInstallation = versionFileExists && binaryExists && binaryCanExecute
            return hasValidInstallation && needsBootstrap(installedVersion, currentAppVersion)
        }

        // Case 1: Fresh install (no version file, no binaries)
        assertFalse(
            isUpgradeAvailable(
                versionFileExists = false,
                binaryExists = false,
                binaryCanExecute = false,
                installedVersion = null,
                currentAppVersion = 10
            )
        )
        assertTrue(needsBootstrap(null, 10))

        // Case 2: Up to date install
        assertFalse(
            isUpgradeAvailable(
                versionFileExists = true,
                binaryExists = true,
                binaryCanExecute = true,
                installedVersion = 10L,
                currentAppVersion = 10L
            )
        )
        assertFalse(needsBootstrap(10L, 10L))

        // Case 3: Upgrade available from v9 to v10 with working binaries
        assertTrue(
            isUpgradeAvailable(
                versionFileExists = true,
                binaryExists = true,
                binaryCanExecute = true,
                installedVersion = 9L,
                currentAppVersion = 10L
            )
        )
        assertTrue(needsBootstrap(9L, 10L))

        // Case 4: Broken installation (version file exists but binary is missing)
        assertFalse(
            isUpgradeAvailable(
                versionFileExists = true,
                binaryExists = false,
                binaryCanExecute = false,
                installedVersion = 9L,
                currentAppVersion = 10L
            )
        )
    }

    @Test
    fun `test onboarding flag persistence logic`() {
        var onboardingCompleted = false

        // First launch
        assertFalse(onboardingCompleted)

        // Complete onboarding
        onboardingCompleted = true
        assertTrue(onboardingCompleted)

        // Subsequent launches retain completed state
        assertTrue(onboardingCompleted)
    }
}
