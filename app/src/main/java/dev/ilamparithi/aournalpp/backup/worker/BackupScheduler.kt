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

    private const val PERIODIC_WORK_NAME = "aournal_daily_cloud_sync"
    private const val ONE_TIME_WORK_NAME = "aournal_on_demand_cloud_sync"
    private const val EXIT_WORK_NAME = "aournal_on_exit_cloud_sync"

    fun updateSchedules(context: Context) {
        val prefs = BackupPreferences(context)
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
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    fun cancelDailySync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
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
