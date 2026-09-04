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
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REQUEST_BACKGROUND_CLOSE -> {
                Log.i("CanvasCommandReceiver", "Received ACTION_REQUEST_BACKGROUND_CLOSE in :canvas process")
                CanvasActivity.handleBackgroundCloseRequest()
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
