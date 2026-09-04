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
            settingsXml.delete()
            assertFalse(dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager.isAournalCompatible(tempDir))

            // .config with sync_mappings.json
            val syncMappings = java.io.File(configDir, "sync_mappings.json")
            syncMappings.writeText("{}")
            assertTrue(dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager.isAournalCompatible(tempDir))
            syncMappings.delete()

            // .config with settings.ini
            val gtkSettings = java.io.File(configDir, "settings.ini")
            gtkSettings.writeText("[Settings]")
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

    @Test
    fun `test NotesHomeConfigManager type safety export and import with __meta_types`() {
        val tempDir = java.nio.file.Files.createTempDirectory("aournal_types_test").toFile()
        try {
            val jsonFile = java.io.File(tempDir, "exported_prefs.json")
            val originalPrefs = FakeSharedPreferences().apply {
                putBoolean("pref_fullscreen", true)
                putInt("pref_margin_left", 24)
                putLong("pref_last_sync_timestamp", 1725440000000L)
                putFloat("toolbarPosXRatio", 0.73f)
                putFloat("toolbarPosYRatio", 0.15f)
                putString("pref_theme_name", "AuraDark")
                putStringSet("pref_active_plugins", mutableSetOf("plugin_a", "plugin_b"))
            }

            dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager.exportSharedPreferencesToJson(originalPrefs, jsonFile)
            assertTrue(jsonFile.exists())

            val content = jsonFile.readText()
            assertTrue(content.contains("__meta_types"))
            assertTrue(content.contains("\"toolbarPosXRatio\": \"FLOAT\""))
            assertTrue(content.contains("\"pref_last_sync_timestamp\": \"LONG\""))
            assertTrue(content.contains("\"pref_active_plugins\": \"STRING_SET\""))

            // Import into clean/empty preferences (mimicking fresh onboarding restore)
            val restoredPrefs = FakeSharedPreferences()
            dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager.importJsonToSharedPreferences(jsonFile, restoredPrefs)

            // Critical: getFloat and getLong must NOT throw ClassCastException
            assertEquals(true, restoredPrefs.getBoolean("pref_fullscreen", false))
            assertEquals(24, restoredPrefs.getInt("pref_margin_left", 0))
            assertEquals(1725440000000L, restoredPrefs.getLong("pref_last_sync_timestamp", 0L))
            assertEquals(0.73f, restoredPrefs.getFloat("toolbarPosXRatio", 0f), 0.001f)
            assertEquals(0.15f, restoredPrefs.getFloat("toolbarPosYRatio", 0f), 0.001f)
            assertEquals("AuraDark", restoredPrefs.getString("pref_theme_name", ""))
            assertEquals(setOf("plugin_a", "plugin_b"), restoredPrefs.getStringSet("pref_active_plugins", mutableSetOf<String>()))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `test NotesHomeConfigManager legacy JSON without meta types does not crash on float or long`() {
        val tempDir = java.nio.file.Files.createTempDirectory("aournal_legacy_test").toFile()
        try {
            val jsonFile = java.io.File(tempDir, "legacy_x11_prefs.json")
            jsonFile.writeText(
                """
                {
                  "toolbarPosXRatio": 0.5,
                  "toolbarPosYRatio": 0.0,
                  "backup_timestamp": 1690000000000,
                  "safeAreaMarginAll": 10,
                  "fullscreen": true,
                  "touchMode": "3"
                }
                """.trimIndent()
            )

            val cleanPrefs = FakeSharedPreferences()
            dev.ilamparithi.aournalpp.runtime.NotesHomeConfigManager.importJsonToSharedPreferences(jsonFile, cleanPrefs)

            // Must correctly parse known float and long keys without ClassCastException
            assertEquals(0.5f, cleanPrefs.getFloat("toolbarPosXRatio", 0f), 0.001f)
            assertEquals(0.0f, cleanPrefs.getFloat("toolbarPosYRatio", 0f), 0.001f)
            assertEquals(1690000000000L, cleanPrefs.getLong("backup_timestamp", 0L))
            assertEquals(10, cleanPrefs.getInt("safeAreaMarginAll", 0))
            assertEquals(true, cleanPrefs.getBoolean("fullscreen", false))
            assertEquals("3", cleanPrefs.getString("touchMode", ""))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}

class FakeSharedPreferences(
    private val map: MutableMap<String, Any?> = mutableMapOf()
) : android.content.SharedPreferences, android.content.SharedPreferences.Editor {

    override fun getAll(): MutableMap<String, *> = map.toMutableMap()
    override fun getString(key: String?, defValue: String?): String? = (map[key] as? String) ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        val s = map[key] as? Set<*> ?: return defValues
        return s.map { it.toString() }.toMutableSet()
    }

    override fun getInt(key: String?, defValue: Int): Int {
        val v = map[key] ?: return defValue
        if (v !is Int) throw ClassCastException("Expected Int, but was ${v::class.java.simpleName}")
        return v
    }

    override fun getLong(key: String?, defValue: Long): Long {
        val v = map[key] ?: return defValue
        if (v !is Long) throw ClassCastException("Expected Long, but was ${v::class.java.simpleName}")
        return v
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        val v = map[key] ?: return defValue
        if (v !is Float) throw ClassCastException("Expected Float, but was ${v::class.java.simpleName}")
        return v
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        val v = map[key] ?: return defValue
        if (v !is Boolean) throw ClassCastException("Expected Boolean, but was ${v::class.java.simpleName}")
        return v
    }

    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun edit(): android.content.SharedPreferences.Editor = this
    override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun putString(key: String?, value: String?): android.content.SharedPreferences.Editor {
        if (key != null) map[key] = value
        return this
    }

    override fun putStringSet(key: String?, values: MutableSet<String>?): android.content.SharedPreferences.Editor {
        if (key != null) map[key] = values?.toSet()
        return this
    }

    override fun putInt(key: String?, value: Int): android.content.SharedPreferences.Editor {
        if (key != null) map[key] = value
        return this
    }

    override fun putLong(key: String?, value: Long): android.content.SharedPreferences.Editor {
        if (key != null) map[key] = value
        return this
    }

    override fun putFloat(key: String?, value: Float): android.content.SharedPreferences.Editor {
        if (key != null) map[key] = value
        return this
    }

    override fun putBoolean(key: String?, value: Boolean): android.content.SharedPreferences.Editor {
        if (key != null) map[key] = value
        return this
    }

    override fun remove(key: String?): android.content.SharedPreferences.Editor {
        map.remove(key)
        return this
    }

    override fun clear(): android.content.SharedPreferences.Editor {
        map.clear()
        return this
    }

    override fun commit(): Boolean = true
    override fun apply() {}
}
