package dev.ilamparithi.aournalpp.runtime

import android.app.WallpaperManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import java.io.File

object WallpaperHelper {
    private const val TAG = "WallpaperHelper"

    const val MODE_SYSTEM = "system"
    const val MODE_CUSTOM = "custom"
    const val MODE_THEME = "theme"

    const val PREF_KEY_WALLPAPER_MODE = "pref_canvas_wallpaper_mode"
    private const val CUSTOM_WALLPAPER_FILENAME = "custom_wallpaper.png"

    fun getCustomWallpaperFile(context: Context): File {
        return File(context.filesDir, CUSTOM_WALLPAPER_FILENAME)
    }

    fun getWallpaperMode(context: Context): String {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        return prefs.getString(PREF_KEY_WALLPAPER_MODE, MODE_SYSTEM) ?: MODE_SYSTEM
    }

    fun setWallpaperMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY_WALLPAPER_MODE, mode).apply()
    }

    fun saveCustomWallpaper(context: Context, sourceUri: Uri): Result<Unit> = runCatching {
        val destFile = getCustomWallpaperFile(context)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: error("Failed to open source image stream")
        setWallpaperMode(context, MODE_CUSTOM)
    }

    fun clearCustomWallpaper(context: Context) {
        val file = getCustomWallpaperFile(context)
        if (file.exists()) {
            file.delete()
        }
        setWallpaperMode(context, MODE_SYSTEM)
    }

    fun resolveWallpaperBitmap(
        context: Context,
        targetWidth: Int = 0,
        targetHeight: Int = 0
    ): Bitmap {
        val mode = getWallpaperMode(context)
        val w = if (targetWidth > 0) targetWidth else context.resources.displayMetrics.widthPixels
        val h = if (targetHeight > 0) targetHeight else context.resources.displayMetrics.heightPixels

        if (mode == MODE_CUSTOM) {
            val customFile = getCustomWallpaperFile(context)
            if (customFile.exists() && customFile.length() > 0L) {
                try {
                    val decoded = BitmapFactory.decodeFile(customFile.absolutePath)
                    if (decoded != null) {
                        return scaleBitmap(decoded, w, h)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to decode custom wallpaper image, falling back to system", e)
                }
            }
        }

        if (mode == MODE_SYSTEM) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                val drawable: Drawable? = try {
                    wallpaperManager.drawable
                } catch (e: SecurityException) {
                    null
                } catch (e: Exception) {
                    null
                }

                if (drawable != null) {
                    val bmp = drawableToBitmap(drawable, w, h)
                    if (bmp != null) {
                        return bmp
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to extract system wallpaper drawable", e)
            }
        }

        return generateThemeBackdrop(context, w, h)
    }

    private fun drawableToBitmap(drawable: Drawable, targetWidth: Int, targetHeight: Int): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return scaleBitmap(drawable.bitmap, targetWidth, targetHeight)
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else targetWidth
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else targetHeight

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return scaleBitmap(bitmap, targetWidth, targetHeight)
    }

    private fun scaleBitmap(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        if (source.width == targetWidth && source.height == targetHeight) {
            return source
        }
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    fun generateThemeBackdrop(context: Context, width: Int, height: Int): Bitmap {
        val w = if (width > 0) width else 1080
        val h = if (height > 0) height else 1920
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDark = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

        val topColor = if (isDark) Color.rgb(26, 27, 38) else Color.rgb(238, 242, 246)
        val bottomColor = if (isDark) Color.rgb(18, 18, 26) else Color.rgb(218, 224, 233)

        val paint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                topColor, bottomColor, Shader.TileMode.CLAMP
            )
        }

        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        return bitmap
    }

    fun exportBitmapToPpm(bitmap: Bitmap, targetFile: File): Result<Unit> = runCatching {
        targetFile.outputStream().buffered(64 * 1024).use { out ->
            val header = "P6\n${bitmap.width} ${bitmap.height}\n255\n".toByteArray(Charsets.US_ASCII)
            out.write(header)

            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val rgbBuffer = ByteArray(pixels.size * 3)
            for (i in pixels.indices) {
                val c = pixels[i]
                rgbBuffer[i * 3 + 0] = ((c shr 16) and 0xFF).toByte()
                rgbBuffer[i * 3 + 1] = ((c shr 8) and 0xFF).toByte()
                rgbBuffer[i * 3 + 2] = (c and 0xFF).toByte()
            }
            out.write(rgbBuffer)
        }
    }
}
