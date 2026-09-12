package dev.ilamparithi.aournalpp.utils

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import kotlinx.coroutines.CoroutineScope
import java.io.File

/**
 * Contract for thumbnail generation, caching, and background prefetching.
 * Decouples Compose UI and testing components from singleton static state.
 */
interface IThumbnailManager {
    fun getCachedThumbnailFile(noteFile: File, lastModifiedMs: Long = 0L): File?
    fun getCachedThumbnail(noteFile: File, lastModifiedMs: Long = 0L): ImageBitmap?
    suspend fun getOrCreateThumbnailBitmap(
        context: Context,
        noteFile: File,
        pdfExportManager: PdfExportManager?,
        lastModifiedMs: Long = 0L
    ): ImageBitmap?
    suspend fun getOrCreateThumbnail(
        context: Context,
        noteFile: File,
        pdfExportManager: PdfExportManager?,
        lastModifiedMs: Long = 0L
    ): File?
    fun prefetchThumbnails(
        context: Context,
        notes: List<NoteDocument>,
        pdfExportManager: PdfExportManager?,
        scope: CoroutineScope
    )
}

/**
 * CompositionLocal provider for IThumbnailManager, defaulting to the production ThumbnailManager singleton.
 */
val LocalThumbnailManager = staticCompositionLocalOf<IThumbnailManager> { ThumbnailManager }
