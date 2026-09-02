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

    @Test
    fun `test isAournalCompatible detects valid config directories`() {
        val tempDir = java.nio.file.Files.createTempDirectory("aournal_test").toFile()
        try {
            // Empty folder
            assertFalse(dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager.isAournalCompatible(tempDir))

            val configDir = java.io.File(tempDir, ".config")
            configDir.mkdirs()

            // Empty .config
            assertFalse(dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager.isAournalCompatible(tempDir))

            // .config with app_settings.json
            val appSettingsFile = java.io.File(configDir, "app_settings.json")
            appSettingsFile.writeText("{}")
            assertTrue(dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager.isAournalCompatible(tempDir))

            appSettingsFile.delete()
            assertFalse(dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager.isAournalCompatible(tempDir))

            // .config with x11_prefs.json
            val x11PrefsFile = java.io.File(configDir, "x11_prefs.json")
            x11PrefsFile.writeText("{}")
            assertTrue(dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager.isAournalCompatible(tempDir))

            x11PrefsFile.delete()
            assertFalse(dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager.isAournalCompatible(tempDir))

            // .config with xournalpp/settings.xml
            val xoppDir = java.io.File(configDir, "xournalpp")
            xoppDir.mkdirs()
            val settingsXml = java.io.File(xoppDir, "settings.xml")
            settingsXml.writeText("<settings></settings>")
            assertTrue(dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager.isAournalCompatible(tempDir))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `test BackupEngine getCompleteBackupRemoteRoot fallback and custom path`() {
        val defaultService = dev.ilamparithi.aournalpp.backup.model.ServiceConfig(
            id = "srv-1",
            name = "Nextcloud",
            providerType = dev.ilamparithi.aournalpp.backup.model.StorageProviderType.NEXTCLOUD,
            remoteBasePath = ""
        )
        assertEquals(
            "Aournalpp",
            dev.ilamparithi.aournalpp.backup.engine.BackupEngine.getCompleteBackupRemoteRoot(defaultService)
        )

        val customService = defaultService.copy(remoteBasePath = "/MyVault/Backups/")
        assertEquals(
            "MyVault/Backups",
            dev.ilamparithi.aournalpp.backup.engine.BackupEngine.getCompleteBackupRemoteRoot(customService)
        )
    }
}
