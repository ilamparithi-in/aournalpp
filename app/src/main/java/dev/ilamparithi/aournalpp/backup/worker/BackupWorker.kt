package dev.ilamparithi.aournalpp.backup.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.backup.engine.BackupEngine

/**
 * Background WorkManager worker executing automated or on-demand multi-service sync
 * with live foreground progress notifications.
 */
class BackupWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "BackupWorker"
        const val CHANNEL_ID = "aournal_cloud_backup_channel"
        const val NOTIFICATION_ID = 4096
    }

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting background cloud backup worker...")
        createNotificationChannel()

        val initialNotification = buildNotification("Preparing cloud backup...", 0, 0, true)
        val foregroundInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ForegroundInfo(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                ForegroundInfo(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            }
        } else {
            ForegroundInfo(NOTIFICATION_ID, initialNotification)
        }

        try {
            setForeground(foregroundInfo)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to run as foreground service, continuing as regular worker", e)
        }

        val prefs = BackupPreferences(appContext)
        val engine = BackupEngine(appContext)

        val results = try {
            engine.performMultiServiceBackup(
                concurrency = prefs.concurrencyWorkers,
                onProgress = { current, total, currentFile ->
                    val notification = buildNotification("Backing up ($current/$total): $currentFile", current, total, false)
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error executing background backup", e)
            val errNotification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Cloud Backup Failed")
                .setContentText(e.message ?: "An unexpected error occurred")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(NOTIFICATION_ID, errNotification)
            return Result.failure()
        }

        val totalUploaded = results.sumOf { it.filesUploaded }
        val totalFailed = results.sumOf { it.filesFailed }

        val completionText = if (totalFailed == 0) {
            if (totalUploaded > 0) "Successfully uploaded $totalUploaded files" else "All cloud files are up-to-date"
        } else {
            "Uploaded $totalUploaded files ($totalFailed failed)"
        }

        val completeNotification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Cloud Backup Finished")
            .setContentText(completionText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, completeNotification)

        return Result.success()
    }

    private fun buildNotification(contentText: String, current: Int, total: Int, indeterminate: Boolean): android.app.Notification {
        return NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Aournal++ Cloud Sync")
            .setContentText(contentText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(if (indeterminate) 0 else total, if (indeterminate) 0 else current, indeterminate)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cloud Backup & Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress and status of background cloud synchronization"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
