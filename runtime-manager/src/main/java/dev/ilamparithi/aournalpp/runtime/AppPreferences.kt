package dev.ilamparithi.aournalpp.runtime

import android.content.Context
import android.content.SharedPreferences

/**
 * Dedicated manager for user-facing application preferences, such as animation settings,
 * onboarding completion, and visual theme preferences.
 */
class AppPreferences(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "aournal_prefs"
        const val PREF_KEY_APP_THEME = "pref_app_theme"
        const val PREF_KEY_ONBOARDING_COMPLETED = "pref_onboarding_completed"
        const val PREF_KEY_REDUCE_ANIMATIONS = "pref_reduce_animations"
        const val PREF_KEY_WALLPAPER_MODE = "pref_canvas_wallpaper_mode"
        const val PREF_KEY_PENDING_AUTOLOAD_NOTIFICATION = "pref_pending_autoload_conflict_notification"
    }

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isReduceAnimations(): Boolean =
        prefs.getBoolean(PREF_KEY_REDUCE_ANIMATIONS, false)

    fun setReduceAnimations(reduce: Boolean) {
        prefs.edit().putBoolean(PREF_KEY_REDUCE_ANIMATIONS, reduce).apply()
    }

    fun isOnboardingCompleted(): Boolean =
        prefs.getBoolean(PREF_KEY_ONBOARDING_COMPLETED, false)

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(PREF_KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun getAppTheme(defaultTheme: String = "system"): String =
        prefs.getString(PREF_KEY_APP_THEME, defaultTheme) ?: defaultTheme

    fun setAppTheme(theme: String) {
        prefs.edit().putString(PREF_KEY_APP_THEME, theme).apply()
    }
}
