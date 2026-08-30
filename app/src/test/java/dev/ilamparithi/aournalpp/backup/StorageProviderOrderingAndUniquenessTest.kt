package dev.ilamparithi.aournalpp.backup

import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageProviderOrderingAndUniquenessTest {

    @Test
    fun testProviderOrderingServicesFirstThenProtocols() {
        val ordered = StorageProviderType.getOrderedTypes()

        // Google Drive and Nextcloud should be first (alphabetical)
        assertEquals(StorageProviderType.GOOGLE_DRIVE, ordered[0])
        assertEquals(StorageProviderType.NEXTCLOUD, ordered[1])

        // Protocols should follow (alphabetical: FTP, SFTP, SMB3, WebDAV)
        assertEquals(StorageProviderType.FTP, ordered[2])
        assertEquals(StorageProviderType.SFTP, ordered[3])
        assertEquals(StorageProviderType.SMB3, ordered[4])
        assertEquals(StorageProviderType.WEBDAV, ordered[5])
    }

    @Test
    fun testNextcloudAccountUniquenessKey() {
        val s1 = ServiceConfig(
            id = "1",
            name = "Work Nextcloud",
            providerType = StorageProviderType.NEXTCLOUD,
            serverUrl = "https://cloud.company.com/",
            username = "alice"
        )
        val s2 = ServiceConfig(
            id = "2",
            name = "Another Name",
            providerType = StorageProviderType.NEXTCLOUD,
            serverUrl = "https://cloud.company.com",
            username = "ALICE"
        )
        val s3 = ServiceConfig(
            id = "3",
            name = "Different User",
            providerType = StorageProviderType.NEXTCLOUD,
            serverUrl = "https://cloud.company.com",
            username = "bob"
        )

        assertEquals(s1.getAccountKey(), s2.getAccountKey())
        assertNotEquals(s1.getAccountKey(), s3.getAccountKey())
    }

    @Test
    fun testSftpAccountUniquenessKey() {
        val s1 = ServiceConfig(
            id = "1",
            name = "Home Server",
            providerType = StorageProviderType.SFTP,
            host = "192.168.1.50",
            port = 22,
            username = "user1"
        )
        val s2 = ServiceConfig(
            id = "2",
            name = "Home Server Duplicate",
            providerType = StorageProviderType.SFTP,
            host = "192.168.1.50",
            port = 22,
            username = "user1"
        )
        val s3 = ServiceConfig(
            id = "3",
            name = "Different Port",
            providerType = StorageProviderType.SFTP,
            host = "192.168.1.50",
            port = 2222,
            username = "user1"
        )

        assertEquals(s1.getAccountKey(), s2.getAccountKey())
        assertNotEquals(s1.getAccountKey(), s3.getAccountKey())
    }

    @Test
    fun testGoogleDriveAccountUniquenessKey() {
        val s1 = ServiceConfig(
            id = "1",
            name = "Personal Drive",
            providerType = StorageProviderType.GOOGLE_DRIVE,
            accountIdentifier = "user@gmail.com"
        )
        val s2 = ServiceConfig(
            id = "2",
            name = "Personal Drive 2",
            providerType = StorageProviderType.GOOGLE_DRIVE,
            accountIdentifier = "USER@GMAIL.COM"
        )
        val s3 = ServiceConfig(
            id = "3",
            name = "Work Drive",
            providerType = StorageProviderType.GOOGLE_DRIVE,
            accountIdentifier = "work@company.com"
        )

        assertEquals(s1.getAccountKey(), s2.getAccountKey())
        assertNotEquals(s1.getAccountKey(), s3.getAccountKey())
    }
}
