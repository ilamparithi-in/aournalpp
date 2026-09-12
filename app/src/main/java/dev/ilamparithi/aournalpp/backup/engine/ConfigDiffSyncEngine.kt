package dev.ilamparithi.aournalpp.backup.engine

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Dedicated sub-engine for configuration file detection, structural JSON equality comparison,
 * content hash calculation, and differential change analysis.
 */
object ConfigDiffSyncEngine {

    /** Set of known configuration file basenames. */
    private val KNOWN_CONFIG_FILENAMES = setOf(
        "x11_prefs.json",
        "app_settings.json",
        "settings.xml",
        "settings.ini",
        "sync_mappings.json"
    )

    /**
     * Identifies whether the given file path or file name corresponds to an application configuration file.
     */
    fun isConfigFile(pathOrName: String): Boolean {
        val clean = pathOrName.replace('\\', '/')
        val fileName = File(clean).name.lowercase()
        return fileName in KNOWN_CONFIG_FILENAMES ||
                clean.startsWith(".config/") ||
                clean.contains("/.config/") ||
                clean == ".config"
    }

    /**
     * Determines whether two files differ in actual content.
     * Uses SHA-256 hashing, semantic JSON comparison for .json files,
     * and whitespace-trimmed line comparison for configuration/text formats.
     */
    fun hasContentChanges(f1: File, f2: File): Boolean {
        if (!f1.exists() || !f2.exists()) return true
        val hash1 = calculateFileHash(f1)
        val hash2 = calculateFileHash(f2)
        if (hash1.isNotEmpty() && hash1 == hash2) {
            return false
        }
        val ext = f1.extension.lowercase()
        if (ext == "json") {
            return try {
                !areJsonFilesEqual(f1, f2)
            } catch (_: Exception) {
                val lines1 = f1.readLines().map { it.trim() }.filter { it.isNotEmpty() }
                val lines2 = f2.readLines().map { it.trim() }.filter { it.isNotEmpty() }
                lines1 != lines2
            }
        }
        if (ext in listOf("xml", "ini", "txt", "conf", "cfg", "properties")) {
            return try {
                val lines1 = f1.readLines().map { it.trim() }.filter { it.isNotEmpty() }
                val lines2 = f2.readLines().map { it.trim() }.filter { it.isNotEmpty() }
                lines1 != lines2
            } catch (_: Exception) {
                true
            }
        }
        return true
    }

    /**
     * Compares two JSON files for structural and value equality.
     */
    fun areJsonFilesEqual(f1: File, f2: File): Boolean {
        val s1 = f1.readText(Charsets.UTF_8).trim()
        val s2 = f2.readText(Charsets.UTF_8).trim()
        if (s1 == s2) return true
        if (s1.startsWith("{") && s2.startsWith("{")) {
            return areJsonObjectsEqual(JSONObject(s1), JSONObject(s2))
        }
        if (s1.startsWith("[") && s2.startsWith("[")) {
            return areJsonArraysEqual(JSONArray(s1), JSONArray(s2))
        }
        return false
    }

    /**
     * Recursively verifies that two JSON objects contain equivalent keys and values.
     */
    fun areJsonObjectsEqual(j1: JSONObject, j2: JSONObject): Boolean {
        if (j1.length() != j2.length()) return false
        val keys = j1.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (!j2.has(key)) return false
            val v1 = j1.get(key)
            val v2 = j2.get(key)
            if (!areJsonValuesEqual(v1, v2)) return false
        }
        return true
    }

    /**
     * Recursively verifies that two JSON array elements match in order and value.
     */
    fun areJsonArraysEqual(a1: JSONArray, a2: JSONArray): Boolean {
        if (a1.length() != a2.length()) return false
        for (i in 0 until a1.length()) {
            if (!areJsonValuesEqual(a1.get(i), a2.get(i))) return false
        }
        return true
    }

    /**
     * Evaluates equality between two JSON node values.
     */
    fun areJsonValuesEqual(v1: Any?, v2: Any?): Boolean {
        if (v1 == null && v2 == null) return true
        if (v1 == null || v2 == null) return false
        if (v1 is JSONObject && v2 is JSONObject) return areJsonObjectsEqual(v1, v2)
        if (v1 is JSONArray && v2 is JSONArray) return areJsonArraysEqual(v1, v2)
        if (v1 is Number && v2 is Number) return v1.toDouble() == v2.toDouble()
        if (v1 is Boolean && v2 is Boolean) return v1 == v2
        return v1.toString() == v2.toString()
    }

    /**
     * Computes the SHA-256 hexadecimal hash for a local file.
     */
    fun calculateFileHash(file: File): String {
        if (!file.exists() || !file.isFile || file.length() == 0L) return ""
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }
}
