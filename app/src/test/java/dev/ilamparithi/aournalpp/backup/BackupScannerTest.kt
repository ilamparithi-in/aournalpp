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

    @Test
    fun testWhitelistModeExclusions() {
        val whitelistFilter = ExclusionFilterConfig(
            isWhitelistMode = true,
            excludedExtensions = setOf("xopp", "pdf"),
            skipDefaultTransient = true
        )

        val scanner = BackupScanner(
            env = null,
            exclusionFilter = whitelistFilter
        )

        val xoppNote = File(rootDir, "ValidNote.xopp")
        val pdfDoc = File(rootDir, "Document.pdf")
        val txtFile = File(rootDir, "Readme.txt")
        val docxFile = File(rootDir, "Essay.docx")
        val autosave = File(rootDir, ".ValidNote.autosave.xopp")

        // In whitelist mode:
        // Matching extensions should NOT be excluded (return false)
        assertFalse(scanner.shouldExcludeFile(xoppNote, rootDir))
        assertFalse(scanner.shouldExcludeFile(pdfDoc, rootDir))

        // Non-matching extensions should BE excluded (return true)
        assertTrue(scanner.shouldExcludeFile(txtFile, rootDir))
        assertTrue(scanner.shouldExcludeFile(docxFile, rootDir))

        // Safety invariant: transient lock files are ALWAYS excluded even if matching extension
        assertTrue(scanner.shouldExcludeFile(autosave, rootDir))
    }

    @Test
    fun testTrashFolderSyncSetting() {
        val trashDir = File(rootDir, ".Trash").apply { mkdirs() }
        val trashFile = File(trashDir, "DeletedNote.xopp").apply { writeText("deleted note") }
        val normalFile = File(rootDir, "ActiveNote.xopp").apply { writeText("active note") }

        val mapping = dev.ilamparithi.aournalpp.backup.model.CustomFolderMapping(
            id = "test_mapping",
            serviceId = "test_service",
            localFolderPath = rootDir.absolutePath,
            remoteFolderPath = "BackupTest",
            isEnabled = true
        )

        // 1. syncTrash = false (default)
        val defaultScanner = BackupScanner(
            env = null,
            exclusionFilter = ExclusionFilterConfig(syncTrash = false)
        )
        assertTrue(defaultScanner.shouldExcludeFile(trashFile, rootDir))
        assertFalse(defaultScanner.shouldExcludeFile(normalFile, rootDir))

        val scannedDefault = defaultScanner.scanCustomMapping(mapping)
        assertTrue(scannedDefault.none { it.file.absolutePath.contains(".Trash") })
        assertTrue(scannedDefault.any { it.file.name == "ActiveNote.xopp" })

        // 2. syncTrash = true
        val syncTrashScanner = BackupScanner(
            env = null,
            exclusionFilter = ExclusionFilterConfig(syncTrash = true)
        )
        assertFalse(syncTrashScanner.shouldExcludeFile(trashFile, rootDir))
        assertFalse(syncTrashScanner.shouldExcludeFile(normalFile, rootDir))

        val scannedWithTrash = syncTrashScanner.scanCustomMapping(mapping)
        assertTrue(scannedWithTrash.any { it.file.name == "DeletedNote.xopp" })
        assertTrue(scannedWithTrash.any { it.file.name == "ActiveNote.xopp" })
    }

    @Test
    fun testMultipleCustomMappingsMetadataIsolation() {
        val folderA = File(rootDir, "FolderA").apply { mkdirs() }
        val folderB = File(rootDir, "FolderB").apply { mkdirs() }

        val fileA = File(folderA, ".folder.json").apply { writeText("{\"color\": 1}") }
        val fileB = File(folderB, ".folder.json").apply { writeText("{\"color\": 2}") }

        val mappingA = dev.ilamparithi.aournalpp.backup.model.CustomFolderMapping(
            id = "mapping_a",
            serviceId = "srv_1",
            localFolderPath = folderA.absolutePath,
            remoteFolderPath = "RemoteA",
            isEnabled = true
        )
        val mappingB = dev.ilamparithi.aournalpp.backup.model.CustomFolderMapping(
            id = "mapping_b",
            serviceId = "srv_1",
            localFolderPath = folderB.absolutePath,
            remoteFolderPath = "RemoteB",
            isEnabled = true
        )

        val scanner = BackupScanner(env = null, exclusionFilter = ExclusionFilterConfig.DEFAULT)

        val scannedA = scanner.scanCustomMapping(mappingA)
        val scannedB = scanner.scanCustomMapping(mappingB)

        org.junit.Assert.assertEquals(1, scannedA.size)
        org.junit.Assert.assertEquals(1, scannedB.size)
        org.junit.Assert.assertEquals(".folder.json", scannedA[0].relativePath)
        org.junit.Assert.assertEquals(".folder.json", scannedB[0].relativePath)
        org.junit.Assert.assertEquals("custom_mapping_a", scannedA[0].scope)
        org.junit.Assert.assertEquals("custom_mapping_b", scannedB[0].scope)

        // Simulating the composite key in BackupEngine: "${scope}:${relativePath}"
        val keyA = "${scannedA[0].scope}:${scannedA[0].relativePath}"
        val keyB = "${scannedB[0].scope}:${scannedB[0].relativePath}"
        org.junit.Assert.assertNotEquals(keyA, keyB)
        org.junit.Assert.assertEquals("custom_mapping_a:.folder.json", keyA)
        org.junit.Assert.assertEquals("custom_mapping_b:.folder.json", keyB)
    }
}

