package dev.ilamparithi.aournalpp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Centralized preferences accessor providing standardized SharedPreferences instances
 * across the application.
 */
object AppPreferences {
    const val PREFS_GENERAL = "aournalpp_general"
    const val PREFS_DOCUMENT_HUB = "aournalpp_document_hub"
    const val PREFS_X11 = "aournalpp_x11"
    const val PREFS_BACKUP = "aournalpp_backup"

    fun getGeneral(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_GENERAL, Context.MODE_PRIVATE)

    fun getDocumentHub(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_DOCUMENT_HUB, Context.MODE_PRIVATE)

    fun getX11(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_X11, Context.MODE_PRIVATE)

    fun getBackup(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_BACKUP, Context.MODE_PRIVATE)

    /**
     * Composable helper returning the general application preferences.
     */
    @Composable
    fun rememberPrefs(): SharedPreferences {
        val context = LocalContext.current
        return remember(context) { getGeneral(context) }
    }

    /**
     * Composable helper returning the Document Hub preferences.
     */
    @Composable
    fun rememberDocumentHubPrefs(): SharedPreferences {
        val context = LocalContext.current
        return remember(context) { getDocumentHub(context) }
    }

    /**
     * Composable helper returning the X11 preferences.
     */
    @Composable
    fun rememberX11Prefs(): SharedPreferences {
        val context = LocalContext.current
        return remember(context) { getX11(context) }
    }
}
