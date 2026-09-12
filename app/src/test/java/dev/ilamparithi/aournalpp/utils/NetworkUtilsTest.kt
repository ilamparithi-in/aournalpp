package dev.ilamparithi.aournalpp.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkUtilsTest {

    @Test
    fun testIsTransientNetworkException() {
        val unknownHost = UnknownHostException("Unable to resolve host \"drive.google.com\": No address associated with hostname")
        assertTrue(NetworkUtils.isTransientNetworkException(unknownHost))

        val timeout = SocketTimeoutException("connect timed out")
        assertTrue(NetworkUtils.isTransientNetworkException(timeout))

        val connectEx = ConnectException("Connection refused")
        assertTrue(NetworkUtils.isTransientNetworkException(connectEx))

        val wrapped = RuntimeException("Failed cloud operation", unknownHost)
        assertTrue(NetworkUtils.isTransientNetworkException(wrapped))

        val authError = IllegalStateException("HTTP 401 Unauthorized: Invalid credentials")
        assertFalse(NetworkUtils.isTransientNetworkException(authError))

        val quotaExceeded = RuntimeException("Storage quota exceeded")
        assertFalse(NetworkUtils.isTransientNetworkException(quotaExceeded))
    }

    @Test
    fun testIsTransientNetworkErrorMessage() {
        assertTrue(NetworkUtils.isTransientNetworkErrorMessage("Connection failed: Unable to resolve host \"drive.google.com\": No address associated with hostname"))
        assertTrue(NetworkUtils.isTransientNetworkErrorMessage("Connection failed: Network is unreachable"))
        assertTrue(NetworkUtils.isTransientNetworkErrorMessage("Failed: connection timed out"))
        assertTrue(NetworkUtils.isTransientNetworkErrorMessage("Connection failed: UnknownHostException"))

        assertFalse(NetworkUtils.isTransientNetworkErrorMessage("Success"))
        assertFalse(NetworkUtils.isTransientNetworkErrorMessage("Completed with 1 errors"))
        assertFalse(NetworkUtils.isTransientNetworkErrorMessage("HTTP 401 Unauthorized"))
        assertFalse(NetworkUtils.isTransientNetworkErrorMessage("Invalid OAuth grant"))
        assertFalse(NetworkUtils.isTransientNetworkErrorMessage(null))
        assertFalse(NetworkUtils.isTransientNetworkErrorMessage(""))
    }
}
