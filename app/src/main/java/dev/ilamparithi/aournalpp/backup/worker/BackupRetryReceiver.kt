package dev.ilamparithi.aournalpp.backup.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver triggered by the "Retry" action button in cloud sync error notifications,
 * or invoked via ACTION_TRIGGER_SYNC to safely schedule a sync from secondary processes (such as :canvas).
 */
class BackupRetryReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BackupRetryReceiver"
        const val ACTION_TRIGGER_SYNC = "dev.ilamparithi.aournalpp.ACTION_TRIGGER_SYNC"
        const val EXTRA_WIFI_ONLY = "extra_wifi_only"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val targetServiceId = intent?.getStringExtra(BackupWorker.KEY_TARGET_SERVICE_ID)
        val wifiOnly = intent?.getBooleanExtra(EXTRA_WIFI_ONLY, false) ?: false
        Log.i(TAG, "Triggering cloud sync via BackupRetryReceiver (action=${intent?.action}, targetServiceId=$targetServiceId, wifiOnly=$wifiOnly)")
        BackupScheduler.triggerImmediateSync(
            context = context.applicationContext,
            wifiOnly = wifiOnly,
            targetServiceId = targetServiceId
        )
    }
}
