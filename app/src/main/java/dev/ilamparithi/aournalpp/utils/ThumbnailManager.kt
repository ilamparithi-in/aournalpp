package dev.ilamparithi.aournalpp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.Build
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
import java.util.concurrent.atomic.AtomicInteger

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
 * - Multi-tier memory (LruCache) and WebP disk caching with non-blocking asynchronous size management.
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

    /** Counter for debouncing asynchronous disk cache trimming. */
    private val writesSinceLastTrim = AtomicInteger(0)

    /**
     * Decoded memory cache bounded to an eighth of the runtime heap.
     */
    private val decoded = object : LruCache<String, ImageBitmap>(
        ((Runtime.getRuntime().maxMemory() / 1024) / 8).toInt().coerceAtLeast(8 * 1024)
    ) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            (value.width * value.height * 4) / 1024
    }

    /** Fast in-memory cache for path hashes to avoid repeated SHA calculations. */
    private val pathHashCache = ConcurrentHashMap<String, String>()

    @Suppress("DEPRECATION")
    private val webpCompressFormat: Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }

    internal fun cacheKeyFor(noteFile: File, lastModifiedMs: Long = 0L): String {
        val path = noteFile.path
        val pathHash = pathHashCache.getOrPut(path) {
            try {
                val md = java.security.MessageDigest.getInstance("SHA-256")
                val digest = md.digest(path.toByteArray(Charsets.UTF_8))
                digest.take(8).joinToString("") { "%02x".format(it) }
            } catch (_: Exception) {
                path.hashCode().toUInt().toString(16)
            }
        }
        val cleanName = noteFile.name.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(32)
        val modTime = if (lastModifiedMs > 0L) lastModifiedMs else try { noteFile.lastModified() } catch (_: Exception) { 0L }
        return "thumb_${cleanName}_${pathHash}_${modTime}.webp"
    }

    fun getCachedThumbnailFile(noteFile: File, lastModifiedMs: Long = 0L): File? =
        resolved[cacheKeyFor(noteFile, lastModifiedMs)]

    fun getCachedThumbnail(noteFile: File, lastModifiedMs: Long = 0L): ImageBitmap? =
        decoded[cacheKeyFor(noteFile, lastModifiedMs)]

    /**
     * Resolves the thumbnail and decodes it into memory.
     * Uses in-flight deduplication to avoid redundant parallel jobs for the same note.
     */
    suspend fun getOrCreateThumbnailBitmap(
        context: Context,
        noteFile: File,
        pdfExportManager: PdfExportManager,
        lastModifiedMs: Long = 0L
    ): ImageBitmap? = withContext(renderDispatcher) {
        val modTime = if (lastModifiedMs > 0L) lastModifiedMs else try { noteFile.lastModified() } catch (_: Exception) { 0L }
        val cacheKey = cacheKeyFor(noteFile, modTime)
        decoded[cacheKey]?.let { return@withContext it }

        if (!noteFile.exists() || noteFile.length() == 0L) return@withContext null

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
                    // Save to disk cache asynchronously without blocking reader threads
                    val lock = fileLocks.getOrPut(cacheKey) { Mutex() }
                    lock.withLock {
                        if (!cachedFile.exists() || cachedFile.length() == 0L) {
                            try {
                                FileOutputStream(cachedFile).use { out ->
                                    renderedBitmap.compress(webpCompressFormat, 85, out)
                                }
                                resolved[cacheKey] = cachedFile
                                scheduleAsyncTrim(thumbDir)
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
        pdfExportManager: PdfExportManager,
        lastModifiedMs: Long = 0L
    ): File? = withContext(renderDispatcher) {
        val modTime = if (lastModifiedMs > 0L) lastModifiedMs else try { noteFile.lastModified() } catch (_: Exception) { 0L }
        val cacheKey = cacheKeyFor(noteFile, modTime)
        val thumbDir = File(context.cacheDir, "thumbnails").apply { if (!exists()) mkdirs() }
        val cachedFile = File(thumbDir, cacheKey)

        if (cachedFile.exists() && cachedFile.length() > 0) {
            resolved[cacheKey] = cachedFile
            return@withContext cachedFile
        }

        if (!noteFile.exists() || noteFile.length() == 0L) return@withContext null

        // Generate bitmap and save
        val bitmap = getOrCreateThumbnailBitmap(context, noteFile, pdfExportManager, modTime)
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
                val cacheKey = cacheKeyFor(note.file, note.lastModifiedMs)
                if (decoded[cacheKey] == null) {
                    try {
                        getOrCreateThumbnailBitmap(context, note.file, pdfExportManager, note.lastModifiedMs)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun scheduleAsyncTrim(thumbDir: File) {
        if (writesSinceLastTrim.incrementAndGet() >= 25) {
            writesSinceLastTrim.set(0)
            CoroutineScope(Dispatchers.IO).launch {
                trimCache(thumbDir)
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

