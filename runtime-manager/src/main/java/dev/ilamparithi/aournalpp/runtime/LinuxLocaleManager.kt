package dev.ilamparithi.aournalpp.runtime

import android.content.Context
import android.util.Log
import java.io.File
import java.util.Locale

/**
 * Metadata model representing a Linux environment locale for Xournal++ / GTK.
 */
data class LinuxLocaleInfo(
    val tag: String,
    val displayName: String,
    val nativeName: String,
    val englishName: String,
    val languageCode: String,
    val isSystemDefault: Boolean = false
)

/**
 * Manages locale discovery and POSIX environment variable generation for the Linux runtime.
 */
object LinuxLocaleManager {
    private const val TAG = "LinuxLocaleManager"
    const val PREF_KEY_LINUX_LOCALE = "pref_linux_locale"
    const val SYSTEM_DEFAULT_TAG = "system"

    /**
     * Catalog of gettext locales supported by upstream Xournal++.
     * Used both for fallback and to augment dynamically discovered directories.
     */
    private val KNOWN_XOURNAL_LOCALES = listOf(
        "en_US" to ("English (United States)" to "English (United States)"),
        "en_GB" to ("English (United Kingdom)" to "English (United Kingdom)"),
        "de_DE" to ("Deutsch (Deutschland)" to "German (Germany)"),
        "fr_FR" to ("Français (France)" to "French (France)"),
        "es_ES" to ("Español (España)" to "Spanish (Spain)"),
        "it_IT" to ("Italiano (Italia)" to "Italian (Italy)"),
        "pt_BR" to ("Português (Brasil)" to "Portuguese (Brazil)"),
        "pt_PT" to ("Português (Portugal)" to "Portuguese (Portugal)"),
        "ru_RU" to ("Русский (Россия)" to "Russian (Russia)"),
        "ja_JP" to ("日本語 (日本)" to "Japanese (Japan)"),
        "zh_CN" to ("简体中文 (中国)" to "Simplified Chinese (China)"),
        "zh_TW" to ("繁體中文 (台灣)" to "Traditional Chinese (Taiwan)"),
        "ko_KR" to ("한국어 (대한민국)" to "Korean (South Korea)"),
        "pl_PL" to ("Polski (Polska)" to "Polish (Poland)"),
        "cs_CZ" to ("Čeština (Česko)" to "Czech (Czechia)"),
        "nl_NL" to ("Nederlands (Nederland)" to "Dutch (Netherlands)"),
        "ca_ES" to ("Català (Espanya)" to "Catalan (Spain)"),
        "tr_TR" to ("Türkçe (Türkiye)" to "Turkish (Turkey)"),
        "uk_UA" to ("Українська (Україна)" to "Ukrainian (Ukraine)"),
        "ar_EG" to ("العربية (مصر)" to "Arabic (Egypt)"),
        "hi_IN" to ("हिन्दी (भारत)" to "Hindi (India)"),
        "el_GR" to ("Ελληνικά (Ελλάδα)" to "Greek (Greece)"),
        "sv_SE" to ("Svenska (Sverige)" to "Swedish (Sweden)"),
        "hu_HU" to ("Magyar (Magyarország)" to "Hungarian (Hungary)"),
        "ro_RO" to ("Română (România)" to "Romanian (Romania)"),
        "id_ID" to ("Bahasa Indonesia (Indonesia)" to "Indonesian (Indonesia)"),
        "vi_VN" to ("Tiếng Việt (Việt Nam)" to "Vietnamese (Vietnam)"),
        "fa_IR" to ("فارسی (ایران)" to "Persian (Iran)"),
        "he_IL" to ("עברית (ישראל)" to "Hebrew (Israel)"),
        "da_DK" to ("Dansk (Danmark)" to "Danish (Denmark)"),
        "fi_FI" to ("Suomi (Suomi)" to "Finnish (Finland)"),
        "nb_NO" to ("Norsk bokmål (Norge)" to "Norwegian Bokmål (Norway)")
    )

