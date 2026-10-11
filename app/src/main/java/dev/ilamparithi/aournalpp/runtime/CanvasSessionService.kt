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
        const val ACTION_STOP = "dev.ilamparithi.aournalpp.action.STOP_CANVAS_SESSION"

        const val EXTRA_NOTE_TITLE = "extra_note_title"

        /**
         * Pure helper to format human-readable notification text.
         */
        fun formatNotificationText(noteTitle: String?): String {
            val title = noteTitle?.trim()
            return if (title.isNullOrBlank()) {
                "Note session active"
            } else {
                "Editing: $title"
            }
        }

        /**
         * Helper to start the foreground canvas session.
         */
        fun startSession(context: Context, noteTitle: String? = null) {
            val intent = Intent(context, CanvasSessionService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_NOTE_TITLE, noteTitle)
            }
            try {
                context.startForegroundService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start CanvasSessionService", e)
            }
        }

        /**
         * Helper to update the displayed note title in the active notification.
         */
        fun updateTitle(context: Context, noteTitle: String?) {
            val intent = Intent(context, CanvasSessionService::class.java).apply {
                action = ACTION_UPDATE_TITLE
                putExtra(EXTRA_NOTE_TITLE, noteTitle)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send title update to CanvasSessionService", e)
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
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
                val newTitle = intent.getStringExtra(EXTRA_NOTE_TITLE)
                if (!newTitle.isNullOrBlank() && newTitle != currentNoteTitle) {
                    currentNoteTitle = newTitle
                    val notification = buildNotification(newTitle)
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    nm?.notify(NOTIFICATION_ID, notification)
                }
                return START_STICKY
            }
            ACTION_START, null -> {
                val title = intent?.getStringExtra(EXTRA_NOTE_TITLE) ?: currentNoteTitle
                currentNoteTitle = title
                val notification = buildNotification(title)
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
                    Log.i(TAG, "CanvasSessionService started in foreground for: $title")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to startForeground CanvasSessionService", e)
                }
                return START_STICKY
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Active Canvas Session",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the X11 and Xournal++ session active in background"
            setShowBadge(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.createNotificationChannel(channel)
    }

    private fun buildNotification(noteTitle: String?): Notification {
        val openCanvasIntent = Intent(this, CanvasActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openCanvasIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = formatNotificationText(noteTitle)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Aournal++ Canvas Session")
            .setContentText(contentText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
    }
}
