package dev.ilamparithi.aournalpp.backup

import dev.ilamparithi.aournalpp.backup.engine.ConflictPersistenceManager
import dev.ilamparithi.aournalpp.backup.model.FileConflictGroup
import dev.ilamparithi.aournalpp.backup.model.FileVersionItem
import dev.ilamparithi.aournalpp.backup.model.FileVersionSource
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ConflictPersistenceManagerTest {

    private lateinit var tempDir: File
    private lateinit var manager: ConflictPersistenceManager

    @Before
    fun setup() {
        tempDir = Files.createTempDirectory("conflicts_test").toFile()
        manager = ConflictPersistenceManager.createForTesting(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testSaveAndLoadConflicts() {
        val localVersion = FileVersionItem(
            source = FileVersionSource.LOCAL,
            fileName = "Note.xopp",
            relativePath = "Notes/Note.xopp",
            localFilePath = "/data/local/Note.xopp",
            sizeBytes = 1000L,
            lastModifiedEpochMs = 1700000000000L
        )
        val remoteVersion = FileVersionItem(
            source = FileVersionSource.REMOTE(
                serviceId = "gdrive-1",
                serviceName = "Google Drive",
                providerType = StorageProviderType.GOOGLE_DRIVE
            ),
            fileName = "Note.xopp",
            relativePath = "Notes/Note.xopp",
            localFilePath = "/data/local/Note.xopp",
            sizeBytes = 1200L,
            lastModifiedEpochMs = 1700000050000L,
            remotePath = "Aournalpp/Notes/Note.xopp"
        )
        val conflict = FileConflictGroup(
            id = "test-group-1",
            relativePath = "Notes/Note.xopp",
            localVersion = localVersion,
            remoteVersions = listOf(remoteVersion),
            description = "Conflicting edits on Note.xopp"
        )

        manager.setConflicts(listOf(conflict))

        val loaded = manager.unresolvedConflicts.value
        assertEquals(1, loaded.size)
        assertEquals("test-group-1", loaded[0].id)
        assertEquals("Notes/Note.xopp", loaded[0].relativePath)
        assertNotNull(loaded[0].localVersion)
        assertEquals(1, loaded[0].remoteVersions.size)
        assertEquals("gdrive-1", (loaded[0].remoteVersions[0].source as FileVersionSource.REMOTE).serviceId)

        // Verify disk persistence by creating a second manager pointing to same directory
        val manager2 = ConflictPersistenceManager.createForTesting(tempDir)
        val reloaded = manager2.unresolvedConflicts.value
        assertEquals(1, reloaded.size)
        assertEquals("test-group-1", reloaded[0].id)

        // Clear conflicts
        manager.clearConflicts()
        assertTrue(manager.unresolvedConflicts.value.isEmpty())

        val manager3 = ConflictPersistenceManager.createForTesting(tempDir)
        assertTrue(manager3.unresolvedConflicts.value.isEmpty())
    }

    @Test
    fun testAddAndDeduplicateConflicts() {
        val conflictA = FileConflictGroup(
            id = "group-a",
            relativePath = "Notes/DocA.xopp",
            localVersion = null,
            remoteVersions = emptyList()
        )
        val conflictB = FileConflictGroup(
            id = "group-b",
            relativePath = "Notes/DocB.xopp",
            localVersion = null,
            remoteVersions = emptyList()
        )

        manager.addConflicts(listOf(conflictA))
        assertEquals(1, manager.unresolvedConflicts.value.size)

        // Add both DocA and DocB; DocA should update, DocB should be added
        val conflictAUpdated = FileConflictGroup(
            id = "group-a-v2",
            relativePath = "Notes/DocA.xopp",
            localVersion = null,
            remoteVersions = emptyList(),
            description = "Updated DocA conflict"
        )
        manager.addConflicts(listOf(conflictAUpdated, conflictB))

        val current = manager.unresolvedConflicts.value
        assertEquals(2, current.size)
        val foundA = current.firstOrNull { it.relativePath == "Notes/DocA.xopp" }
        assertNotNull(foundA)
        assertEquals("group-a-v2", foundA?.id)
        assertEquals("Updated DocA conflict", foundA?.description)

        // Remove DocB
        manager.removeConflict("group-b")
        assertEquals(1, manager.unresolvedConflicts.value.size)
        assertEquals("group-a-v2", manager.unresolvedConflicts.value[0].id)
    }
}
