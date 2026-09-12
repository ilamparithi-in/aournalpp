package dev.ilamparithi.aournalpp.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BootstrapInstallerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `test BootstrapManifest parse valid JSON`() {
        val json = """
            {
              "manifest_version": 2,
              "arch": "aarch64",
              "abi": "arm64-v8a",
              "bootstrap_series": "bookworm",
              "archive_compressed_bytes": 1048576,
              "archive_uncompressed_bytes": 5242880,
              "archive_sha256": "abcdef123456",
              "generated_at": "2026-09-12T12:00:00Z",
              "packages": {
                "xournalpp": {
                  "version": "1.2.3",
                  "installed_size": 2048000,
                  "deb_size": 512000
                },
                "libgtk-3-0": {
                  "version": "3.24.38",
                  "installed_size": 3000000,
                  "deb_size": 800000
                }
              }
            }
        """.trimIndent()

        val manifest = BootstrapManifest.parse(json)

        assertEquals(2, manifest.manifestVersion)
        assertEquals("aarch64", manifest.arch)
        assertEquals("arm64-v8a", manifest.abi)
        assertEquals("bookworm", manifest.bootstrapSeries)
        assertEquals(1048576L, manifest.archiveCompressedBytes)
        assertEquals(5242880L, manifest.archiveUncompressedBytes)
        assertEquals("abcdef123456", manifest.archiveSha256)
        assertEquals(2, manifest.packages.size)

        val xopp = manifest.packages["xournalpp"]
        assertNotNull(xopp)
        assertEquals("xournalpp", xopp?.name)
        assertEquals("1.2.3", xopp?.version)
        assertEquals(2048000L, xopp?.installedSize)
        assertEquals(512000L, xopp?.debSize)
    }

    @Test
    fun `test computeDiff fresh install`() {
        val incoming = BootstrapManifest(
            manifestVersion = 1,
            arch = "aarch64",
            abi = "arm64-v8a",
            bootstrapSeries = "bookworm",
            archiveCompressedBytes = 1000L,
            archiveUncompressedBytes = 5000L,
            archiveSha256 = "hash1",
            generatedAt = "now",
            packages = mapOf(
                "xournalpp" to BootstrapPackageInfo("xournalpp", "1.2.0", 2000L, 500L),
                "glib" to BootstrapPackageInfo("glib", "2.0.0", 1000L, 200L)
            )
        )

        val diff = BootstrapInstaller.computeDiff(
            installed = null,
            incoming = incoming,
            availableStorageBytes = 10000L,
            requiredStorageBytes = 5000L
        )

        assertEquals(2, diff.added.size)
        assertEquals(0, diff.updated.size)
        assertEquals(0, diff.removed.size)
        assertEquals(2, diff.totalChanges)
        assertTrue(diff.hasSufficientSpace)
        assertEquals("glib", diff.added[0].name)
        assertEquals("xournalpp", diff.added[1].name)
    }

    @Test
    fun `test computeDiff with upgrade and removed package`() {
        val installed = BootstrapManifest(
            manifestVersion = 1,
            arch = "aarch64",
            abi = "arm64-v8a",
            bootstrapSeries = "bookworm",
            archiveCompressedBytes = 1000L,
            archiveUncompressedBytes = 5000L,
            archiveSha256 = "hash1",
            generatedAt = "before",
            packages = mapOf(
                "xournalpp" to BootstrapPackageInfo("xournalpp", "1.2.0", 2000L, 500L),
                "old-pkg" to BootstrapPackageInfo("old-pkg", "1.0.0", 500L, 100L)
            )
        )

        val incoming = BootstrapManifest(
            manifestVersion = 2,
            arch = "aarch64",
            abi = "arm64-v8a",
            bootstrapSeries = "bookworm",
            archiveCompressedBytes = 1200L,
            archiveUncompressedBytes = 6000L,
            archiveSha256 = "hash2",
            generatedAt = "after",
            packages = mapOf(
                "xournalpp" to BootstrapPackageInfo("xournalpp", "1.2.1", 2100L, 520L),
                "new-pkg" to BootstrapPackageInfo("new-pkg", "0.5.0", 800L, 200L)
            )
        )

        val diff = BootstrapInstaller.computeDiff(
            installed = installed,
            incoming = incoming,
            availableStorageBytes = 2000L,
            requiredStorageBytes = 5000L
        )

        assertEquals(1, diff.added.size)
        assertEquals("new-pkg", diff.added[0].name)

        assertEquals(1, diff.updated.size)
        assertEquals("xournalpp", diff.updated[0].name)
        assertEquals("1.2.0", diff.updated[0].oldVersion)
        assertEquals("1.2.1", diff.updated[0].newVersion)

        assertEquals(1, diff.removed.size)
        assertEquals("old-pkg", diff.removed[0].name)

        assertFalse(diff.hasSufficientSpace)
        assertEquals(3, diff.totalChanges)
    }

    @Test
    fun `test needsBootstrap and isUpgradeAvailable evaluation`() {
        // Fresh install: installedVersion is null
        assertTrue(BootstrapInstaller.needsBootstrap(null, 100L))
        assertFalse(BootstrapInstaller.isUpgradeAvailable(hasValidInstallation = false, installedVersion = null, currentAppVersion = 100L))

        // Same version: installed == current
        assertFalse(BootstrapInstaller.needsBootstrap(100L, 100L))
        assertFalse(BootstrapInstaller.isUpgradeAvailable(hasValidInstallation = true, installedVersion = 100L, currentAppVersion = 100L))

        // Newer version available with valid installation
        assertTrue(BootstrapInstaller.needsBootstrap(99L, 100L))
        assertTrue(BootstrapInstaller.isUpgradeAvailable(hasValidInstallation = true, installedVersion = 99L, currentAppVersion = 100L))

        // Newer version available but installation is broken
        assertFalse(BootstrapInstaller.isUpgradeAvailable(hasValidInstallation = false, installedVersion = 99L, currentAppVersion = 100L))
    }

    @Test
    fun `test isPathWithinRoot safety check`() {
        val root = tempFolder.newFolder("rootfs")
        val safeChild = File(root, "usr/bin/xournalpp")
        safeChild.parentFile?.mkdirs()
        safeChild.createNewFile()

        val outside = tempFolder.newFolder("outside")
        val unsafeFile = File(outside, "malicious.sh")
        unsafeFile.createNewFile()

        assertTrue(BootstrapInstaller.isPathWithinRoot(root, safeChild))
        assertFalse(BootstrapInstaller.isPathWithinRoot(root, unsafeFile))
    }

    @Test
    fun `test isSymlinkTargetWithinRoot safety check`() {
        val root = tempFolder.newFolder("rootfs")
        val binDir = File(root, "bin").apply { mkdirs() }

        // Relative symlink staying inside root
        assertTrue(BootstrapInstaller.isSymlinkTargetWithinRoot(root, binDir, "../usr/bin/xopp"))

        // Traversing outside root
        assertFalse(BootstrapInstaller.isSymlinkTargetWithinRoot(root, binDir, "../../../../../etc/passwd"))
    }
}
