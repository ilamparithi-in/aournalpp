package dev.ilamparithi.aournalpp.utils.xopp

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import kotlin.math.pow

data class XoppPageBackground(
    val type: String, // "solid", "pdf", "pixmap"
    val color: Int = 0xFFFFFFFF.toInt(),
    val style: String = "plain", // "plain", "lined", "ruled", "graph", "grid", "dotted", "iso_dot"
    val pdfFilename: String? = null,
    val pdfPageNo: Int = 1,
    val pdfDomain: String? = null
)

data class XoppStrokeElement(
    val tool: String, // "pen", "highlighter", "eraser"
    val color: Int,
    val width: Float,
    val points: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as XoppStrokeElement
        if (tool != other.tool) return false
        if (color != other.color) return false
        if (width != other.width) return false
        if (!points.contentEquals(other.points)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = tool.hashCode()
        result = 31 * result + color
        result = 31 * result + width.hashCode()
        result = 31 * result + points.contentHashCode()
        return result
    }
}

data class XoppTextElement(
    val text: String,
    val x: Float,
    val y: Float,
    val size: Float,
    val color: Int
)

data class XoppImageElement(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val base64Data: String
)

data class XoppParsedPageZero(
    val creatorVersion: String,
    val pageWidth: Float,
    val pageHeight: Float,
    val background: XoppPageBackground?,
    val strokes: List<XoppStrokeElement>,
    val texts: List<XoppTextElement>,
    val images: List<XoppImageElement>
)

/**
 * Streaming XML pull parser for Xournal++ (.xopp) and Xournal (.xoj) document files.
 * Isolates XML parsing and geometric coordinate decoding from Canvas rendering.
 */
object XoppParser {

