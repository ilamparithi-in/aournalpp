package dev.ilamparithi.aournalpp.backup.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.ilamparithi.aournalpp.MainActivity
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.backup.engine.BackupEngine
import dev.ilamparithi.aournalpp.backup.model.BackupResult
import dev.ilamparithi.aournalpp.backup.queue.FileTransferQueueManager
import dev.ilamparithi.aournalpp.backup.security.CredentialsVault
import dev.ilamparithi.aournalpp.AppTab

/**
 * Background WorkManager worker executing automated or on-demand multi-service sync
 * with live foreground progress notifications, cancellation actions, and error summaries.
 */
class BackupWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "BackupWorker"
        const val CHANNEL_ID = "aournal_cloud_backup_channel"
        const val NOTIFICATION_ID = 4096
        const val ERROR_NOTIFICATION_ID = 4097
        const val KEY_TARGET_SERVICE_ID = "target_service_id"
    }

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun getForegroundInfo(): ForegroundInfo {
        createNotificationChannel()
        val initialNotification = buildNotification("Preparing cloud backup...", 0, 0, true)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, initialNotification)
        }
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting background cloud backup worker...")
        val prefs = BackupPreferences(appContext)
        val netCheck = dev.ilamparithi.aournalpp.utils.NetworkUtils.checkSyncNetworkPreconditions(
            appContext,
            wifiOnly = prefs.isWifiOnlyEnabled
        )
        if (!netCheck.canSync) {
            Log.i(TAG, "Network preconditions not met for background backup: ${netCheck.errorMessage}. Retrying later.")
            return Result.retry()
        }

        FileTransferQueueManager.setSyncActive(true)
        try {
            val foregroundInfo = getForegroundInfo()
            try {
                setForeground(foregroundInfo)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to run as foreground service, continuing as regular worker", e)
            }

            val prefs = BackupPreferences(appContext)
            val engine = BackupEngine(appContext)
            val targetServiceId = inputData.getString(KEY_TARGET_SERVICE_ID)

            val results: List<BackupResult> = try {
                if (targetServiceId != null) {
                    val vault = CredentialsVault.getInstance(appContext)
                    val service = vault.getAllServices().firstOrNull { it.id == targetServiceId }
                    if (service != null && service.isEnabled) {
                        val singleResult = engine.performBackup(
                            serviceConfig = service,
                            concurrency = prefs.concurrencyWorkers,
                            onProgress = { current, total, currentFile ->
                                val notification = buildNotification(
                                    "Backing up ($current/$total): $currentFile",
                                    current,
                                    total,
                                    false
                                )
                                notificationManager.notify(NOTIFICATION_ID, notification)
                            }
                        )
                        listOf(singleResult)
                    } else {
                        emptyList()
                    }
                } else {
                    engine.performMultiServiceBackup(
                        concurrency = prefs.concurrencyWorkers,
                        onProgress = { current, total, currentFile ->
                            val notification = buildNotification(
                                "Backing up ($current/$total): $currentFile",
                                current,
                                total,
                                false
                            )
                            notificationManager.notify(NOTIFICATION_ID, notification)
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing background backup", e)
                val openQueueIntent = createOpenQueuePendingIntent()
                val errNotification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Cloud Backup Failed")
                    .setContentText(e.message ?: "An unexpected error occurred")
                    .setContentIntent(openQueueIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
                notificationManager.notify(ERROR_NOTIFICATION_ID, errNotification)
                return Result.failure()
            }

            val totalUploaded = results.sumOf { it.filesUploaded }
            val totalFailed = results.sumOf { it.filesFailed }
            val openQueueIntent = createOpenQueuePendingIntent()

            if (totalFailed > 0) {
                val allErrors = results.flatMap { it.errors }
                val summary = appContext.resources.getQuantityString(
                    R.plurals.backup_worker_uploaded_files,
                    totalUploaded,
                    totalUploaded
                ) + " ($totalFailed failed)"

                val bigText = buildString {
                    append(summary)
                    if (allErrors.isNotEmpty()) {
                        append("\n\nFailed items:")
                        allErrors.take(5).forEach { err ->
                            append("\n• ").append(err)
                        }
                        if (allErrors.size > 5) {
                            append("\n• and ${allErrors.size - 5} more…")
                        }
                    }
                }

                val failureNotification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Cloud Sync Issues Detected")
                    .setContentText(summary)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                    .setContentIntent(openQueueIntent)
                    .addAction(
                        android.R.drawable.ic_menu_view,
                        appContext.getString(R.string.notification_action_view_queue),
                        openQueueIntent
                    )
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(ERROR_NOTIFICATION_ID, failureNotification)
            } else {
                val completionText = if (totalUploaded > 0) {
                    appContext.resources.getQuantityString(
                        R.plurals.backup_worker_success_files,
                        totalUploaded,
                        totalUploaded
                    )
                } else {
                    "All cloud files are up-to-date"
                }

                val completeNotification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Cloud Backup Finished")
                    .setContentText(completionText)
                    .setContentIntent(openQueueIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(NOTIFICATION_ID, completeNotification)
            }

            return Result.success()
        } finally {
            FileTransferQueueManager.setSyncActive(false)
        }
    }

    private fun createOpenQueuePendingIntent(): PendingIntent {
        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            action = "dev.ilamparithi.aournalpp.ACTION_OPEN_TRANSFER_QUEUE"
            putExtra("EXTRA_OPEN_TAB", AppTab.CLOUD.id)
            putExtra("EXTRA_CLOUD_SUBPAGE", "TRANSFER_QUEUE")
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        return PendingIntent.getActivity(appContext, 1001, openIntent, flags)
    }

    private fun buildNotification(
        contentText: String,
        current: Int,
        total: Int,
        indeterminate: Boolean
    ): android.app.Notification {
        val openQueueIntent = createOpenQueuePendingIntent()
        val cancelPendingIntent = WorkManager.getInstance(appContext).createCancelPendingIntent(id)

        val percent = if (!indeterminate && total > 0) (current * 100) / total else 0
        val subText = if (!indeterminate && total > 0) "$percent% ($current/$total)" else null

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Aournal++ Cloud Sync")
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openQueueIntent)
            .setProgress(if (indeterminate) 0 else total, if (indeterminate) 0 else current, indeterminate)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                appContext.getString(R.string.notification_action_cancel_sync),
                cancelPendingIntent
            )

        if (subText != null) {
            builder.setSubText(subText)
        }

        return builder.build()
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

