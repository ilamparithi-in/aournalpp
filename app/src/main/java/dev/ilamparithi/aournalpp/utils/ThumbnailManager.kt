package dev.ilamparithi.aournalpp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

object ThumbnailManager {

    private const val TAG = "ThumbnailManager"
    private const val MAX_CACHE_BYTES = 32L * 1024 * 1024

    private val renderMutex = Mutex()

    /** Thumbnails resolved this session, so list items do not hit the disk to check. */
    private val resolved = ConcurrentHashMap<String, File>()

    /** Includes the folder so same-named notes in different folders do not collide. */
    private fun cacheKeyFor(noteFile: File): String {
        val folderHash = Integer.toHexString(noteFile.parent.orEmpty().hashCode())
        return "${noteFile.nameWithoutExtension}_${folderHash}_${noteFile.lastModified()}_hd_thumb.png"
    }

    fun getCachedThumbnailFile(noteFile: File): File? = resolved[cacheKeyFor(noteFile)]

    /**
     * Decoded thumbnails, bounded to an eighth of the heap.
     */
    private val decoded = object : LruCache<String, ImageBitmap>(
        ((Runtime.getRuntime().maxMemory() / 1024) / 8).toInt().coerceAtLeast(8 * 1024)
    ) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            (value.width * value.height * 4) / 1024
    }

    fun getCachedThumbnail(noteFile: File): ImageBitmap? = decoded[cacheKeyFor(noteFile)]

    /** Resolves the thumbnail and decodes it off the main thread. */
    suspend fun getOrCreateThumbnailBitmap(
        context: Context,
        noteFile: File,
        pdfExportManager: PdfExportManager
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        val cacheKey = cacheKeyFor(noteFile)
        decoded[cacheKey]?.let { return@withContext it }

        val file = getOrCreateThumbnail(context, noteFile, pdfExportManager) ?: return@withContext null
        val bitmap = try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode thumbnail for ${noteFile.name}", e)
            null
        } ?: return@withContext null

        val image = bitmap.asImageBitmap()
        decoded.put(cacheKey, image)
        image
    }

    suspend fun getOrCreateThumbnail(
        context: Context,
        noteFile: File,
        pdfExportManager: PdfExportManager
    ): File? = withContext(Dispatchers.IO) {
        if (!noteFile.exists() || noteFile.length() == 0L) return@withContext null

        val thumbDir = File(context.cacheDir, "thumbnails").apply { if (!exists()) mkdirs() }
        val cacheKey = cacheKeyFor(noteFile)
        val cachedFile = File(thumbDir, cacheKey)

        if (cachedFile.exists() && cachedFile.length() > 0) {
            resolved[cacheKey] = cachedFile
            return@withContext cachedFile
        }

        renderMutex.withLock {
            if (cachedFile.exists() && cachedFile.length() > 0) {
                resolved[cacheKey] = cachedFile
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

            if (cachedFile.exists() && cachedFile.length() > 0) {
                resolved[cacheKey] = cachedFile
                trimCache(thumbDir)
                cachedFile
            } else {
                null
            }
        }
    }

    /** Drops the oldest thumbnails once the cache grows past [MAX_CACHE_BYTES]. */
    private fun trimCache(thumbDir: File) {
        try {
            val files = thumbDir.listFiles()?.filter { it.isFile } ?: return
            var total = files.sumOf { it.length() }
            if (total <= MAX_CACHE_BYTES) return

            for (file in files.sortedBy { it.lastModified() }) {
                if (total <= MAX_CACHE_BYTES) break
                val size = file.length()
                if (file.delete()) {
                    total -= size
                    resolved.remove(file.name)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to trim thumbnail cache", e)
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
                val targetWidth = 1200
                val targetHeight = ((1200f * page.height) / page.width).toInt().coerceIn(600, 2400)

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
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
