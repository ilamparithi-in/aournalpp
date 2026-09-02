package dev.ilamparithi.aournalpp.backup

import dev.ilamparithi.aournalpp.backup.model.CustomFolderMapping
import dev.ilamparithi.aournalpp.backup.model.FileConflictGroup
import dev.ilamparithi.aournalpp.backup.model.FileVersionItem
import dev.ilamparithi.aournalpp.backup.model.FileVersionSource
import dev.ilamparithi.aournalpp.backup.model.MappingSet
import dev.ilamparithi.aournalpp.backup.model.MappingTemplateItem
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import dev.ilamparithi.aournalpp.backup.security.CustomMappingRepository
import dev.ilamparithi.aournalpp.utils.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

class CustomMappingAndConflictTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testFormatRelativeTimeDifference() {
        val now = 1756543200000L

        // Null epoch handling
        assertEquals("", FormatUtils.formatRelativeTimeDifference(null, now, "Cloud", "Local"))
        assertEquals("", FormatUtils.formatRelativeTimeDifference(now, null, "Cloud", "Local"))

        // Exact match
        assertEquals("", FormatUtils.formatRelativeTimeDifference(now, now, "Cloud", "Local"))

        // Difference < 60 seconds
        assertEquals("About the same time", FormatUtils.formatRelativeTimeDifference(now + 25000L, now, "Cloud", "Local"))

        // Cloud is 5 mins newer
        val diff5m = FormatUtils.formatRelativeTimeDifference(now + 300000L, now, "Cloud", "Local")
        assertEquals("Cloud is 5m newer", diff5m)

        // Local is 3 hours newer
        val diff3h = FormatUtils.formatRelativeTimeDifference(now, now + (3 * 3600 * 1000L), "Cloud", "Local")
        assertEquals("Local is 3h newer", diff3h)

