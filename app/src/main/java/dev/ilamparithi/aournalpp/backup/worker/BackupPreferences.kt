package dev.ilamparithi.aournalpp.backup.worker

import android.content.Context
import android.content.SharedPreferences
import dev.ilamparithi.aournalpp.backup.model.ConflictResolutionPolicy

/**
 * Storage and configuration for background automation preferences and sync constraints.
 */
class BackupPreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "aournal_cloud_backup_prefs"
        private const val KEY_AUTO_BACKUP_ON_EXIT = "auto_backup_on_exit"
        private const val KEY_DAILY_SCHEDULED_SYNC = "daily_scheduled_sync"
        private const val KEY_DAILY_SCHEDULED_HOUR = "daily_scheduled_hour"
        private const val KEY_DAILY_SCHEDULED_MINUTE = "daily_scheduled_minute"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_CONCURRENCY_WORKERS = "concurrency_workers"
        private const val KEY_CONFLICT_POLICY = "conflict_policy"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isAutoBackupOnExitEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_BACKUP_ON_EXIT, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_BACKUP_ON_EXIT, value).apply()

    var isDailyScheduledSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_SCHEDULED_SYNC, false)
        set(value) = prefs.edit().putBoolean(KEY_DAILY_SCHEDULED_SYNC, value).apply()

    var dailyScheduledHour: Int
        get() = prefs.getInt(KEY_DAILY_SCHEDULED_HOUR, 2) // Default 02:00 AM
        set(value) = prefs.edit().putInt(KEY_DAILY_SCHEDULED_HOUR, value).apply()

    var dailyScheduledMinute: Int
        get() = prefs.getInt(KEY_DAILY_SCHEDULED_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_DAILY_SCHEDULED_MINUTE, value).apply()

    var isWifiOnlyEnabled: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY, true)
        set(value) = prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()

    var concurrencyWorkers: Int
        get() = prefs.getInt(KEY_CONCURRENCY_WORKERS, 2).coerceIn(1, 4)
        set(value) = prefs.edit().putInt(KEY_CONCURRENCY_WORKERS, value.coerceIn(1, 4)).apply()

    var defaultConflictPolicy: ConflictResolutionPolicy
        get() {
            val id = prefs.getString(KEY_CONFLICT_POLICY, ConflictResolutionPolicy.KEEP_NEWER.id) ?: ConflictResolutionPolicy.KEEP_NEWER.id
            return ConflictResolutionPolicy.fromId(id)
        }
        set(value) = prefs.edit().putString(KEY_CONFLICT_POLICY, value.id).apply()
}
