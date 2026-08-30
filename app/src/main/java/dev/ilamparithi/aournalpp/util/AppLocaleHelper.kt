package dev.ilamparithi.aournalpp.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Data model representing a supported Android App interface language.
 */
data class AppLanguageInfo(
    val tag: String,
    val displayName: String,
    val nativeName: String,
    val englishName: String,
    val isSystemDefault: Boolean = false
)

/**
 * Helper for managing per-app language on Android 13+ (system action)
 * and backward compatibility via AppCompat on Android 12 and below.
 */
object AppLocaleHelper {
    private const val TAG = "AppLocaleHelper"
    const val SYSTEM_DEFAULT_TAG = "system"

    /**
     * Supported application languages (matching locales_config.xml).
     */
    private val SUPPORTED_APP_LOCALES = listOf(
        "en" to ("English" to "English"),
        "en-US" to ("English (US)" to "English (US)"),
        "en-GB" to ("English (UK)" to "English (UK)"),
        "de" to ("Deutsch" to "German"),
        "fr" to ("Français" to "French"),
        "es" to ("Español" to "Spanish"),
        "it" to ("Italiano" to "Italian"),
        "pt-BR" to ("Português (Brasil)" to "Portuguese (Brazil)"),
        "pt-PT" to ("Português (Portugal)" to "Portuguese (Portugal)"),
        "ru" to ("Русский" to "Russian"),
        "ja" to ("日本語" to "Japanese"),
        "zh-CN" to ("简体中文" to "Simplified Chinese"),
        "zh-TW" to ("繁體中文" to "Traditional Chinese"),
        "ko" to ("한국어" to "Korean"),
        "pl" to ("Polski" to "Polish"),
        "cs" to ("Čeština" to "Czech"),
        "nl" to ("Nederlands" to "Dutch"),
        "ca" to ("Català" to "Catalan"),
        "tr" to ("Türkçe" to "Turkish"),
        "uk" to ("Українська" to "Ukrainian"),
        "ar" to ("العربية" to "Arabic"),
        "hi" to ("हिन्दी" to "Hindi"),
        "el" to ("Ελληνικά" to "Greek"),
        "sv" to ("Svenska" to "Swedish"),
        "hu" to ("Magyar" to "Hungarian"),
        "ro" to ("Română" to "Romanian"),
        "id" to ("Bahasa Indonesia" to "Indonesian"),
        "vi" to ("Tiếng Việt" to "Vietnamese"),
        "fa" to ("فارسی" to "Persian"),
        "he" to ("עברית" to "Hebrew"),
        "da" to ("Dansk" to "Danish"),
        "fi" to ("Suomi" to "Finnish"),
        "nb" to ("Norsk bokmål" to "Norwegian Bokmål"),
        "en-XA" to ("English (Pseudolocale [en-XA])" to "English (Pseudolocale [en-XA])"),
        "ar-XB" to ("العربية (Pseudobidi [ar-XB])" to "Arabic (Pseudobidi [ar-XB])")
    )

    /**
     * Returns the full list of selectable app languages, with System Default first.
     */
    fun getSupportedAppLanguages(): List<AppLanguageInfo> {
        val list = mutableListOf<AppLanguageInfo>()
        val defaultLocale = Locale.getDefault()
        val defaultNative = defaultLocale.getDisplayName(defaultLocale).replaceFirstChar { it.uppercase() }
        val defaultEnglish = defaultLocale.getDisplayName(Locale.ENGLISH).replaceFirstChar { it.uppercase() }

        list.add(
            AppLanguageInfo(
                tag = SYSTEM_DEFAULT_TAG,
                displayName = "System Default ($defaultNative)",
                nativeName = defaultNative,
                englishName = defaultEnglish,
                isSystemDefault = true
            )
        )

        for ((tag, names) in SUPPORTED_APP_LOCALES) {
            val (nativeName, englishName) = names
            list.add(
                AppLanguageInfo(
                    tag = tag,
                    displayName = if (nativeName.equals(englishName, ignoreCase = true)) nativeName else "$nativeName ($englishName)",
                    nativeName = nativeName,
                    englishName = englishName
                )
            )
        }

        return list
    }

    /**
     * Gets the currently active language tag (e.g. "en", "de", or "system").
     */
    fun getCurrentLanguageTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) {
            locales[0]?.toLanguageTag() ?: SYSTEM_DEFAULT_TAG
        } else {
            SYSTEM_DEFAULT_TAG
        }
    }

    /**
     * Gets human-readable display name for the currently active app language.
     */
    fun getCurrentAppLanguageDisplayName(): String {
        val tag = getCurrentLanguageTag()
        if (tag == SYSTEM_DEFAULT_TAG) {
            val defaultLocale = Locale.getDefault()
            val nativeName = defaultLocale.getDisplayName(defaultLocale).replaceFirstChar { it.uppercase() }
            return "System Default ($nativeName)"
        }
        val match = getSupportedAppLanguages().firstOrNull { it.tag.equals(tag, ignoreCase = true) }
        if (match != null) {
            return match.displayName
        }
        val loc = Locale.forLanguageTag(tag)
        val nat = loc.getDisplayName(loc).replaceFirstChar { it.uppercase() }
        val eng = loc.getDisplayName(Locale.ENGLISH).replaceFirstChar { it.uppercase() }
        return if (nat.equals(eng, ignoreCase = true)) nat else "$nat ($eng)"
    }

    /**
     * Navigates to the system app language settings on Android 13+ (API 33+),
     * or invokes onFallbackInApp on older OS versions or when system activity fails.
     */
    fun openAppLanguageSettings(activity: Activity, onFallbackInApp: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                }
                activity.startActivity(intent)
                Log.i(TAG, "Launched ACTION_APP_LOCALE_SETTINGS")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch ACTION_APP_LOCALE_SETTINGS, falling back to in-app switcher", e)
                onFallbackInApp()
            }
        } else {
            onFallbackInApp()
        }
    }

    /**
     * Sets the app's language via AppCompatDelegate.
     * Use "system" to reset to system-wide default.
     */
    fun setAppLanguage(languageTag: String) {
        if (languageTag == SYSTEM_DEFAULT_TAG || languageTag.isBlank()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            Log.i(TAG, "Reset app locales to system default")
        } else {
            val localeList = LocaleListCompat.forLanguageTags(languageTag)
            AppCompatDelegate.setApplicationLocales(localeList)
            Log.i(TAG, "Set app locales to: $languageTag")
        }
    }
}
