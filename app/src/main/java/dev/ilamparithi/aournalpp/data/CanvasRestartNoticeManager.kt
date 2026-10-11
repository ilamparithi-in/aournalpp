package dev.ilamparithi.aournalpp.data

import android.content.Context
import dev.ilamparithi.aournalpp.runtime.ActiveSessionInfo
import dev.ilamparithi.aournalpp.runtime.ActiveSessionTracker
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.LinuxLocaleManager
import java.io.File
import kotlin.math.abs

import androidx.core.content.edit

/**
 * Tracks whether settings requiring a canvas restart were modified while
 * an active canvas session is running, and coordinates notice presentation.
 */
object CanvasRestartNoticeManager {
    const val PREF_KEY_PENDING_RESTART_NOTICE = "pref_pending_canvas_restart_notice"

    val RESTART_REQUIRED_KEYS = setOf(
        "pref_ui_scale",
        LinuxLocaleManager.PREF_KEY_LINUX_LOCALE,
        LinuxEnvironment.PREF_KEY_GTK_THEME,
        LinuxEnvironment.PREF_KEY_NOTES_DIR
    )

    /**
     * Checks if current preferences for the 4 restart-requiring settings differ
     * from the baseline recorded when the active session started.
     */
    fun hasSettingsDifferences(context: Context, session: ActiveSessionInfo): Boolean {
        val prefs = AppPreferences.getGeneral(context)

        // 1. UI Scale
        session.baseUiScale?.let { baseScale ->
            val currentScaleStr = prefs.getString("pref_ui_scale", "1.0") ?: "1.0"
            val currentScale = currentScaleStr.toFloatOrNull() ?: 1.0f
            val baseScaleFloat = baseScale.toFloatOrNull() ?: 1.0f
            if (abs(currentScale - baseScaleFloat) > 0.001f) {
                return true
            }
        }

        // 2. Linux Locale
        session.baseLocale?.let { baseLocale ->
            val currentLocale = try {
                LinuxLocaleManager.getSavedLocale(context)
            } catch (_: Exception) {
                prefs.getString(LinuxLocaleManager.PREF_KEY_LINUX_LOCALE, LinuxLocaleManager.SYSTEM_DEFAULT_TAG) ?: LinuxLocaleManager.SYSTEM_DEFAULT_TAG
            }
            if (!currentLocale.equals(baseLocale, ignoreCase = true)) {
                return true
            }
        }

        // 3. GTK Theme
        session.baseGtkTheme?.let { baseTheme ->
            val currentTheme = prefs.getString(LinuxEnvironment.PREF_KEY_GTK_THEME, "system") ?: "system"
            if (!currentTheme.equals(baseTheme, ignoreCase = true)) {
                return true
            }
        }

        // 4. Notes Directory
        session.baseNotesDir?.let { baseNotesDir ->
            val currentNotesDir = try {
                LinuxEnvironment(context).getNotesDirectory().absolutePath
            } catch (_: Exception) {
                prefs.getString(LinuxEnvironment.PREF_KEY_NOTES_DIR, null) ?: ""
            }
            val currentFile = File(currentNotesDir)
            val baseFile = File(baseNotesDir)
            if (currentFile.normalize().absolutePath != baseFile.normalize().absolutePath) {
                return true
            }
        }

        return false
    }

    fun onPreferenceChanged(context: Context, key: String) {
        if (key in RESTART_REQUIRED_KEYS) {
            try {
                if (!ActiveSessionTracker.isSessionActive(context)) {
                    clearPendingNotice(context)
                    return
                }

                val activeSession = try { ActiveSessionTracker.getActiveSession(context) } catch (_: Throwable) { null }
                val prefs = AppPreferences.getGeneral(context)

                if (activeSession != null) {
                    val hasBaselines = activeSession.baseUiScale != null ||
                                       activeSession.baseLocale != null ||
                                       activeSession.baseGtkTheme != null ||
                                       activeSession.baseNotesDir != null
                    if (hasBaselines) {
                        val differs = hasSettingsDifferences(context, activeSession)
                        if (differs) {
                            prefs.edit { putBoolean(PREF_KEY_PENDING_RESTART_NOTICE, true) }
                        } else {
                            prefs.edit { remove(PREF_KEY_PENDING_RESTART_NOTICE) }
                        }
                    } else {
                        prefs.edit { putBoolean(PREF_KEY_PENDING_RESTART_NOTICE, true) }
                    }
                } else {
                    prefs.edit { putBoolean(PREF_KEY_PENDING_RESTART_NOTICE, true) }
                }
            } catch (_: Exception) {}
        }
    }

    fun hasPendingNotice(context: Context): Boolean {
        try {
            // If the canvas session has ended in the meantime, clean up and suppress
            if (!ActiveSessionTracker.isSessionActive(context)) {
                clearPendingNotice(context)
                return false
            }

            val activeSession = try { ActiveSessionTracker.getActiveSession(context) } catch (_: Throwable) { null }
            if (activeSession != null) {
                val hasBaselines = activeSession.baseUiScale != null ||
                                   activeSession.baseLocale != null ||
                                   activeSession.baseGtkTheme != null ||
                                   activeSession.baseNotesDir != null

                if (hasBaselines) {
                    val differs = hasSettingsDifferences(context, activeSession)
                    if (!differs) {
                        clearPendingNotice(context)
                        return false
                    }
                }
            }

            val prefs = AppPreferences.getGeneral(context)
            return prefs.getBoolean(PREF_KEY_PENDING_RESTART_NOTICE, false)
        } catch (_: Exception) {
            return false
        }
    }

    fun clearPendingNotice(context: Context) {
        try {
            val prefs = AppPreferences.getGeneral(context)
            prefs.edit { remove(PREF_KEY_PENDING_RESTART_NOTICE) }
        } catch (_: Exception) {}
    }
}
