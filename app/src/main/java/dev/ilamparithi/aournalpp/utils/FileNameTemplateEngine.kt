package dev.ilamparithi.aournalpp.utils

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Robust Template Engine for generating dynamic file names across Aournal.
 *
 * Supported placeholders:
 * - Date & Time:
 *   - {date}: Current date (yyyy-MM-dd)
 *   - {time}: Current time (HH-mm)
 *   - {datetime}: Current date and time (yyyy-MM-dd_HH-mm)
 *   - {year}: 4-digit year (yyyy)
 *   - {month}: 2-digit month (MM)
 *   - {day}: 2-digit day (dd)
 *   - {hour}: 2-digit hour (HH)
 *   - {minute}: 2-digit minute (mm)
 *   - {second}: 2-digit second (ss)
 *   - {datetime:PATTERN}: Custom date/time pattern (e.g. {datetime:yyyyMMdd_HHmmss})
 *
 * - Folders:
 *   - {folder}: Immediate parent folder name
 *   - {folder:0}: Immediate parent folder name
 *   - {folder:1}: Grandparent folder name (1 level up)
 *   - {folder:n}: n-levels up folder name
 *   - {folders}: Relative folder path with underscores
 *
 * - File Info:
 *   - {filename}: Original file name with extension
 *   - {name}: Original file name without extension
 *   - {ext}: File extension without dot
 *
 * - Timestamps:
 *   - {created}: Note creation date (yyyy-MM-dd)
 *   - {created:PATTERN}: Custom pattern for creation timestamp
 *   - {modified}: Note last modified date (yyyy-MM-dd)
 *   - {modified:PATTERN}: Custom pattern for last modified timestamp
 *
 * - Random:
 *   - {random}: 4-character random alphanumeric string
 *   - {random:N}: N-character random alphanumeric string (e.g. {random:6})
 */
object FileNameTemplateEngine {

    const val PREF_KEY_TEMPLATE_NEW_FILE = "pref_template_new_file"
    const val PREF_KEY_TEMPLATE_SAVE_AS = "pref_template_save_as"
    const val PREF_KEY_TEMPLATE_EXPORT_PDF = "pref_template_export_pdf"
    const val PREF_KEY_TEMPLATE_SHARE_PDF = "pref_template_share_pdf"
    const val PREF_KEY_TEMPLATE_SHARE_XOPP = "pref_template_share_xopp"

    const val DEFAULT_TEMPLATE_NEW_FILE = "{datetime:yyyy-MM-dd-'Note'-HH-mm}"
    const val DEFAULT_TEMPLATE_SAVE_AS = "{name}_copy"
    const val DEFAULT_TEMPLATE_EXPORT_PDF = "{name}"
    const val DEFAULT_TEMPLATE_SHARE_PDF = "{name}"
    const val DEFAULT_TEMPLATE_SHARE_XOPP = "{name}"

    private val ALPHANUMERIC_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()

    private val REGEX_CUSTOM_DATETIME = Regex("""\{datetime:([^}]+)\}""")
    private val REGEX_FOLDER_LEVEL = Regex("""\{folder:(\d+)\}""")
    private val REGEX_CREATED_CUSTOM = Regex("""\{created:([^}]+)\}""")
    private val REGEX_MODIFIED_CUSTOM = Regex("""\{modified:([^}]+)\}""")
    private val REGEX_RANDOM_CUSTOM = Regex("""\{random:(\d+)\}""")
    private val REGEX_ILLEGAL_CHARS = Regex("""[/\\:*?"<>|]""")
    private val REGEX_WHITESPACE = Regex("""[\r\n\t]""")

    private val dateFormatCache = ThreadLocal.withInitial { mutableMapOf<String, SimpleDateFormat>() }

    private fun getCachedSimpleDateFormat(pattern: String, locale: Locale = Locale.getDefault()): SimpleDateFormat {
        val key = "$pattern|${locale.toLanguageTag()}"
        val cache = dateFormatCache.get() ?: mutableMapOf<String, SimpleDateFormat>().also { dateFormatCache.set(it) }
        return cache.getOrPut(key) { SimpleDateFormat(pattern, locale) }
    }

    private fun formatDate(date: Date, pattern: String, locale: Locale = Locale.getDefault()): String {
        return try {
            getCachedSimpleDateFormat(pattern, locale).format(date)
        } catch (_: Exception) {
            SimpleDateFormat(pattern, locale).format(date)
        }
    }

    data class TemplateContext(
        val existingFile: File? = null,
        val parentFolder: File? = null,
        val rootNotesDir: File? = null,
        val createdTimestamp: Long? = null,
        val modifiedTimestamp: Long? = null,
        val fallbackName: String = "Note"
    )

