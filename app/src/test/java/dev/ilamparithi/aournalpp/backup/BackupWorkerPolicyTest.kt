package dev.ilamparithi.aournalpp.backup

import android.content.Context
import androidx.work.WorkManager
import dev.ilamparithi.aournalpp.backup.security.CredentialsVault
import dev.ilamparithi.aournalpp.backup.worker.BackupScheduler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class BackupWorkerPolicyTest {

    private lateinit var mockContext: Context
    private lateinit var mockWorkManager: WorkManager
    private lateinit var mockVault: CredentialsVault

    @Before
    fun setUp() {
        CredentialsVault.resetInstanceForTesting()
        mockContext = mockk<Context>(relaxed = true)
        every { mockContext.applicationContext } returns mockContext

        mockWorkManager = mockk<WorkManager>(relaxed = true)
        mockVault = mockk<CredentialsVault>(relaxed = true)
    }

    @After
    fun tearDown() {
        CredentialsVault.resetInstanceForTesting()
    }

    @Test
    fun testUpdateSchedulesCancelsAllWhenZeroCloudServices() {
        every { mockVault.hasActiveEnabledServices() } returns false

        BackupScheduler.updateSchedules(mockContext, mockWorkManager, mockVault)

        // Verifies periodic work is cancelled and not scheduled
        verify { mockWorkManager.cancelUniqueWork("aournal_daily_cloud_sync") }
        verify { mockWorkManager.cancelUniqueWork("aournal_interval_cloud_sync") }
        verify(exactly = 0) {
            mockWorkManager.enqueueUniquePeriodicWork(
                any<String>(),
                any<androidx.work.ExistingPeriodicWorkPolicy>(),
                any<androidx.work.PeriodicWorkRequest>()
            )
        }
    }

    @Test
    fun testTriggerImmediateSyncNoOpWhenZeroCloudServices() {
        every { mockVault.hasActiveEnabledServices() } returns false

        BackupScheduler.triggerImmediateSync(
            context = mockContext,
            workManager = mockWorkManager,
            vault = mockVault
        )

        // Verifies no one-time work is enqueued when zero cloud services configured
        verify(exactly = 0) {
            mockWorkManager.enqueueUniqueWork(
                any<String>(),
                any<androidx.work.ExistingWorkPolicy>(),
                any<androidx.work.OneTimeWorkRequest>()
            )
        }
    }
}
