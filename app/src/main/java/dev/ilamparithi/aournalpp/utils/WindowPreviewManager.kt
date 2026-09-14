package dev.ilamparithi.aournalpp.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Manages full-resolution window preview snapshots on disk and in-memory.
 * Enables the :canvas process to persist live window captures and the main process
 * to display them in the Active Workspace Return Portal without IPC Binder limits.
 */
object WindowPreviewManager {
    private const val TAG = "WindowPreviewManager"
    private const val PREVIEWS_DIR_NAME = "window_previews"

    @Suppress("DEPRECATION")
    private val webpCompressFormat: Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }

    // Cache up to 8 full-resolution bitmaps in memory in the main process
    private val memoryCache = object : LruCache<String, Bitmap>(8) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    fun getPreviewsDir(baseDir: File): File {
        val dir = File(baseDir, PREVIEWS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getPreviewFile(baseDir: File, windowId: String): File {
        val safeName = windowId.replace("0x", "x").replace(":", "_")
        return File(getPreviewsDir(baseDir), "win_$safeName.webp")
    }

    fun getStageCompositeFile(baseDir: File): File {
        return File(getPreviewsDir(baseDir), "stage_composite.webp")
    }

    /**
     * Persists a window preview snapshot to disk (called from the :canvas process).
     */
    fun savePreview(baseDir: File, windowId: String, bitmap: Bitmap): Boolean {
        return saveBitmapToFile(getPreviewFile(baseDir, windowId), bitmap)
    }

    /**
     * Persists the combined stage layout snapshot to disk (called from the :canvas process).
     */
    fun saveStageComposite(baseDir: File, bitmap: Bitmap): Boolean {
        return saveBitmapToFile(getStageCompositeFile(baseDir), bitmap)
    }

    private fun saveBitmapToFile(targetFile: File, bitmap: Bitmap): Boolean {
        val tmpFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
        return try {
            FileOutputStream(tmpFile).use { out ->
                bitmap.compress(webpCompressFormat, 95, out)
            }
            if (!tmpFile.renameTo(targetFile)) {
                targetFile.delete()
                tmpFile.renameTo(targetFile)
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save preview bitmap to $targetFile", e)
            try { tmpFile.delete() } catch (_: Exception) {}
            false
        }
    }

    /**
     * Loads a window preview bitmap (called from the main process), checking the memory
     * cache first, then reading from disk asynchronously on Dispatchers.IO.
     * Note: Never falls back to stage composite to prevent window preview mismatches.
     */
    suspend fun loadPreview(baseDir: File, windowId: String, timestamp: Long = 0L): Bitmap? = withContext(Dispatchers.IO) {
        val file = getPreviewFile(baseDir, windowId)
        if (!file.exists() || file.length() < 100) return@withContext null

        val lastModified = file.lastModified()
        val cacheKey = "${windowId}_$lastModified"
        memoryCache.get(cacheKey)?.let { return@withContext it }

        try {
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            if (bmp != null) {
                memoryCache.put(cacheKey, bmp)
            }
            bmp
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode preview for window $windowId from ${file.name}", e)
            null
        }
    }

    /**
     * Loads the compound stage composite preview bitmap asynchronously on Dispatchers.IO.
     */
    suspend fun loadStageComposite(baseDir: File, timestamp: Long = 0L): Bitmap? = withContext(Dispatchers.IO) {
        val file = getStageCompositeFile(baseDir)
        if (!file.exists() || file.length() < 100) return@withContext null

        val lastModified = file.lastModified()
        val cacheKey = "stage_composite_$lastModified"
        memoryCache.get(cacheKey)?.let { return@withContext it }

        try {
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            if (bmp != null) {
                memoryCache.put(cacheKey, bmp)
            }
            bmp
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode stage composite preview", e)
            null
        }
    }

    fun clearMemoryCache() {
        memoryCache.evictAll()
    }
}
