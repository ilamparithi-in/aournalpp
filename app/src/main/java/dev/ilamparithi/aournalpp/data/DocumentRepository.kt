package dev.ilamparithi.aournalpp.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dev.ilamparithi.aournalpp.model.AutosaveInfo
import dev.ilamparithi.aournalpp.model.FolderItem
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentRepository(private val context: Context) {

    companion object {
        val SUPPORTED_EXTENSIONS = setOf("xopp", "xoj", "pdf")
        private const val FOLDER_META_FILE = ".folder.json"
        private const val TRASH_DIR_NAME = ".Trash"
        private const val TRASH_MANIFEST_FILE = ".trash_manifest.json"
    }

    private val env = LinuxEnvironment(context)

    fun getLinuxEnvironment(): LinuxEnvironment = env

    fun getRootNotesDirectory(): File = env.getNotesDirectory()

    fun getTrashDirectory(): File = File(env.getNotesDirectory(), TRASH_DIR_NAME).apply {
        if (!exists()) mkdirs()
    }

    private fun isOpenableFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return SUPPORTED_EXTENSIONS.contains(ext)
    }

    fun scanDirectory(
        targetDir: File,
        query: String = "",
        showHidden: Boolean = false
    ): Pair<List<FolderItem>, List<NoteDocument>> {
        env.ensureDirectoryTree()
        if (!targetDir.exists()) targetDir.mkdirs()

        val allFiles = targetDir.listFiles() ?: return Pair(emptyList(), emptyList())
        val dateFormat = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())

        // 1. Scan Subfolders
        val folderItems = mutableListOf<FolderItem>()
        val directories = allFiles.filter { it.isDirectory && it.name != TRASH_DIR_NAME }

        for (dir in directories) {
            val isHidden = dir.name.startsWith(".")
            if (isHidden && !showHidden) continue

            if (query.isNotBlank() && !dir.name.contains(query.trim(), ignoreCase = true)) {
                // If query is present and folder name doesn't match, check if any inner notes match
            }

            val metaColor = readFolderColor(dir)
            val itemCount = dir.listFiles()?.count { file ->
                file.isFile && isOpenableFile(file) && !file.name.startsWith(".")
            } ?: 0

            folderItems.add(
                FolderItem(
                    file = dir,
                    name = dir.name,
                    colorHex = metaColor,
                    itemCount = itemCount,
                    lastModifiedMs = dir.lastModified(),
                    isHidden = isHidden
                )
            )
        }

        // 2. Scan Note Documents in targetDir
        val seenMainPaths = mutableSetOf<String>()
        val resultNotes = mutableListOf<NoteDocument>()
        val matchedAutosavePaths = mutableSetOf<String>()

        // 2a. Gather normal, non-hidden openable files (.xopp, .xoj, .pdf)
        val mainFiles = allFiles.filter { file ->
            file.isFile &&
            isOpenableFile(file) &&
            !file.name.startsWith(".") &&
            !file.name.endsWith("~") &&
            !file.name.contains(".autosave.", ignoreCase = true)
        }

        for (file in mainFiles) {
            val canonical = try { file.canonicalPath } catch (e: Exception) { file.absolutePath }
            if (!seenMainPaths.add(canonical)) continue

            if (query.isNotBlank() && !file.name.contains(query.trim(), ignoreCase = true)) {
                continue
            }

            val autosaveCandidate = findMatchingAutosave(targetDir, file)
            val autosaveInfo = autosaveCandidate?.let { autoFile ->
                matchedAutosavePaths.add(try { autoFile.canonicalPath } catch (e: Exception) { autoFile.absolutePath })
                AutosaveInfo(
                    autosaveFile = autoFile,
                    mainFile = file,
                    mainLastModifiedMs = file.lastModified(),
                    autosaveLastModifiedMs = autoFile.lastModified(),
                    mainSizeBytes = file.length(),
                    autosaveSizeBytes = autoFile.length()
                )
            }

            val sizeKb = (file.length() + 1023) / 1024
            resultNotes.add(
                NoteDocument(
                    file = file,
                    title = file.nameWithoutExtension,
                    path = file.absolutePath,
                    lastModifiedMs = file.lastModified(),
                    sizeBytes = file.length(),
                    lastModifiedFormatted = dateFormat.format(Date(file.lastModified())),
                    sizeFormatted = "${sizeKb} KB",
                    autosaveInfo = autosaveInfo,
                    isHidden = false,
                    folder = targetDir.name
                )
            )
        }

        // 2b. If showHidden is true, include hidden/backup files that are strictly openable by Xournal++
        if (showHidden) {
            val hiddenOrBackupFiles = allFiles.filter { file ->
                file.isFile &&
                (file.name.startsWith(".") || file.name.endsWith("~") || file.name.contains(".autosave.", ignoreCase = true)) &&
                isOpenableCandidate(file)
            }

            for (file in hiddenOrBackupFiles) {
                val canonical = try { file.canonicalPath } catch (e: Exception) { file.absolutePath }
                if (matchedAutosavePaths.contains(canonical) || seenMainPaths.contains(canonical)) continue

                if (query.isNotBlank() && !file.name.contains(query.trim(), ignoreCase = true)) {
                    continue
                }

                val sizeKb = (file.length() + 1023) / 1024
                resultNotes.add(
                    NoteDocument(
                        file = file,
                        title = file.name,
                        path = file.absolutePath,
                        lastModifiedMs = file.lastModified(),
                        sizeBytes = file.length(),
                        lastModifiedFormatted = dateFormat.format(Date(file.lastModified())),
                        sizeFormatted = "${sizeKb} KB",
                        autosaveInfo = null,
                        isHidden = true,
                        folder = targetDir.name
                    )
                )
            }
        }

        return Pair(
            folderItems.sortedBy { it.name.lowercase() },
            resultNotes.sortedByDescending { it.lastModifiedMs }
        )
    }

    private fun isOpenableCandidate(file: File): Boolean {
        val lower = file.name.lowercase()
        return lower.endsWith(".xopp") || lower.endsWith(".xoj") || lower.endsWith(".pdf") ||
               lower.endsWith(".xopp~") || lower.endsWith(".xoj~") || lower.endsWith(".pdf~") ||
               lower.contains(".autosave.xopp") || lower.contains(".autosave.xoj")
    }

    private fun findMatchingAutosave(parentDir: File, mainFile: File): File? {
        val base = mainFile.nameWithoutExtension
        val ext = mainFile.extension
        val candidates = listOf(
            File(parentDir, ".${mainFile.name}.autosave.$ext"),
            File(parentDir, ".$base.autosave.$ext"),
            File(parentDir, ".${mainFile.name}~"),
            File(parentDir, "${mainFile.name}~"),
            File(parentDir, ".$base.$ext~")
        )

        return candidates.firstOrNull { it.exists() && it.isFile && it.length() > 0 }
    }

    // Folder Management
    fun createFolder(parentDir: File, name: String, colorHex: String? = null): Result<File> {
        val cleanName = name.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_")
        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Folder name cannot be blank"))

        val newDir = File(parentDir, cleanName)
        if (newDir.exists()) return Result.failure(IllegalArgumentException("Folder '$cleanName' already exists"))

        if (!newDir.mkdirs()) {
            return Result.failure(IllegalStateException("Failed to create folder '$cleanName'"))
        }

        colorHex?.let { writeFolderColor(newDir, it) }
        return Result.success(newDir)
    }

    fun setFolderColor(folderDir: File, colorHex: String): Result<Unit> {
        return writeFolderColor(folderDir, colorHex)
    }

    private fun readFolderColor(folderDir: File): String? {
        val metaFile = File(folderDir, FOLDER_META_FILE)
        if (!metaFile.exists()) return null
        return try {
            val json = JSONObject(metaFile.readText())
            json.optString("color").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun writeFolderColor(folderDir: File, colorHex: String): Result<Unit> = runCatching {
        val metaFile = File(folderDir, FOLDER_META_FILE)
        val json = if (metaFile.exists()) {
            try { JSONObject(metaFile.readText()) } catch (e: Exception) { JSONObject() }
        } else {
            JSONObject()
        }
        json.put("color", colorHex)
        metaFile.writeText(json.toString(2))
    }

    fun getAllFolders(root: File = getRootNotesDirectory()): List<FolderItem> {
        val list = mutableListOf<FolderItem>()
        fun recurse(dir: File) {
            val subdirs = dir.listFiles { f -> f.isDirectory && f.name != TRASH_DIR_NAME && !f.name.startsWith(".") } ?: return
            for (sub in subdirs) {
                val metaColor = readFolderColor(sub)
                val count = sub.listFiles { f -> f.isFile && isOpenableFile(f) && !f.name.startsWith(".") }?.size ?: 0
                list.add(
                    FolderItem(
                        file = sub,
                        name = sub.name,
                        colorHex = metaColor,
                        itemCount = count,
                        lastModifiedMs = sub.lastModified()
                    )
                )
                recurse(sub)
            }
        }
        recurse(root)
        return list.sortedBy { it.name.lowercase() }
    }

    suspend fun moveNotesToFolder(notes: List<NoteDocument>, destFolder: File): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            if (!destFolder.exists()) destFolder.mkdirs()
            var movedCount = 0

            for (note in notes) {
                if (!note.file.exists()) continue
                var destFile = File(destFolder, note.file.name)
                if (destFile.exists() && destFile.canonicalPath != note.file.canonicalPath) {
                    val nameWithoutExt = note.file.nameWithoutExtension
                    val ext = note.file.extension
                    var counter = 1
                    while (destFile.exists()) {
                        destFile = File(destFolder, "${nameWithoutExt}_$counter.$ext")
                        counter++
                    }
                }

                if (note.file.renameTo(destFile)) {
                    movedCount++

                    // Move associated autosave if exists
                    note.autosaveInfo?.autosaveFile?.let { autoFile ->
                        if (autoFile.exists()) {
                            val autoExt = autoFile.extension
                            val destAuto = File(destFolder, ".${destFile.nameWithoutExtension}.autosave.$autoExt")
                            autoFile.renameTo(destAuto)
                        }
                    }
                }
            }
            movedCount
        }
    }

    // Trashcan Operations
    suspend fun moveToTrash(notes: List<NoteDocument>): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val trashDir = getTrashDirectory()
            val manifestFile = File(trashDir, TRASH_MANIFEST_FILE)
            val manifest = if (manifestFile.exists()) {
                try { JSONObject(manifestFile.readText()) } catch (e: Exception) { JSONObject() }
            } else {
                JSONObject()
            }

            var movedCount = 0
            val timestamp = System.currentTimeMillis()

            for (note in notes) {
                if (!note.file.exists()) continue
                val trashFileName = "${timestamp}_${note.file.name}"
                val targetTrashFile = File(trashDir, trashFileName)

                if (note.file.renameTo(targetTrashFile)) {
                    manifest.put(trashFileName, note.file.absolutePath)
                    movedCount++

                    // Move associated autosave if present
                    note.autosaveInfo?.autosaveFile?.let { autoFile ->
                        if (autoFile.exists()) {
                            val autoTrashFile = File(trashDir, "${timestamp}_${autoFile.name}")
                            autoFile.renameTo(autoTrashFile)
                            manifest.put(autoTrashFile.name, autoFile.absolutePath)
                        }
                    }
                }
            }

            manifestFile.writeText(manifest.toString(2))
            movedCount
        }
    }

    suspend fun moveFolderToTrash(folder: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val trashDir = getTrashDirectory()
            val manifestFile = File(trashDir, TRASH_MANIFEST_FILE)
            val manifest = if (manifestFile.exists()) {
                try { JSONObject(manifestFile.readText()) } catch (e: Exception) { JSONObject() }
            } else {
                JSONObject()
            }

            val timestamp = System.currentTimeMillis()
            val trashFolderName = "${timestamp}_${folder.name}"
            val targetTrashFolder = File(trashDir, trashFolderName)

            if (folder.renameTo(targetTrashFolder)) {
                manifest.put(trashFolderName, folder.absolutePath)
                manifestFile.writeText(manifest.toString(2))
            } else {
                error("Failed to move folder to trash")
            }
        }
    }

    fun scanTrash(): List<NoteDocument> {
        val trashDir = getTrashDirectory()
        val allFiles = trashDir.listFiles() ?: return emptyList()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())

        return allFiles.filter { it.isFile && isOpenableFile(it) }
            .map { file ->
                val sizeKb = (file.length() + 1023) / 1024
                NoteDocument(
                    file = file,
                    title = file.name.substringAfter("_"),
                    path = file.absolutePath,
                    lastModifiedMs = file.lastModified(),
                    sizeBytes = file.length(),
                    lastModifiedFormatted = dateFormat.format(Date(file.lastModified())),
                    sizeFormatted = "${sizeKb} KB",
                    folder = "Trash"
                )
            }.sortedByDescending { it.lastModifiedMs }
    }

    suspend fun restoreFromTrash(note: NoteDocument): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val trashDir = getTrashDirectory()
            val manifestFile = File(trashDir, TRASH_MANIFEST_FILE)
            val manifest = if (manifestFile.exists()) {
                try { JSONObject(manifestFile.readText()) } catch (e: Exception) { JSONObject() }
            } else {
                JSONObject()
            }

            val originalPath = manifest.optString(note.file.name).takeIf { it.isNotBlank() }
            val destFile = if (!originalPath.isNullOrBlank()) {
                File(originalPath)
            } else {
                File(env.getNotesDirectory(), note.title)
            }

            destFile.parentFile?.mkdirs()
            if (note.file.renameTo(destFile)) {
                manifest.remove(note.file.name)
                manifestFile.writeText(manifest.toString(2))
                destFile
            } else {
                error("Failed to restore ${note.title}")
            }
        }
    }

    suspend fun emptyTrash(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val trashDir = getTrashDirectory()
            trashDir.listFiles()?.forEach { it.deleteRecursively() }
            Unit
        }
    }

    // CRUD for individual note
    suspend fun renameNote(doc: NoteDocument, newTitle: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanTitle = newTitle.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_")
            if (cleanTitle.isBlank()) error("Document name cannot be blank")

            val ext = doc.file.extension
            val targetName = if (cleanTitle.endsWith(".$ext", ignoreCase = true)) {
                cleanTitle
            } else {
                "$cleanTitle.$ext"
            }

            val parentDir = doc.file.parentFile ?: error("Parent directory not found")
            val targetFile = File(parentDir, targetName)

            if (targetFile.exists() && targetFile.canonicalPath != doc.file.canonicalPath) {
                error("A file named '$targetName' already exists")
            }

            if (!doc.file.renameTo(targetFile)) {
                error("Failed to rename file to '$targetName'")
            }

            doc.autosaveInfo?.autosaveFile?.let { autoFile ->
                if (autoFile.exists()) {
                    val autoExt = autoFile.extension
                    val targetAuto = File(parentDir, ".${targetFile.nameWithoutExtension}.autosave.$autoExt")
                    autoFile.renameTo(targetAuto)
                }
            }

            targetFile
        }
    }

    suspend fun duplicateNote(doc: NoteDocument): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val parentDir = doc.file.parentFile ?: error("Parent directory not found")
            val baseName = doc.file.nameWithoutExtension
            val ext = doc.file.extension

            var counter = 1
            var candidate = File(parentDir, "$baseName (Copy).$ext")
            while (candidate.exists()) {
                counter++
                candidate = File(parentDir, "$baseName (Copy $counter).$ext")
            }

            doc.file.copyTo(candidate, overwrite = false)
            candidate
        }
    }

    suspend fun deleteNote(doc: NoteDocument): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            moveToTrash(listOf(doc)).getOrThrow()
            Unit
        }
    }

    // Sharing Actions
    fun shareNoteAsXopp(context: Context, doc: NoteDocument) {
        shareMultipleNotesAsXopp(context, listOf(doc))
    }

    fun shareMultipleNotesAsXopp(context: Context, docs: List<NoteDocument>) {
        if (docs.isEmpty()) return

        if (docs.size == 1) {
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                docs.first().file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (docs.first().file.extension.equals("pdf", ignoreCase = true)) "application/pdf" else "application/x-xopp"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, docs.first().title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Note"))
        } else {
            val uris = ArrayList<Uri>()
            for (doc in docs) {
                uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", doc.file))
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${docs.size} Notes"))
        }
    }

    suspend fun shareNoteAsPdf(
        context: Context,
        doc: NoteDocument,
        pdfExportManager: PdfExportManager
    ): Result<Unit> {
        return shareMultipleNotesAsPdf(context, listOf(doc), pdfExportManager)
    }

    suspend fun shareMultipleNotesAsPdf(
        context: Context,
        docs: List<NoteDocument>,
        pdfExportManager: PdfExportManager
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (docs.isEmpty()) return@runCatching

            val pdfUris = ArrayList<Uri>()
            for (doc in docs) {
                val pdfFile = if (doc.file.extension.equals("pdf", ignoreCase = true)) {
                    doc.file
                } else {
                    pdfExportManager.renderPdfForSharing(context, doc.file).getOrThrow()
                }
                pdfUris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile))
            }

            withContext(Dispatchers.Main) {
                if (pdfUris.size == 1) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, pdfUris.first())
                        putExtra(Intent.EXTRA_SUBJECT, "${docs.first().title}.pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share PDF"))
                } else {
                    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "application/pdf"
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, pdfUris)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share ${docs.size} PDFs"))
                }
            }
            Unit
        }
    }

    fun replaceWithAutosave(note: NoteDocument): File {
        val autoInfo = note.autosaveInfo ?: return note.file
        try {
            autoInfo.autosaveFile.copyTo(note.file, overwrite = true)
            autoInfo.autosaveFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return note.file
    }

    fun keepBoth(note: NoteDocument): File {
        val autoInfo = note.autosaveInfo ?: return note.file
        try {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = sdf.format(Date(autoInfo.autosaveLastModifiedMs))
            val backupFile = File(note.file.parentFile, "${note.file.nameWithoutExtension}_autosave_$timestamp.${note.file.extension}")
            autoInfo.autosaveFile.copyTo(backupFile, overwrite = true)
            autoInfo.autosaveFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return note.file
    }

    fun discardAutosave(note: NoteDocument): File {
        val autoInfo = note.autosaveInfo ?: return note.file
        try {
            if (autoInfo.autosaveFile.exists()) {
                autoInfo.autosaveFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return note.file
    }

    fun openEmergencyRecoverySession(recoveryFile: File): File {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val defaultName = "Recovered_Session_${sdf.format(Date(recoveryFile.lastModified()))}.xopp"
        return saveEmergencyRecoveryToNotes(recoveryFile, defaultName)
    }

    fun saveEmergencyRecoveryToNotes(recoveryFile: File, userSpecifiedName: String): File {
        val notesDir = env.getNotesDirectory()
        if (!notesDir.exists()) notesDir.mkdirs()

        val cleanName = if (userSpecifiedName.endsWith(".xopp", ignoreCase = true)) {
            userSpecifiedName
        } else {
            "$userSpecifiedName.xopp"
        }

        val target = File(notesDir, cleanName)
        recoveryFile.copyTo(target, overwrite = true)
        recoveryFile.delete()
        env.clearQuarantinedEmergencySave()
        return target
    }

    fun discardEmergencyRecovery() {
        env.clearQuarantinedEmergencySave()
    }
}
