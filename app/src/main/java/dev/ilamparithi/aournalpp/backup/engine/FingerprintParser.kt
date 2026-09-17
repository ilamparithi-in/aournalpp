package dev.ilamparithi.aournalpp.backup.engine

import java.io.ByteArrayInputStream
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream

/**
 * Parses and extracts origin application and device fingerprints
 * from .xopp XML headers and decompressed byte slices.
 */
object FingerprintParser {
    private val CREATOR_REGEX = Pattern.compile("""creator="([^"]+)"""")
    private val DEVICE_IN_PARENS_REGEX = Pattern.compile("""\((?:Device:\s*)?([^)]+)\)""")

    fun parseCreatorString(creator: String): Pair<String, String?> {
        val trimmed = creator.trim()
        val matcher = DEVICE_IN_PARENS_REGEX.matcher(trimmed)
        var originDevice: String? = null
        var baseApp = trimmed
        if (matcher.find()) {
            originDevice = matcher.group(1)?.trim()
            baseApp = trimmed.substring(0, matcher.start()).trim()
        }

        val originApp = when {
            baseApp.startsWith("xournalpp", ignoreCase = true) || baseApp.startsWith("Xournal++", ignoreCase = true) -> {
                val ver = baseApp.removePrefix("xournalpp").removePrefix("Xournal++").trim()
                if (ver.isNotEmpty()) "Desktop Xournal++ $ver" else "Desktop Xournal++"
            }
            baseApp.startsWith("Aournal++", ignoreCase = true) -> baseApp
            else -> baseApp
        }

        if (originDevice == null) {
            originDevice = if (originApp.startsWith("Desktop")) "Desktop PC" else null
        }

        return Pair(originApp, originDevice)
    }

    fun extractFingerprintFromGzipBytes(compressedBytes: ByteArray): Pair<String?, String?> {
        if (compressedBytes.size < 10) return null to null
        return try {
            GZIPInputStream(ByteArrayInputStream(compressedBytes)).use { gis ->
                val buffer = ByteArray(2048)
                val read = gis.read(buffer)
                if (read > 0) {
                    val xmlHeader = String(buffer, 0, read, Charsets.UTF_8)
                    val m = CREATOR_REGEX.matcher(xmlHeader)
                    if (m.find()) {
                        val rawCreator = m.group(1) ?: return null to null
                        parseCreatorString(rawCreator)
                    } else null to null
                } else null to null
            }
        } catch (_: Exception) {
            null to null
        }
    }
}
