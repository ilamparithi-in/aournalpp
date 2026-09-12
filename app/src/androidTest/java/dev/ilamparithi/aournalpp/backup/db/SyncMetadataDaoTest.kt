package dev.ilamparithi.aournalpp.backup.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class SyncMetadataDaoTest {

    private lateinit var db: SyncDatabase
    private lateinit var dao: SyncMetadataDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SyncDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.syncMetadataDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        if (::db.isInitialized) {
            db.close()
        }
    }

    @Test
    fun testInsertAndQueryMetadataByServiceScopeAndPath() = runBlocking {
        val entity = SyncMetadataEntity(
            serviceId = "webdav_main",
            relativePath = "notes/Math.xopp",
            scope = "notes",
            localSha256 = "hash123",
            remoteHash = "etag456",
            localLastModified = 1000L,
            sizeBytes = 2048L,
            lastSyncedAt = 5000L
        )

        dao.insertOrUpdate(entity)

        val retrieved = dao.getByServiceScopeAndPath("webdav_main", "notes", "notes/Math.xopp")
        assertNotNull(retrieved)
        assertEquals("hash123", retrieved?.localSha256)
        assertEquals("etag456", retrieved?.remoteHash)
        assertEquals(2048L, retrieved?.sizeBytes)
    }

    @Test
    fun testInsertOrUpdateUpdatesExistingEntityOnPrimaryKeyCollision() = runBlocking {
        val initial = SyncMetadataEntity(
            serviceId = "gdrive",
            relativePath = "notes/Physics.xopp",
            scope = "notes",
            localSha256 = "hashA",
            remoteHash = "etagA",
            localLastModified = 1000L,
            sizeBytes = 1024L,
            lastSyncedAt = 2000L
        )
        dao.insertOrUpdate(initial)

        val updated = initial.copy(
            localSha256 = "hashB",
            remoteHash = "etagB",
            sizeBytes = 1050L,
            lastSyncedAt = 3000L
        )
        dao.insertOrUpdate(updated)

        val retrieved = dao.getByServiceScopeAndPath("gdrive", "notes", "notes/Physics.xopp")
        assertNotNull(retrieved)
        assertEquals("hashB", retrieved?.localSha256)
        assertEquals("etagB", retrieved?.remoteHash)
        assertEquals(1050L, retrieved?.sizeBytes)
    }

    @Test
    fun testDeleteAndClearAllOperations() = runBlocking {
        val e1 = SyncMetadataEntity(
            serviceId = "nextcloud",
            relativePath = "notes/Chem.xopp",
            scope = "notes",
            localSha256 = "h1",
            remoteHash = "r1",
            localLastModified = 1000L,
            sizeBytes = 500L,
            lastSyncedAt = 2000L
        )
        val e2 = SyncMetadataEntity(
            serviceId = "nextcloud",
            relativePath = "configs/settings.xml",
            scope = "config",
            localSha256 = "h2",
            remoteHash = "r2",
            localLastModified = 1000L,
            sizeBytes = 800L,
            lastSyncedAt = 2000L
        )

        dao.insertOrUpdateAll(listOf(e1, e2))
        assertEquals(2, dao.getAllForService("nextcloud").size)

        dao.delete("nextcloud", "notes", "notes/Chem.xopp")
        assertEquals(1, dao.getAllForService("nextcloud").size)
        assertNull(dao.getByServiceScopeAndPath("nextcloud", "notes", "notes/Chem.xopp"))

        dao.clearAll()
        assertTrue(dao.getAllForService("nextcloud").isEmpty())
    }
}
