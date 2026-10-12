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
import dev.ilamparithi.aournalpp.AppTab
import dev.ilamparithi.aournalpp.CanvasCommandReceiver
import dev.ilamparithi.aournalpp.MainActivity
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.backup.engine.BackupEngine
import dev.ilamparithi.aournalpp.backup.engine.ConflictPersistenceManager
import dev.ilamparithi.aournalpp.backup.model.BackupResult
import dev.ilamparithi.aournalpp.backup.model.TransferDirection
import dev.ilamparithi.aournalpp.backup.model.TransferItem
import dev.ilamparithi.aournalpp.backup.model.TransferStatus
import dev.ilamparithi.aournalpp.backup.queue.FileTransferQueueManager
import dev.ilamparithi.aournalpp.backup.security.CredentialsVault
import dev.ilamparithi.aournalpp.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Background WorkManager worker executing automated or on-demand multi-service sync
 * with silent per-worker transfer progress notifications, persistent canvas card updates,
 * separate conflict alerts, and quiet zero-notification completion when nothing changes.
 */
class BackupWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "BackupWorker"
        const val CHANNEL_ID = "aournal_cloud_backup_channel"
        const val CONFLICT_CHANNEL_ID = "aournal_cloud_conflicts_channel"
        const val NOTIFICATION_ID = 4096
        const val ERROR_NOTIFICATION_ID = 4097
        const val CONFLICT_NOTIFICATION_ID = 4098
        const val WORKER_NOTIFICATION_BASE_ID = 4100 // 4101..4104 for workers 1..4
        const val KEY_TARGET_SERVICE_ID = "target_service_id"
    }

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun getForegroundInfo(): ForegroundInfo {
        createNotificationChannels()
        val initialNotification = buildNotification(appContext.getString(R.string.notification_sync_preparing), 0, 0, true)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, initialNotification)
        }
    }

    override suspend fun doWork(): Result {
        createNotificationChannels()
        Log.i(TAG, "Starting background cloud backup worker...")

        // 1. Centralized check: verify active configured cloud services exist
        val vault = CredentialsVault.getInstance(appContext)
        if (!vault.hasActiveEnabledServices()) {
            Log.i(TAG, "No enabled cloud services configured. Exiting background worker immediately.")
            BackupScheduler.updateSchedules(appContext)
            return Result.success()
        }

        // 2. Network precondition check
        val prefs = BackupPreferences(appContext)
        val netCheck = NetworkUtils.checkSyncNetworkPreconditions(
            appContext,
            wifiOnly = prefs.isWifiOnlyEnabled
        )
        if (!netCheck.canSync) {
            Log.i(TAG, "Network preconditions not met for background backup: ${netCheck.errorMessage}. Retrying later.")
            broadcastSyncProgress(null, 0, 0, false)
            return Result.retry()
        }

        FileTransferQueueManager.setSyncActive(true)
        broadcastSyncProgress(null, 0, 0, true)

        val activeWorkerSlots = ConcurrentHashMap<String, Int>()
        val lastWorkerNotifyMs = ConcurrentHashMap<Int, Long>()
        var workerCollectorJob: Job? = null

        try {
            val foregroundInfo = getForegroundInfo()
            try {
                setForeground(foregroundInfo)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to run as foreground service, continuing as regular worker", e)
            }

            // Launch collector for individual per-worker byte-transfer progress notifications
            workerCollectorJob = CoroutineScope(Dispatchers.Default).launch {
                FileTransferQueueManager.items.collect { items ->
                    updateWorkerNotifications(items, activeWorkerSlots, lastWorkerNotifyMs)
                }
            }

            val engine = BackupEngine(appContext)
            val targetServiceId = inputData.getString(KEY_TARGET_SERVICE_ID)

            val results: List<BackupResult> = try {
                if (targetServiceId != null) {
                    val service = vault.getAllServices().firstOrNull { it.id == targetServiceId }
                    if (service != null && service.isEnabled) {
                        val singleResult = engine.performBackup(
                            serviceConfig = service,
                            concurrency = prefs.concurrencyWorkers,
                            onProgress = { current, total, currentFile ->
                                broadcastSyncProgress(service.name, current, total, true)
                                val notification = buildNotification(
                                    appContext.getString(
                                        R.string.notification_syncing_service_file,
                                        service.name,
                                        current,
                                        total,
                                        currentFile
                                    ),
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
                            val srvName = currentFile.substringAfter("[").substringBefore("]").takeIf { currentFile.startsWith("[") }
                            broadcastSyncProgress(srvName, current, total, true)
                            val notification = buildNotification(
                                appContext.getString(
                                    R.string.notification_backing_up_file,
                                    current,
                                    total,
                                    currentFile
                                ),
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
                val retryPendingIntent = createRetryPendingIntent(targetServiceId)
                val isTransient = NetworkUtils.isTransientNetworkException(e) ||
                        NetworkUtils.isTransientNetworkErrorMessage(e.message)
                val errNotification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(appContext.getString(R.string.notification_sync_interrupted_title))
                    .setContentText(e.message ?: appContext.getString(R.string.notification_unexpected_error))
                    .setContentIntent(openQueueIntent)
                    .addAction(
                        android.R.drawable.ic_menu_rotate,
                        appContext.getString(R.string.notification_action_retry),
                        retryPendingIntent
                    )
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
                notificationManager.notify(ERROR_NOTIFICATION_ID, errNotification)
                return if (isTransient || runAttemptCount < 3) Result.retry() else Result.failure()
            }

            val totalUploaded = results.sumOf { it.filesUploaded }
            val totalFailed = results.sumOf { it.filesFailed }
            val openQueueIntent = createOpenQueuePendingIntent()
            val persistedConflicts = ConflictPersistenceManager.getInstance(appContext).getConflicts()
            val allConflicts = (results.flatMap { it.detectedConflicts } + persistedConflicts).distinctBy { it.id }

            // 1. Separate notification for sync conflicts (IMPORTANCE_DEFAULT alert)
            if (allConflicts.isNotEmpty()) {
                ConflictPersistenceManager.getInstance(appContext).addConflicts(allConflicts)
                val conflictNotification = NotificationCompat.Builder(appContext, CONFLICT_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(appContext.getString(R.string.notification_conflicts_title))
                    .setContentText(
                        appContext.resources.getQuantityString(
                            R.plurals.notification_conflicts_desc,
                            allConflicts.size,
                            allConflicts.size
                        )
                    )
                    .setContentIntent(createOpenConflictsPendingIntent())
                    .addAction(
                        android.R.drawable.ic_menu_edit,
                        appContext.getString(R.string.notification_action_resolve_conflicts),
                        createOpenConflictsPendingIntent()
                    )
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
                notificationManager.notify(CONFLICT_NOTIFICATION_ID, conflictNotification)
            } else {
                notificationManager.cancel(CONFLICT_NOTIFICATION_ID)
            }

            // 2. Completion notifications
            if (totalFailed > 0) {
                val allErrors = results.flatMap { it.errors }
                val summary = appContext.resources.getQuantityString(
                    R.plurals.backup_worker_uploaded_files,
                    totalUploaded,
                    totalUploaded
                ) + " " + appContext.getString(R.string.notification_issues_failed_count, totalFailed)

                val bigText = buildString {
                    append(summary)
                    if (allErrors.isNotEmpty()) {
                        append("\n\n").append(appContext.getString(R.string.notification_failed_items_header))
                        allErrors.take(5).forEach { err ->
                            append("\n• ").append(err)
                        }
                        if (allErrors.size > 5) {
                            append("\n• ").append(
                                appContext.getString(
                                    R.string.notification_failed_items_more,
                                    allErrors.size - 5
                                )
                            )
                        }
                    }
                }

                val failureNotification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(appContext.getString(R.string.notification_issues_detected_title))
                    .setContentText(summary)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                    .setContentIntent(openQueueIntent)
                    .addAction(
                        android.R.drawable.ic_menu_rotate,
                        appContext.getString(R.string.notification_action_retry),
                        createRetryPendingIntent(targetServiceId)
                    )
                    .addAction(
                        android.R.drawable.ic_menu_view,
                        appContext.getString(R.string.notification_action_view_queue),
                        openQueueIntent
                    )
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(ERROR_NOTIFICATION_ID, failureNotification)
            } else if (totalUploaded > 0) {
                val completionText = appContext.resources.getQuantityString(
                    R.plurals.backup_worker_success_files,
                    totalUploaded,
                    totalUploaded
                )

                val completeNotification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(appContext.getString(R.string.notification_sync_finished_title))
                    .setContentText(completionText)
                    .setContentIntent(openQueueIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(NOTIFICATION_ID, completeNotification)
            } else {
                // totalUploaded == 0 && totalFailed == 0 && allConflicts.isEmpty()
                // Quiet exit: do not post any notification, auto-cancel foreground progress notification cleanly
                notificationManager.cancel(NOTIFICATION_ID)
            }

            return Result.success()
        } finally {
            workerCollectorJob?.cancel()
            for (slot in 1..4) {
                notificationManager.cancel(WORKER_NOTIFICATION_BASE_ID + slot)
            }
            FileTransferQueueManager.setSyncActive(false)
            broadcastSyncProgress(null, 0, 0, false)
        }
    }

    private fun updateWorkerNotifications(
        items: List<TransferItem>,
        activeSlots: ConcurrentHashMap<String, Int>,
        lastNotifyMs: ConcurrentHashMap<Int, Long>
    ) {
        val inProgress = items.filter { it.status == TransferStatus.IN_PROGRESS }
        val inProgressIds = inProgress.map { it.id }.toSet()

        // 1. Cancel slots for transfers that are no longer in progress
        val removed = activeSlots.filterKeys { it !in inProgressIds }
        for ((id, slot) in removed) {
            activeSlots.remove(id)
            lastNotifyMs.remove(slot)
            notificationManager.cancel(WORKER_NOTIFICATION_BASE_ID + slot)
        }

        // 2. Assign slots (1..4) to newly started in-progress transfers
        val usedSlots = activeSlots.values.toSet()
        for (item in inProgress) {
            if (!activeSlots.containsKey(item.id)) {
                val freeSlot = (1..4).firstOrNull { it !in usedSlots && it !in activeSlots.values }
                if (freeSlot != null) {
                    activeSlots[item.id] = freeSlot
                }
            }
        }

        // 3. Post / update individual notification for each active worker
        val now = System.currentTimeMillis()
        for (item in inProgress) {
            val slot = activeSlots[item.id] ?: continue
            val lastTime = lastNotifyMs[slot] ?: 0L
            val isFinished = item.totalBytes > 0 && item.transferredBytes >= item.totalBytes
            if (lastTime != 0L && !isFinished && (now - lastTime) < 350L) {
                continue
            }
            lastNotifyMs[slot] = now

            val notif = buildWorkerNotification(item, slot)
            notificationManager.notify(WORKER_NOTIFICATION_BASE_ID + slot, notif)
        }
    }

    private fun buildWorkerNotification(
        item: TransferItem,
        slot: Int
    ): android.app.Notification {
        val openQueueIntent = createOpenQueuePendingIntent()
        val cancelPendingIntent = WorkManager.getInstance(appContext).createCancelPendingIntent(id)

        val transferred = item.transferredBytes
        val total = item.totalBytes
        val percent = if (total > 0) ((transferred * 100) / total).toInt().coerceIn(0, 100) else 0

        val transferredFormatted = android.text.format.Formatter.formatFileSize(appContext, transferred)
        val totalFormatted = android.text.format.Formatter.formatFileSize(appContext, total)
        val subText = if (total > 0) "$percent%" else null
        val contentText = "$transferredFormatted / $totalFormatted"

        val maxKb = (total / 1024L).coerceAtLeast(1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val progressKb = (transferred / 1024L).coerceAtLeast(0L).coerceAtMost(maxKb.toLong()).toInt()

        val actionWord = if (item.direction == TransferDirection.DOWNLOAD) {
            appContext.getString(R.string.notification_transfer_downloading)
        } else {
            appContext.getString(R.string.notification_transfer_uploading)
        }

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(appContext.getString(R.string.notification_worker_title, slot, item.fileName))
            .setContentText(appContext.getString(R.string.notification_transfer_progress, actionWord, contentText))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openQueueIntent)
            .setProgress(maxKb, progressKb, total <= 0)
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

    private fun broadcastSyncProgress(serviceName: String?, current: Int, total: Int, isSyncing: Boolean) {
        try {
            val flagFile = File(appContext.cacheDir, "sync_active.flag")
            if (isSyncing) {
                flagFile.createNewFile()
            } else {
                flagFile.delete()
            }
        } catch (_: Exception) {}

        try {
            val intent = Intent(CanvasCommandReceiver.ACTION_SYNC_STATE_CHANGED).apply {
                setPackage(appContext.packageName)
                putExtra(CanvasCommandReceiver.EXTRA_IS_SYNCING, isSyncing)
                putExtra(CanvasCommandReceiver.EXTRA_SERVICE_NAME, serviceName)
                putExtra(CanvasCommandReceiver.EXTRA_CURRENT_INDEX, current)
                putExtra(CanvasCommandReceiver.EXTRA_TOTAL_COUNT, total)
            }
            appContext.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast sync progress: isSyncing=$isSyncing", e)
        }
    }

    private fun createRetryPendingIntent(targetServiceId: String?): PendingIntent {
        val retryIntent = Intent(appContext, BackupRetryReceiver::class.java).apply {
            if (targetServiceId != null) {
                putExtra(KEY_TARGET_SERVICE_ID, targetServiceId)
            }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        return PendingIntent.getBroadcast(appContext, 2001, retryIntent, flags)
    }

    private fun createOpenQueuePendingIntent(): PendingIntent {
        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            action = "dev.ilamparithi.aournalpp.ACTION_OPEN_TRANSFER_QUEUE"
            putExtra("EXTRA_OPEN_TAB", AppTab.CLOUD.id)
            putExtra("EXTRA_CLOUD_SUBPAGE", "TRANSFER_QUEUE")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        return PendingIntent.getActivity(appContext, 1001, openIntent, flags)
    }

    private fun createOpenConflictsPendingIntent(): PendingIntent {
        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            action = "dev.ilamparithi.aournalpp.ACTION_OPEN_CONFLICTS"
            putExtra("EXTRA_OPEN_TAB", AppTab.CLOUD.id)
            putExtra("EXTRA_CLOUD_SUBPAGE", "CONFLICT_RESOLUTION")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        return PendingIntent.getActivity(appContext, 1002, openIntent, flags)
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
            .setContentTitle(appContext.getString(R.string.notification_sync_title))
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

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val syncChannel = NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.notification_channel_backup_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = appContext.getString(R.string.notification_channel_backup_desc)
                setShowBadge(false)
            }
            val conflictChannel = NotificationChannel(
                CONFLICT_CHANNEL_ID,
                appContext.getString(R.string.notification_channel_conflicts_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = appContext.getString(R.string.notification_channel_conflicts_desc)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannels(listOf(syncChannel, conflictChannel))
        }
    }
}