    /**
     * Parses Page 0 and stops parsing immediately for optimal rendering performance.
     */
    fun parsePageZero(stream: InputStream): XoppParsedPageZero? {
        val parser = try {
            Xml.newPullParser()
        } catch (_: Throwable) {
            XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }.newPullParser()
        }
        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setFeature("http://xmlpull.org/v1/doc/features.html#process-docdecl", false)
        } catch (_: Exception) {}
        parser.setInput(stream, "UTF-8")

        var pageWidth = 595.28f
        var pageHeight = 841.89f
        var background: XoppPageBackground? = null
        val strokes = mutableListOf<XoppStrokeElement>()
        val texts = mutableListOf<XoppTextElement>()
        val images = mutableListOf<XoppImageElement>()

        var inPage = false
        var inLayer = false
        var currentTag: String? = null
        val textBuffer = StringBuilder()

        var currentTool = "pen"
        var currentColor = 0xFF000000.toInt()
        var currentWidth = 1.41f
        var currentTextSize = 12f
        var currentTextX = 0f
        var currentTextY = 0f
        var currentTextColor = 0xFF000000.toInt()
        var imgLeft = 0f
        var imgTop = 0f
        var imgRight = 0f
        var imgBottom = 0f

        var creatorVersion = "unknown"

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    textBuffer.setLength(0)
                    when (parser.name) {
                        "xournal" -> {
                            creatorVersion = parser.getAttributeValue(null, "creator") ?: "unknown"
                        }
                        "page" -> {
                            if (!inPage) {
                                inPage = true
                                val wStr = parser.getAttributeValue(null, "width")
                                val hStr = parser.getAttributeValue(null, "height")
                                pageWidth = wStr?.toFloatOrNull()?.coerceAtLeast(100f) ?: 595.28f
                                pageHeight = hStr?.toFloatOrNull()?.coerceAtLeast(100f) ?: 841.89f
                            }
                        }
                        "background" -> {
                            if (inPage) {
                                val type = parser.getAttributeValue(null, "type") ?: "solid"
                                val colorStr = parser.getAttributeValue(null, "color")
                                val style = parser.getAttributeValue(null, "style") ?: "plain"
                                val pdfFile = parser.getAttributeValue(null, "filename")
                                val pdfPage = parser.getAttributeValue(null, "pageno")?.toIntOrNull() ?: 1
                                val domain = parser.getAttributeValue(null, "domain")

                                val color = if (!colorStr.isNullOrBlank()) {
                                    parseXoppColor(colorStr, 0xFFFFFFFF.toInt())
                                } else {
                                    0xFFFFFFFF.toInt()
                                }

                                background = XoppPageBackground(
                                    type = type,
                                    color = color,
                                    style = style.lowercase(),
                                    pdfFilename = pdfFile,
                                    pdfPageNo = pdfPage,
                                    pdfDomain = domain
                                )
                            }
                        }
                        "layer" -> {
                            if (inPage) inLayer = true
                        }
                        "stroke" -> {
                            if (inPage && inLayer) {
                                currentTool = parser.getAttributeValue(null, "tool") ?: "pen"
                                val colorStr = parser.getAttributeValue(null, "color")
                                currentColor = parseXoppColor(colorStr, 0xFF000000.toInt())
                                val widthStr = parser.getAttributeValue(null, "width")
                                currentWidth = parseStrokeWidth(widthStr)
                            }
                        }
                        "text" -> {
                            if (inPage && inLayer) {
                                val sizeStr = parser.getAttributeValue(null, "size")
                                currentTextSize = sizeStr?.toFloatOrNull() ?: 12f
                                val xStr = parser.getAttributeValue(null, "x")
                                val yStr = parser.getAttributeValue(null, "y")
                                currentTextX = xStr?.toFloatOrNull() ?: 0f
                                currentTextY = yStr?.toFloatOrNull() ?: 0f
                                val colorStr = parser.getAttributeValue(null, "color")
                                currentTextColor = parseXoppColor(colorStr, 0xFF000000.toInt())
                            }
                        }
                        "image", "teximage" -> {
                            if (inPage && inLayer) {
                                imgLeft = parser.getAttributeValue(null, "left")?.toFloatOrNull() ?: 0f
                                imgTop = parser.getAttributeValue(null, "top")?.toFloatOrNull() ?: 0f
                                imgRight = parser.getAttributeValue(null, "right")?.toFloatOrNull() ?: 0f
                                imgBottom = parser.getAttributeValue(null, "bottom")?.toFloatOrNull() ?: 0f
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inPage && inLayer) {
                        when (currentTag) {
                            "stroke" -> {
                                val text = parser.text
                                if (!text.isNullOrBlank() && strokes.size < 5000) {
                                    val parsedPoints = parseCoordinates(text)
                                    if (parsedPoints.isNotEmpty()) {
                                        strokes.add(
                                            XoppStrokeElement(
                                                tool = currentTool,
                                                color = currentColor,
                                                width = currentWidth,
                                                points = parsedPoints
                                            )
                                        )
                                    }
                                }
                            }
                            "text" -> {
                                val text = parser.text
                                if (!text.isNullOrBlank()) {
                                    textBuffer.append(text)
                                }
                            }
                            "image", "teximage" -> {
                                val text = parser.text
                                if (!text.isNullOrBlank() && images.size < 5) {
                                    textBuffer.append(text)
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "text" -> {
                            if (inPage && inLayer && texts.size < 200) {
                                val str = textBuffer.toString().trim()
                                if (str.isNotEmpty()) {
                                    texts.add(XoppTextElement(str, currentTextX, currentTextY, currentTextSize, currentTextColor))
                                }
                            }
                            textBuffer.setLength(0)
                            currentTag = null
                        }
                        "image", "teximage" -> {
                            if (inPage && inLayer && images.size < 5) {
                                val str = textBuffer.toString().trim()
                                if (str.isNotEmpty()) {
                                    images.add(XoppImageElement(imgLeft, imgTop, imgRight, imgBottom, str))
                                }
                            }
                            textBuffer.setLength(0)
                            currentTag = null
                        }
                        "stroke" -> {
                            currentTag = null
                        }
                        "layer" -> {
                            inLayer = false
                            currentTag = null
                        }
                        "page" -> {
                            // Page 0 completed
                            return XoppParsedPageZero(
                                creatorVersion = creatorVersion,
                                pageWidth = pageWidth,
                                pageHeight = pageHeight,
                                background = background,
                                strokes = strokes,
                                texts = texts,
                                images = images
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return if (inPage) {
            XoppParsedPageZero(
                creatorVersion = creatorVersion,
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                background = background,
                strokes = strokes,
                texts = texts,
                images = images
            )
        } else {
            null
        }
    }

    fun parseCoordinates(raw: String): FloatArray {
        var tokenCount = 0
        var inToken = false
        val len = raw.length
        for (i in 0 until len) {
            val c = raw[i]
            if (c > ' ') {
                if (!inToken) {
                    tokenCount++
                    inToken = true
                }
            } else {
                inToken = false
            }
        }
        if (tokenCount < 2) return FloatArray(0)

        val result = FloatArray(tokenCount)
        var writeIdx = 0
        var i = 0
        while (i < len) {
            while (i < len && raw[i] <= ' ') {
                i++
            }
            if (i >= len) break

            val start = i
            var isNegative = false
            if (raw[i] == '-') {
                isNegative = true
                i++
            } else if (raw[i] == '+') {
                i++
            }

            var whole = 0.0
            while (i < len && raw[i] in '0'..'9') {
                whole = whole * 10.0 + (raw[i] - '0')
                i++
            }

            var frac = 0.0
            var div = 1.0
            if (i < len && raw[i] == '.') {
                i++
                while (i < len && raw[i] in '0'..'9') {
                    frac = frac * 10.0 + (raw[i] - '0')
                    div *= 10.0
                    i++
                }
            }

            var exp = 0
            if (i < len && (raw[i] == 'e' || raw[i] == 'E')) {
                i++
                var expNeg = false
                if (i < len && raw[i] == '-') {
                    expNeg = true
                    i++
                } else if (i < len && raw[i] == '+') {
                    i++
                }
                while (i < len && raw[i] in '0'..'9') {
                    exp = exp * 10 + (raw[i] - '0')
                    i++
                }
                if (expNeg) exp = -exp
            }

            var value = (whole + (frac / div))
            if (exp != 0) {
                value *= 10.0.pow(exp)
            }
            if (isNegative) value = -value

            if (i > start) {
                result[writeIdx++] = value.toFloat()
            } else {
                while (i < len && raw[i] > ' ') {
                    i++
                }
            }
        }

        return if (writeIdx == tokenCount) result else result.copyOf(writeIdx)
    }

    fun parseStrokeWidth(raw: String?): Float {
        if (raw.isNullOrBlank()) return 1.41f
        var i = 0
        val len = raw.length
        while (i < len && raw[i] <= ' ') i++
        val start = i
        while (i < len && raw[i] > ' ') i++
        if (start >= len) return 1.41f
        val first = raw.substring(start, i)
        return first.toFloatOrNull()?.coerceIn(0.2f, 80f) ?: 1.41f
    }

    fun parseXoppColor(raw: String?, defaultColor: Int): Int {
        if (raw.isNullOrBlank()) return defaultColor
        val s = raw.trim()

        if (s.startsWith("#")) {
            val hex = s.substring(1)
            return when (hex.length) {
                8 -> {
                    val r = hex.substring(0, 2).toIntOrNull(16) ?: 0
                    val g = hex.substring(2, 4).toIntOrNull(16) ?: 0
                    val b = hex.substring(4, 6).toIntOrNull(16) ?: 0
                    val a = hex.substring(6, 8).toIntOrNull(16) ?: 255
                    ((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
                }
                6 -> {
                    val r = hex.substring(0, 2).toIntOrNull(16) ?: 0
                    val g = hex.substring(2, 4).toIntOrNull(16) ?: 0
                    val b = hex.substring(4, 6).toIntOrNull(16) ?: 0
                    (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
                }
                3 -> {
                    val r = hex.substring(0, 1).repeat(2).toIntOrNull(16) ?: 0
                    val g = hex.substring(1, 2).repeat(2).toIntOrNull(16) ?: 0
                    val b = hex.substring(2, 3).repeat(2).toIntOrNull(16) ?: 0
                    (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
                }
                else -> defaultColor
            }
        }

        return when (s.lowercase()) {
            "black" -> 0xFF000000.toInt()
            "blue" -> 0xFF3333CC.toInt()
            "red" -> 0xFFFF0000.toInt()
            "green" -> 0xFF008000.toInt()
            "gray", "grey" -> 0xFF808080.toInt()
            "lightgray", "lightgrey" -> 0xFFD3D3D3.toInt()
            "darkgray", "darkgrey" -> 0xFF404040.toInt()
            "yellow" -> 0xFFFFFF00.toInt()
            "magenta" -> 0xFFFF00FF.toInt()
            "cyan" -> 0xFF00FFFF.toInt()
            "orange" -> 0xFFFFA500.toInt()
            "brown" -> 0xFF8B4513.toInt()
            "pink" -> 0xFFFFC0CB.toInt()
            "white" -> 0xFFFFFFFF.toInt()
            "lightblue" -> 0xFFADD8E6.toInt()
            "lightgreen" -> 0xFF90EE90.toInt()
            else -> defaultColor
        }
    }
}
