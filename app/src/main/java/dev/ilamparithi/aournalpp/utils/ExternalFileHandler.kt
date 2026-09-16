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

            val originalMtime = getLastModified(context, uri)
            if (originalMtime != null && originalMtime > 0L) {
                destFile.setLastModified(originalMtime)
            }

            if (!destFile.exists() || destFile.length() == 0L) {
                error("Staged temporary file is empty or was not created: ${destFile.absolutePath}")
            }

            Log.i(TAG, "Successfully staged external file to cache: ${destFile.absolutePath} (${destFile.length()} bytes)")
            destFile
        }
    }

    /**
     * Resolves whether the given URI corresponds to a file already within the user's notes directory.
     * If so, returns the existing [File] so it can be opened directly without re-importing or staging.
     */
    fun resolveIfInNotesDirectory(context: Context, uri: Uri, rootNotesDir: File): File? {
        val rootPath = rootNotesDir.canonicalPath

        // 1. Direct file:// URI
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            val file = File(path)
            if (file.exists()) {
                val canonical = file.canonicalPath
                if (canonical.startsWith(rootPath)) return file
            }
        }

        // 2. Content URI from our own FileProvider
        if (uri.scheme == "content" && uri.authority == "${context.packageName}.fileprovider") {
            val path = uri.path
            if (path != null) {
                val internalPrefix = "/internal_notes"
                val externalPrefix = "/external_documents"
                val file = when {
                    path.startsWith(internalPrefix) -> {
                        val rel = path.removePrefix(internalPrefix).trimStart('/')
                        File(rootNotesDir, rel)
                    }
                    path.startsWith(externalPrefix) -> {
                        val rel = path.removePrefix(externalPrefix).trimStart('/')
                        File(rootNotesDir, rel)
                    }
                    else -> null
                }
                if (file != null && file.exists() && file.canonicalPath.startsWith(rootPath)) {
                    return file
                }
            }
        }

        // 3. Storage Access Framework (SAF) / DocumentsProvider (e.g. com.android.externalstorage.documents)
        if (uri.scheme == "content") {
            try {
                val authority = uri.authority.orEmpty()
                if (authority.contains("externalstorage")) {
                    val docId = try {
                        android.provider.DocumentsContract.getDocumentId(uri)
                    } catch (_: Exception) {
                        uri.lastPathSegment
                    }
                    if (docId != null && docId.startsWith("primary:")) {
                        val relPath = docId.removePrefix("primary:")
                        val file = File(android.os.Environment.getExternalStorageDirectory(), relPath)
                        if (file.exists() && file.canonicalPath.startsWith(rootPath)) {
                            return file
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resolve SAF document id for $uri", e)
            }

            // 4. MediaStore queries (DATA column)
            try {
                val projection = arrayOf(android.provider.MediaStore.MediaColumns.DATA)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val dataIdx = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                        if (dataIdx != -1) {
                            val dataPath = cursor.getString(dataIdx)
                            if (!dataPath.isNullOrEmpty()) {
                                val file = File(dataPath)
                                if (file.exists() && file.canonicalPath.startsWith(rootPath)) {
                                    return file
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            // 5. Decoded URI path matching (third-party file manager providers containing filesystem path)
            try {
                val decoded = java.net.URLDecoder.decode(uri.toString(), "UTF-8")
                val idx = decoded.indexOf(rootNotesDir.absolutePath)
                if (idx != -1) {
                    val pathPart = decoded.substring(idx)
                    val file = File(pathPart)
                    if (file.exists() && file.canonicalPath.startsWith(rootPath)) {
                        return file
                    }
                }
            } catch (_: Exception) {}

            // 6. Content matching fallback for opaque file manager URIs (Google Files, Solid Explorer, etc.)
            try {
                val displayName = getDisplayName(context, uri)
                val fileSize = getFileSize(context, uri)
                if (!displayName.isNullOrBlank() && rootNotesDir.exists()) {
                    val candidates = rootNotesDir.walkTopDown()
                        .filter { it.isFile && it.name.equals(displayName, ignoreCase = true) }
                        .toList()

                    for (candidate in candidates) {
                        if (fileSize > 0L && candidate.length() != fileSize) {
                            continue
                        }
                        if (isContentIdenticalPrefix(context, uri, candidate)) {
                            Log.i(TAG, "Opaque URI $uri matched note in notes dir: ${candidate.absolutePath}")
                            return candidate
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed content matching for URI $uri", e)
            }
        }

        return null
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
                val originalMtime = sourceFile.lastModified()
                sourceFile.copyTo(destFile, overwrite = true)
                if (originalMtime > 0L) {
                    destFile.setLastModified(originalMtime)
                }
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
            val originalMtime = getLastModified(context, uri)
            if (originalMtime != null && originalMtime > 0L) {
                destFile.setLastModified(originalMtime)
            }
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

    private fun getLastModified(context: Context, uri: Uri): Long? {
        if (uri.scheme == "file") {
            val path = uri.path
            if (path != null) {
                val f = File(path)
                if (f.exists() && f.lastModified() > 0L) return f.lastModified()
            }
        }

        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                        if (idx != -1 && !cursor.isNull(idx)) {
                            val lm = cursor.getLong(idx)
                            if (lm > 0L) return lm
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resolve last modified from ContentResolver", e)
            }
        }

        return null
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        if (uri.scheme == "file") {
            val path = uri.path ?: return 0L
            val f = File(path)
            if (f.exists()) return f.length()
        }
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.SIZE),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                            return cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resolve file size from ContentResolver", e)
            }
        }
        return 0L
    }

    private fun isContentIdenticalPrefix(context: Context, uri: Uri, candidate: File): Boolean {
        return try {
            val bufUri = ByteArray(1024)
            val bufFile = ByteArray(1024)
            val readUri = context.contentResolver.openInputStream(uri)?.use { it.read(bufUri) } ?: -1
            val readFile = candidate.inputStream().use { it.read(bufFile) }
            if (readUri > 0 && readUri == readFile) {
                bufUri.sliceArray(0 until readUri).contentEquals(bufFile.sliceArray(0 until readFile))
            } else {
                readUri == readFile
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not compare content prefix for $candidate", e)
            false
        }
    }
}
