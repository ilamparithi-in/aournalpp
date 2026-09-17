package dev.ilamparithi.aournalpp.backup

import android.content.Context
import android.content.SharedPreferences
import dev.ilamparithi.aournalpp.backup.model.DeviceInfo
import dev.ilamparithi.aournalpp.backup.model.DeviceIdentity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeviceIdentityTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        DeviceIdentity.resetForTesting()
        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)

        every { mockContext.getSharedPreferences("aournal_device_identity", Context.MODE_PRIVATE) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
    }

    @After
    fun tearDown() {
        DeviceIdentity.resetForTesting()
    }

    @Test
    fun testDeviceIdGenerationAndPersistence() {
        // Initial state: no stored UUID
        every { mockPrefs.getString("stable_device_id", null) } returns null

        val deviceId1 = DeviceIdentity.getDeviceId(mockContext)
        assertNotNull(deviceId1)
        assertTrue(deviceId1.isNotEmpty())

        // Verify it was persisted to preferences
        verify { mockEditor.putString("stable_device_id", deviceId1) }

        // Second call with cached memory
        val deviceId2 = DeviceIdentity.getDeviceId(mockContext)
        assertEquals(deviceId1, deviceId2)

        // Reset memory cache, but simulate stored UUID in SharedPreferences
        DeviceIdentity.resetForTesting()
        every { mockPrefs.getString("stable_device_id", null) } returns deviceId1

        val deviceIdFromPrefs = DeviceIdentity.getDeviceId(mockContext)
        assertEquals(deviceId1, deviceIdFromPrefs)
    }

    @Test
    fun testDeviceInfoJsonSerializationAndDeserialization() {
        val info = DeviceInfo(
            deviceId = "test-device-uuid-999",
            deviceName = "Pixel 9 Pro",
            manufacturer = "Google",
            model = "Pixel 9 Pro",
            osVersion = 34,
            appVersion = "1.2.3",
            lastSyncedAtEpochMs = 1756543200000L
        )

        val json = info.toJson()
        assertEquals("test-device-uuid-999", json.getString("deviceId"))
        assertEquals("Pixel 9 Pro", json.getString("deviceName"))
        assertEquals("Google", json.getString("manufacturer"))
        assertEquals(34, json.getInt("osVersion"))
        assertEquals("1.2.3", json.getString("appVersion"))
        assertEquals(1756543200000L, json.getLong("lastSyncedAtEpochMs"))

        val deserialized = DeviceInfo.fromJson(json)
        assertEquals(info, deserialized)

        val jsonString = info.toJsonString()
        assertTrue(jsonString.contains("\"deviceId\": \"test-device-uuid-999\""))
    }
}
