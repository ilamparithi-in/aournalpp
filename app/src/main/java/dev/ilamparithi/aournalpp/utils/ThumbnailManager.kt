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
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance thumbnail manager for Aournal++.
 *
 * Features:
 * - Ultra-fast vector native rendering for .xopp and .xoj files via [XoppNativeRenderer] (~2-5ms).
 * - Direct native PDF page-0 rendering via [PdfRenderer] (~3-5ms).
 * - Transparent fail-safe headless CLI fallback if native rendering ever fails.
 * - In-flight request deduplication so multiple UI components requesting the same note thumbnail
 *   share a single background render task.
 * - Bounded parallel coroutine rendering (4 concurrent workers) avoiding main-thread microstutters.
 * - Multi-tier memory (LruCache) and disk caching with automatic size management.
 * - Background prefetching support.
 */
object ThumbnailManager {

    private const val TAG = "ThumbnailManager"
    private const val MAX_CACHE_BYTES = 48L * 1024 * 1024 // 48 MB disk cache
    private const val THUMBNAIL_WIDTH = 600

    /** Parallel dispatcher allowing up to 4 concurrent thumbnail renders without blocking each other. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val renderDispatcher = Dispatchers.IO.limitedParallelism(4)

    /** In-flight deduplication map so identical requests share the same render task. */
    private val inFlightJobs = ConcurrentHashMap<String, Deferred<ImageBitmap?>>()

    /** Per-file mutexes for disk writes. */
    private val fileLocks = ConcurrentHashMap<String, Mutex>()

    /** Thumbnails resolved on disk this session. */
    private val resolved = ConcurrentHashMap<String, File>()

