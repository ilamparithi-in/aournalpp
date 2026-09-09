package dev.ilamparithi.aournalpp.backup

import dev.ilamparithi.aournalpp.backup.model.ServiceConfig
import dev.ilamparithi.aournalpp.backup.model.StorageProviderType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDriveTokenRefreshTest {

    @Test
    fun testOAuthTokenFieldsSerializationAndDeserialization() {
        val originalConfig = ServiceConfig(
            id = "gdrive-1",
            name = "Google Drive (user@gmail.com)",
            providerType = StorageProviderType.GOOGLE_DRIVE,
            authToken = "ya29.old_token",
            refreshToken = "1//refresh_token_secret",
            tokenExpiryEpochMs = 1700000000000L,
            accountIdentifier = "user@gmail.com",
            isEnabled = true,
            lastSyncedAtEpochMs = 1699999900000L,
            lastSyncStatus = "Success"
        )

        // Simulate CredentialsVault serialization
        val json = JSONObject().apply {
            put("id", originalConfig.id)
            put("name", originalConfig.name)
            put("providerType", originalConfig.providerType.id)
            put("authToken", originalConfig.authToken)
            put("refreshToken", originalConfig.refreshToken)
            put("tokenExpiryEpochMs", originalConfig.tokenExpiryEpochMs)
            put("accountIdentifier", originalConfig.accountIdentifier)
            put("isEnabled", originalConfig.isEnabled)
            put("lastSyncedAtEpochMs", originalConfig.lastSyncedAtEpochMs)
            put("lastSyncStatus", originalConfig.lastSyncStatus ?: "")
        }

        // Simulate CredentialsVault deserialization
        val deserialized = ServiceConfig(
            id = json.getString("id"),
            name = json.getString("name"),
            providerType = StorageProviderType.fromId(json.getString("providerType")),
            authToken = json.optString("authToken", ""),
            refreshToken = json.optString("refreshToken", ""),
            tokenExpiryEpochMs = json.optLong("tokenExpiryEpochMs", 0L),
            accountIdentifier = json.optString("accountIdentifier", ""),
            isEnabled = json.optBoolean("isEnabled", true),
            lastSyncedAtEpochMs = json.optLong("lastSyncedAtEpochMs", 0L),
            lastSyncStatus = json.optString("lastSyncStatus", "").ifEmpty { null }
        )

        assertEquals("ya29.old_token", deserialized.authToken)
        assertEquals("1//refresh_token_secret", deserialized.refreshToken)
        assertEquals(1700000000000L, deserialized.tokenExpiryEpochMs)
        assertEquals("user@gmail.com", deserialized.accountIdentifier)
        assertEquals("Success", deserialized.lastSyncStatus)
    }

    @Test
    fun testTokenExpiryDetection() {
        fun isTokenExpired(tokenExpiryEpochMs: Long, currentTimeMs: Long): Boolean {
            return tokenExpiryEpochMs > 0L && currentTimeMs >= (tokenExpiryEpochMs - 60_000L)
        }

        val expiryTime = 1700000000000L

        // Current time is well before expiry
        assertFalse(isTokenExpired(expiryTime, expiryTime - 120_000L))

        // Current time is within the 60-second proactive refresh window
        assertTrue(isTokenExpired(expiryTime, expiryTime - 30_000L))

        // Current time is at or after expiry
        assertTrue(isTokenExpired(expiryTime, expiryTime))
        assertTrue(isTokenExpired(expiryTime, expiryTime + 10_000L))

        // No expiry set (0L)
        assertFalse(isTokenExpired(0L, expiryTime))
    }

    @Test
    fun testGoogleDriveAccountUniqueness() {
        val s1 = ServiceConfig(
            id = "1",
            name = "Google Drive Personal",
            providerType = StorageProviderType.GOOGLE_DRIVE,
            accountIdentifier = "alice@gmail.com"
        )
        val s2 = ServiceConfig(
            id = "2",
            name = "My GDrive",
            providerType = StorageProviderType.GOOGLE_DRIVE,
            accountIdentifier = "ALICE@GMAIL.COM"
        )
        val s3 = ServiceConfig(
            id = "3",
            name = "Work Drive",
            providerType = StorageProviderType.GOOGLE_DRIVE,
            accountIdentifier = "bob@corp.com"
        )

        assertEquals(s1.getAccountKey(), s2.getAccountKey())
        assertTrue(s1.getAccountKey() != s3.getAccountKey())
    }

    @Test
    fun testMultiServiceIndependence() {
        // Verify Nextcloud and Google Drive configs are independent
        val nextcloud = ServiceConfig(
            id = "nc-1",
            name = "Nextcloud",
            providerType = StorageProviderType.NEXTCLOUD,
            serverUrl = "https://cloud.example.com",
            username = "user",
            passwordOrSecret = "password",
            lastSyncedAtEpochMs = 1700000500000L,
            lastSyncStatus = "Success"
        )

        val gdrive = ServiceConfig(
            id = "gd-1",
            name = "Google Drive",
            providerType = StorageProviderType.GOOGLE_DRIVE,
            refreshToken = "token",
            accountIdentifier = "user@gmail.com",
            lastSyncedAtEpochMs = 1700000000000L,
            lastSyncStatus = "Success"
        )

        val services = listOf(nextcloud, gdrive)
        assertEquals(2, services.size)
        assertTrue(services.all { it.isEnabled })

        // Failure in Google Drive does not alter Nextcloud's lastSyncedAtEpochMs
        val updatedGdrive = gdrive.copy(
            lastSyncStatus = "Failed: Connection refused"
        )
        assertEquals(1700000500000L, nextcloud.lastSyncedAtEpochMs)
        assertEquals("Success", nextcloud.lastSyncStatus)
        assertEquals("Failed: Connection refused", updatedGdrive.lastSyncStatus)
    }
}
