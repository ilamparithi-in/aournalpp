package dev.ilamparithi.aournalpp.backup.security

import android.net.Uri
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class NextcloudCredentials(
    val serverUrl: String,
    val username: String,
    val appPassword: String
)

object NextcloudQrParser {

    /**
     * Parses QR code content from Nextcloud web app password generation or login flow.
     * Returns NextcloudCredentials if successfully extracted.
     */
    fun parse(rawText: String): NextcloudCredentials? {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return null

        // Format 1: nc://login/server:<server>&user:<user>&password:<password> or nc://login?server=...
        if (trimmed.startsWith("nc://", ignoreCase = true)) {
            val withoutScheme = trimmed.substring(5)
            val cleanContent = if (withoutScheme.startsWith("login/", ignoreCase = true)) {
                withoutScheme.substring(6)
            } else if (withoutScheme.startsWith("login?", ignoreCase = true)) {
                withoutScheme.substring(6)
            } else {
                withoutScheme
            }

            var server = ""
            var user = ""
            var pass = ""

            // Split by '&' or ';'
            val pairs = cleanContent.split('&', ';')
            for (pair in pairs) {
                if (pair.contains(':') || pair.contains('=')) {
                    val delim = if (pair.contains(':')) ':' else '='
                    val key = pair.substringBefore(delim).trim().lowercase()
                    val value = URLDecoder.decode(pair.substringAfter(delim).trim(), StandardCharsets.UTF_8.name())
                    when (key) {
                        "server", "s", "url" -> server = value
                        "user", "u", "username" -> user = value
                        "password", "p", "pass", "token", "apppassword" -> pass = value
                    }
                }
            }

            if (server.isNotEmpty() || user.isNotEmpty() || pass.isNotEmpty()) {
                return NextcloudCredentials(
                    serverUrl = normalizeServerUrl(server),
                    username = user,
                    appPassword = pass
                )
            }
        }

        // Format 2: server:<url>;user:<username>;password:<password>
        if (trimmed.contains("server:", ignoreCase = true) || trimmed.contains("user:", ignoreCase = true) || trimmed.contains("password:", ignoreCase = true)) {
            var server = ""
            var user = ""
            var pass = ""

            val pairs = trimmed.split(';', '\n', '&')
            for (pair in pairs) {
                if (pair.contains(':')) {
                    val key = pair.substringBefore(':').trim().lowercase()
                    val value = pair.substringAfter(':').trim()
                    when (key) {
                        "server", "url" -> server = value
                        "user", "username" -> user = value
                        "password", "pass", "apppassword" -> pass = value
                    }
                }
            }

            if (server.isNotEmpty() || user.isNotEmpty() || pass.isNotEmpty()) {
                return NextcloudCredentials(
                    serverUrl = normalizeServerUrl(server),
                    username = user,
                    appPassword = pass
                )
            }
        }

        // Format 3: JSON {"server": "...", "user": "...", "password": "..."}
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val server = extractJsonKey(trimmed, "server").ifBlank { extractJsonKey(trimmed, "url") }
            val user = extractJsonKey(trimmed, "user").ifBlank { extractJsonKey(trimmed, "username") }
            val pass = extractJsonKey(trimmed, "password").ifBlank { extractJsonKey(trimmed, "apppassword").ifBlank { extractJsonKey(trimmed, "token") } }

            if (server.isNotEmpty() || user.isNotEmpty() || pass.isNotEmpty()) {
                return NextcloudCredentials(
                    serverUrl = normalizeServerUrl(server),
                    username = user,
                    appPassword = pass
                )
            }
        }

        // Format 4: URL with token or plain app password (e.g., abcd-efgh-ijkl-mnop)
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return NextcloudCredentials(
                serverUrl = normalizeServerUrl(trimmed),
                username = "",
                appPassword = ""
            )
        }

        // Format 5: Raw App Password formatted as xxxx-xxxx-xxxx-xxxx
        if (trimmed.matches(Regex("^[a-zA-Z0-9]{4,}(-[a-zA-Z0-9]{4,})+$"))) {
            return NextcloudCredentials(
                serverUrl = "",
                username = "",
                appPassword = trimmed
            )
        }

        return null
    }

    private fun extractJsonKey(json: String, key: String): String {
        val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
        return pattern.find(json)?.groupValues?.get(1) ?: ""
    }

    private fun normalizeServerUrl(url: String): String {
        var clean = url.trim()
        if (clean.isEmpty()) return ""
        if (!clean.startsWith("http://", ignoreCase = true) && !clean.startsWith("https://", ignoreCase = true)) {
            clean = "https://$clean"
        }
        return clean.trimEnd('/')
    }
}
