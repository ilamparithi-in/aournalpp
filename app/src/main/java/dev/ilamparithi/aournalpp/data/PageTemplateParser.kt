package dev.ilamparithi.aournalpp.data

import java.io.File
import java.util.Locale

/**
 * Represents the page configuration used when creating new Xournal++ (.xopp) files.
 *
 * @param width Page width in PostScript points (72 pt = 1 inch).
 * @param height Page height in PostScript points.
 * @param style Solid background pattern style (e.g., "lined", "ruled", "plain", "graph", "dotted").
 * @param color 8-character hex color code in `#RRGGBBAA` format (e.g., "#ffffffff").
 * @param config Optional background layout configuration (e.g., "m1=-72" for right margin).
 */
data class PageTemplate(
    val width: Double = DEFAULT_WIDTH,
    val height: Double = DEFAULT_HEIGHT,
    val style: String = DEFAULT_STYLE,
    val color: String = DEFAULT_COLOR,
    val config: String? = null
) {
    companion object {
        // Standard ISO A4 dimensions in points (210mm x 297mm)
        const val DEFAULT_WIDTH = 595.27559100
        const val DEFAULT_HEIGHT = 841.88976400
        const val DEFAULT_STYLE = "lined"
        const val DEFAULT_COLOR = "#ffffffff"

        /**
         * Parses the page template from Xournal++ `settings.xml`.
         *
         * Xournal++ stores template defaults under:
         * `<property name="pageTemplate" value="xoj/template&#10;..."/>`
         *
         * @param settingsXml The raw XML content of settings.xml.
         * @return [PageTemplate] with parsed or fallback values.
         */
        fun parseFromSettingsXml(settingsXml: String?): PageTemplate {
            if (settingsXml.isNullOrBlank()) {
                return PageTemplate()
            }

            val rawTemplate = extractProperty(settingsXml, "pageTemplate") ?: return PageTemplate()
            return parseTemplateString(rawTemplate)
        }

        /**
         * Parses template from a settings.xml [File].
         */
        fun parseFromSettingsFile(settingsFile: File?): PageTemplate {
            return if (settingsFile != null && settingsFile.exists()) {
                try {
                    parseFromSettingsXml(settingsFile.readText())
                } catch (_: Exception) {
                    PageTemplate()
                }
            } else {
                PageTemplate()
            }
        }

        /**
         * Parses the multiline key-value content stored inside the `pageTemplate` property.
         */
        fun parseTemplateString(rawTemplate: String): PageTemplate {
            // Unescape XML entities if present (e.g. &#10; or &#xA; for newline)
            val normalized = rawTemplate
                .replace("&#10;", "\n")
                .replace("&#xA;", "\n", ignoreCase = true)
                .replace("&amp;", "&")

            var width = DEFAULT_WIDTH
            var height = DEFAULT_HEIGHT
            var style = DEFAULT_STYLE
            var color = DEFAULT_COLOR
            var config: String? = null

            for (line in normalized.lines()) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed == "xoj/template") continue

                val eqIdx = trimmed.indexOf('=')
                if (eqIdx <= 0 || eqIdx >= trimmed.length - 1) continue

                val key = trimmed.substring(0, eqIdx).trim()
                val value = trimmed.substring(eqIdx + 1).trim()

                when (key) {
                    "size" -> {
                        val parts = value.split('x', 'X')
                        if (parts.size == 2) {
                            parts[0].toDoubleOrNull()?.let { width = it }
                            parts[1].toDoubleOrNull()?.let { height = it }
                        }
                    }
                    "backgroundType" -> {
                        if (value.isNotBlank() && !value.startsWith(":")) {
                            style = value
                        }
                    }
                    "backgroundColor" -> {
                        color = normalizeColor(value)
                    }
                    "backgroundTypeConfig" -> {
                        if (value.isNotBlank()) {
                            config = value
                        }
                    }
                }
            }

            return PageTemplate(
                width = width,
                height = height,
                style = style,
                color = color,
                config = config
            )
        }

        /**
         * Normalizes a color string into `#RRGGBBAA` (8-hex with leading hash).
         *
         * Critical note: Xournal++ C++ parser decodes `#` hex color as:
         * `Color{(color >> 8U) | (color << 24U)}` expecting RRGGBBAA byte order.
         * If a 6-digit hex code `#ffffff` is passed, `color >> 8` shifts the red channel to 0,
         * resulting in `#00ffff` (Cyan/Blue)!
         * We therefore guarantee 8-hex format for all RGB hex codes.
         */
        fun normalizeColor(rawColor: String): String {
            val trimmed = rawColor.trim()
            if (trimmed.startsWith("#")) {
                val hex = trimmed.substring(1)
                return when (hex.length) {
                    3 -> {
                        // #RGB -> #RRGGBBff
                        val r = hex[0]
                        val g = hex[1]
                        val b = hex[2]
                        "#$r$r$g$g$b${b}ff".lowercase(Locale.ROOT)
                    }
                    6 -> {
                        // #RRGGBB -> #RRGGBBff (append full alpha)
                        "#${hex}ff".lowercase(Locale.ROOT)
                    }
                    8 -> {
                        // #RRGGBBAA
                        "#$hex".lowercase(Locale.ROOT)
                    }
                    else -> DEFAULT_COLOR
                }
            }

            // Named colors fallback
            return when (trimmed.lowercase(Locale.ROOT)) {
                "white" -> "#ffffffff"
                "black" -> "#000000ff"
                "blue" -> "#add8e6ff"
                "pink" -> "#ffc0cbff"
                "green" -> "#7fffd4ff"
                "orange" -> "#ffa07aff"
                "yellow" -> "#f0e68cff"
                else -> DEFAULT_COLOR
            }
        }

        private fun extractProperty(xml: String, propertyName: String): String? {
            // Match <property name="propertyName" value="..."/>
            val regex = Regex("""<property\s+[^>]*name\s*=\s*["']${Regex.escape(propertyName)}["'][^>]*value\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
            val match = regex.find(xml)
            if (match != null) {
                return match.groupValues[1]
            }

            // Match attribute order reversed: value="..." name="..."
            val regexReversed = Regex("""<property\s+[^>]*value\s*=\s*["']([^"']*)["'][^>]*name\s*=\s*["']${Regex.escape(propertyName)}["']""", RegexOption.IGNORE_CASE)
            return regexReversed.find(xml)?.groupValues?.get(1)
        }
    }

    /**
     * Generates a valid minimal Xournal++ v4 XML string based on this template.
     */
    fun buildXml(): String {
        val configAttr = if (!config.isNullOrBlank()) """ config="$config"""" else ""
        val widthStr = String.format(Locale.US, "%.8f", width)
        val heightStr = String.format(Locale.US, "%.8f", height)

        return """
            <?xml version="1.0" standalone="no"?>
            <xournal creator="Aournal++ 1.3.7" fileversion="4">
              <title>Xournal++ document - see https://github.com/xournalpp/xournalpp</title>
              <page width="$widthStr" height="$heightStr">
                <background type="solid" color="$color" style="$style"$configAttr/>
                <layer/>
              </page>
            </xournal>
        """.trimIndent()
    }
}
