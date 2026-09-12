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
                val trashFileName = "${timestamp}_${file.name}"
                val targetTrashFile = File(trashDir, trashFileName)
                val srcAssociated = findAssociatedFiles(file)

                if (file.renameTo(targetTrashFile)) {
                    manifest.put(trashFileName, file.absolutePath)
                    onRemoveOpenedNoteHistory(file.absolutePath)
                    movedCount++
                    trashFileNames.add(trashFileName)

                    // Move all associated autosave and backup files to Trash
                    for (assoc in srcAssociated) {
                        if (assoc.exists()) {
                            val autoTrashName = "${timestamp}_${assoc.name}"
                            val autoTrashFile = File(trashDir, autoTrashName)
                            if (assoc.renameTo(autoTrashFile)) {
                                manifest.put(autoTrashName, assoc.absolutePath)
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
            val trashFolderName = "${timestamp}_${folder.name}"
            val targetTrashFolder = File(trashDir, trashFolderName)

            if (folder.renameTo(targetTrashFolder)) {
                manifest.put(trashFolderName, folder.absolutePath)
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
                val originalPath = manifest.optString(name).takeIf { it.isNotBlank() }
                val destFile = if (!originalPath.isNullOrBlank()) {
                    File(originalPath)
                } else {
                    File(notesDirectoryProvider(), name.substringAfter("_"))
                }
                destFile.parentFile?.mkdirs()
                if (trashFile.renameTo(destFile)) {
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

            val originalPath = manifest.optString(trashFolderName).takeIf { it.isNotBlank() }
            val targetTrashFolder = File(trashDir, trashFolderName)
            if (!targetTrashFolder.exists()) error("Trash folder does not exist")

            val destDir = if (!originalPath.isNullOrBlank()) {
                File(originalPath)
            } else {
                File(notesDirectoryProvider(), trashFolderName.substringAfter("_"))
            }

            destDir.parentFile?.mkdirs()
            if (targetTrashFolder.renameTo(destDir)) {
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

            val originalPath = manifest.optString(note.file.name).takeIf { it.isNotBlank() }
            val destFile = if (!originalPath.isNullOrBlank()) {
                File(originalPath)
            } else {
                File(notesDirectoryProvider(), note.title)
            }

            destFile.parentFile?.mkdirs()
            if (note.file.renameTo(destFile)) {
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
                val originalPath = manifest.optString(file.name).takeIf { it.isNotBlank() }
                val destFile = if (!originalPath.isNullOrBlank()) {
                    File(originalPath)
                } else {
                    File(notesDirectoryProvider(), title)
                }

                destFile.parentFile?.mkdirs()
                if (file.renameTo(destFile)) {
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
