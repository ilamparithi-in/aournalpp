package dev.ilamparithi.aournalpp.backup

import android.content.Context
import android.util.Log
import dev.ilamparithi.aournalpp.backup.model.CustomFolderMapping
import dev.ilamparithi.aournalpp.backup.model.ExclusionFilterConfig
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import dev.ilamparithi.aournalpp.backup.security.CredentialsVault
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.UUID

class CredentialsVaultSerializationRoundTripTest {

    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0
        every { Log.w(any(), any<String>(), any<Throwable>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        CredentialsVault.resetInstanceForTesting()
        mockContext = mockk<Context>(relaxed = true)
        every { mockContext.applicationContext } returns mockContext
    }

    @After
    fun tearDown() {
        CredentialsVault.resetInstanceForTesting()
    }

    @Test
    fun testAllServiceConfigFieldsRoundTripIdentically() {
        val vault = CredentialsVault.createForTesting(mockContext)

        val serviceId = UUID.randomUUID().toString()
        val originalService = ServiceConfig(
            id = serviceId,
            name = "Enterprise Nextcloud Storage",
            providerType = StorageProviderType.NEXTCLOUD,
            serverUrl = "https://nextcloud.example.org:8443/remote.php/webdav",
            host = "nextcloud.example.org",
            port = 8443,
            username = "admin_user",
            passwordOrSecret = "super-secret-password-123",
            privateKey = "-----BEGIN OPENSSH PRIVATE KEY-----\ntest\n-----END OPENSSH PRIVATE KEY-----",
            privateKeyPassphrase = "passphrase-test",
            authToken = "oauth-access-token-xyz",
            refreshToken = "oauth-refresh-token-abc",
            tokenExpiryEpochMs = 1750000000000L,
            accountIdentifier = "user@company.com",
            shareName = "shared_notes_share",
            domain = "CORP_DOMAIN",
            remoteBasePath = "Notes/WorkArchive",
            isFtpsImplicit = true,
            isFtpsExplicit = false,
            isCompleteBackupEnabled = false,
            isEnabled = true,
            hostKeyFingerprint = "SHA256:4a3b2c1d0e...",
            lastSyncedAtEpochMs = 1720000000000L,
            lastSyncStatus = "Completed successfully",
            customMappings = listOf(
                CustomFolderMapping(
                    id = "mapping-1",
                    serviceId = serviceId,
                    name = "Projects Folder",
                    localFolderPath = "/storage/emulated/0/Documents/Projects",
                    remoteFolderPath = "Work/Projects",
                    isEnabled = true
                ),
                CustomFolderMapping(
                    id = "mapping-2",
                    serviceId = serviceId,
                    name = "Archive Folder",
                    localFolderPath = "/storage/emulated/0/Documents/Archive",
                    remoteFolderPath = "Work/Archive",
                    isEnabled = false
                )
            )
        )

        vault.saveService(originalService)
        vault.reloadServicesFromDisk()

        val loaded = vault.getService(serviceId)
        assertNotNull("Service should be loaded from vault after reload", loaded)

        // Validate all 23 scalar properties and collections
        assertEquals(originalService.id, loaded?.id)
        assertEquals(originalService.name, loaded?.name)
        assertEquals(originalService.providerType, loaded?.providerType)
        assertEquals(originalService.serverUrl, loaded?.serverUrl)
        assertEquals(originalService.host, loaded?.host)
        assertEquals(originalService.port, loaded?.port)
        assertEquals(originalService.username, loaded?.username)
        assertEquals(originalService.passwordOrSecret, loaded?.passwordOrSecret)
        assertEquals(originalService.privateKey, loaded?.privateKey)
        assertEquals(originalService.privateKeyPassphrase, loaded?.privateKeyPassphrase)
        assertEquals(originalService.authToken, loaded?.authToken)
        assertEquals(originalService.refreshToken, loaded?.refreshToken)
        assertEquals(originalService.tokenExpiryEpochMs, loaded?.tokenExpiryEpochMs)
        assertEquals(originalService.accountIdentifier, loaded?.accountIdentifier)
        assertEquals(originalService.shareName, loaded?.shareName)
        assertEquals(originalService.domain, loaded?.domain)
        assertEquals(originalService.remoteBasePath, loaded?.remoteBasePath)
        assertEquals(originalService.isFtpsImplicit, loaded?.isFtpsImplicit)
        assertEquals(originalService.isFtpsExplicit, loaded?.isFtpsExplicit)
        assertEquals(originalService.isCompleteBackupEnabled, loaded?.isCompleteBackupEnabled)
        assertEquals(originalService.isEnabled, loaded?.isEnabled)
        assertEquals(originalService.hostKeyFingerprint, loaded?.hostKeyFingerprint)
        assertEquals(originalService.lastSyncedAtEpochMs, loaded?.lastSyncedAtEpochMs)
        assertEquals(originalService.lastSyncStatus, loaded?.lastSyncStatus)

        // Validate nested customMappings
        assertEquals(2, loaded?.customMappings?.size)
        val m1 = loaded?.customMappings?.get(0)
        assertEquals("mapping-1", m1?.id)
        assertEquals(serviceId, m1?.serviceId)
        assertEquals("Projects Folder", m1?.name)
        assertEquals("/storage/emulated/0/Documents/Projects", m1?.localFolderPath)
        assertEquals("Work/Projects", m1?.remoteFolderPath)
        assertEquals(true, m1?.isEnabled)

        val m2 = loaded?.customMappings?.get(1)
        assertEquals("mapping-2", m2?.id)
        assertEquals(serviceId, m2?.serviceId)
        assertEquals("Archive Folder", m2?.name)
        assertEquals("/storage/emulated/0/Documents/Archive", m2?.localFolderPath)
        assertEquals("Work/Archive", m2?.remoteFolderPath)
        assertEquals(false, m2?.isEnabled)
    }

    @Test
    fun testTransientNetworkErrorMessageSanitization() {
        val vault = CredentialsVault.createForTesting(mockContext)
        val service = ServiceConfig(
            id = "test-transient-id",
            name = "Transient Error Test",
            providerType = StorageProviderType.WEBDAV,
            lastSyncStatus = "Connection timed out"
        )

        vault.saveService(service)
        vault.reloadServicesFromDisk()
        val loaded = vault.getService("test-transient-id")
        // Transient error should be sanitized to null on deserialization
        assertNull(loaded?.lastSyncStatus)
    }

    @Test
    fun testExclusionFilterConfigRoundTrip() {
        val vault = CredentialsVault.createForTesting(mockContext)
        val filter = ExclusionFilterConfig(
            isWhitelistMode = true,
            syncTrash = true,
            regexPatterns = listOf(".*\\.bak$", ".*\\.tmp$"),
            excludedExtensions = setOf("tmp", "log"),
            includedExtensions = setOf("xopp", "pdf"),
            excludedFolderPaths = setOf("/Trash", "/.backup"),
            skipDefaultTransient = false
        )

        vault.saveExclusionFilter(filter)
        val loaded = vault.getExclusionFilter()

        assertEquals(filter.isWhitelistMode, loaded.isWhitelistMode)
        assertEquals(filter.syncTrash, loaded.syncTrash)
        assertEquals(filter.regexPatterns, loaded.regexPatterns)
        assertEquals(filter.excludedExtensions, loaded.excludedExtensions)
        assertEquals(filter.includedExtensions, loaded.includedExtensions)
        assertEquals(filter.excludedFolderPaths, loaded.excludedFolderPaths)
        assertEquals(filter.skipDefaultTransient, loaded.skipDefaultTransient)
    }
}