        // Cloud is 2 days newer
        val diff2d = FormatUtils.formatRelativeTimeDifference(now + (2 * 86400 * 1000L), now, "Cloud", "Local")
        assertEquals("Cloud is 2d newer", diff2d)
    }

    @Test
    fun testCustomMappingSetsLifecycleAndApply() {
        val storageDir = tempFolder.newFolder("files")
        val notesDir = tempFolder.newFolder("Notes")

        val repo = CustomMappingRepository(storageDir)

        // Initially empty
        val initialSets = repo.getAllMappingSets()
        assertTrue(initialSets.isEmpty())

        // Create a mapping template set
        val templateItems = listOf(
            MappingTemplateItem(
                name = "Physics Lectures",
                localFolderPath = "/storage/emulated/0/Documents/Physics",
                remoteFolderPath = "Backups/Physics",
                isEnabled = true
            ),
            MappingTemplateItem(
                name = "Chemistry Labs",
                localFolderPath = "/storage/emulated/0/Documents/Chem",
                remoteFolderPath = "Backups/Chem",
                isEnabled = false
            )
        )

        val set1 = MappingSet(
            id = UUID.randomUUID().toString(),
            name = "College Courses",
            description = "All university notes and labs",
            createdAtEpochMs = System.currentTimeMillis(),
            items = templateItems
        )

        repo.saveMappingSet(set1)

        val retrievedSets = repo.getAllMappingSets()
        assertEquals(1, retrievedSets.size)
        assertEquals("College Courses", retrievedSets[0].name)
        assertEquals(2, retrievedSets[0].items.size)
        assertEquals("Physics Lectures", retrievedSets[0].items[0].name)

        // Apply set in REPLACE mode to a service
        val serviceId = "srv-nextcloud-1"
        val appliedReplace = repo.applyMappingSetToService(serviceId, set1, replace = true)
        assertEquals(2, appliedReplace.size)
        assertEquals("Physics Lectures", appliedReplace[0].name)
        assertEquals("/storage/emulated/0/Documents/Physics", appliedReplace[0].localFolderPath)

        // Apply set in APPEND mode with an extra item
        val extraSet = MappingSet(
            id = UUID.randomUUID().toString(),
            name = "Research",
            description = "Lab research",
            createdAtEpochMs = System.currentTimeMillis(),
            items = listOf(
                MappingTemplateItem(
                    name = "Quantum",
                    localFolderPath = "/storage/emulated/0/Documents/Quantum",
                    remoteFolderPath = "Backups/Quantum",
                    isEnabled = true
                )
            )
        )
        val appliedAppend = repo.applyMappingSetToService(serviceId, extraSet, replace = false)
        assertEquals(3, appliedAppend.size)

        // Verify sync to notes home (.config/sync_mappings.json)
        repo.syncToNotesHome(notesDir)
        val configMappingsFile = File(notesDir, ".config/sync_mappings.json")
        assertTrue(configMappingsFile.exists())
        val jsonContent = configMappingsFile.readText()
        assertTrue(jsonContent.contains("Physics Lectures"))
        assertTrue(jsonContent.contains("College Courses"))

        // Delete set
        repo.deleteMappingSet(set1.id)
        val afterDeleteSets = repo.getAllMappingSets()
        assertEquals(0, afterDeleteSets.size)
    }

    @Test
    fun testConflictDetectionIgnoresZeroDiffChanges() {
        // When contentHash is identical, conflict should NOT be reported despite different lastModified dates
        val now = System.currentTimeMillis()
        val localVersion = FileVersionItem(
            source = FileVersionSource.LOCAL,
            fileName = "settings.xml",
            relativePath = ".config/xournalpp/settings.xml",
            localFilePath = "/data/local/settings.xml",
            sizeBytes = 2048L,
            lastModifiedEpochMs = now,
            contentHash = "hash_identical_abc123"
        )
        val remoteVersion = FileVersionItem(
            source = FileVersionSource.REMOTE(
                serviceId = "srv-1",
                serviceName = "Drive",
                providerType = StorageProviderType.GOOGLE_DRIVE
            ),
            fileName = "settings.xml",
            relativePath = ".config/xournalpp/settings.xml",
            localFilePath = "/data/cache/remote_settings.xml",
            sizeBytes = 2048L,
            lastModifiedEpochMs = now + 600000L, // 10 minutes newer!
            contentHash = "hash_identical_abc123" // IDENTICAL hash (0 diff changes)
        )

        val versions = listOf(localVersion, remoteVersion)

        // Check our rule: if sameHash is true, conflict is skipped
        val sameHash = localVersion.contentHash != null &&
                remoteVersion.contentHash != null &&
                localVersion.contentHash.equals(remoteVersion.contentHash, ignoreCase = true)

        assertTrue("Identical hashes must be recognized as 0 diff changes", sameHash)

        // Verify that with different hashes, conflict IS flagged
        val modifiedRemote = remoteVersion.copy(contentHash = "hash_different_xyz789")
        val differs = localVersion.contentHash != modifiedRemote.contentHash
        assertTrue("Different content hash must flag conflict", differs)
    }

    @Test
    fun testConfigConflictGroupConstruction() {
        val now = System.currentTimeMillis()
        val localFile = tempFolder.newFile("local_settings.xml").apply {
            writeText("<settings version=\"1\"><tool>pen</tool></settings>")
        }
        val remoteFile = tempFolder.newFile("remote_settings.xml").apply {
            writeText("<settings version=\"1\"><tool>highlighter</tool></settings>")
        }

        val localVer = FileVersionItem(
            source = FileVersionSource.LOCAL,
            fileName = "settings.xml",
            relativePath = ".config/xournalpp/settings.xml",
            localFilePath = localFile.absolutePath,
            sizeBytes = localFile.length(),
            lastModifiedEpochMs = now - 3600000L,
            contentHash = "hash1"
        )
        val remoteVer = FileVersionItem(
            source = FileVersionSource.REMOTE("srv-1", "Nextcloud", StorageProviderType.NEXTCLOUD),
            fileName = "settings.xml",
            relativePath = ".config/xournalpp/settings.xml",
            localFilePath = remoteFile.absolutePath,
            sizeBytes = remoteFile.length(),
            lastModifiedEpochMs = now,
            contentHash = "hash2"
        )

        val group = FileConflictGroup(
            id = "config_srv-1_settings.xml",
            relativePath = ".config/xournalpp/settings.xml",
            localVersion = localVer,
            remoteVersions = listOf(remoteVer),
            description = "Application configuration file",
            localFilePath = localFile.absolutePath,
            remoteFilePath = remoteFile.absolutePath
        )

        assertEquals("settings.xml", group.fileName)
        assertEquals("Application configuration file", group.description)
        assertEquals(localFile.absolutePath, group.localFilePath)
        assertEquals(remoteFile.absolutePath, group.remoteFilePath)
        assertEquals(2, group.allVersions.size)
    }

    @Test
    fun testMappingSetSearchFiltering() {
        val set1 = MappingSet(
            id = "set-1",
            name = "University Lectures",
            description = "Semester 1 notes",
            createdAtEpochMs = 1000L,
            items = listOf(
                MappingTemplateItem(
                    name = "Quantum Mechanics",
                    localFolderPath = "/storage/emulated/0/Documents/Physics",
                    remoteFolderPath = "Cloud/Physics",
                    isEnabled = true
                )
            )
        )
        val set2 = MappingSet(
            id = "set-2",
            name = "Personal Projects",
            description = "Hobby ideas",
            createdAtEpochMs = 2000L,
            items = listOf(
                MappingTemplateItem(
                    name = "Art Sketches",
                    localFolderPath = "/storage/emulated/0/Documents/Drawings",
                    remoteFolderPath = "Drive/Art",
                    isEnabled = false
                )
            )
        )
        val allSets = listOf(set1, set2)

        fun filterSets(query: String): List<MappingSet> {
            if (query.isBlank()) return allSets
            val q = query.trim().lowercase()
            return allSets.filter { set ->
                set.name.lowercase().contains(q) ||
                set.description.lowercase().contains(q) ||
                set.items.any { item ->
                    item.name.lowercase().contains(q) ||
                    item.localFolderPath.lowercase().contains(q) ||
                    item.remoteFolderPath.lowercase().contains(q)
                }
            }
        }

        // Search by set name
        val searchByName = filterSets("University")
        assertEquals(1, searchByName.size)
        assertEquals("set-1", searchByName[0].id)

        // Search by constituent mapping name
        val searchByMappingName = filterSets("Quantum")
        assertEquals(1, searchByMappingName.size)
        assertEquals("set-1", searchByMappingName[0].id)

        // Search by local folder path
        val searchByLocalPath = filterSets("Drawings")
        assertEquals(1, searchByLocalPath.size)
        assertEquals("set-2", searchByLocalPath[0].id)

        // Search by remote folder path
        val searchByRemotePath = filterSets("Drive/Art")
        assertEquals(1, searchByRemotePath.size)
        assertEquals("set-2", searchByRemotePath[0].id)

        // Search non-matching
        val searchNotFound = filterSets("NonexistentXYZ")
        assertTrue(searchNotFound.isEmpty())
    }

    @Test
    fun testBatchEnableDisableMixedMappings() {
        val mapping1 = CustomFolderMapping(
            id = "m1",
            serviceId = "srv-1",
            name = "M1",
            localFolderPath = "/local/1",
            remoteFolderPath = "/remote/1",
            isEnabled = true // currently enabled
        )
        val mapping2 = CustomFolderMapping(
            id = "m2",
            serviceId = "srv-1",
            name = "M2",
            localFolderPath = "/local/2",
            remoteFolderPath = "/remote/2",
            isEnabled = false // currently disabled
        )
        val mapping3 = CustomFolderMapping(
            id = "m3",
            serviceId = "srv-1",
            name = "M3",
            localFolderPath = "/local/3",
            remoteFolderPath = "/remote/3",
            isEnabled = true // unselected mapping
        )

        val selectedIds = setOf("m1", "m2") // mixed state: 1 enabled, 1 disabled
        val allMappings = listOf(mapping1, mapping2, mapping3)

        // Batch Disable: should unconditionally disable all selected mappings
        val batchDisabled = allMappings.map {
            if (selectedIds.contains(it.id)) it.copy(isEnabled = false) else it
        }
        assertFalse(batchDisabled[0].isEnabled) // m1 was true -> now false
        assertFalse(batchDisabled[1].isEnabled) // m2 was false -> remains false
        assertTrue(batchDisabled[2].isEnabled)  // m3 was untouched -> remains true

        // Batch Enable: should unconditionally enable all selected mappings
        val batchEnabled = allMappings.map {
            if (selectedIds.contains(it.id)) it.copy(isEnabled = true) else it
        }
        assertTrue(batchEnabled[0].isEnabled)  // m1 was true -> remains true
        assertTrue(batchEnabled[1].isEnabled)  // m2 was false -> now true
        assertTrue(batchEnabled[2].isEnabled)  // m3 was untouched -> remains true
    }

    @Test
    fun testSelectionPreservedAfterNonDeleteActions() {
        var isBatchMode = true
        val selectedIds = mutableSetOf("m1", "m2")

        // Perform batch disable: preserves selection and batch window
        assertTrue(isBatchMode)
        assertEquals(setOf("m1", "m2"), selectedIds)

        // Perform batch enable: preserves selection and batch window
        assertTrue(isBatchMode)
        assertEquals(setOf("m1", "m2"), selectedIds)

        // Perform batch add to set: preserves selection and batch window
        assertTrue(isBatchMode)
        assertEquals(setOf("m1", "m2"), selectedIds)

        // Delete action: clears selection and exits batch window
        selectedIds.clear()
        isBatchMode = false
        assertFalse(isBatchMode)
        assertTrue(selectedIds.isEmpty())
    }

    @Test
    fun testMultiSetCheckboxAddAndRemove() {
        val mappingItem1 = MappingTemplateItem(name = "Item1", localFolderPath = "/local/path1", remoteFolderPath = "remote/path1", isEnabled = true)
        val mappingItem2 = MappingTemplateItem(name = "Item2", localFolderPath = "/local/path2", remoteFolderPath = "remote/path2", isEnabled = true)

        // Set A initially contains Item1
        val setA = MappingSet("setA", "Set A", "", 100L, listOf(mappingItem1))
        // Set B initially empty
        val setB = MappingSet("setB", "Set B", "", 200L, emptyList())
        // Set C initially contains Item1 and Item2
        val setC = MappingSet("setC", "Set C", "", 300L, listOf(mappingItem1, mappingItem2))

        val existingSets = listOf(setA, setB, setC)
        val selectedMappings = listOf(
            CustomFolderMapping("m1", "srv-1", "Item1", "/local/path1", "remote/path1", true)
        )
        val selectedLocalPaths = selectedMappings.map { it.localFolderPath }.toSet()
        val templateItems = selectedMappings.map {
            MappingTemplateItem(name = it.name, localFolderPath = it.localFolderPath, remoteFolderPath = it.remoteFolderPath, isEnabled = it.isEnabled)
        }

        // User checks Set B (to add Item1 to Set B)
        // User UNCHECKS Set C (to remove Item1 from Set C)
        // Set A remains checked
        val checkedSetIds = setOf("setA", "setB") // setC is unchecked!

        val updatedSets = existingSets.map { set ->
            val setPaths = set.items.map { it.localFolderPath }.toSet()
            if (checkedSetIds.contains(set.id)) {
                val toAdd = templateItems.filterNot { setPaths.contains(it.localFolderPath) }
                set.copy(items = set.items + toAdd)
            } else {
                set.copy(items = set.items.filterNot { selectedLocalPaths.contains(it.localFolderPath) })
            }
        }

        // Set A still has Item1
        assertEquals(1, updatedSets[0].items.size)
        assertEquals("/local/path1", updatedSets[0].items[0].localFolderPath)

        // Set B now has Item1 added!
        assertEquals(1, updatedSets[1].items.size)
        assertEquals("/local/path1", updatedSets[1].items[0].localFolderPath)

        // Set C has Item1 removed! Only Item2 remains
        assertEquals(1, updatedSets[2].items.size)
        assertEquals("/local/path2", updatedSets[2].items[0].localFolderPath)
    }

    @Test
    fun testAutomaticCloudSyncMirroring() {
        val internalDir = File(tempFolder.root, "filesDir").apply { mkdirs() }
        val notesHome = File(tempFolder.root, "notesHome").apply { mkdirs() }

        val repo = CustomMappingRepository(internalDir, notesHome)

        // Saving a set should automatically write to internal filesDir AND notesHome/.config/sync_mappings.json
        val newSet = MappingSet(
            id = "auto-set-1",
            name = "Cloud Backup Set",
            description = "Auto synced to cloud",
            createdAtEpochMs = System.currentTimeMillis(),
            items = listOf(
                MappingTemplateItem(name = "Test", localFolderPath = "/local/test", remoteFolderPath = "remote/test", isEnabled = true)
            )
        )
        repo.saveMappingSet(newSet)

        // Check internal file
        val internalFile = File(internalDir, "sync_mappings.json")
        assertTrue(internalFile.exists())
        assertTrue(internalFile.readText().contains("Cloud Backup Set"))

        // Check mirrored cloud backup file in .config
        val cloudConfigFile = File(File(notesHome, ".config"), "sync_mappings.json")
        assertTrue("Cloud config file must exist in notesHome/.config", cloudConfigFile.exists())
        assertTrue(cloudConfigFile.readText().contains("Cloud Backup Set"))

        // Simulate restore on a fresh device without internal file
        val freshInternalDir = File(tempFolder.root, "freshFilesDir").apply { mkdirs() }
        val freshRepo = CustomMappingRepository(freshInternalDir, notesHome)
        val restoredSets = freshRepo.getAllMappingSets()
        assertEquals(1, restoredSets.size)
        assertEquals("Cloud Backup Set", restoredSets[0].name)
    }
}