    /**
     * Gets the user's saved Linux locale preference tag (defaults to "system").
     * Syncs with Xournal++'s native settings.xml if the user modified the language inside Xournal++.
     */
    fun getSavedLocale(context: Context): String {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        try {
            val env = LinuxEnvironment(context)
            val settingsFile = File(env.xournalConfigDir, "settings.xml")
            if (settingsFile.exists()) {
                val content = settingsFile.readText()
                val match = Regex("<property\\s+name=\"preferredLocale\"\\s+value=\"([^\"]*)\"/>").find(content)
                if (match != null) {
                    val xmlValue = match.groupValues[1].trim()
                    val mappedTag = if (xmlValue.isEmpty() || xmlValue.equals("default", ignoreCase = true) || xmlValue.equals("system", ignoreCase = true)) {
                        SYSTEM_DEFAULT_TAG
                    } else if (!xmlValue.endsWith(".UTF-8")) {
                        "${xmlValue}.UTF-8"
                    } else {
                        xmlValue
                    }
                    val currentPref = prefs.getString(PREF_KEY_LINUX_LOCALE, SYSTEM_DEFAULT_TAG) ?: SYSTEM_DEFAULT_TAG
                    if (mappedTag != currentPref) {
                        prefs.edit().putString(PREF_KEY_LINUX_LOCALE, mappedTag).apply()
                        Log.i(TAG, "Synchronized preferredLocale from Xournal++ settings.xml ($xmlValue) into Android preference: $mappedTag")
                    }
                    return mappedTag
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing preferredLocale from settings.xml", e)
        }
        return prefs.getString(PREF_KEY_LINUX_LOCALE, SYSTEM_DEFAULT_TAG) ?: SYSTEM_DEFAULT_TAG
    }

    /**
     * Sets the user's saved Linux locale preference tag and syncs it to settings.xml.
     */
    fun setSavedLocale(context: Context, localeTag: String) {
        val normalizedTag = localeTag.trim()
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY_LINUX_LOCALE, normalizedTag).apply()
        Log.i(TAG, "Updated Linux locale setting to: $normalizedTag")
        try {
            val env = LinuxEnvironment(context)
            env.ensureXournalppSettings()
        } catch (e: Exception) {
            Log.w(TAG, "Failed syncing updated locale to settings.xml", e)
        }
    }

    /**
     * Dynamically discovers all available Linux locales by inspecting the extracted Linux share/locale
     * directory in addition to the known Xournal++ translation catalog.
     */
    fun getSupportedLocales(context: Context): List<LinuxLocaleInfo> {
        val results = mutableListOf<LinuxLocaleInfo>()

        // 1. First entry is always "System Default" (Follow Android System / App)
        val defaultLocale = Locale.getDefault()
        val defaultNative = defaultLocale.getDisplayName(defaultLocale).replaceFirstChar { it.uppercase() }
        val defaultEnglish = defaultLocale.getDisplayName(Locale.ENGLISH).replaceFirstChar { it.uppercase() }
        results.add(
            LinuxLocaleInfo(
                tag = SYSTEM_DEFAULT_TAG,
                displayName = "Follow System ($defaultNative)",
                nativeName = defaultNative,
                englishName = defaultEnglish,
                languageCode = defaultLocale.language,
                isSystemDefault = true
            )
        )

        val seenTags = mutableSetOf<String>()

        // 2. Discover from Linux filesystem ($PREFIX/share/locale or $PREFIX/share/xournalpp)
        try {
            val env = LinuxEnvironment(context)
            val localeDir = File(env.shareDir, "locale")
            if (localeDir.exists() && localeDir.isDirectory) {
                val subDirs = localeDir.listFiles { f -> f.isDirectory } ?: emptyArray()
                for (dir in subDirs) {
                    val code = dir.name
                    // Ignore non-locale folders like 'locale.alias'
                    if (code.contains(".")) continue

                    val localeObj = parseLocaleFromCode(code)
                    val nativeName = localeObj.getDisplayName(localeObj).replaceFirstChar { it.uppercase() }
                    val englishName = localeObj.getDisplayName(Locale.ENGLISH).replaceFirstChar { it.uppercase() }
                    val posixTag = if (code.contains("_")) "$code.UTF-8" else "${code}_${code.uppercase()}.UTF-8"

                    if (!seenTags.contains(posixTag) && nativeName.isNotBlank()) {
                        seenTags.add(posixTag)
                        results.add(
                            LinuxLocaleInfo(
                                tag = posixTag,
                                displayName = if (nativeName.equals(englishName, ignoreCase = true)) nativeName else "$nativeName ($englishName)",
                                nativeName = nativeName,
                                englishName = englishName,
                                languageCode = localeObj.language
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning Linux locale directories", e)
        }

        // 3. Augment with official Xournal++ catalog
        for ((code, names) in KNOWN_XOURNAL_LOCALES) {
            val posixTag = "$code.UTF-8"
            if (!seenTags.contains(posixTag)) {
                seenTags.add(posixTag)
                val (nativeName, englishName) = names
                val localeObj = parseLocaleFromCode(code)
                results.add(
                    LinuxLocaleInfo(
                        tag = posixTag,
                        displayName = if (nativeName.equals(englishName, ignoreCase = true)) nativeName else "$nativeName ($englishName)",
                        nativeName = nativeName,
                        englishName = englishName,
                        languageCode = localeObj.language
                    )
                )
            }
        }

        return results
    }

    /**
     * Resolves human-readable label for a given locale tag.
     */
    fun getLocaleDisplayName(context: Context, localeTag: String): String {
        if (localeTag == SYSTEM_DEFAULT_TAG) {
            val def = Locale.getDefault()
            val nativeName = def.getDisplayName(def).replaceFirstChar { it.uppercase() }
            return "Follow System ($nativeName)"
        }
        val supported = getSupportedLocales(context)
        val match = supported.firstOrNull { it.tag.equals(localeTag, ignoreCase = true) }
        if (match != null) {
            return match.displayName
        }
        val cleaned = localeTag.removeSuffix(".UTF-8")
        val loc = parseLocaleFromCode(cleaned)
        val nat = loc.getDisplayName(loc).replaceFirstChar { it.uppercase() }
        val eng = loc.getDisplayName(Locale.ENGLISH).replaceFirstChar { it.uppercase() }
        return if (nat.equals(eng, ignoreCase = true)) nat else "$nat ($eng)"
    }

    /**
     * Computes the effective POSIX environment variables for process execution:
     * Returns Triple(LANG, LANGUAGE, LC_ALL).
     */
    fun getEffectiveLocaleEnv(context: Context): Triple<String, String, String> {
        val savedTag = getSavedLocale(context)
        return if (savedTag == SYSTEM_DEFAULT_TAG) {
            val loc = Locale.getDefault()
            val lang = loc.language.ifBlank { "en" }
            val country = loc.country.ifBlank { lang.uppercase() }
            val langPosix = "${lang}_${country}.UTF-8"
            val languageVar = "${lang}_${country}:${lang}:en_US:en"
            Triple(langPosix, languageVar, langPosix)
        } else {
            val normalizedTag = if (savedTag.endsWith(".UTF-8")) savedTag else "$savedTag.UTF-8"
            val code = normalizedTag.removeSuffix(".UTF-8")
            val lang = code.substringBefore('_')
            val languageVar = "$code:$lang:en_US:en"
            Triple(normalizedTag, languageVar, normalizedTag)
        }
    }

    private fun parseLocaleFromCode(code: String): Locale {
        return try {
            val bcp47 = code.replace('_', '-')
            val loc = Locale.forLanguageTag(bcp47)
            if (loc.language.isNotEmpty()) {
                loc
            } else {
                Locale.Builder().setLanguage(code.substringBefore('_')).build()
            }
        } catch (e: Exception) {
            Locale.getDefault()
        }
    }
}
