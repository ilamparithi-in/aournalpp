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

    /**
     * Checks whether an exception is caused by a transient network/DNS issue (device offline, DNS resolving, timeout)
     * rather than an actual service or authentication error.
     */
    fun isTransientNetworkException(throwable: Throwable?): Boolean {
        if (throwable == null) return false
        var current: Throwable? = throwable
        while (current != null) {
            if (current is java.net.UnknownHostException ||
                current is java.net.SocketTimeoutException ||
                current is java.net.ConnectException ||
                current is java.net.NoRouteToHostException ||
                current is java.net.PortUnreachableException ||
                current is javax.net.ssl.SSLHandshakeException
            ) {
                return true
            }
            val msg = current.message?.lowercase() ?: ""
            if (msg.contains("unable to resolve host") ||
                msg.contains("no address associated with hostname") ||
                msg.contains("network is unreachable") ||
                msg.contains("connection timed out") ||
                msg.contains("failed to connect to") ||
                msg.contains("software caused connection abort") ||
                msg.contains("connection reset")
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    /**
     * Checks whether a status message indicates a transient network/DNS disconnection.
     */
    fun isTransientNetworkErrorMessage(message: String?): Boolean {
        if (message.isNullOrBlank()) return false
        val lower = message.lowercase()
        return lower.contains("unable to resolve host") ||
                lower.contains("no address associated with hostname") ||
                lower.contains("network is unreachable") ||
                lower.contains("connection timed out") ||
                lower.contains("failed to connect to") ||
                lower.contains("connection refused") ||
                lower.contains("unknownhostexception") ||
                lower.contains("sockettimeoutexception") ||
                lower.contains("connectexception")
    }
}

