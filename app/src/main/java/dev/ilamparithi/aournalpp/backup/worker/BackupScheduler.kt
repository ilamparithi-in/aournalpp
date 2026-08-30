package dev.ilamparithi.aournalpp.backup.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object BackupScheduler {

    private const val PERIODIC_DAILY_WORK_NAME = "aournal_daily_cloud_sync"
    private const val PERIODIC_INTERVAL_WORK_NAME = "aournal_interval_cloud_sync"
    private const val ONE_TIME_WORK_NAME = "aournal_on_demand_cloud_sync"
    private const val EXIT_WORK_NAME = "aournal_on_exit_cloud_sync"

    fun updateSchedules(context: Context) {
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
            cancelDailySync(context)
        }

        // 2. Interval-based periodic sync (e.g. every 15m, 30m, 60m)
        val intervalMinutes = prefs.periodicSyncIntervalMinutes
        if (intervalMinutes in 15..1439) {
            scheduleIntervalSync(context, intervalMinutes.toLong(), prefs.isWifiOnlyEnabled)
        } else {
            cancelIntervalSync(context)
        }
    }

    fun scheduleIntervalSync(context: Context, intervalMinutes: Long, wifiOnly: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<BackupWorker>(intervalMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag(BackupWorker.TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_INTERVAL_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    fun cancelIntervalSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_INTERVAL_WORK_NAME)
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
            .addTag(BackupWorker.TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_DAILY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    fun cancelDailySync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_DAILY_WORK_NAME)
    }

    fun triggerImmediateSync(context: Context, wifiOnly: Boolean = false) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(constraints)
            .addTag(BackupWorker.TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun triggerOnAppExitSync(context: Context) {
        val prefs = BackupPreferences(context)
        if (!prefs.isAutoBackupOnExitEnabled) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (prefs.isWifiOnlyEnabled) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(constraints)
            .addTag(BackupWorker.TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            EXIT_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
