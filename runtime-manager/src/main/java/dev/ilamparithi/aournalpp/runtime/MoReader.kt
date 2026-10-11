package dev.ilamparithi.aournalpp.runtime

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Lightweight, zero-dependency GNU gettext MO (Machine Object) binary translation reader.
 * Parses compiled `.mo` files in `$PREFIX/share/locale/<lang>/LC_MESSAGES/` to dynamically
 * extract translated UI window titles for Openbox window management rules.
 */
object MoReader {

    private const val MO_MAGIC_LITTLE = 0x950412de.toInt()
    private const val MO_MAGIC_BIG = 0xde120495.toInt()

    /**
     * Reads translations for the given [keys] from a compiled `.mo` [file].
     * Uses binary search over the sorted original string offsets table.
     */
    fun readTranslations(file: File, keys: Collection<String>): Map<String, String> {
        if (!file.exists() || !file.canRead() || file.length() < 28) {
            return emptyMap()
        }

        return try {
            val bytes = file.readBytes()
            parseMoBytes(bytes, keys)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /**
     * Parses byte buffer of a `.mo` file and searches for [keys].
     */
    internal fun parseMoBytes(bytes: ByteArray, keys: Collection<String>): Map<String, String> {
        if (bytes.size < 28) return emptyMap()

        val order = when {
            bytes[0] == 0xde.toByte() && bytes[1] == 0x12.toByte() && bytes[2] == 0x04.toByte() && bytes[3] == 0x95.toByte() -> {
                ByteOrder.LITTLE_ENDIAN
            }
            bytes[0] == 0x95.toByte() && bytes[1] == 0x04.toByte() && bytes[2] == 0x12.toByte() && bytes[3] == 0xde.toByte() -> {
                ByteOrder.BIG_ENDIAN
            }
            else -> return emptyMap() // Invalid MO magic number
        }

        val buf = ByteBuffer.wrap(bytes).order(order)
        val count = buf.getInt(8)
        val origTabOffset = buf.getInt(12)
        val transTabOffset = buf.getInt(16)

        if (count <= 0 || origTabOffset < 28 || transTabOffset < 28) return emptyMap()
        if (origTabOffset + count * 8L > bytes.size || transTabOffset + count * 8L > bytes.size) return emptyMap()

        val results = mutableMapOf<String, String>()

        for (targetKey in keys) {
            // Binary search in sorted original strings table
            var low = 0
            var high = count - 1
            while (low <= high) {
                val mid = (low + high) ushr 1
                val oLen = buf.getInt(origTabOffset + mid * 8)
                val oOff = buf.getInt(origTabOffset + mid * 8 + 4)

                if (oOff < 0 || oOff + oLen > bytes.size) break
                val origStr = String(bytes, oOff, oLen, Charsets.UTF_8)

                val cmp = origStr.compareTo(targetKey)
                if (cmp == 0) {
                    val tLen = buf.getInt(transTabOffset + mid * 8)
                    val tOff = buf.getInt(transTabOffset + mid * 8 + 4)
                    if (tOff >= 0 && tOff + tLen <= bytes.size) {
                        val transStr = String(bytes, tOff, tLen, Charsets.UTF_8)
                        // Filter out empty translations
                        if (transStr.isNotBlank()) {
                            results[targetKey] = transStr
                        }
                    }
                    break
                } else if (cmp < 0) {
                    low = mid + 1
                } else {
                    high = mid - 1
                }
            }
        }

        return results
    }

    /**
     * Searches for translations of the specified [keys] across all `.mo` files in [localeDir].
     * If [activeLocale] is provided, prioritizes matching `$localeDir/<activeLocale>/LC_MESSAGES/xournalpp.mo`.
     *
     * Returns a map of `key -> Set<translatedStrings>`.
     */
    fun findTranslations(
        localeDir: File?,
        keys: Collection<String>,
        activeLocale: String? = null
    ): Map<String, Set<String>> {
        val resultMap = mutableMapOf<String, MutableSet<String>>()
        keys.forEach { resultMap[it] = mutableSetOf() }

        if (localeDir == null || !localeDir.exists() || !localeDir.isDirectory) {
            return resultMap
        }

        // 1. If activeLocale is provided, query it first
        if (!activeLocale.isNullOrBlank() && activeLocale != "system") {
            val candidateCodes = listOf(
                activeLocale.removeSuffix(".UTF-8"),
                activeLocale.substringBefore('.'),
                activeLocale.substringBefore('_')
            ).distinct()

            for (code in candidateCodes) {
                val moFile = File(localeDir, "$code/LC_MESSAGES/xournalpp.mo")
                if (moFile.exists()) {
                    val found = readTranslations(moFile, keys)
                    for ((k, v) in found) {
                        resultMap[k]?.add(v)
                    }
                }
            }
        }

        // 2. Scan all subdirectories in localeDir to capture all installed language catalogs
        try {
            val subDirs = localeDir.listFiles { f -> f.isDirectory } ?: emptyArray()
            for (dir in subDirs) {
                val moFile = File(dir, "LC_MESSAGES/xournalpp.mo")
                if (moFile.exists()) {
                    val found = readTranslations(moFile, keys)
                    for ((k, v) in found) {
                        resultMap[k]?.add(v)
                    }
                }
            }
        } catch (_: Exception) {
            // Safe fallback
        }

        return resultMap
    }
}
