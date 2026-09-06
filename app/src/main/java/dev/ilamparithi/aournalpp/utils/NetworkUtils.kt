package dev.ilamparithi.aournalpp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Network verification utilities for pre-flight cloud sync checks and connectivity monitoring.
 */
object NetworkUtils {

    enum class NetworkCheckResult {
        ONLINE,
        OFFLINE,
        WIFI_REQUIRED;

        val canSync: Boolean get() = this == ONLINE
        val errorMessage: String? get() = when (this) {
            ONLINE -> null
            OFFLINE -> "Device is offline. Please check your internet connection."
            WIFI_REQUIRED -> "Wi-Fi is required for cloud sync per your settings."
        }
    }

    /**
     * Checks if the device has an active, validated internet connection.
     */
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Checks if the device is connected to Wi-Fi or an unmetered network.
     */
    fun isWifiOrUnmetered(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false

        val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        val isUnmetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

        return isWifi || isUnmetered
    }

    /**
     * Performs a pre-flight network check before scheduling or queueing cloud synchronization.
     * Prevents queueing transfers and generating cascading socket/DNS timeouts when offline.
     */
    fun checkSyncNetworkPreconditions(context: Context, wifiOnly: Boolean): NetworkCheckResult {
        if (!isOnline(context)) {
            return NetworkCheckResult.OFFLINE
        }
        if (wifiOnly && !isWifiOrUnmetered(context)) {
            return NetworkCheckResult.WIFI_REQUIRED
        }
        return NetworkCheckResult.ONLINE
    }
}
