package dev.ilamparithi.aournalpp.data

import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.utils.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Receipt returned after moving notes or items into the Trash bin.
 */
data class TrashReceipt(
    val movedCount: Int,
    val trashFileNames: List<String>
)

/**
 * Dedicated repository for managing the application's Trash bin (.Trash),
 * handling item lifecycle, manifest tracking (.trash_manifest.json), restoration,
 * and permanent deletion.
 */
class TrashRepository(
    private val notesDirectoryProvider: () -> File,
    private val findAssociatedFiles: (File) -> List<File> = { emptyList() },
    private val onRemoveOpenedNoteHistory: (String) -> Unit = {},
    private val onInvalidateCaches: () -> Unit = {},
    private val isOpenableFile: (File) -> Boolean = { true }
) {
    companion object {
        const val TRASH_DIR_NAME = ".Trash"
        const val TRASH_MANIFEST_FILE = ".trash_manifest.json"
    }

    fun getTrashDirectory(): File = File(notesDirectoryProvider(), TRASH_DIR_NAME).apply {
        if (!exists()) mkdirs()
    }

    private fun readManifest(manifestFile: File): JSONObject {
        return if (manifestFile.exists()) {
            try {
                JSONObject(manifestFile.readText())
            } catch (_: Exception) {
                JSONObject()
            }
        } else {
            JSONObject()
        }
    }

    private fun extractManifestEntry(manifest: JSONObject, key: String): Pair<String?, Long?> {
        if (!manifest.has(key)) return Pair(null, null)
        val obj = manifest.optJSONObject(key)
        if (obj != null) {
            val path = obj.optString("path").takeIf { it.isNotBlank() }
            val mtime = obj.optLong("lastModified", 0L).takeIf { it > 0L }
            return Pair(path, mtime)
        }
        val plainStr = manifest.optString(key).takeIf { it.isNotBlank() }
        return Pair(plainStr, null)
    }

    suspend fun moveToTrash(notes: List<NoteDocument>): Result<TrashReceipt> = withContext(Dispatchers.IO) {
        runCatching {
            val trashDir = getTrashDirectory()
            val manifestFile = File(trashDir, TRASH_MANIFEST_FILE)
            val manifest = readManifest(manifestFile)

            var movedCount = 0
            val timestamp = System.currentTimeMillis()
            val trashFileNames = mutableListOf<String>()

            for ((file) in notes) {
                if (!file.exists()) continue
                val originalMtime = file.lastModified()
                val trashFileName = "${timestamp}_${file.name}"
                val targetTrashFile = File(trashDir, trashFileName)
                val srcAssociated = findAssociatedFiles(file)

                if (file.renameTo(targetTrashFile)) {
                    val metaObj = JSONObject().apply {
                        put("path", file.absolutePath)
                        put("lastModified", originalMtime)
                    }
                    manifest.put(trashFileName, metaObj)
                    onRemoveOpenedNoteHistory(file.absolutePath)
                    movedCount++
                    trashFileNames.add(trashFileName)

                    // Move all associated autosave and backup files to Trash
                    for (assoc in srcAssociated) {
                        if (assoc.exists()) {
                            val assocMtime = assoc.lastModified()
                            val autoTrashName = "${timestamp}_${assoc.name}"
                            val autoTrashFile = File(trashDir, autoTrashName)
                            if (assoc.renameTo(autoTrashFile)) {
                                val assocObj = JSONObject().apply {
                                    put("path", assoc.absolutePath)
                                    put("lastModified", assocMtime)
                                }
                                manifest.put(autoTrashName, assocObj)
                                trashFileNames.add(autoTrashName)
                            }
                        }
                    }
                }
            }

            manifestFile.writeText(manifest.toString(2))
            TrashReceipt(movedCount, trashFileNames).also { onInvalidateCaches() }
        }
    }

    suspend fun moveFolderToTrash(folder: File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val trashDir = getTrashDirectory()
            val manifestFile = File(trashDir, TRASH_MANIFEST_FILE)
            val manifest = readManifest(manifestFile)

            val timestamp = System.currentTimeMillis()
            val folderMtime = folder.lastModified()
            val trashFolderName = "${timestamp}_${folder.name}"
            val targetTrashFolder = File(trashDir, trashFolderName)

            if (folder.renameTo(targetTrashFolder)) {
                val folderObj = JSONObject().apply {
                    put("path", folder.absolutePath)
                    put("lastModified", folderMtime)
                }
                manifest.put(trashFolderName, folderObj)
                manifestFile.writeText(manifest.toString(2))
                onInvalidateCaches()
                trashFolderName
            } else {
                error("Failed to move folder to trash")
            }
        }
    }

    suspend fun restoreTrashItems(trashFileNames: List<String>): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val trashDir = getTrashDirectory()
            val manifestFile = File(trashDir, TRASH_MANIFEST_FILE)
            val manifest = readManifest(manifestFile)

            var count = 0
            for (name in trashFileNames) {
                val trashFile = File(trashDir, name)
                if (!trashFile.exists()) continue
                val (originalPath, savedMtime) = extractManifestEntry(manifest, name)
                val destFile = if (!originalPath.isNullOrBlank()) {
                    File(originalPath)
                } else {
                    File(notesDirectoryProvider(), name.substringAfter("_"))
                }
                val mtimeToRestore = savedMtime ?: trashFile.lastModified()
                destFile.parentFile?.mkdirs()
                if (trashFile.renameTo(destFile)) {
                    if (mtimeToRestore > 0L) {
                        try { destFile.setLastModified(mtimeToRestore) } catch (_: Exception) {}
                    }
                    manifest.remove(name)
                    count++
                }
            }
            manifestFile.writeText(manifest.toString(2))
            count.also { onInvalidateCaches() }
        }
    }

    suspend fun restoreFolderFromTrash(trashFolderName: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val trashDir = getTrashDirectory()
            val manifestFile = File(trashDir, TRASH_MANIFEST_FILE)
            val manifest = readManifest(manifestFile)

            val (originalPath, savedMtime) = extractManifestEntry(manifest, trashFolderName)
            val targetTrashFolder = File(trashDir, trashFolderName)
            if (!targetTrashFolder.exists()) error("Trash folder does not exist")

            val destDir = if (!originalPath.isNullOrBlank()) {
                File(originalPath)
            } else {
                File(notesDirectoryProvider(), trashFolderName.substringAfter("_"))
            }
            val mtimeToRestore = savedMtime ?: targetTrashFolder.lastModified()

            destDir.parentFile?.mkdirs()
            if (targetTrashFolder.renameTo(destDir)) {
                if (mtimeToRestore > 0L) {
                    try { destDir.setLastModified(mtimeToRestore) } catch (_: Exception) {}
                }
                manifest.remove(trashFolderName)
                manifestFile.writeText(manifest.toString(2))
                onInvalidateCaches()
                destDir
            } else {
                error("Failed to restore folder from trash")
            }
        }
    }

    suspend fun scanTrash(): List<NoteDocument> = withContext(Dispatchers.IO) {
        val trashDir = getTrashDirectory()
        val allFiles = trashDir.listFiles() ?: return@withContext emptyList()
        allFiles.filter { it.isFile && isOpenableFile(it) }
            .map { file ->
                NoteDocument(
                    file = file,
                    title = file.name.substringAfter("_"),
                    path = file.absolutePath,
                    lastModifiedMs = file.lastModified(),
                    sizeBytes = file.length(),
                    lastModifiedFormatted = FormatUtils.formatDateTimeMedium(file.lastModified()),
                    sizeFormatted = FormatUtils.formatFileSize(file.length()),
                    folder = "Trash"
                )
            }.sortedByDescending { it.lastModifiedMs }
    }

    suspend fun restoreFromTrash(note: NoteDocument): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val trashDir = getTrashDirectory()
            val manifestFile = File(trashDir, TRASH_MANIFEST_FILE)
            val manifest = readManifest(manifestFile)

            val (originalPath, savedMtime) = extractManifestEntry(manifest, note.file.name)
            val destFile = if (!originalPath.isNullOrBlank()) {
                File(originalPath)
            } else {
                File(notesDirectoryProvider(), note.title)
            }
            val mtimeToRestore = savedMtime ?: note.file.lastModified()

            destFile.parentFile?.mkdirs()
            if (note.file.renameTo(destFile)) {
                if (mtimeToRestore > 0L) {
                    try { destFile.setLastModified(mtimeToRestore) } catch (_: Exception) {}
                }
                manifest.remove(note.file.name)
                manifestFile.writeText(manifest.toString(2))
                onInvalidateCaches()
                destFile
            } else {
                error("Failed to restore ${note.title}")
            }
        }
    }

    suspend fun restoreMultipleFromTrash(notes: List<NoteDocument>): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val trashDir = getTrashDirectory()
            val manifestFile = File(trashDir, TRASH_MANIFEST_FILE)
            val manifest = readManifest(manifestFile)

            var count = 0
            for ((file, title) in notes) {
                val (originalPath, savedMtime) = extractManifestEntry(manifest, file.name)
                val destFile = if (!originalPath.isNullOrBlank()) {
                    File(originalPath)
                } else {
                    File(notesDirectoryProvider(), title)
                }
                val mtimeToRestore = savedMtime ?: file.lastModified()

                destFile.parentFile?.mkdirs()
                if (file.renameTo(destFile)) {
                    if (mtimeToRestore > 0L) {
                        try { destFile.setLastModified(mtimeToRestore) } catch (_: Exception) {}
                    }
                    manifest.remove(file.name)
                    count++
                }
            }

            manifestFile.writeText(manifest.toString(2))
            count.also { onInvalidateCaches() }
        }
    }

    suspend fun deletePermanently(notes: List<NoteDocument>): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val trashDir = getTrashDirectory()
            val manifestFile = File(trashDir, TRASH_MANIFEST_FILE)
            val manifest = readManifest(manifestFile)

            var count = 0
            for ((file) in notes) {
                if (file.deleteRecursively()) {
                    manifest.remove(file.name)
                    count++
                }
            }

            manifestFile.writeText(manifest.toString(2))
            count.also { onInvalidateCaches() }
        }
    }

    suspend fun emptyTrash(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val trashDir = getTrashDirectory()
            trashDir.listFiles()?.forEach { it.deleteRecursively() }
            onInvalidateCaches()
        }
    }
}
