package dev.ilamparithi.aournalpp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ThumbnailManager {

    private const val TAG = "ThumbnailManager"
    private val renderMutex = Mutex()

    fun getCachedThumbnailFile(context: Context, noteFile: File): File? {
        val thumbDir = File(context.cacheDir, "thumbnails")
        val cacheKey = "${noteFile.nameWithoutExtension}_${noteFile.lastModified()}_thumb.png"
        val cached = File(thumbDir, cacheKey)
        return if (cached.exists() && cached.length() > 0) cached else null
    }

    suspend fun getOrCreateThumbnail(
        context: Context,
        noteFile: File,
        pdfExportManager: PdfExportManager
    ): File? = withContext(Dispatchers.IO) {
        if (!noteFile.exists() || noteFile.length() == 0L) return@withContext null

        val thumbDir = File(context.cacheDir, "thumbnails").apply { if (!exists()) mkdirs() }
        val cacheKey = "${noteFile.nameWithoutExtension}_${noteFile.lastModified()}_thumb.png"
        val cachedFile = File(thumbDir, cacheKey)

        if (cachedFile.exists() && cachedFile.length() > 0) {
            return@withContext cachedFile
        }

        renderMutex.withLock {
            if (cachedFile.exists() && cachedFile.length() > 0) {
                return@withContext cachedFile
            }

            try {
                if (noteFile.extension.equals("pdf", ignoreCase = true)) {
                    renderPdfPageToThumbnail(noteFile, cachedFile)
                } else if (noteFile.extension.equals("xopp", ignoreCase = true) ||
                    noteFile.extension.equals("xoj", ignoreCase = true)
                ) {
                    val tempPdf = File(context.cacheDir, "temp_thumb_${System.currentTimeMillis()}.pdf")
                    try {
                        val converted = pdfExportManager.convertXoppToPdf(noteFile, tempPdf)
                        if (converted.isSuccess) {
                            renderPdfPageToThumbnail(tempPdf, cachedFile)
                        }
                    } finally {
                        if (tempPdf.exists()) tempPdf.delete()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to render thumbnail for ${noteFile.name}", e)
            }

            if (cachedFile.exists() && cachedFile.length() > 0) cachedFile else null
        }
    }

    private fun renderPdfPageToThumbnail(pdfFile: File, destFile: File) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null

        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (renderer.pageCount > 0) {
                page = renderer.openPage(0)
                val targetWidth = 400
                val targetHeight = ((400f * page.height) / page.width).toInt().coerceIn(200, 600)

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
                }
                bitmap.recycle()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error rendering PDF page for thumbnail", e)
        } finally {
            try { page?.close() } catch (e: Exception) {}
            try { renderer?.close() } catch (e: Exception) {}
            try { pfd?.close() } catch (e: Exception) {}
        }
    }
}