    /**
     * Resolves a template string by replacing all placeholders with actual contextual values.
     */
    fun evaluate(template: String, context: TemplateContext = TemplateContext()): String {
        val now = Date()
        var result = template.trim()

        if (result.isEmpty()) {
            result = context.fallbackName
        }

        // 1. Evaluate custom datetime patterns: {datetime:PATTERN}
        result = REGEX_CUSTOM_DATETIME.replace(result) { matchResult ->
            val pattern = matchResult.groupValues[1]
            try {
                formatDate(now, pattern)
            } catch (_: Exception) {
                formatDate(now, "yyyy-MM-dd_HH-mm")
            }
        }

        // 2. Standard Date & Time placeholders
        result = result
            .replace("{date}", formatDate(now, "yyyy-MM-dd"))
            .replace("{time}", formatDate(now, "HH-mm"))
            .replace("{datetime}", formatDate(now, "yyyy-MM-dd_HH-mm"))
            .replace("{year}", formatDate(now, "yyyy"))
            .replace("{month}", formatDate(now, "MM"))
            .replace("{day}", formatDate(now, "dd"))
            .replace("{hour}", formatDate(now, "HH"))
            .replace("{minute}", formatDate(now, "mm"))
            .replace("{second}", formatDate(now, "ss"))

        // 3. File Info placeholders
        val originalFile = context.existingFile
        val fileNameWithoutExt = originalFile?.nameWithoutExtension ?: context.fallbackName
        val fullFileName = originalFile?.name ?: "${context.fallbackName}.xopp"
        val fileExt = originalFile?.extension ?: "xopp"

        result = result
            .replace("{filename}", fullFileName)
            .replace("{name}", fileNameWithoutExt)
            .replace("{ext}", fileExt)

        // 4. Folder placeholders
        val parentFolder = context.parentFolder ?: originalFile?.parentFile
        val folderLevels = mutableListOf<String>()
        var currentFolder = parentFolder
        while (currentFolder != null && currentFolder.exists() && currentFolder != context.rootNotesDir?.parentFile) {
            folderLevels.add(currentFolder.name)
            currentFolder = currentFolder.parentFile
        }

        val immediateFolder = folderLevels.firstOrNull() ?: "Notes"
        result = result.replace("{folder}", immediateFolder)

        result = REGEX_FOLDER_LEVEL.replace(result) { matchResult ->
            val level = matchResult.groupValues[1].toIntOrNull() ?: 0
            if (level in folderLevels.indices) {
                folderLevels[level]
            } else {
                immediateFolder
            }
        }

        if (context.rootNotesDir != null && parentFolder != null) {
            try {
                val relPath = parentFolder.relativeToOrNull(context.rootNotesDir)?.path
                val safePath = if (!relPath.isNullOrBlank()) relPath.replace(File.separatorChar, '_') else immediateFolder
                result = result.replace("{folders}", safePath)
            } catch (_: Exception) {
                result = result.replace("{folders}", immediateFolder)
            }
        } else {
            result = result.replace("{folders}", immediateFolder)
        }

        // 5. Created & Modified timestamps
        val createdTime = context.createdTimestamp ?: originalFile?.lastModified() ?: now.time
        val modifiedTime = context.modifiedTimestamp ?: originalFile?.lastModified() ?: now.time

        result = REGEX_CREATED_CUSTOM.replace(result) { matchResult ->
            val pattern = matchResult.groupValues[1]
            try {
                formatDate(Date(createdTime), pattern)
            } catch (_: Exception) {
                formatDate(Date(createdTime), "yyyy-MM-dd")
            }
        }
        result = result.replace("{created}", formatDate(Date(createdTime), "yyyy-MM-dd"))

        result = REGEX_MODIFIED_CUSTOM.replace(result) { matchResult ->
            val pattern = matchResult.groupValues[1]
            try {
                formatDate(Date(modifiedTime), pattern)
            } catch (_: Exception) {
                formatDate(Date(modifiedTime), "yyyy-MM-dd")
            }
        }
        result = result.replace("{modified}", formatDate(Date(modifiedTime), "yyyy-MM-dd"))

        // 6. Random Alphanumeric placeholders: {random} and {random:N}
        result = REGEX_RANDOM_CUSTOM.replace(result) { matchResult ->
            val len = (matchResult.groupValues[1].toIntOrNull() ?: 4).coerceIn(1, 32)
            generateRandomAlphanumeric(len)
        }
        result = result.replace("{random}", generateRandomAlphanumeric(4))

        return sanitizeFileName(result, context.fallbackName)
    }

    private fun generateRandomAlphanumeric(length: Int): String {
        return (1..length)
            .map { ALPHANUMERIC_CHARS[Random.nextInt(ALPHANUMERIC_CHARS.size)] }
            .joinToString("")
    }

    /**
     * Sanitizes illegal filesystem characters.
     */
    fun sanitizeFileName(input: String, fallback: String = "Note"): String {
        val sanitized = input
            .replace(REGEX_ILLEGAL_CHARS, "_")
            .replace(REGEX_WHITESPACE, " ")
            .trim()
            .trim('.')

        return sanitized.ifEmpty { fallback }
    }

    /**
     * Context-based evaluate overload for convenient calling from UI components.
     */
    fun evaluate(template: String, context: Context, existingFile: File? = null): String {
        return evaluate(template, TemplateContext(existingFile = existingFile))
    }

    /**
     * Preconfigured template getters.
     */
    fun getNewFileTemplate(context: Context): String = getTemplate(context, PREF_KEY_TEMPLATE_NEW_FILE, DEFAULT_TEMPLATE_NEW_FILE)
    fun getSaveAsTemplate(context: Context): String = getTemplate(context, PREF_KEY_TEMPLATE_SAVE_AS, DEFAULT_TEMPLATE_SAVE_AS)
    fun getExportPdfTemplate(context: Context): String = getTemplate(context, PREF_KEY_TEMPLATE_EXPORT_PDF, DEFAULT_TEMPLATE_EXPORT_PDF)
    fun getSharePdfTemplate(context: Context): String = getTemplate(context, PREF_KEY_TEMPLATE_SHARE_PDF, DEFAULT_TEMPLATE_SHARE_PDF)
    fun getShareXoppTemplate(context: Context): String = getTemplate(context, PREF_KEY_TEMPLATE_SHARE_XOPP, DEFAULT_TEMPLATE_SHARE_XOPP)

    /**
     * Helper to get configured template from SharedPreferences.
     */
    fun getTemplate(context: Context, key: String, default: String): String {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        return prefs.getString(key, default) ?: default
    }

    /**
     * Helper to save configured template into SharedPreferences.
     */
    fun setTemplate(context: Context, key: String, template: String) {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(key, template).apply()
    }
}
