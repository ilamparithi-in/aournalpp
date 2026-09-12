package dev.ilamparithi.aournalpp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class FolderMetadataManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var rootDir: File
    private lateinit var manager: FolderMetadataManager
    private val cache = ConcurrentHashMap<String, Pair<Long, FolderMetaData>>()
    private var cacheInvalidated = false

    @Before
    fun setUp() {
        rootDir = tempFolder.newFolder("notes")
        cache.clear()
        cacheInvalidated = false

        manager = FolderMetadataManager(
            folderMetaCache = cache,
            isEmergencySavesFolder = { it.name.equals("Emergency Saves", ignoreCase = true) },
            getImportedCanonical = { File(rootDir, "Imported").canonicalPath },
            getAudioCanonical = { File(rootDir, "Audio").canonicalPath },
            onInvalidateCaches = { cacheInvalidated = true }
        )
    }

    @Test
    fun testDefaultMetadataForNonExistentMetaFile() {
        val folder = File(rootDir, "Math").apply { mkdirs() }
        val meta = manager.getFolderMeta(folder)

        assertNull(meta.colorHex)
        assertNull(meta.iconEmoji)
        assertNull(meta.iconType)
        assertNull(meta.role)
        assertFalse(meta.excludeFromRecents)
        assertFalse(meta.isPinned)
    }

    @Test
    fun testDetectedRoleForSpecialFolders() {
        val emergencyFolder = File(rootDir, "Emergency Saves").apply { mkdirs() }
        val emergencyMeta = manager.getFolderMeta(emergencyFolder)
        assertEquals("emergency", emergencyMeta.role)
        assertEquals(FolderMetadataManager.EMERGENCY_SAVES_DEFAULT_COLOR, emergencyMeta.colorHex)
        assertEquals(FolderMetadataManager.EMERGENCY_SAVES_DEFAULT_ICON, emergencyMeta.iconType)
        assertTrue(emergencyMeta.isPinned)

        val importedFolder = File(rootDir, "Imported").apply { mkdirs() }
        val importedMeta = manager.getFolderMeta(importedFolder)
        assertEquals("import", importedMeta.role)
        assertEquals("import", importedMeta.iconType)
        assertTrue(importedMeta.isPinned)

        val audioFolder = File(rootDir, "Audio").apply { mkdirs() }
        val audioMeta = manager.getFolderMeta(audioFolder)
        assertEquals("audio", audioMeta.role)
        assertEquals("audio", audioMeta.iconType)
        assertFalse(audioMeta.isPinned)
    }

    @Test
    fun testWriteAndReadFolderMeta() {
        val folder = File(rootDir, "Physics").apply { mkdirs() }

        val writeResult = manager.writeFolderMeta(
            folderDir = folder,
            colorHex = "#336699",
            iconEmoji = "⚛️",
            iconType = null,
            role = "study",
            excludeFromRecents = true,
            pinned = true
        )
        assertTrue(writeResult.isSuccess)
        assertTrue(cacheInvalidated)

        val metaFile = File(folder, FolderMetadataManager.FOLDER_META_FILE)
        assertTrue(metaFile.exists())
        assertEquals(".aoppfolder", metaFile.name)

        val meta = manager.getFolderMeta(folder)
        assertEquals("#336699", meta.colorHex)
        assertEquals("⚛️", meta.iconEmoji)
        assertNull(meta.iconType)
        assertEquals("study", meta.role)
        assertTrue(meta.excludeFromRecents)
        assertTrue(meta.isPinned)
    }

    @Test
    fun testMutations() {
        val folder = File(rootDir, "Chemistry").apply { mkdirs() }

        manager.setFolderColor(folder, "#00FF00")
        assertEquals("#00FF00", manager.getFolderMeta(folder).colorHex)

        manager.setFolderEmoji(folder, "🧪")
        assertEquals("🧪", manager.getFolderMeta(folder).iconEmoji)

        manager.setFolderExcludeFromRecents(folder, true)
        assertTrue(manager.getFolderMeta(folder).excludeFromRecents)

        manager.setFolderExcludeFromRecents(folder, false)
        assertFalse(manager.getFolderMeta(folder).excludeFromRecents)

        manager.setFolderPinned(folder, true)
        assertTrue(manager.getFolderMeta(folder).isPinned)

        manager.setFolderPinned(folder, false)
        assertFalse(manager.getFolderMeta(folder).isPinned)
    }

    @Test
    fun testUnpinningSpecialFolderPersistsAcrossCacheClears() {
        val emergencyFolder = File(rootDir, "Emergency Saves").apply { mkdirs() }
        assertTrue(manager.getFolderMeta(emergencyFolder).isPinned)

        // Explicitly unpin
        manager.setFolderPinned(emergencyFolder, false)
        assertFalse(manager.getFolderMeta(emergencyFolder).isPinned)

        // Simulate reinstall or complete cache wipe
        cache.clear()
        val reloadedMeta = manager.getFolderMeta(emergencyFolder)
        assertFalse(reloadedMeta.isPinned)
    }
}
