package dev.ilamparithi.aournalpp.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint

/**
 * Utility helpers for managing window previews, freeze frame dimensions,
 * and floating toolbar resizing debouncing during multi-window switching.
 */
object WindowPreviewUtils {

    /**
     * Checks whether a cached freeze frame bitmap matches the current viewport dimensions
     * and is safe to use for instantaneous 0ms transition overlays without stretching.
     */
    fun isFreezeFrameDimensionMatching(
        cachedBitmap: Bitmap?,
        targetWidth: Int,
        targetHeight: Int
    ): Boolean {
        if (cachedBitmap == null || cachedBitmap.isRecycled) return false
        if (targetWidth <= 0 || targetHeight <= 0) return false
        return cachedBitmap.width == targetWidth && cachedBitmap.height == targetHeight
    }

    /**
     * Calculates the debounce break duration for toolbar resizing when switching windows.
     * Guarantees at least 2000ms (or half the collapse timeout, whichever is greater).
     */
    fun calculateToolbarResizeDebounceMs(autoCollapseTimeoutMs: Int): Long {
        return maxOf(2000L, (autoCollapseTimeoutMs / 2).toLong())
    }

    /**
     * Scales and adapts a background window preview bitmap to the new viewport dimensions
     * using center-crop geometry to preserve aspect ratio without stretching or distortion.
     */
    fun adaptBitmapToSize(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap? {
        if (source.isRecycled || targetWidth <= 0 || targetHeight <= 0) return null
        if (source.width == targetWidth && source.height == targetHeight) return source
        return try {
            val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            canvas.drawColor(Color.argb(255, 25, 25, 25))

            val scale = maxOf(targetWidth.toFloat() / source.width, targetHeight.toFloat() / source.height)
            val scaledW = source.width * scale
            val scaledH = source.height * scale
            val dx = (targetWidth - scaledW) / 2f
            val dy = (targetHeight - scaledH) / 2f

            val matrix = Matrix().apply {
                postScale(scale, scale)
                postTranslate(dx, dy)
            }
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(source, matrix, paint)
            result
        } catch (e: Throwable) {
            null
        }
    }
}
