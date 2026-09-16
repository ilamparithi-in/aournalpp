package dev.ilamparithi.aournalpp.utils

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import com.j256.simplemagic.ContentInfoUtil
import dev.ilamparithi.aournalpp.MainActivity
import dev.ilamparithi.aournalpp.R
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream

/**
 * File type detector using magic bytes to reliably identify note files (.xopp, .xoj, .pdf)
 * and distinguish them from arbitrary binaries matching `application/octet-stream`.
 *
 * When an unsupported file is opened, provides intent redirection to launch an external
 * application chooser using the proper detected MIME type, explicitly excluding Aournal++
 * to prevent redirection loops.
 */
object FileTypeDetector {

    private const val TAG = "FileTypeDetector"
    private val contentInfoUtil by lazy { ContentInfoUtil() }

    enum class NoteType {
        XOPP,
        XOJ,
        PDF
    }

    data class DetectionResult(
        val isSupportedNote: Boolean,
        val detectedMimeType: String,
        val noteType: NoteType?
    )

    /**
     * Inspects a file's content and filename to determine if it is a supported Aournal++ note.
     */
    fun detect(file: File): DetectionResult {
        val ext = file.extension.lowercase()

        // 1. Fast path for known extensions
        if (ext == "pdf") {
            return DetectionResult(isSupportedNote = true, detectedMimeType = "application/pdf", noteType = NoteType.PDF)
        }
        if (ext == "xopp") {
            return DetectionResult(isSupportedNote = true, detectedMimeType = "application/x-xopp", noteType = NoteType.XOPP)
        }
        if (ext == "xoj") {
            return DetectionResult(isSupportedNote = true, detectedMimeType = "application/x-xoj", noteType = NoteType.XOJ)
        }

        // 2. Use magic bytes to identify content type via simplemagic
        val magicMime = try {
            contentInfoUtil.findMatch(file)?.mimeType
        } catch (e: Exception) {
            Log.w(TAG, "Failed to analyze magic bytes for ${file.name}", e)
            null
        }

        // 3. Check for PDF magic
        if (magicMime == "application/pdf" || isPdfMagic(file)) {
            return DetectionResult(isSupportedNote = true, detectedMimeType = "application/pdf", noteType = NoteType.PDF)
        }

        // 4. Check for GZIP (potential .xopp or compressed .xoj)
        if (magicMime == "application/x-gzip" || magicMime == "application/gzip" || isGzipMagic(file)) {
            val isXournalDoc = isGzipXournalNote(file)
            if (isXournalDoc) {
                return DetectionResult(
                    isSupportedNote = true,
                    detectedMimeType = "application/x-xopp",
                    noteType = NoteType.XOPP
                )
            }
        }

        // 5. Check for XML (potential uncompressed legacy .xoj)
        if (magicMime == "application/xml" || magicMime == "text/xml") {
            if (isPlainXmlXournalNote(file)) {
                return DetectionResult(
                    isSupportedNote = true,
                    detectedMimeType = "application/x-xoj",
                    noteType = NoteType.XOJ
                )
            }
        }

        // 6. Non-note file: resolve the most accurate MIME type possible
        val resolvedMime = magicMime
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext).takeUnless { it.isNullOrEmpty() }
            ?: "application/octet-stream"

        return DetectionResult(
            isSupportedNote = false,
            detectedMimeType = resolvedMime,
            noteType = null
        )
    }

    /**
     * Checks if the file starts with the PDF magic header `%PDF-`.
     */
    private fun isPdfMagic(file: File): Boolean {
        return try {
            FileInputStream(file).use { input ->
                val header = ByteArray(5)
                val read = input.read(header)
                read == 5 && header[0] == 0x25.toByte() && // %
                        header[1] == 0x50.toByte() && // P
                        header[2] == 0x44.toByte() && // D
                        header[3] == 0x46.toByte() && // F
                        header[4] == 0x2D.toByte()    // -
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if the file starts with gzip magic bytes 0x1F, 0x8B.
     */
    private fun isGzipMagic(file: File): Boolean {
        return try {
            FileInputStream(file).use { input ->
                val b1 = input.read()
                val b2 = input.read()
                b1 == 0x1F && b2 == 0x8B
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Peeks inside a gzip file's first few decompressed lines to see if it starts with `<xournal`.
     */
    private fun isGzipXournalNote(file: File): Boolean {
        return try {
            GZIPInputStream(FileInputStream(file)).use { gzip ->
                peekStreamForXournalTag(gzip)
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Peeks inside a plain text XML file to check for `<xournal`.
     */
    private fun isPlainXmlXournalNote(file: File): Boolean {
        return try {
            FileInputStream(file).use { input ->
                peekStreamForXournalTag(input)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun peekStreamForXournalTag(input: InputStream): Boolean {
        val buffer = ByteArray(1024)
        val bytesRead = input.read(buffer)
        if (bytesRead <= 0) return false
        val content = String(buffer, 0, bytesRead, Charsets.UTF_8)
        return content.contains("<xournal")
    }

    /**
     * Redirects an external file intent to the system app chooser using the proper detected MIME type.
     * Crucially excludes Aournal++ from the chooser using [Intent.EXTRA_EXCLUDE_COMPONENTS]
     * to prevent an infinite loop where the chooser picks Aournal++ again.
     *
     * @param context Calling Activity context
     * @param uri The original file URI
     * @param detectedMimeType The concrete MIME type (e.g. "image/png", "application/zip", "video/mp4")
     * @return true if the chooser was launched successfully, false otherwise.
     */
    fun redirectIntent(context: Context, uri: Uri, detectedMimeType: String): Boolean {
        Log.i(TAG, "Redirecting external intent with resolved MIME '$detectedMimeType' for URI: $uri")

        val redirectIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, detectedMimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooserTitle = context.getString(R.string.open_with)
        val chooser = Intent.createChooser(redirectIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                putExtra(
                    Intent.EXTRA_EXCLUDE_COMPONENTS,
                    arrayOf(ComponentName(context, MainActivity::class.java))
                )
            }
        }

        return try {
            context.startActivity(chooser)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No app available to handle redirect for MIME $detectedMimeType: $uri", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to redirect intent for MIME $detectedMimeType: $uri", e)
            false
        }
    }
}
