package dev.ilamparithi.aournalpp.backup

import dev.ilamparithi.aournalpp.backup.model.ExclusionFilterConfig
import dev.ilamparithi.aournalpp.backup.scanner.BackupScanner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BackupScannerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var rootDir: File

    @Before
    fun setup() {
        rootDir = tempFolder.newFolder("test_root")
    }

    @Test
    fun testDefaultTransientExclusions() {
        val scanner = BackupScanner(
            env = null,
            exclusionFilter = ExclusionFilterConfig.DEFAULT
        )

        val autosave = File(rootDir, ".note.autosave.xopp")
        val backupTilde = File(rootDir, ".note.xopp~")
        val lockFile = File(rootDir, ".X0-lock")
        val sockFile = File(rootDir, "ipc.sock")
        val tmpFile = File(rootDir, "temp.tmp")
        val validNote = File(rootDir, "Lecture.xopp")

        assertTrue(scanner.shouldExcludeFile(autosave, rootDir))
        assertTrue(scanner.shouldExcludeFile(backupTilde, rootDir))
        assertTrue(scanner.shouldExcludeFile(lockFile, rootDir))
        assertTrue(scanner.shouldExcludeFile(sockFile, rootDir))
        assertTrue(scanner.shouldExcludeFile(tmpFile, rootDir))
        assertFalse(scanner.shouldExcludeFile(validNote, rootDir))
    }

    @Test
    fun testCustomRegexExclusions() {
        val customFilter = ExclusionFilterConfig(
            regexPatterns = listOf("^.*_draft\\.xopp$", "^secret_.*$"),
            excludedExtensions = emptySet(),
            skipDefaultTransient = true
        )

        val scanner = BackupScanner(
            env = null,
            exclusionFilter = customFilter
        )

        val draftFile = File(rootDir, "Biology_draft.xopp")
        val secretFile = File(rootDir, "secret_notes.pdf")
        val normalFile = File(rootDir, "Biology_final.xopp")

        assertTrue(scanner.shouldExcludeFile(draftFile, rootDir))
        assertTrue(scanner.shouldExcludeFile(secretFile, rootDir))
        assertFalse(scanner.shouldExcludeFile(normalFile, rootDir))
    }

    @Test
    fun testExcludedExtensions() {
        val customFilter = ExclusionFilterConfig(
            excludedExtensions = setOf("bak", "log", "tmp")
        )

        val scanner = BackupScanner(
            env = null,
            exclusionFilter = customFilter
        )

        val bakFile = File(rootDir, "backup.bak")
        val logFile = File(rootDir, "session.log")
        val xoppFile = File(rootDir, "document.xopp")

        assertTrue(scanner.shouldExcludeFile(bakFile, rootDir))
        assertTrue(scanner.shouldExcludeFile(logFile, rootDir))
        assertFalse(scanner.shouldExcludeFile(xoppFile, rootDir))
    }

    @Test
    fun testExcludedFolderPaths() {
        val excludedDir = File(rootDir, "Archive")
        excludedDir.mkdirs()

        val customFilter = ExclusionFilterConfig(
            excludedFolderPaths = setOf(excludedDir.absolutePath)
        )

        val scanner = BackupScanner(
            env = null,
            exclusionFilter = customFilter
        )

        val fileInsideExcluded = File(excludedDir, "OldNote.xopp")
        val fileOutside = File(rootDir, "ActiveNote.xopp")

        assertTrue(scanner.shouldExcludeFile(fileInsideExcluded, rootDir))
        assertFalse(scanner.shouldExcludeFile(fileOutside, rootDir))
    }

    @Test
    fun testCachedMetadataFastPath() {
        val testFile = File(rootDir, "CachedNote.xopp").apply {
            writeText("Test note content for backup scanner")
        }

        val mapping = dev.ilamparithi.aournalpp.backup.model.CustomFolderMapping(
            id = "custom_test",
            serviceId = "test_service",
            localFolderPath = rootDir.absolutePath,
            remoteFolderPath = "BackupTest",
            isEnabled = true
        )

        val cachedEntity = dev.ilamparithi.aournalpp.backup.db.SyncMetadataEntity(
            serviceId = "test_service",
            relativePath = "CachedNote.xopp",
            scope = "custom_test",
            localSha256 = "precomputed_mock_sha256_hash",
            remoteHash = null,
            localLastModified = testFile.lastModified(),
            sizeBytes = testFile.length(),
            lastSyncedAt = System.currentTimeMillis()
        )

        val scanner = BackupScanner(
            env = null,
            exclusionFilter = ExclusionFilterConfig.DEFAULT
        )

        val scanned = scanner.scanCustomMapping(mapping, mapOf("CachedNote.xopp" to cachedEntity))
        org.junit.Assert.assertEquals(1, scanned.size)
        org.junit.Assert.assertEquals("precomputed_mock_sha256_hash", scanned[0].sha256)
    }
}
