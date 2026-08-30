package dev.ilamparithi.aournalpp.backup

import dev.ilamparithi.aournalpp.backup.model.ConflictResolutionAction
import dev.ilamparithi.aournalpp.backup.model.FileConflictGroup
import dev.ilamparithi.aournalpp.backup.model.FileVersionItem
import dev.ilamparithi.aournalpp.backup.model.FileVersionSource
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class MultiServiceConflictTest {

    @Test
    fun testSingleCloudServiceConflictGroup() {
        val nowEpoch = 1756543200000L // UTC Epoch ms
        val localVersion = FileVersionItem(
            source = FileVersionSource.LOCAL,
            fileName = "Calculus.xopp",
            relativePath = "Notes/Math/Calculus.xopp",
            localFilePath = "/data/data/dev.ilamparithi.aournalpp/files/home/Notes/Math/Calculus.xopp",
            sizeBytes = 1024L,
            lastModifiedEpochMs = nowEpoch - 60000L, // 1 min older
            contentHash = "hash_local_123"
        )

        val nextcloudVersion = FileVersionItem(
            source = FileVersionSource.REMOTE(
                serviceId = "nc-1",
                serviceName = "My Nextcloud",
                providerType = StorageProviderType.NEXTCLOUD
            ),
            fileName = "Calculus.xopp",
            relativePath = "Notes/Math/Calculus.xopp",
            localFilePath = "/data/data/dev.ilamparithi.aournalpp/files/home/Notes/Math/Calculus.xopp",
            sizeBytes = 2048L,
            lastModifiedEpochMs = nowEpoch,
            contentHash = "hash_nc_456",
            remotePath = "Aournalpp/Notes/Math/Calculus.xopp"
        )

        val group = FileConflictGroup(
            relativePath = "Notes/Math/Calculus.xopp",
            localVersion = localVersion,
            remoteVersions = listOf(nextcloudVersion)
        )

        assertEquals("Calculus.xopp", group.fileName)
        assertEquals(2, group.allVersions.size)

        // Newest version should be Nextcloud
        val newest = group.allVersions.maxByOrNull { it.lastModifiedEpochMs }
        assertNotNull(newest)
        assertEquals(nextcloudVersion, newest)
    }

    @Test
    fun testMultiCloudAndCustomMappingsConflictDetection() {
        val nowEpoch = 1756543200000L
        val localAbsPath = "/data/data/dev.ilamparithi.aournalpp/files/home/Notes/Biology/Lecture.xopp"

        val local = FileVersionItem(
            source = FileVersionSource.LOCAL,
            fileName = "Lecture.xopp",
            relativePath = "Notes/Biology/Lecture.xopp",
            localFilePath = localAbsPath,
            sizeBytes = 4096L,
            lastModifiedEpochMs = nowEpoch - 120000L,
            contentHash = "hash_local"
        )

        // Complete Backup endpoint on Nextcloud
        val nextcloudComplete = FileVersionItem(
            source = FileVersionSource.REMOTE("nc-1", "Nextcloud", StorageProviderType.NEXTCLOUD),
            fileName = "Lecture.xopp",
            relativePath = "Notes/Biology/Lecture.xopp",
            localFilePath = localAbsPath,
            sizeBytes = 4120L,
            lastModifiedEpochMs = nowEpoch - 60000L,
            contentHash = "hash_nc_complete",
            remotePath = "Aournalpp/Notes/Biology/Lecture.xopp"
        )

        // Custom Mapping endpoint on Nextcloud
        val nextcloudCustomMapping = FileVersionItem(
            source = FileVersionSource.REMOTE(
                serviceId = "nc-1",
                serviceName = "Nextcloud",
                providerType = StorageProviderType.NEXTCLOUD,
                mappingId = "m-1",
                mappingRemotePath = "University/Bio"
            ),
            fileName = "Lecture.xopp",
            relativePath = "University/Bio/Lecture.xopp",
            localFilePath = localAbsPath,
            sizeBytes = 4500L,
            lastModifiedEpochMs = nowEpoch - 30000L,
            contentHash = "hash_nc_custom",
            remotePath = "University/Bio/Lecture.xopp"
        )

        // Custom Mapping endpoint on Google Drive
        val googleDriveCustom = FileVersionItem(
            source = FileVersionSource.REMOTE(
                serviceId = "gd-1",
                serviceName = "Google Drive",
                providerType = StorageProviderType.GOOGLE_DRIVE,
                mappingId = "m-2",
                mappingRemotePath = "Shared/BiologyNotes"
            ),
            fileName = "Lecture.xopp",
            relativePath = "Shared/BiologyNotes/Lecture.xopp",
            localFilePath = localAbsPath,
            sizeBytes = 5000L,
            lastModifiedEpochMs = nowEpoch,
            contentHash = "hash_gd_custom",
            remotePath = "Shared/BiologyNotes/Lecture.xopp"
        )

        val group = FileConflictGroup(
            relativePath = "Notes/Biology/Lecture.xopp",
            localVersion = local,
            remoteVersions = listOf(nextcloudComplete, nextcloudCustomMapping, googleDriveCustom)
        )

        assertEquals(4, group.allVersions.size)
        assertEquals(3, group.remoteVersions.size)

        // Verify display names
        assertEquals("Nextcloud", nextcloudComplete.source.displayName)
        assertEquals("Nextcloud (University/Bio)", nextcloudCustomMapping.source.displayName)
        assertEquals("Google Drive (Shared/BiologyNotes)", googleDriveCustom.source.displayName)

        // Verify alongside filenames
        val nameWithoutExt = File(local.fileName).nameWithoutExtension
        val ext = ".xopp"

        val ncCompleteName = "$nameWithoutExt (${nextcloudComplete.source.sanitizedFileSuffix})$ext"
        val ncCustomName = "$nameWithoutExt (${nextcloudCustomMapping.source.sanitizedFileSuffix})$ext"
        val gdCustomName = "$nameWithoutExt (${googleDriveCustom.source.sanitizedFileSuffix})$ext"

        assertEquals("Lecture (Nextcloud).xopp", ncCompleteName)
        assertEquals("Lecture (Nextcloud - University_Bio).xopp", ncCustomName)
        assertEquals("Lecture (Google Drive - Shared_BiologyNotes).xopp", gdCustomName)
    }
}