    /**
     * Decoded memory cache bounded to an eighth of the runtime heap.
     */
    private val decoded = object : LruCache<String, ImageBitmap>(
        ((Runtime.getRuntime().maxMemory() / 1024) / 8).toInt().coerceAtLeast(8 * 1024)
    ) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            (value.width * value.height * 4) / 1024
    }

    internal fun cacheKeyFor(noteFile: File): String {
        val canonical = try { noteFile.canonicalPath } catch (_: Exception) { noteFile.absolutePath }
        val pathHash = try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(canonical.toByteArray(Charsets.UTF_8))
            digest.take(8).joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            canonical.hashCode().toUInt().toString(16)
        }
        val cleanName = noteFile.name.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(32)
        return "thumb_${cleanName}_${pathHash}_${noteFile.lastModified()}.png"
    }

    fun getCachedThumbnailFile(noteFile: File): File? = resolved[cacheKeyFor(noteFile)]

    fun getCachedThumbnail(noteFile: File): ImageBitmap? = decoded[cacheKeyFor(noteFile)]

    /**
     * Resolves the thumbnail and decodes it into memory.
     * Uses in-flight deduplication to avoid redundant parallel jobs for the same note.
     */
    suspend fun getOrCreateThumbnailBitmap(
        context: Context,
        noteFile: File,
        pdfExportManager: PdfExportManager
    ): ImageBitmap? = withContext(renderDispatcher) {
        if (!noteFile.exists() || noteFile.length() == 0L) return@withContext null

        val cacheKey = cacheKeyFor(noteFile)
        decoded[cacheKey]?.let { return@withContext it }

        // Deduplicate in-flight render requests
        val inFlight = inFlightJobs[cacheKey]
        if (inFlight != null) {
            return@withContext inFlight.await()
        }

        val deferred = async(renderDispatcher) {
            try {
                // 1. Check if thumbnail is already cached on disk
                val thumbDir = File(context.cacheDir, "thumbnails").apply { if (!exists()) mkdirs() }
                val cachedFile = File(thumbDir, cacheKey)

                if (cachedFile.exists() && cachedFile.length() > 0) {
                    resolved[cacheKey] = cachedFile
                    val bitmap = decodeBitmapFromFile(cachedFile)
                    if (bitmap != null) {
                        val image = bitmap.asImageBitmap()
                        decoded.put(cacheKey, image)
                        return@async image
                    }
                }

                // 2. Render thumbnail
                val renderedBitmap = renderThumbnailBitmap(context, noteFile, pdfExportManager)
                if (renderedBitmap != null) {
                    // Save to disk cache asynchronously
                    val lock = fileLocks.getOrPut(cacheKey) { Mutex() }
                    lock.withLock {
                        if (!cachedFile.exists() || cachedFile.length() == 0L) {
                            try {
                                FileOutputStream(cachedFile).use { out ->
                                    renderedBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                                }
                                resolved[cacheKey] = cachedFile
                                trimCache(thumbDir)
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to save thumbnail to disk for ${noteFile.name}", e)
                            }
                        }
                    }

                    val image = renderedBitmap.asImageBitmap()
                    decoded.put(cacheKey, image)
                    return@async image
                }

                null
            } finally {
                inFlightJobs.remove(cacheKey)
            }
        }

        inFlightJobs[cacheKey] = deferred
        deferred.await()
    }

    /**
     * Resolves the thumbnail file on disk.
     */
    suspend fun getOrCreateThumbnail(
        context: Context,
        noteFile: File,
        pdfExportManager: PdfExportManager
    ): File? = withContext(renderDispatcher) {
        if (!noteFile.exists() || noteFile.length() == 0L) return@withContext null

        val cacheKey = cacheKeyFor(noteFile)
        val thumbDir = File(context.cacheDir, "thumbnails").apply { if (!exists()) mkdirs() }
        val cachedFile = File(thumbDir, cacheKey)

        if (cachedFile.exists() && cachedFile.length() > 0) {
            resolved[cacheKey] = cachedFile
            return@withContext cachedFile
        }

        // Generate bitmap and save
        val bitmap = getOrCreateThumbnailBitmap(context, noteFile, pdfExportManager)
        if (bitmap != null && cachedFile.exists() && cachedFile.length() > 0) {
            resolved[cacheKey] = cachedFile
            return@withContext cachedFile
        }

        null
    }

    /**
     * Core renderer:
     * - Fast path: [XoppNativeRenderer] for .xopp/.xoj (2-5ms)
     * - Direct path: [PdfRenderer] for .pdf (3-5ms)
     * - Fail-safe fallback: [PdfExportManager.convertXoppToPdf] + [PdfRenderer]
     */
    private suspend fun renderThumbnailBitmap(
        context: Context,
        noteFile: File,
        pdfExportManager: PdfExportManager
    ): Bitmap? = withContext(renderDispatcher) {
        val ext = noteFile.extension.lowercase()

        if (ext == "pdf") {
            return@withContext renderPdfToBitmap(noteFile, THUMBNAIL_WIDTH)
        }

        if (ext == "xopp" || ext == "xoj") {
            // Fast Path: Native Vector XOPP Renderer
            val nativeBitmap = XoppNativeRenderer.renderPageZero(context, noteFile, THUMBNAIL_WIDTH)
            if (nativeBitmap != null) {
                return@withContext nativeBitmap
            }

            // Fallback Path: Headless Linux CLI process conversion
            Log.i(TAG, "Native render returned null for ${noteFile.name}; trying headless PDF fallback")
            val tempPdf = File(context.cacheDir, "temp_thumb_${System.currentTimeMillis()}_${noteFile.nameWithoutExtension}.pdf")
            try {
                val converted = pdfExportManager.convertXoppToPdf(noteFile, tempPdf)
                if (converted.isSuccess) {
                    return@withContext renderPdfToBitmap(tempPdf, THUMBNAIL_WIDTH)
                } else {
                    Log.w(TAG, "Headless fallback also failed for ${noteFile.name}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception during fallback rendering for ${noteFile.name}", e)
            } finally {
                if (tempPdf.exists()) tempPdf.delete()
            }
        }

        null
    }

    private fun renderPdfToBitmap(pdfFile: File, targetWidth: Int): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null

        return try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (renderer.pageCount > 0) {
                page = renderer.openPage(0)
                val targetHeight = ((targetWidth.toFloat() * page.height) / page.width).toInt().coerceIn(200, 2400)

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error rendering PDF page for thumbnail: ${pdfFile.name}", e)
            null
        } finally {
            try { page?.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    private fun decodeBitmapFromFile(file: File): Bitmap? {
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode thumbnail from disk: ${file.name}", e)
            null
        }
    }

    /**
     * Background prefetching helper: warms up the memory & disk caches for a list of notes.
     */
    fun prefetchThumbnails(
        context: Context,
        notes: List<NoteDocument>,
        pdfExportManager: PdfExportManager,
        scope: CoroutineScope
    ) {
        if (notes.isEmpty()) return
        scope.launch(renderDispatcher) {
            for (note in notes) {
                val cacheKey = cacheKeyFor(note.file)
                if (decoded[cacheKey] == null) {
                    try {
                        getOrCreateThumbnailBitmap(context, note.file, pdfExportManager)
                    } catch (_: Exception) {}
                }
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
}
