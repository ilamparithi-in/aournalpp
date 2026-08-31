package dev.ilamparithi.aournalpp.utils

import java.text.DateFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utility functions for locale-aware formatting of dates, times, file sizes, percentages, and numbers.
 */
object FormatUtils {

    private val dateFormatCache = ThreadLocal.withInitial { mutableMapOf<String, DateFormat>() }
    private val numberFormatCache = ThreadLocal.withInitial { mutableMapOf<String, NumberFormat>() }
    private val calendarLocal = ThreadLocal.withInitial { Calendar.getInstance() }

    private fun getCachedDateFormat(key: String, factory: () -> DateFormat): DateFormat {
        val map = dateFormatCache.get() ?: mutableMapOf<String, DateFormat>().also { dateFormatCache.set(it) }
        return map.getOrPut(key, factory)
    }

    private fun getCachedNumberFormat(locale: Locale, minDecimals: Int, maxDecimals: Int): NumberFormat {
        val key = "${locale.toLanguageTag()}_${minDecimals}_${maxDecimals}"
        val map = numberFormatCache.get() ?: mutableMapOf<String, NumberFormat>().also { numberFormatCache.set(it) }
        return map.getOrPut(key) {
            NumberFormat.getNumberInstance(locale).apply {
                minimumFractionDigits = minDecimals
                maximumFractionDigits = maxDecimals
            }
        }
    }

    /**
     * Formats an epoch timestamp into a medium localized date (e.g. "Aug 30, 2026", "30.08.2026").
     */
    fun formatDateMedium(epochMs: Long, locale: Locale = Locale.getDefault()): String {
        if (epochMs <= 0) return ""
        return try {
            val key = "date_medium_${locale.toLanguageTag()}"
            val df = getCachedDateFormat(key) { DateFormat.getDateInstance(DateFormat.MEDIUM, locale) }
            df.format(Date(epochMs))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Formats an epoch timestamp into a medium localized date and short time (e.g. "Aug 30, 2026, 5:45 PM", "30.08.2026, 17:45").
     */
    fun formatDateTimeMedium(epochMs: Long, locale: Locale = Locale.getDefault()): String {
        if (epochMs <= 0) return ""
        return try {
            val key = "datetime_medium_short_${locale.toLanguageTag()}"
            val df = getCachedDateFormat(key) { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale) }
            df.format(Date(epochMs))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Formats an hour (0-23) and minute (0-59) into a localized time string (e.g. "5:30 PM" or "17:30")
     * according to user/system locale conventions.
     */
    fun formatTime(hour: Int, minute: Int, locale: Locale = Locale.getDefault()): String {
        return try {
            val cal = calendarLocal.get() ?: Calendar.getInstance().also { calendarLocal.set(it) }
            cal.set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            cal.set(Calendar.MINUTE, minute.coerceIn(0, 59))
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            val key = "time_short_${locale.toLanguageTag()}"
            val df = getCachedDateFormat(key) { DateFormat.getTimeInstance(DateFormat.SHORT, locale) }
            df.format(cal.time)
        } catch (e: Exception) {
            String.format(locale, "%02d:%02d", hour, minute)
        }
    }

    /**
     * Formats byte size into human readable, locale-formatted string (e.g. "1.5 MB", "1,5 MB", "420 KB", "0 B")
     * respecting localized decimal separator (dot vs comma).
     */
    fun formatFileSize(bytes: Long, locale: Locale = Locale.getDefault()): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())

        val nf = getCachedNumberFormat(locale, minDecimals = 0, maxDecimals = 1)
        return "${nf.format(value)} ${units[digitGroups]}"
    }

    /**
     * Formats percentage with localized decimal and grouping separators (e.g. "45%", "99.5%", "99,5%").
     */
    fun formatPercentage(percentage: Number, decimals: Int = 1, locale: Locale = Locale.getDefault()): String {
        val nf = getCachedNumberFormat(locale, minDecimals = 0, maxDecimals = decimals)
        return "${nf.format(percentage)}%"
    }

    /**
     * Formats floating/integer numbers with locale formatting (e.g. 1.25 -> "1.25" or "1,25").
     */
    fun formatNumber(number: Number, maxDecimals: Int = 2, minDecimals: Int = 0, locale: Locale = Locale.getDefault()): String {
        val nf = getCachedNumberFormat(locale, minDecimals = minDecimals, maxDecimals = maxDecimals)
        return nf.format(number)
    }
}
