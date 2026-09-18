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

    @Test
    fun testNewCloudFilePrimaryRemoteSelectionFirstCloudOverMapping() {
        val nowEpoch = 1756543200000L
        val localAbsPath = "/data/data/dev.ilamparithi.aournalpp/files/home/Notes/History/Rome.xopp"

        // Custom Mapping endpoint (e.g. mapping template)
        val customMappingVer = FileVersionItem(
            source = FileVersionSource.REMOTE(
                serviceId = "nc-1",
                serviceName = "Nextcloud",
                providerType = StorageProviderType.NEXTCLOUD,
                mappingId = "m-history",
                mappingRemotePath = "HistoryVault/Rome"
            ),
            fileName = "Rome.xopp",
            relativePath = "HistoryVault/Rome/Rome.xopp",
            localFilePath = localAbsPath,
            sizeBytes = 2048L,
            lastModifiedEpochMs = nowEpoch + 10000L,
            contentHash = "hash_rome_123",
            remotePath = "HistoryVault/Rome/Rome.xopp"
        )

        // Complete Backup endpoint on Cloud (mappingId == null)
        val cloudCompleteVer = FileVersionItem(
            source = FileVersionSource.REMOTE(
                serviceId = "nc-1",
                serviceName = "Nextcloud",
                providerType = StorageProviderType.NEXTCLOUD,
                mappingId = null,
                mappingRemotePath = null
            ),
            fileName = "Rome.xopp",
            relativePath = "Notes/History/Rome.xopp",
            localFilePath = localAbsPath,
            sizeBytes = 2048L,
            lastModifiedEpochMs = nowEpoch,
            contentHash = "hash_rome_123",
            remotePath = "Aournalpp/Notes/History/Rome.xopp"
        )

        val remotes = listOf(customMappingVer, cloudCompleteVer)

        // Primary remote selection rule: first cloud (mappingId == null) is primary
        val primaryRemote = remotes.firstOrNull { 
            (it.source as? FileVersionSource.REMOTE)?.mappingId == null 
        } ?: remotes.first()

        assertEquals(cloudCompleteVer, primaryRemote)
        assertEquals("Nextcloud", primaryRemote.source.displayName)
    }

    @Test
    fun testNewCloudFilePrimaryRemoteSelectionWhenOnlyMappingsExist() {
        val nowEpoch = 1756543200000L
        val localAbsPath = "/data/data/dev.ilamparithi.aournalpp/files/home/Notes/Physics/Lab.xopp"

        val mappingA = FileVersionItem(
            source = FileVersionSource.REMOTE(
                serviceId = "nc-1",
                serviceName = "Nextcloud",
                providerType = StorageProviderType.NEXTCLOUD,
                mappingId = "m-alpha",
                mappingRemotePath = "Alpha/Lab"
            ),
            fileName = "Lab.xopp",
            relativePath = "Alpha/Lab/Lab.xopp",
            localFilePath = localAbsPath,
            sizeBytes = 1024L,
            lastModifiedEpochMs = nowEpoch,
            contentHash = "hash_lab",
            remotePath = "Alpha/Lab/Lab.xopp"
        )

        val mappingB = FileVersionItem(
            source = FileVersionSource.REMOTE(
                serviceId = "gd-1",
                serviceName = "Google Drive",
                providerType = StorageProviderType.GOOGLE_DRIVE,
                mappingId = "m-beta",
                mappingRemotePath = "Beta/Lab"
            ),
            fileName = "Lab.xopp",
            relativePath = "Beta/Lab/Lab.xopp",
            localFilePath = localAbsPath,
            sizeBytes = 1024L,
            lastModifiedEpochMs = nowEpoch + 5000L,
            contentHash = "hash_lab",
            remotePath = "Beta/Lab/Lab.xopp"
        )

        val remotes = listOf(mappingA, mappingB)

        // When only custom mappings are set, the first mapping is selected as primary
        val primaryRemote = remotes.firstOrNull { 
            (it.source as? FileVersionSource.REMOTE)?.mappingId == null 
        } ?: remotes.first()

        assertEquals(mappingA, primaryRemote)
        assertEquals("Nextcloud (Alpha/Lab)", primaryRemote.source.displayName)
    }

    @Test
    fun testNewCloudFileZeroDiscrepancyCheck() {
        val nowEpoch = 1756543200000L
        val v1 = FileVersionItem(
            source = FileVersionSource.REMOTE("nc-1", "Nextcloud", StorageProviderType.NEXTCLOUD),
            fileName = "Summary.xopp",
            relativePath = "Notes/Summary.xopp",
            localFilePath = "/data/Summary.xopp",
            sizeBytes = 4096L,
            lastModifiedEpochMs = nowEpoch,
            contentHash = "sha256_identical_abc",
            remotePath = "Aournalpp/Notes/Summary.xopp"
        )
        val v2 = FileVersionItem(
            source = FileVersionSource.REMOTE("gd-1", "Google Drive", StorageProviderType.GOOGLE_DRIVE),
            fileName = "Summary.xopp",
            relativePath = "Notes/Summary.xopp",
            localFilePath = "/data/Summary.xopp",
            sizeBytes = 4096L,
            lastModifiedEpochMs = nowEpoch + 15000L, // Different upload timestamp
            contentHash = "sha256_identical_abc", // Identical hash
            remotePath = "Aournalpp/Notes/Summary.xopp"
        )

        val remotes = listOf(v1, v2)
        var hasDiscrepancy = false
        for (i in 0 until remotes.size) {
            for (j in i + 1 until remotes.size) {
                val a = remotes[i]
                val b = remotes[j]
                if (a.sizeBytes != b.sizeBytes) {
                    hasDiscrepancy = true
                    break
                }
                if (a.contentHash != null && b.contentHash != null) {
                    if (!a.contentHash.equals(b.contentHash, ignoreCase = true)) {
                        hasDiscrepancy = true
                        break
                    }
                }
            }
        }

        // Even though timestamps differed by 15s, matching hash and size means zero discrepancy -> auto-pull
        org.junit.Assert.assertFalse(hasDiscrepancy)
    }

    @Test
    fun testNewCloudFileDiscrepancyCheckTriggersConflict() {
        val nowEpoch = 1756543200000L
        val v1Cloud = FileVersionItem(
            source = FileVersionSource.REMOTE("nc-1", "Nextcloud", StorageProviderType.NEXTCLOUD),
            fileName = "Draft.xopp",
            relativePath = "Notes/Draft.xopp",
            localFilePath = "/data/Draft.xopp",
            sizeBytes = 4096L,
            lastModifiedEpochMs = nowEpoch,
            contentHash = "hash_cloud_1",
            remotePath = "Aournalpp/Notes/Draft.xopp"
        )
        val v2Custom = FileVersionItem(
            source = FileVersionSource.REMOTE(
                serviceId = "nc-1",
                serviceName = "Nextcloud",
                providerType = StorageProviderType.NEXTCLOUD,
                mappingId = "m-draft",
                mappingRemotePath = "Work/Draft"
            ),
            fileName = "Draft.xopp",
            relativePath = "Work/Draft/Draft.xopp",
            localFilePath = "/data/Draft.xopp",
            sizeBytes = 4200L, // Different size
            lastModifiedEpochMs = nowEpoch + 5000L,
            contentHash = "hash_cloud_2",
            remotePath = "Work/Draft/Draft.xopp"
        )

        val remotes = listOf(v2Custom, v1Cloud)
        var hasDiscrepancy = false
        for (i in 0 until remotes.size) {
            for (j in i + 1 until remotes.size) {
                if (remotes[i].sizeBytes != remotes[j].sizeBytes ||
                    (remotes[i].contentHash != null && remotes[j].contentHash != null && remotes[i].contentHash != remotes[j].contentHash)) {
                    hasDiscrepancy = true
                    break
                }
            }
        }

        org.junit.Assert.assertTrue(hasDiscrepancy)

        // When creating conflict group, first cloud (v1Cloud) is primary and placed first
        val primaryRemote = remotes.firstOrNull { 
            (it.source as? FileVersionSource.REMOTE)?.mappingId == null 
        } ?: remotes.first()
        val orderedRemotes = listOf(primaryRemote) + (remotes.filter { it != primaryRemote })

        val conflictGroup = FileConflictGroup(
            relativePath = "Notes/Draft.xopp",
            localVersion = null,
            remoteVersions = orderedRemotes
        )

        org.junit.Assert.assertNull(conflictGroup.localVersion)
        assertEquals(v1Cloud, conflictGroup.remoteVersions[0])
        assertEquals("Nextcloud", conflictGroup.remoteVersions[0].source.displayName)
    }
}
