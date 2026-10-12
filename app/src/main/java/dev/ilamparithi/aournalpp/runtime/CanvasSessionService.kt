package dev.ilamparithi.aournalpp.runtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.ilamparithi.aournalpp.CanvasActivity
import dev.ilamparithi.aournalpp.R
import java.io.File

/**
 * Foreground Service running in the isolated `:canvas` process.
 *
 * Keeps the Linux environment (`libXlorie.so`, Openbox, and Xournal++) alive in RAM
 * with an `oom_score_adj <= 200` (FOREGROUND_SERVICE) priority when the user temporarily
 * switches apps or turns off the screen.
 *
 * Prevents Android's Low Memory Killer (LMK) and Phantom Process Killer from terminating
 * the X11 and Xournal++ processes while a note session is active.
 */
class CanvasSessionService : Service() {

    companion object {
        private const val TAG = "CanvasSessionService"
        const val CHANNEL_ID = "canvas_session_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "dev.ilamparithi.aournalpp.action.START_CANVAS_SESSION"
        const val ACTION_UPDATE_TITLE = "dev.ilamparithi.aournalpp.action.UPDATE_CANVAS_SESSION_TITLE"
        const val ACTION_UPDATE_SYNC_PROGRESS = "dev.ilamparithi.aournalpp.action.UPDATE_CANVAS_SYNC_PROGRESS"
        const val ACTION_STOP = "dev.ilamparithi.aournalpp.action.STOP_CANVAS_SESSION"

        const val EXTRA_NOTE_TITLE = "extra_note_title"
        const val EXTRA_NOTE_COUNT = "extra_note_count"
        const val EXTRA_IS_SYNCING = "extra_is_syncing"
        const val EXTRA_SYNC_SERVICE_NAME = "extra_sync_service_name"
        const val EXTRA_SYNC_CURRENT_INDEX = "extra_sync_current_index"
        const val EXTRA_SYNC_TOTAL_COUNT = "extra_sync_total_count"

        @Volatile
        private var isRunning: Boolean = false

        fun isSessionActive(): Boolean = isRunning

        /**
         * Pure helper to format human-readable notification text.
         */
        fun formatNotificationText(noteTitle: String?, noteCount: Int = 1): String {
            val title = noteTitle?.trim()
            if (title.isNullOrBlank()) {
                return if (noteCount > 1) "Note session active +${noteCount - 1}" else "Note session active"
            }
            val formattedTitle = if (noteCount > 1) "$title +${noteCount - 1}" else title
            return "Editing: $formattedTitle"
        }

        fun formatNotificationText(context: Context, noteTitle: String?, noteCount: Int = 1): String {
            val title = noteTitle?.trim()
            if (title.isNullOrBlank()) {
                val base = context.getString(R.string.notification_canvas_active)
                return if (noteCount > 1) "$base +${noteCount - 1}" else base
            }
            val formattedTitle = if (noteCount > 1) "$title +${noteCount - 1}" else title
            return context.getString(R.string.notification_canvas_editing, formattedTitle)
        }

        /**
         * Pure helper to format sync subtext for the persistent canvas card.
         */
        fun formatSyncSubText(serviceName: String?, current: Int, total: Int): String? {
            if (total <= 0) return null
            val srv = serviceName?.trim()?.takeIf { it.isNotEmpty() }
            return if (srv != null) {
                "Syncing $srv ($current/$total)"
            } else {
                "Syncing ($current/$total)"
            }
        }

        fun formatSyncSubText(context: Context, serviceName: String?, current: Int, total: Int): String? {
            if (total <= 0) return null
            val srv = serviceName?.trim()?.takeIf { it.isNotEmpty() }
            return if (srv != null) {
                context.getString(R.string.notification_sync_subtext_service, srv, current, total)
            } else {
                context.getString(R.string.notification_sync_subtext, current, total)
            }
        }

        /**
         * Helper to start the foreground canvas session.
         */
        fun startSession(context: Context, noteTitle: String? = null, noteCount: Int = 1) {
            val intent = Intent(context, CanvasSessionService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_NOTE_TITLE, noteTitle)
                putExtra(EXTRA_NOTE_COUNT, noteCount)
            }
            try {
                context.startForegroundService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start CanvasSessionService", e)
            }
        }

