package dev.ilamparithi.aournalpp.backup

import android.content.Context
import android.util.Log
import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import dev.ilamparithi.aournalpp.backup.security.CredentialsVault
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class CredentialsVaultTest {

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
        unmockkStatic(Log::class)
    }

    @Test
    fun testSingletonInstanceIdentity() {
        val instance1 = CredentialsVault.getInstance(mockContext)
        val instance2 = CredentialsVault.getInstance(mockContext)

        assertSame("getInstance must return the exact same instance across calls", instance1, instance2)
    }

    @Test
    fun testSaveServiceUpdatesStateFlowAndGetAllServices() {
        val vault = CredentialsVault.createForTesting(mockContext)
        assertTrue(vault.getAllServices().isEmpty())
        assertTrue(vault.servicesFlow.value.isEmpty())

        val gdriveService = ServiceConfig(
            id = UUID.randomUUID().toString(),
            name = "My Google Drive",
            providerType = StorageProviderType.GOOGLE_DRIVE,
            accountIdentifier = "user@example.com",
            authToken = "test-token-123",
            refreshToken = "test-refresh-123",
            isCompleteBackupEnabled = true,
            isEnabled = true
        )

        vault.saveService(gdriveService)

        // Verify synchronous StateFlow emission and getAllServices
        assertEquals(1, vault.servicesFlow.value.size)
        assertEquals(1, vault.getAllServices().size)
        val saved = vault.getService(gdriveService.id)
        assertNotNull(saved)
        assertEquals("My Google Drive", saved?.name)
        assertEquals(StorageProviderType.GOOGLE_DRIVE, saved?.providerType)
        assertEquals("user@example.com", saved?.accountIdentifier)
        assertTrue(saved?.isCompleteBackupEnabled == true)
    }

    @Test
    fun testMultipleCloudServicesRetainDistinctProviderTypes() {
        val vault = CredentialsVault.createForTesting(mockContext)

        val nextcloud = ServiceConfig(
            id = "nc-1",
            name = "Nextcloud Home",
            providerType = StorageProviderType.NEXTCLOUD,
            serverUrl = "https://cloud.example.com",
            username = "alice"
        )
        val webdav = ServiceConfig(
            id = "wd-1",
            name = "Fastmail WebDAV",
            providerType = StorageProviderType.WEBDAV,
            serverUrl = "https://webdav.fastmail.com",
            username = "bob"
        )
        val smb = ServiceConfig(
            id = "smb-1",
            name = "Office NAS",
            providerType = StorageProviderType.SMB3,
            host = "192.168.1.100",
            shareName = "notes"
        )

        vault.saveService(nextcloud)
        vault.saveService(webdav)
        vault.saveService(smb)

        val services = vault.getAllServices()
        assertEquals(3, services.size)

        val providerMap = services.associate { it.id to it.providerType }
        assertEquals(StorageProviderType.NEXTCLOUD, providerMap["nc-1"])
        assertEquals(StorageProviderType.WEBDAV, providerMap["wd-1"])
        assertEquals(StorageProviderType.SMB3, providerMap["smb-1"])
    }

    @Test
    fun testUpdateServicePreservesCloudAndModifiesStateFlow() {
        val vault = CredentialsVault.createForTesting(mockContext)

        val initialService = ServiceConfig(
            id = "svc-toggle",
            name = "Sync Service",
            providerType = StorageProviderType.NEXTCLOUD,
            isCompleteBackupEnabled = true
        )
        vault.saveService(initialService)
        assertTrue(vault.servicesFlow.value.first().isCompleteBackupEnabled)

        // Toggle complete sync off (as performed on onboarding screen)
        val updated = initialService.copy(isCompleteBackupEnabled = false)
        vault.saveService(updated)

        assertEquals(1, vault.servicesFlow.value.size)
        assertFalse(vault.servicesFlow.value.first().isCompleteBackupEnabled)
        assertFalse(vault.getAllServices().first().isCompleteBackupEnabled)
    }

    @Test
    fun testPendingDeletionsFlow() {
        val vault = CredentialsVault.createForTesting(mockContext)

        val service = ServiceConfig(
            id = "svc-del-1",
            name = "Temporary Service",
            providerType = StorageProviderType.WEBDAV
        )
        vault.saveService(service)

        vault.markServicePendingDeletion("svc-del-1")
        assertTrue(vault.isServicePendingDeletion("svc-del-1"))
        assertTrue(vault.pendingDeletionsFlow.value.contains("svc-del-1"))
        assertEquals(0, vault.getActiveConfiguredServices().size)
        assertEquals(1, vault.getAllServices().size)

        vault.restorePendingDeletedService("svc-del-1")
        assertFalse(vault.isServicePendingDeletion("svc-del-1"))
        assertFalse(vault.pendingDeletionsFlow.value.contains("svc-del-1"))
        assertEquals(1, vault.getActiveConfiguredServices().size)
    }
}
