package dev.ilamparithi.aournalpp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.ilamparithi.aournalpp.data.X11Preferences

/**
 * BroadcastReceiver running in the isolated :canvas process.
 * Receives background close requests and preference changes from the main process.
 */
class CanvasCommandReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REQUEST_BACKGROUND_CLOSE = "dev.ilamparithi.aournalpp.ACTION_REQUEST_BACKGROUND_CLOSE"
        const val ACTION_REQUEST_PARALLEL_CLOSE = "dev.ilamparithi.aournalpp.ACTION_REQUEST_PARALLEL_CLOSE"
        const val ACTION_PARALLEL_CLOSE_CONFLICT = "dev.ilamparithi.aournalpp.ACTION_PARALLEL_CLOSE_CONFLICT"
        const val ACTION_PARALLEL_CLOSE_BLOCKING = "dev.ilamparithi.aournalpp.ACTION_PARALLEL_CLOSE_BLOCKING"
        const val ACTION_REQUEST_SAVE_WINDOW = "dev.ilamparithi.aournalpp.ACTION_REQUEST_SAVE_WINDOW"
        const val ACTION_SAVE_WINDOW_SUCCESS = "dev.ilamparithi.aournalpp.ACTION_SAVE_WINDOW_SUCCESS"
        const val ACTION_SAVE_WINDOW_UNABLE_TO_SEND = "dev.ilamparithi.aournalpp.ACTION_SAVE_WINDOW_UNABLE_TO_SEND"
        const val ACTION_REQUEST_CLOSE_WINDOW = "dev.ilamparithi.aournalpp.ACTION_REQUEST_CLOSE_WINDOW"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REQUEST_BACKGROUND_CLOSE -> {
                Log.i("CanvasCommandReceiver", "Received ACTION_REQUEST_BACKGROUND_CLOSE in :canvas process")
                CanvasActivity.handleBackgroundCloseRequest()
            }
            ACTION_REQUEST_PARALLEL_CLOSE -> {
                Log.i("CanvasCommandReceiver", "Received ACTION_REQUEST_PARALLEL_CLOSE in :canvas process")
                CanvasActivity.executeParallelCloseFromReceiver(context)
            }
            ACTION_REQUEST_SAVE_WINDOW -> {
                val targetWid = intent.getStringExtra("target_window_id") ?: ""
                Log.i("CanvasCommandReceiver", "Received ACTION_REQUEST_SAVE_WINDOW in :canvas process for wid=$targetWid")
                CanvasActivity.executeSaveWindowFromReceiver(context, targetWid)
            }
            ACTION_REQUEST_CLOSE_WINDOW -> {
                val targetWid = intent.getStringExtra("target_window_id") ?: ""
                Log.i("CanvasCommandReceiver", "Received ACTION_REQUEST_CLOSE_WINDOW in :canvas process for wid=$targetWid")
                CanvasActivity.executeCloseWindowFromReceiver(context, targetWid)
            }
            X11Preferences.ACTION_PREFERENCES_CHANGED -> {
                val key = intent.getStringExtra("key")
                val valueType = intent.getStringExtra("value_type")
                Log.d("CanvasCommandReceiver", "Received ACTION_PREFERENCES_CHANGED in :canvas process: key=$key, type=$valueType")
                if (key != null && valueType != null) {
                    val prefs = X11Preferences.getPrefs(context)
                    val editor = prefs.edit()
                    when (valueType) {
                        "boolean" -> editor.putBoolean(key, intent.getBooleanExtra("value_boolean", false))
                        "int" -> editor.putInt(key, intent.getIntExtra("value_int", 0))
                        "float" -> editor.putFloat(key, intent.getFloatExtra("value_float", 0f))
                        "long" -> editor.putLong(key, intent.getLongExtra("value_long", 0L))
                        "string" -> editor.putString(key, intent.getStringExtra("value_string"))
                    }
                    editor.commit()
                }
                if (key != null) {
                    CanvasActivity.notifyPreferenceChanged(key)
                }
            }
        }
    }
}
