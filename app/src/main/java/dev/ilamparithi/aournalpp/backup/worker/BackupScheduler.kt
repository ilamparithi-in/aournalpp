package dev.ilamparithi.aournalpp.backup.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object BackupScheduler {

    private const val PERIODIC_DAILY_WORK_NAME = "aournal_daily_cloud_sync"
    private const val PERIODIC_INTERVAL_WORK_NAME = "aournal_interval_cloud_sync"
    private const val ONE_TIME_WORK_NAME = "aournal_on_demand_cloud_sync"
    private const val EXIT_WORK_NAME = "aournal_on_exit_cloud_sync"

    fun updateSchedules(
        context: Context,
        workManager: WorkManager = WorkManager.getInstance(context),
        vault: dev.ilamparithi.aournalpp.backup.security.CredentialsVault = dev.ilamparithi.aournalpp.backup.security.CredentialsVault.getInstance(context)
    ) {
        if (!vault.hasActiveEnabledServices()) {
            cancelDailySync(workManager)
            cancelIntervalSync(workManager)
            return
        }

        val prefs = BackupPreferences(context)

        // 1. Daily scheduled sync
        if (prefs.isDailyScheduledSyncEnabled) {
            scheduleDailySync(
                context = context,
                hour = prefs.dailyScheduledHour,
                minute = prefs.dailyScheduledMinute,
                wifiOnly = prefs.isWifiOnlyEnabled
            )
        } else {
            cancelDailySync(workManager)
        }

        // 2. Interval-based periodic sync (e.g. every 15m, 30m, 60m)
        val intervalMinutes = prefs.periodicSyncIntervalMinutes
        if (intervalMinutes in 15..1439) {
            scheduleIntervalSync(context, intervalMinutes.toLong(), prefs.isWifiOnlyEnabled)
        } else {
            cancelIntervalSync(workManager)
        }
    }

    fun scheduleIntervalSync(context: Context, intervalMinutes: Long, wifiOnly: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<BackupWorker>(intervalMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(BackupWorker.TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_INTERVAL_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    fun scheduleDailySync(context: Context, hour: Int, minute: Int, wifiOnly: Boolean) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelayMs = target.timeInMillis - now.timeInMillis

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(BackupWorker.TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_DAILY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    fun cancelDailySync(workManager: WorkManager) {
        workManager.cancelUniqueWork(PERIODIC_DAILY_WORK_NAME)
    }

    fun cancelDailySync(context: Context) {
        cancelDailySync(WorkManager.getInstance(context))
    }

    fun cancelIntervalSync(workManager: WorkManager) {
        workManager.cancelUniqueWork(PERIODIC_INTERVAL_WORK_NAME)
    }

    fun cancelIntervalSync(context: Context) {
        cancelIntervalSync(WorkManager.getInstance(context))
    }

    fun triggerImmediateSync(
        context: Context,
        wifiOnly: Boolean = false,
        targetServiceId: String? = null,
        workManager: WorkManager = WorkManager.getInstance(context),
        vault: dev.ilamparithi.aournalpp.backup.security.CredentialsVault = dev.ilamparithi.aournalpp.backup.security.CredentialsVault.getInstance(context)
    ) {
        if (targetServiceId != null) {
            val service = vault.getAllServices().firstOrNull { it.id == targetServiceId }
            if (service == null || !service.isEnabled) return
        } else if (!vault.hasActiveEnabledServices()) {
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val inputData = if (targetServiceId != null) {
            androidx.work.workDataOf(BackupWorker.KEY_TARGET_SERVICE_ID to targetServiceId)
        } else {
            androidx.work.Data.EMPTY
        }

        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(BackupWorker.TAG)
            .build()

        workManager.enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun triggerOnAppExitSync(context: Context) {
        val vault = dev.ilamparithi.aournalpp.backup.security.CredentialsVault.getInstance(context)
        if (!vault.hasActiveEnabledServices()) return

        val prefs = BackupPreferences(context)
        if (!prefs.isAutoBackupOnExitEnabled) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (prefs.isWifiOnlyEnabled) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(BackupWorker.TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            EXIT_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
