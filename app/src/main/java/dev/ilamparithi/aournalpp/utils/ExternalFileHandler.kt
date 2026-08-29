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

    /**
     * Stages an external URI to a temporary cache file so it can be previewed or passed to prompts
     * without immediately creating a permanent copy in the user's Imported notes directory.
     */
    suspend fun stageExternalUri(
        context: Context,
        uri: Uri,
        env: LinuxEnvironment
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            Log.i(TAG, "Staging external file URI to temporary cache: $uri")

            val rawFileName = getDisplayName(context, uri) ?: "temp_${System.currentTimeMillis()}.xopp"
            val sanitizedFileName = rawFileName.replace(Regex("[/\\\\:*?\"<>|]"), "_")

            val stagingDir = File(context.cacheDir, "staged_imports").apply {
                if (!exists()) mkdirs()
            }

            val destFile = File(stagingDir, sanitizedFileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: error("Failed to open input stream for URI: $uri")

            if (!destFile.exists() || destFile.length() == 0L) {
                error("Staged temporary file is empty or was not created: ${destFile.absolutePath}")
            }

            Log.i(TAG, "Successfully staged external file to cache: ${destFile.absolutePath} (${destFile.length()} bytes)")
            destFile
        }
    }

    /**
     * Copies a file (e.g. from temporary staging or URI) into the user's official Imported directory
     * when editing in Xournal++. Reuses/overwrites if same name exists rather than generating timestamp clones.
     */
    suspend fun importToImportedDir(
        sourceFile: File,
        env: LinuxEnvironment
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val importedDir = env.getImportedDirectory()
            if (!importedDir.exists()) importedDir.mkdirs()

            val destFile = File(importedDir, sourceFile.name)
            if (sourceFile.canonicalPath != destFile.canonicalPath) {
                sourceFile.copyTo(destFile, overwrite = true)
            }
            Log.i(TAG, "Imported file to ${destFile.absolutePath} (${destFile.length()} bytes)")
            destFile
        }
    }

    /**
     * Imports an external URI directly into a specified target directory (e.g. the currently viewed folder).
     */
    suspend fun importUriToDirectory(
        context: Context,
        uri: Uri,
        targetDir: File
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            if (!targetDir.exists()) targetDir.mkdirs()
            val rawFileName = getDisplayName(context, uri) ?: "imported_${System.currentTimeMillis()}.pdf"
            val sanitizedFileName = rawFileName.replace(Regex("[/\\\\:*?\"<>|]"), "_")
            val destFile = File(targetDir, sanitizedFileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: error("Failed to open input stream for URI: $uri")
            Log.i(TAG, "Imported external file to target dir: ${destFile.absolutePath} (${destFile.length()} bytes)")
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