        /**
         * Helper to update the displayed note title and active note count in the active notification.
         */
        fun updateTitle(context: Context, noteTitle: String?, noteCount: Int = 1) {
            if (!isRunning) return
            val intent = Intent(context, CanvasSessionService::class.java).apply {
                action = ACTION_UPDATE_TITLE
                putExtra(EXTRA_NOTE_TITLE, noteTitle)
                putExtra(EXTRA_NOTE_COUNT, noteCount)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send title update to CanvasSessionService", e)
            }
        }

        /**
         * Helper to update sync progress displayed on the persistent notification card.
         */
        fun updateSyncProgress(
            context: Context,
            isSyncing: Boolean,
            serviceName: String? = null,
            currentIndex: Int = 0,
            totalCount: Int = 0
        ) {
            if (!isRunning) return
            val intent = Intent(context, CanvasSessionService::class.java).apply {
                action = ACTION_UPDATE_SYNC_PROGRESS
                putExtra(EXTRA_IS_SYNCING, isSyncing)
                putExtra(EXTRA_SYNC_SERVICE_NAME, serviceName)
                putExtra(EXTRA_SYNC_CURRENT_INDEX, currentIndex)
                putExtra(EXTRA_SYNC_TOTAL_COUNT, totalCount)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send sync progress to CanvasSessionService", e)
            }
        }

        /**
         * Helper to stop the foreground canvas session.
         */
        fun stopSession(context: Context) {
            val intent = Intent(context, CanvasSessionService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop CanvasSessionService", e)
            }
        }
    }

    private var currentNoteTitle: String? = null
    private var currentNoteCount: Int = 1
    private var isSyncingState: Boolean = false
    private var syncServiceName: String? = null
    private var syncCurrentIndex: Int = 0
    private var syncTotalCount: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                Log.i(TAG, "Stopping canvas session foreground service")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_TITLE -> {
                if (!isRunning) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                val newTitle = intent.getStringExtra(EXTRA_NOTE_TITLE)
                val newCount = intent.getIntExtra(EXTRA_NOTE_COUNT, currentNoteCount)
                if ((!newTitle.isNullOrBlank() && newTitle != currentNoteTitle) || newCount != currentNoteCount) {
                    currentNoteTitle = newTitle ?: currentNoteTitle
                    currentNoteCount = newCount
                    val notification = buildNotification(currentNoteTitle, currentNoteCount)
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    nm?.notify(NOTIFICATION_ID, notification)
                }
                return START_STICKY
            }
            ACTION_UPDATE_SYNC_PROGRESS -> {
                if (!isRunning) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                isSyncingState = intent.getBooleanExtra(EXTRA_IS_SYNCING, false)
                syncServiceName = intent.getStringExtra(EXTRA_SYNC_SERVICE_NAME)
                syncCurrentIndex = intent.getIntExtra(EXTRA_SYNC_CURRENT_INDEX, 0)
                syncTotalCount = intent.getIntExtra(EXTRA_SYNC_TOTAL_COUNT, 0)
                val notification = buildNotification(currentNoteTitle, currentNoteCount)
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.notify(NOTIFICATION_ID, notification)
                return START_STICKY
            }
            ACTION_START -> {
                val title = intent.getStringExtra(EXTRA_NOTE_TITLE) ?: currentNoteTitle
                val count = intent.getIntExtra(EXTRA_NOTE_COUNT, currentNoteCount)
                currentNoteTitle = title
                currentNoteCount = count
                val notification = buildNotification(title, count)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                    Log.i(TAG, "CanvasSessionService started in foreground for: $title (count=$count)")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to startForeground CanvasSessionService", e)
                }
                return START_STICKY
            }
            else -> {
                if (!isRunning) {
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_canvas_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_canvas_desc)
            setShowBadge(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.createNotificationChannel(channel)
    }

    private fun buildNotification(noteTitle: String?, noteCount: Int = currentNoteCount): Notification {
        val openCanvasIntent = Intent(this, CanvasActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openCanvasIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = formatNotificationText(this, noteTitle, noteCount)
        val subText = if (isSyncingState) formatSyncSubText(this, syncServiceName, syncCurrentIndex, syncTotalCount) else null

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_canvas_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(pendingIntent)

        if (subText != null) {
            builder.setSubText(subText)
            if (syncTotalCount > 0) {
                builder.setProgress(syncTotalCount, syncCurrentIndex, false)
            }
        }

        return builder.build()
    }
}
