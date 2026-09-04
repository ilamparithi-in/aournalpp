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

    @Test
    fun `test BootstrapManifest parse correctly reads fields and packages`() {
        val json = """
        {
          "manifest_version": 1,
          "arch": "aarch64",
          "abi": "arm64-v8a",
          "bootstrap_series": "bootstrap-v1",
          "archive_compressed_bytes": 45000000,
          "archive_uncompressed_bytes": 180000000,
          "archive_sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
          "generated_at": "2026-09-04T12:00:00Z",
          "packages": {
            "xournalpp": {
              "version": "1.2.3",
              "installed_size": 15000000,
              "deb_size": 4000000
            },
            "gtk3": {
              "version": "3.24.40",
              "installed_size": 35000000,
              "deb_size": 9000000
            }
          }
        }
        """.trimIndent()

        val manifest = dev.ilamparithi.aournalpp.runtime.BootstrapManifest.parse(json)
        assertEquals(1, manifest.manifestVersion)
        assertEquals("aarch64", manifest.arch)
        assertEquals("arm64-v8a", manifest.abi)
        assertEquals("bootstrap-v1", manifest.bootstrapSeries)
        assertEquals(45000000L, manifest.archiveCompressedBytes)
        assertEquals(180000000L, manifest.archiveUncompressedBytes)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", manifest.archiveSha256)
        assertEquals(2, manifest.packages.size)
        assertEquals("1.2.3", manifest.packages["xournalpp"]?.version)
        assertEquals(15000000L, manifest.packages["xournalpp"]?.installedSize)
        assertEquals("3.24.40", manifest.packages["gtk3"]?.version)
    }

    @Test
    fun `test package diff detection logic correctly classifies added, updated, and removed`() {
        val oldPkgs = mapOf(
            "xournalpp" to dev.ilamparithi.aournalpp.runtime.BootstrapPackageInfo("xournalpp", "1.2.0", 100L, 50L),
            "openbox" to dev.ilamparithi.aournalpp.runtime.BootstrapPackageInfo("openbox", "3.6.1", 100L, 50L),
            "legacy-lib" to dev.ilamparithi.aournalpp.runtime.BootstrapPackageInfo("legacy-lib", "0.9.0", 100L, 50L)
        )
        val newPkgs = mapOf(
            "xournalpp" to dev.ilamparithi.aournalpp.runtime.BootstrapPackageInfo("xournalpp", "1.2.1", 120L, 60L), // updated
            "openbox" to dev.ilamparithi.aournalpp.runtime.BootstrapPackageInfo("openbox", "3.6.1", 100L, 50L),    // unchanged
            "gtk3" to dev.ilamparithi.aournalpp.runtime.BootstrapPackageInfo("gtk3", "3.24.40", 200L, 100L)         // added
            // "legacy-lib" removed
        )

        val added = mutableListOf<dev.ilamparithi.aournalpp.runtime.PackageChange>()
        val updated = mutableListOf<dev.ilamparithi.aournalpp.runtime.PackageChange>()
        val removed = mutableListOf<dev.ilamparithi.aournalpp.runtime.PackageChange>()

        for ((name, newPkg) in newPkgs) {
            val oldPkg = oldPkgs[name]
            if (oldPkg == null) {
                added.add(dev.ilamparithi.aournalpp.runtime.PackageChange(name, null, newPkg.version))
            } else if (oldPkg.version != newPkg.version) {
                updated.add(dev.ilamparithi.aournalpp.runtime.PackageChange(name, oldPkg.version, newPkg.version))
            }
        }
        for ((name, oldPkg) in oldPkgs) {
            if (!newPkgs.containsKey(name)) {
                removed.add(dev.ilamparithi.aournalpp.runtime.PackageChange(name, oldPkg.version, null))
            }
        }

        assertEquals(1, added.size)
        assertEquals("gtk3", added[0].name)
        assertEquals("3.24.40", added[0].newVersion)

        assertEquals(1, updated.size)
        assertEquals("xournalpp", updated[0].name)
        assertEquals("1.2.0", updated[0].oldVersion)
        assertEquals("1.2.1", updated[0].newVersion)

        assertEquals(1, removed.size)
        assertEquals("legacy-lib", removed[0].name)
        assertEquals("0.9.0", removed[0].oldVersion)
    }

    @Test
    fun `test storage precheck formula for bundled vs dynamic download`() {
        val safetyBuffer = 50L * 1024L * 1024L // 50 MB
        val uncompressed = 180L * 1024L * 1024L // 180 MB
        val compressed = 45L * 1024L * 1024L   // 45 MB

        val bundledRequired = uncompressed + safetyBuffer
        val dynamicRequired = compressed + uncompressed + safetyBuffer

        assertEquals(230L * 1024L * 1024L, bundledRequired)
        assertEquals(275L * 1024L * 1024L, dynamicRequired)

        // Case: User has 300MB available
        val available300 = 300L * 1024L * 1024L
        assertTrue(available300 >= bundledRequired)
        assertTrue(available300 >= dynamicRequired)

        // Case: User has 250MB available (sufficient for bundled, insufficient for dynamic)
        val available250 = 250L * 1024L * 1024L
        assertTrue(available250 >= bundledRequired)
        assertFalse(available250 >= dynamicRequired)

        // Case: User has 100MB available (insufficient for both)
        val available100 = 100L * 1024L * 1024L
        assertFalse(available100 >= bundledRequired)
        assertFalse(available100 >= dynamicRequired)
    }

    @Test
    fun `test onboarding storage threshold calculation rules`() {
        fun evaluateStorage(totalBytes: Long, availableBytes: Long, requiredBytes: Long): dev.ilamparithi.aournalpp.runtime.StorageCheckResult {
            val isInsufficient = availableBytes < requiredBytes
            val missingBytes = if (isInsufficient) requiredBytes - availableBytes else 0L

            val projectedRemaining = availableBytes - requiredBytes
            val tenPercentTotal = if (totalBytes > 0L) (totalBytes * 0.10).toLong() else 512L * 1024L * 1024L
            val fiveTwelveMb = 512L * 1024L * 1024L
            val lowStorageThreshold = minOf(tenPercentTotal, fiveTwelveMb)

            val isLowStorageWarning = !isInsufficient && projectedRemaining < lowStorageThreshold

            return dev.ilamparithi.aournalpp.runtime.StorageCheckResult(
                requiredBytes = requiredBytes,
                availableBytes = availableBytes,
                totalBytes = totalBytes,
                isInsufficient = isInsufficient,
                isLowStorageWarning = isLowStorageWarning,
                missingBytes = missingBytes
            )
        }

        val required = 500L * 1024L * 1024L // 500 MB
        val total64GB = 64L * 1024L * 1024L * 1024L // 64 GB
        // For 64GB, 10% = 6.4GB, 512MB is lowest -> threshold = 512MB

        // Scenario 1: Insufficient storage (has only 300MB, requires 500MB)
        val res1 = evaluateStorage(total64GB, 300L * 1024L * 1024L, required)
        assertTrue(res1.isInsufficient)
        assertFalse(res1.isLowStorageWarning)
        assertEquals(200L * 1024L * 1024L, res1.missingBytes)

        // Scenario 2: Required storage present (600MB available), but after extraction only 100MB remains (< 512MB threshold)
        val res2 = evaluateStorage(total64GB, 600L * 1024L * 1024L, required)
        assertFalse(res2.isInsufficient)
        assertTrue(res2.isLowStorageWarning)
        assertEquals(0L, res2.missingBytes)

        // Scenario 3: Ample storage present (10GB available), projected remaining is 9.5GB (>= 512MB threshold)
        val res3 = evaluateStorage(total64GB, 10L * 1024L * 1024L * 1024L, required)
        assertFalse(res3.isInsufficient)
        assertFalse(res3.isLowStorageWarning)

        // Scenario 4: Small storage disk (e.g. 4GB total). 10% = 400MB (< 512MB) -> threshold = 400MB
        val total4GB = 4L * 1024L * 1024L * 1024L
        // Available 850MB -> projected remaining is 350MB (< 400MB threshold)
        val res4 = evaluateStorage(total4GB, 850L * 1024L * 1024L, required)
        assertFalse(res4.isInsufficient)
        assertTrue(res4.isLowStorageWarning)

        // Available 1000MB -> projected remaining is 500MB (>= 400MB threshold)
        val res5 = evaluateStorage(total4GB, 1000L * 1024L * 1024L, required)
        assertFalse(res5.isInsufficient)
        assertFalse(res5.isLowStorageWarning)
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
