package dev.ilamparithi.aournalpp.backup

import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import dev.ilamparithi.aournalpp.backup.provider.FtpStorageProvider
import dev.ilamparithi.aournalpp.backup.provider.SftpStorageProvider
import dev.ilamparithi.aournalpp.backup.provider.SmbStorageProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class StorageProviderPathResolutionTest {

    @Test
    fun testSftpResolveRemotePathDoesNotDuplicateBasePath() {
        val config = ServiceConfig(
            id = "sftp-1",
            name = "SFTP Backup",
            providerType = StorageProviderType.SFTP,
            remoteBasePath = "Aournalpp",
            host = "localhost"
        )
        val provider = SftpStorageProvider(config)

        // If path already starts with base, do NOT duplicate
        assertEquals("Aournalpp", provider.resolveRemotePath("Aournalpp"))
        assertEquals("Aournalpp/Notes", provider.resolveRemotePath("Aournalpp/Notes"))
        assertEquals("Aournalpp/Notes", provider.resolveRemotePath("/Aournalpp/Notes/"))
        assertEquals("Aournalpp/Notes/file.xopp", provider.resolveRemotePath("Aournalpp/Notes/file.xopp"))

        // If path is relative without base, prepend base
        assertEquals("Aournalpp/Notes", provider.resolveRemotePath("Notes"))
        assertEquals("Aournalpp/Notes/file.xopp", provider.resolveRemotePath("Notes/file.xopp"))

        // If empty path or root
        assertEquals("Aournalpp", provider.resolveRemotePath(""))
        assertEquals("Aournalpp", provider.resolveRemotePath("/"))
    }

    @Test
    fun testSftpResolveRemotePathWithEmptyBasePath() {
        val config = ServiceConfig(
            id = "sftp-2",
            name = "SFTP Root",
            providerType = StorageProviderType.SFTP,
            remoteBasePath = "",
            host = "localhost"
        )
        val provider = SftpStorageProvider(config)

        assertEquals("Notes", provider.resolveRemotePath("Notes"))
        assertEquals("Notes/file.xopp", provider.resolveRemotePath("Notes/file.xopp"))
        assertEquals("", provider.resolveRemotePath(""))
    }

    @Test
    fun testFtpResolveRemotePathDoesNotDuplicateBasePath() {
        val config = ServiceConfig(
            id = "ftp-1",
            name = "FTP Backup",
            providerType = StorageProviderType.FTP,
            remoteBasePath = "Aournalpp",
            host = "localhost"
        )
        val provider = FtpStorageProvider(config)

        assertEquals("Aournalpp", provider.resolveRemotePath("Aournalpp"))
        assertEquals("Aournalpp/Notes", provider.resolveRemotePath("Aournalpp/Notes"))
        assertEquals("Aournalpp/Notes", provider.resolveRemotePath("/Aournalpp/Notes/"))
        assertEquals("Aournalpp/Notes/lecture.xopp", provider.resolveRemotePath("Aournalpp/Notes/lecture.xopp"))

        assertEquals("Aournalpp/Notes", provider.resolveRemotePath("Notes"))
        assertEquals("Aournalpp/Notes/lecture.xopp", provider.resolveRemotePath("Notes/lecture.xopp"))
    }

    @Test
    fun testSmbNormalizePathDoesNotDuplicateBasePath() {
        val config = ServiceConfig(
            id = "smb-1",
            name = "SMB Backup",
            providerType = StorageProviderType.SMB3,
            remoteBasePath = "Aournalpp",
            shareName = "Backups",
            host = "localhost"
        )
        val provider = SmbStorageProvider(config)

        assertEquals("Aournalpp", provider.normalizePath("Aournalpp"))
        assertEquals("Aournalpp\\Notes", provider.normalizePath("Aournalpp/Notes"))
        assertEquals("Aournalpp\\Notes", provider.normalizePath("Aournalpp\\Notes"))
        assertEquals("Aournalpp\\Notes\\math.xopp", provider.normalizePath("Aournalpp/Notes/math.xopp"))

        assertEquals("Aournalpp\\Notes", provider.normalizePath("Notes"))
        assertEquals("Aournalpp\\Notes\\math.xopp", provider.normalizePath("Notes\\math.xopp"))
        assertEquals("Aournalpp", provider.normalizePath(""))
    }
}
