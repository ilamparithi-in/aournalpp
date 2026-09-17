package dev.ilamparithi.aournalpp.backup.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver triggered by the "Retry" action button in cloud sync error notifications.
 */
class BackupRetryReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BackupRetryReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.i(TAG, "User triggered cloud sync retry from notification action")
        val targetServiceId = intent?.getStringExtra(BackupWorker.KEY_TARGET_SERVICE_ID)
        BackupScheduler.triggerImmediateSync(
            context = context.applicationContext,
            wifiOnly = false,
            targetServiceId = targetServiceId
        )
    }
}
