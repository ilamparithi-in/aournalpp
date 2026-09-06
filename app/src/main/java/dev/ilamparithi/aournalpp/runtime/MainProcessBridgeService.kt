package dev.ilamparithi.aournalpp.runtime

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * Lightweight Bound Service running in the main application process.
 * When CanvasActivity (running in the isolated :canvas process) binds to this service with
 * BIND_IMPORTANT, Android's OomAdjuster elevates the main process priority so that
 * the Linux Low Memory Killer (LMK) will not kill MainActivity's process
 * while the user is actively editing notes in the canvas.
 */
class MainProcessBridgeService : Service() {

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): MainProcessBridgeService = this@MainProcessBridgeService
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
