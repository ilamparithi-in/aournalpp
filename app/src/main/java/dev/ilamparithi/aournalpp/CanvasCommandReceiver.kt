package dev.ilamparithi.aournalpp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver running in the isolated :canvas process.
 * Receives background close requests from MainActivity and delegates to CanvasActivity.
 */
class CanvasCommandReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REQUEST_BACKGROUND_CLOSE = "dev.ilamparithi.aournalpp.ACTION_REQUEST_BACKGROUND_CLOSE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REQUEST_BACKGROUND_CLOSE) {
            Log.i("CanvasCommandReceiver", "Received ACTION_REQUEST_BACKGROUND_CLOSE in :canvas process")
            CanvasActivity.handleBackgroundCloseRequest()
        }
    }
}
