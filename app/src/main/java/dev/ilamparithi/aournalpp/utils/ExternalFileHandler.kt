package dev.ilamparithi.aournalpp.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExternalFileHandler {

    private const val TAG = "ExternalFileHandler"

    suspend fun stageExternalUri(
        context: Context,
        uri: Uri,
        env: LinuxEnvironment
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            Log.i(TAG, "Staging external file URI: $uri")

            val rawFileName = getDisplayName(context, uri) ?: "imported_${System.currentTimeMillis()}.xopp"
            val sanitizedFileName = rawFileName.replace(Regex("[/\\\\:*?\"<>|]"), "_")

            val importedDir = File(env.getNotesDirectory(), "Imported").apply {
                if (!exists()) mkdirs()
            }

            var destFile = File(importedDir, sanitizedFileName)
            if (destFile.exists()) {
                val nameWithoutExt = sanitizedFileName.substringBeforeLast(".")
                val ext = sanitizedFileName.substringAfterLast(".", "")
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val newName = if (ext.isNotEmpty()) "${nameWithoutExt}_$timestamp.$ext" else "${nameWithoutExt}_$timestamp"
                destFile = File(importedDir, newName)
            }

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: error("Failed to open input stream for URI: $uri")

            if (!destFile.exists() || destFile.length() == 0L) {
                error("Imported file is empty or was not created: ${destFile.absolutePath}")
            }

            Log.i(TAG, "Successfully staged external file to ${destFile.absolutePath} (${destFile.length()} bytes)")
            destFile
        }
    }

    private fun getDisplayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.lastPathSegment
        }

        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            return cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resolve display name from ContentResolver", e)
            }
        }

        return uri.lastPathSegment?.substringAfterLast('/')
    }
}
