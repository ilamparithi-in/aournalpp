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
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPOutputStream

class DocumentRepository(private val context: Context) {

    companion object {
        val SUPPORTED_EXTENSIONS = setOf("xopp", "xoj", "pdf")
        private const val FOLDER_META_FILE = ".folder.json"
        private const val TRASH_DIR_NAME = ".Trash"
        private const val TRASH_MANIFEST_FILE = ".trash_manifest.json"
        const val EMERGENCY_SAVES_DEFAULT_COLOR = "#F44336"
        const val EMERGENCY_SAVES_DEFAULT_ICON = "emergency"
    }

    data class FolderMetaData(
        val colorHex: String? = null,
        val iconEmoji: String? = null,
        val iconType: String? = null
    )

    private val env = LinuxEnvironment(context)
    private val prefs = context.getSharedPreferences("aournal_doc_hub_prefs", Context.MODE_PRIVATE)

    fun getLinuxEnvironment(): LinuxEnvironment = env

    fun getRootNotesDirectory(): File = env.getNotesDirectory()

    fun getTrashDirectory(): File = File(env.getNotesDirectory(), TRASH_DIR_NAME).apply {
        if (!exists()) mkdirs()
    }

    private fun isOpenableFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return SUPPORTED_EXTENSIONS.contains(ext)
    }

    // Pinned Notes Persistence
    fun getPinnedNotePaths(): List<String> {
        val raw = prefs.getString("pref_pinned_notes_order_json", null) ?: return emptyList()
        return try {
            val array = org.json.JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val p = array.optString(i)
                if (p.isNotBlank()) list.add(p)
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isNotePinned(path: String): Boolean {
        val pinned = getPinnedNotePaths()
        return pinned.contains(path)
    }

    fun pinNote(path: String) {
        val current = getPinnedNotePaths().toMutableList()
        current.remove(path)
        current.add(0, path)
        savePinnedNotes(current)
    }

    fun unpinNote(path: String) {
        val current = getPinnedNotePaths().toMutableList()
        current.remove(path)
        savePinnedNotes(current)
    }

    fun togglePinNote(path: String): Boolean {
        val current = getPinnedNotePaths().toMutableList()
        val willPin = if (current.contains(path)) {
            current.remove(path)
            false
        } else {
            current.add(0, path)
            true
        }
        savePinnedNotes(current)
        return willPin
    }

    private fun savePinnedNotes(paths: List<String>) {
        val array = org.json.JSONArray()
        paths.forEach { array.put(it) }
        prefs.edit().putString("pref_pinned_notes_order_json", array.toString()).apply()
    }

    private fun canonicalOf(file: File): String =
        try { file.canonicalPath } catch (e: Exception) { file.absolutePath }

    /** Per-scan cache for the values every file in a scan shares. */
    private inner class ScanCache {
        val pinnedPaths: Set<String> by lazy { getPinnedNotePaths().toSet() }

        val openedTimestamps: Map<String, Long> by lazy {
            val raw = prefs.getString("pref_opened_notes_timestamps_json", null) ?: return@lazy emptyMap()
            try {
                val obj = org.json.JSONObject(raw)
                val map = HashMap<String, Long>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = obj.optLong(k)
                }
                map
            } catch (e: Exception) {
                emptyMap()
            }
        }

        val dateFormat: SimpleDateFormat by lazy {
            SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
        }

        private val rootCanonical: String by lazy { canonicalOf(getRootNotesDirectory()) }
        private val folderMetas = HashMap<String, FolderMetaData>()

        fun folderMeta(dir: File): FolderMetaData =
            folderMetas.getOrPut(dir.absolutePath) { readFolderMeta(dir) }

        fun isRoot(dir: File): Boolean = canonicalOf(dir) == rootCanonical
    }

    suspend fun scanDirectory(
        targetDir: File,
        query: String = "",
        showHidden: Boolean = false
    ): Pair<List<FolderItem>, List<NoteDocument>> = withContext(Dispatchers.IO) {
        if (!targetDir.exists()) targetDir.mkdirs()

        val allFiles = targetDir.listFiles() ?: return@withContext Pair(emptyList(), emptyList())
        val cache = ScanCache()
        val trimmedQuery = query.trim()

        // 1. Scan Subfolders
        val folderItems = mutableListOf<FolderItem>()
        val directories = allFiles.filter { it.isDirectory && it.name != TRASH_DIR_NAME }

        for (dir in directories) {
            val isHidden = dir.name.startsWith(".")
            if (isHidden && !showHidden) continue

            if (trimmedQuery.isNotEmpty() && !dir.name.contains(trimmedQuery, ignoreCase = true)) {
                continue
            }

            val meta = cache.folderMeta(dir)
            val isEmergency = isEmergencySavesFolder(dir)
            val itemCount = dir.listFiles()?.count { file ->
                file.isFile && isOpenableFile(file) && !file.name.startsWith(".")
            } ?: 0

            folderItems.add(
                FolderItem(
                    file = dir,
                    name = dir.name,
                    colorHex = meta.colorHex,
                    iconEmoji = meta.iconEmoji,
                    iconType = meta.iconType,
                    isEmergencyFolder = isEmergency,
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

        val isRoot = cache.isRoot(targetDir)
        val targetFolderMeta = if (!isRoot) cache.folderMeta(targetDir) else FolderMetaData()

        for (file in mainFiles) {
            if (!seenMainPaths.add(file.absolutePath)) continue

            if (trimmedQuery.isNotEmpty() && !file.name.contains(trimmedQuery, ignoreCase = true)) {
                continue
            }

            val autosaveCandidate = findMatchingAutosave(targetDir, file)
            val autosaveInfo = autosaveCandidate?.let { autoFile ->
                matchedAutosavePaths.add(autoFile.absolutePath)
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
                    lastModifiedFormatted = cache.dateFormat.format(Date(file.lastModified())),
                    sizeFormatted = "${sizeKb} KB",
                    autosaveInfo = autosaveInfo,
                    isHidden = false,
                    isPinned = cache.pinnedPaths.contains(file.absolutePath),
                    folder = if (isRoot) "Notes Home" else targetDir.name,
                    folderColorHex = targetFolderMeta.colorHex,
                    folderIconEmoji = targetFolderMeta.iconEmoji,
                    folderIconType = targetFolderMeta.iconType
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
                val path = file.absolutePath
                if (matchedAutosavePaths.contains(path) || seenMainPaths.contains(path)) continue

                if (trimmedQuery.isNotEmpty() && !file.name.contains(trimmedQuery, ignoreCase = true)) {
                    continue
                }

                val sizeKb = (file.length() + 1023) / 1024
                resultNotes.add(
                    NoteDocument(
                        file = file,
                        title = file.name,
                        path = path,
                        lastModifiedMs = file.lastModified(),
                        sizeBytes = file.length(),
                        lastModifiedFormatted = cache.dateFormat.format(Date(file.lastModified())),
                        sizeFormatted = "${sizeKb} KB",
                        autosaveInfo = null,
                        isHidden = true,
                        folder = targetDir.name,
                        folderColorHex = targetFolderMeta.colorHex,
                        folderIconEmoji = targetFolderMeta.iconEmoji,
                        folderIconType = targetFolderMeta.iconType
                    )
                )
            }
        }

        Pair(
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

    fun isEmergencySavesFolder(folderDir: File): Boolean {
        return folderDir.name.equals("Emergency Saves", ignoreCase = true) ||
               try { folderDir.canonicalPath == env.getEmergencySavesDirectory().canonicalPath } catch (e: Exception) { false }
    }

    // Folder Management
    fun createFolder(
        parentDir: File,
        name: String,
        colorHex: String? = null,
        iconEmoji: String? = null,
        iconType: String? = null
    ): Result<File> {
        val cleanName = name.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_")
        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Folder name cannot be blank"))

        val newDir = File(parentDir, cleanName)
        if (newDir.exists()) return Result.failure(IllegalArgumentException("Folder '$cleanName' already exists"))

        if (!newDir.mkdirs()) {
            return Result.failure(IllegalStateException("Failed to create folder '$cleanName'"))
        }

        if (colorHex != null || iconEmoji != null || iconType != null) {
            writeFolderMeta(newDir, colorHex, iconEmoji, iconType)
        }
        return Result.success(newDir)
    }

    fun setFolderColor(folderDir: File, colorHex: String): Result<Unit> {
        val meta = readFolderMeta(folderDir)
        return writeFolderMeta(folderDir, colorHex, meta.iconEmoji, meta.iconType)
    }

    fun setFolderEmoji(folderDir: File, emoji: String?): Result<Unit> {
        val meta = readFolderMeta(folderDir)
        return writeFolderMeta(folderDir, meta.colorHex, emoji, if (emoji == null) "folder" else null)
    }

    fun setFolderIcon(folderDir: File, iconType: String?): Result<Unit> {
        val meta = readFolderMeta(folderDir)
        return writeFolderMeta(folderDir, meta.colorHex, null, iconType)
    }

    fun updateFolderMeta(folderDir: File, colorHex: String?, iconEmoji: String?, iconType: String? = null): Result<Unit> {
        return writeFolderMeta(folderDir, colorHex, iconEmoji, iconType)
    }

    suspend fun renameFolder(folderDir: File, newFolderName: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanName = newFolderName.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_")
            if (cleanName.isBlank()) error("Folder name cannot be blank")

            if (folderDir.canonicalPath == getRootNotesDirectory().canonicalPath) {
                error("Cannot rename root Notes directory")
            }

            val parentDir = folderDir.parentFile ?: error("Parent directory not found")
            val targetDir = File(parentDir, cleanName)

            if (targetDir.exists() && targetDir.canonicalPath != folderDir.canonicalPath) {
                error("A folder named '$cleanName' already exists")
            }

            if (targetDir.canonicalPath == folderDir.canonicalPath) {
                return@runCatching folderDir
            }

            if (!folderDir.renameTo(targetDir)) {
                error("Failed to rename folder to '$cleanName'")
            }

            targetDir
        }
    }

    fun getFolderMeta(folderDir: File): FolderMetaData {
        return readFolderMeta(folderDir)
    }

    private fun readFolderMeta(folderDir: File): FolderMetaData {
        val isEmergency = isEmergencySavesFolder(folderDir)
        val metaFile = File(folderDir, FOLDER_META_FILE)
        if (!metaFile.exists()) {
            return if (isEmergency) {
                FolderMetaData(
                    colorHex = EMERGENCY_SAVES_DEFAULT_COLOR,
                    iconEmoji = null,
                    iconType = EMERGENCY_SAVES_DEFAULT_ICON
                )
            } else {
                FolderMetaData()
            }
        }
        return try {
            val json = JSONObject(metaFile.readText())
            val color = if (json.has("color")) {
                json.optString("color").takeIf { it.isNotBlank() }
            } else if (isEmergency) {
                EMERGENCY_SAVES_DEFAULT_COLOR
            } else {
                null
            }
            val emoji = if (json.has("emoji")) {
                json.optString("emoji").takeIf { it.isNotBlank() }
            } else {
                null
            }
            val icon = if (json.has("icon")) {
                json.optString("icon").takeIf { it.isNotBlank() }
            } else if (isEmergency && emoji == null) {
                EMERGENCY_SAVES_DEFAULT_ICON
            } else {
                null
            }
            FolderMetaData(color, emoji, icon)
        } catch (e: Exception) {
            if (isEmergency) {
                FolderMetaData(
                    colorHex = EMERGENCY_SAVES_DEFAULT_COLOR,
                    iconEmoji = null,
                    iconType = EMERGENCY_SAVES_DEFAULT_ICON
                )
            } else {
                FolderMetaData()
            }
        }
    }

    private fun writeFolderMeta(folderDir: File, colorHex: String?, iconEmoji: String?, iconType: String? = null): Result<Unit> = runCatching {
        val metaFile = File(folderDir, FOLDER_META_FILE)
        val json = if (metaFile.exists()) {
            try { JSONObject(metaFile.readText()) } catch (e: Exception) { JSONObject() }
        } else {
            JSONObject()
        }
        if (colorHex != null) {
            json.put("color", colorHex)
        } else {
            json.remove("color")
        }
        if (iconEmoji != null && iconEmoji.isNotBlank()) {
            json.put("emoji", iconEmoji.trim())
            json.remove("icon")
        } else {
            json.remove("emoji")
            if (iconType != null && iconType.isNotBlank()) {
                json.put("icon", iconType.trim())
            } else {
                json.remove("icon")
            }
        }
        metaFile.writeText(json.toString(2))
    }

    suspend fun getAllFolders(root: File = getRootNotesDirectory()): List<FolderItem> = withContext(Dispatchers.IO) {
        val cache = ScanCache()
        val list = mutableListOf<FolderItem>()
        fun recurse(dir: File) {
            val subdirs = dir.listFiles { f -> f.isDirectory && f.name != TRASH_DIR_NAME && !f.name.startsWith(".") } ?: return
            for (sub in subdirs) {
                val meta = cache.folderMeta(sub)
                val isEmergency = isEmergencySavesFolder(sub)
                val count = sub.listFiles { f -> f.isFile && isOpenableFile(f) && !f.name.startsWith(".") }?.size ?: 0
                list.add(
                    FolderItem(
                        file = sub,
                        name = sub.name,
                        colorHex = meta.colorHex,
                        iconEmoji = meta.iconEmoji,
                        iconType = meta.iconType,
                        isEmergencyFolder = isEmergency,
                        itemCount = count,
                        lastModifiedMs = sub.lastModified()
                    )
                )
                recurse(sub)
            }
        }
        recurse(root)
        list.sortedBy { it.name.lowercase() }
    }

    private fun findAssociatedAutosaveAndBackupFiles(mainFile: File): List<File> {
        val parentDir = mainFile.parentFile ?: return emptyList()
        val baseName = mainFile.nameWithoutExtension
        val fileName = mainFile.name
        val ext = mainFile.extension

        val candidates = parentDir.listFiles { file ->
            file.isFile && (
                file.name == ".$fileName.autosave.$ext" ||
                file.name == ".$baseName.autosave.$ext" ||
                file.name.startsWith(".$fileName.autosave.") ||
                file.name.startsWith(".$baseName.autosave.") ||
                file.name == "$fileName~" ||
                file.name == ".$fileName~" ||
                file.name == ".$baseName.$ext~"
            )
        } ?: emptyArray()

        return candidates.toList()
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

                val srcAssociated = findAssociatedAutosaveAndBackupFiles(note.file)

                if (note.file.renameTo(destFile)) {
                    movedCount++

                    // Move all associated autosave and backup files into destFolder as well
                    for (assoc in srcAssociated) {
                        if (assoc.exists()) {
                            val newAssocName = if (assoc.name.contains(note.file.name)) {
                                assoc.name.replace(note.file.name, destFile.name)
                            } else if (assoc.name.contains(note.file.nameWithoutExtension)) {
                                assoc.name.replace(note.file.nameWithoutExtension, destFile.nameWithoutExtension)
                            } else {
                                assoc.name
                            }
                            val destAssocFile = File(destFolder, newAssocName)
                            assoc.renameTo(destAssocFile)
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
                val srcAssociated = findAssociatedAutosaveAndBackupFiles(note.file)

                if (note.file.renameTo(targetTrashFile)) {
                    manifest.put(trashFileName, note.file.absolutePath)
                    removeOpenedNoteHistory(note.file.absolutePath)
                    movedCount++

                    // Move all associated autosave and backup files to Trash
                    for (assoc in srcAssociated) {
                        if (assoc.exists()) {
                            val autoTrashName = "${timestamp}_${assoc.name}"
                            val autoTrashFile = File(trashDir, autoTrashName)
                            if (assoc.renameTo(autoTrashFile)) {
                                manifest.put(autoTrashName, assoc.absolutePath)
                            }
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

    suspend fun scanTrash(): List<NoteDocument> = withContext(Dispatchers.IO) {
        val trashDir = getTrashDirectory()
        val allFiles = trashDir.listFiles() ?: return@withContext emptyList()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())

        allFiles.filter { it.isFile && isOpenableFile(it) }
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

            updateOpenedNotePath(doc.file.absolutePath, targetFile.absolutePath)

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


    /**
     * Creates a new blank Xournal++ note file at [targetFolder]/[name].xopp.
     *
     * The written content is a gzip-compressed XML document using fileversion="4" — the format
     * that has been stable since 2019 and is what xournalpp itself produces for a new blank note.
     * Because this app bundles a pinned xournalpp binary, format compatibility is always
     * guaranteed; xournalpp also migrates older fileversions forward, so this cannot regress.
     *
     * Filename conflicts are resolved by appending a counter suffix (_2, _3, …).
     *
     * @param name       The desired note name (with or without ".xopp" extension).
     * @param targetFolder  Directory in which to create the file. Created if absent.
     * @return [Result.success] wrapping the created [File], or [Result.failure] on I/O error.
     */
    suspend fun createBlankNote(
        name: String,
        targetFolder: File = getRootNotesDirectory()
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!targetFolder.exists()) targetFolder.mkdirs()

            // Sanitise the base name
            val rawBase = if (name.endsWith(".xopp", ignoreCase = true)) {
                name.substring(0, name.length - 5)
            } else {
                name
            }
            val cleanBase = rawBase.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_").ifBlank { "New Note" }

            // Resolve a non-conflicting filename (_2, _3, …)
            var candidate = File(targetFolder, "$cleanBase.xopp")
            var counter = 2
            while (candidate.exists()) {
                candidate = File(targetFolder, "${cleanBase}_$counter.xopp")
                counter++
            }

            // Write minimal valid Xournal++ v4 document (gzip-compressed XML)
            val xml = """
                <?xml version="1.0" standalone="no"?>
                <xournal creator="Xournal++ 1.2.x" fileversion="4">
                  <title>Xournal++ document - see https://github.com/xournalpp/xournalpp</title>
                  <page width="595.27559100" height="841.88976400">
                    <background type="solid" color="#ffffff" style="plain"/>
                    <layer/>
                  </page>
                </xournal>
            """.trimIndent()

            GZIPOutputStream(FileOutputStream(candidate)).use { gzip ->
                gzip.write(xml.toByteArray(Charsets.UTF_8))
            }

            Result.success(candidate)
        } catch (e: Exception) {
            Result.failure(e)
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

    fun saveAutosaveAsNote(
        autoInfo: dev.ilamparithi.aournalpp.model.AutosaveInfo,
        userSpecifiedName: String,
        targetFolder: File = getRootNotesDirectory()
    ): File {
        if (!targetFolder.exists()) targetFolder.mkdirs()

        val rawBase = if (userSpecifiedName.endsWith(".xopp", ignoreCase = true)) {
            userSpecifiedName.substring(0, userSpecifiedName.length - 5)
        } else {
            userSpecifiedName
        }
        val cleanBase = rawBase.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_")
        val effectiveName = if (cleanBase.isBlank()) {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            "Autosave_${sdf.format(Date(autoInfo.autosaveLastModifiedMs))}.xopp"
        } else {
            "$cleanBase.xopp"
        }

        val target = File(targetFolder, effectiveName)
        autoInfo.autosaveFile.copyTo(target, overwrite = true)
        autoInfo.autosaveFile.delete()
        return target
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

    fun openEmergencyRecoverySession(
        recoveryFile: File,
        targetFolder: File = env.getEmergencySavesDirectory()
    ): File {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val defaultName = "Recovered_Session_${sdf.format(Date(recoveryFile.lastModified()))}.xopp"
        return saveEmergencyRecoveryToNotes(recoveryFile, defaultName, targetFolder)
    }

    fun saveEmergencyRecoveryToNotes(
        recoveryFile: File,
        userSpecifiedName: String,
        targetFolder: File = getRootNotesDirectory()
    ): File {
        if (!targetFolder.exists()) targetFolder.mkdirs()

        val rawBase = if (userSpecifiedName.endsWith(".xopp", ignoreCase = true)) {
            userSpecifiedName.substring(0, userSpecifiedName.length - 5)
        } else {
            userSpecifiedName
        }
        val cleanBase = rawBase.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_")
        val effectiveName = if (cleanBase.isBlank()) {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            "Recovered_Session_${sdf.format(Date(recoveryFile.lastModified()))}.xopp"
        } else {
            "$cleanBase.xopp"
        }

        val target = File(targetFolder, effectiveName)
        recoveryFile.copyTo(target, overwrite = true)
        recoveryFile.delete()
        env.clearQuarantinedEmergencySave()
        return target
    }

    fun discardEmergencyRecovery() {
        env.clearQuarantinedEmergencySave()
    }

    /** Collects openable files under [root], skipping trash, hidden dirs, backups and autosaves. */
    private fun collectOpenableFiles(root: File): List<File> {
        val found = mutableListOf<File>()
        fun scan(dir: File) {
            val children = dir.listFiles() ?: return
            for (c in children) {
                if (c.isDirectory && c.name != TRASH_DIR_NAME && !c.name.startsWith(".")) {
                    scan(c)
                } else if (c.isFile && isOpenableFile(c) && !c.name.startsWith(".") && !c.name.endsWith("~") && !c.name.contains(".autosave.")) {
                    found.add(c)
                }
            }
        }
        scan(root)
        return found
    }

    private fun buildNoteDocument(file: File, cache: ScanCache): NoteDocument? {
        if (!file.exists() || !file.isFile || !isOpenableFile(file)) return null
        val sizeKb = (file.length() + 1023) / 1024
        val parentDir = file.parentFile ?: getRootNotesDirectory()
        val autosaveCandidate = findMatchingAutosave(parentDir, file)
        val autosaveInfo = autosaveCandidate?.let { autoFile ->
            AutosaveInfo(
                autosaveFile = autoFile,
                mainFile = file,
                mainLastModifiedMs = file.lastModified(),
                autosaveLastModifiedMs = autoFile.lastModified(),
                mainSizeBytes = file.length(),
                autosaveSizeBytes = autoFile.length()
            )
        }

        val isRoot = cache.isRoot(parentDir)
        val parentMeta = if (!isRoot) cache.folderMeta(parentDir) else FolderMetaData()
        val lastOpened = cache.openedTimestamps[file.absolutePath]
            ?: cache.openedTimestamps[canonicalOf(file)]

        return NoteDocument(
            file = file,
            title = file.nameWithoutExtension,
            path = file.absolutePath,
            lastModifiedMs = file.lastModified(),
            sizeBytes = file.length(),
            lastModifiedFormatted = cache.dateFormat.format(Date(file.lastModified())),
            sizeFormatted = "${sizeKb} KB",
            autosaveInfo = autosaveInfo,
            isHidden = file.name.startsWith("."),
            isPinned = cache.pinnedPaths.contains(file.absolutePath),
            folder = if (isRoot) "Notes Home" else parentDir.name,
            folderColorHex = parentMeta.colorHex,
            folderIconEmoji = parentMeta.iconEmoji,
            folderIconType = parentMeta.iconType,
            lastOpenedMs = lastOpened
        )
    }

    // Open History Tracking
    fun recordNoteOpened(path: String) {
        if (path.isBlank()) return
        val raw = prefs.getString("pref_opened_notes_history_json", null)
        val list = try {
            val array = org.json.JSONArray(raw ?: "[]")
            val l = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val p = array.optString(i)
                if (p.isNotBlank() && p != path) {
                    l.add(p)
                }
            }
            l
        } catch (e: Exception) {
            mutableListOf<String>()
        }
        list.add(0, path) // Most recently opened at the front
        val trimmed = list.take(50)
        val jsonArray = org.json.JSONArray()
        trimmed.forEach { jsonArray.put(it) }

        val rawTimestamps = prefs.getString("pref_opened_notes_timestamps_json", null)
        val timestampsObj = try {
            if (rawTimestamps != null) org.json.JSONObject(rawTimestamps) else org.json.JSONObject()
        } catch (e: Exception) {
            org.json.JSONObject()
        }
        timestampsObj.put(path, System.currentTimeMillis())

        prefs.edit()
            .putString("pref_opened_notes_history_json", jsonArray.toString())
            .putString("pref_opened_notes_timestamps_json", timestampsObj.toString())
            .putString("pref_last_opened_note_path", path)
            .apply()

        try {
            context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("pref_last_opened_note_path", path)
                .apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun removeOpenedNoteHistory(path: String) {
        if (path.isBlank()) return
        val raw = prefs.getString("pref_opened_notes_history_json", null) ?: return
        try {
            val array = org.json.JSONArray(raw)
            val jsonArray = org.json.JSONArray()
            for (i in 0 until array.length()) {
                val p = array.optString(i)
                if (p.isNotBlank() && p != path) {
                    jsonArray.put(p)
                }
            }
            prefs.edit().putString("pref_opened_notes_history_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun updateOpenedNotePath(oldPath: String, newPath: String) {
        if (oldPath.isBlank() || newPath.isBlank()) return
        val raw = prefs.getString("pref_opened_notes_history_json", null) ?: return
        try {
            val array = org.json.JSONArray(raw)
            val jsonArray = org.json.JSONArray()
            for (i in 0 until array.length()) {
                val p = array.optString(i)
                if (p == oldPath) {
                    jsonArray.put(newPath)
                } else if (p.isNotBlank()) {
                    jsonArray.put(p)
                }
            }
            prefs.edit().putString("pref_opened_notes_history_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            // ignore
        }
        if (prefs.getString("pref_last_opened_note_path", null) == oldPath) {
            prefs.edit().putString("pref_last_opened_note_path", newPath).apply()
        }
    }

    fun getRecentlyOpenedPaths(): List<String> {
        val raw = prefs.getString("pref_opened_notes_history_json", null)
        val list = mutableListOf<String>()
        if (!raw.isNullOrBlank()) {
            try {
                val array = org.json.JSONArray(raw)
                for (i in 0 until array.length()) {
                    val p = array.optString(i)
                    if (p.isNotBlank() && !list.contains(p)) list.add(p)
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        val mainPrefsLastOpened = try {
            context.getSharedPreferences("aournal_prefs", Context.MODE_PRIVATE)
                .getString("pref_last_opened_note_path", null)
        } catch (e: Exception) {
            null
        }
        if (!mainPrefsLastOpened.isNullOrBlank() && !list.contains(mainPrefsLastOpened)) {
            list.add(0, mainPrefsLastOpened)
        }
        val docHubLastOpened = prefs.getString("pref_last_opened_note_path", null)
        if (!docHubLastOpened.isNullOrBlank() && !list.contains(docHubLastOpened)) {
            list.add(0, docHubLastOpened)
        }
        return list
    }

    suspend fun getLastOpenedOrModifiedNote(): NoteDocument? = withContext(Dispatchers.IO) {
        val cache = ScanCache()
        // 1. Check open history first (the most recently opened valid note)
        for (path in getRecentlyOpenedPaths()) {
            val file = File(path)
            if (file.exists() && file.isFile && isOpenableFile(file) && !file.absolutePath.contains("/.Trash/")) {
                val doc = buildNoteDocument(file, cache)
                if (doc != null) return@withContext doc
            }
        }
        // 2. Fallback to latest modified file
        collectOpenableFiles(getRootNotesDirectory())
            .sortedByDescending { it.lastModified() }
            .firstOrNull()
            ?.let { buildNoteDocument(it, cache) }
    }

    suspend fun getNoteDocumentForFile(file: File): NoteDocument? = withContext(Dispatchers.IO) {
        buildNoteDocument(file, ScanCache())
    }

    suspend fun countAllNotes(): Int = withContext(Dispatchers.IO) {
        collectOpenableFiles(getRootNotesDirectory()).size
    }

    suspend fun getAllRecentNotes(limit: Int = 10): List<NoteDocument> = withContext(Dispatchers.IO) {
        val cache = ScanCache()
        val seenPaths = mutableSetOf<String>()
        val result = mutableListOf<NoteDocument>()

        // 1. Prioritize recently opened notes
        for (path in getRecentlyOpenedPaths()) {
            val file = File(path)
            if (file.exists() && file.isFile && isOpenableFile(file) && !file.absolutePath.contains("/.Trash/")) {
                val canonical = canonicalOf(file)
                if (!seenPaths.contains(file.absolutePath) && !seenPaths.contains(canonical)) {
                    seenPaths.add(file.absolutePath)
                    seenPaths.add(canonical)
                    buildNoteDocument(file, cache)?.let { doc ->
                        result.add(doc)
                    }
                }
            }
            if (result.size >= limit) break
        }

        // 2. Fill remaining slots with latest modified files
        if (result.size < limit) {
            val remainingFiles = collectOpenableFiles(getRootNotesDirectory())
                .sortedByDescending { it.lastModified() }
            for (file in remainingFiles) {
                val canonical = canonicalOf(file)
                if (!seenPaths.contains(file.absolutePath) && !seenPaths.contains(canonical)) {
                    seenPaths.add(file.absolutePath)
                    seenPaths.add(canonical)
                    buildNoteDocument(file, cache)?.let { doc ->
                        result.add(doc)
                    }
                }
                if (result.size >= limit) break
            }
        }

        result.take(limit)
    }

    suspend fun getHomeNotes(limit: Int = 16): List<NoteDocument> = withContext(Dispatchers.IO) {
        val cache = ScanCache()
        val pinnedDocs = mutableListOf<NoteDocument>()
        val seenPaths = mutableSetOf<String>()

        for (path in getPinnedNotePaths()) {
            val file = File(path)
            if (file.exists() && file.isFile && !file.absolutePath.contains("/.Trash/")) {
                buildNoteDocument(file, cache)?.let { doc ->
                    pinnedDocs.add(doc.copy(isPinned = true))
                    seenPaths.add(doc.path)
                    seenPaths.add(canonicalOf(doc.file))
                }
            }
        }

        val remainingLimit = limit - pinnedDocs.size
        val dynamicDocs = mutableListOf<NoteDocument>()
        if (remainingLimit > 0) {
            // Add recently opened first
            for (path in getRecentlyOpenedPaths()) {
                val file = File(path)
                if (file.exists() && file.isFile && isOpenableFile(file) && !file.absolutePath.contains("/.Trash/")) {
                    val canonical = canonicalOf(file)
                    if (!seenPaths.contains(file.absolutePath) && !seenPaths.contains(canonical)) {
                        seenPaths.add(file.absolutePath)
                        seenPaths.add(canonical)
                        buildNoteDocument(file, cache)?.let { doc ->
                            dynamicDocs.add(doc)
                        }
                    }
                }
                if (dynamicDocs.size >= remainingLimit) break
            }

            // Fill remaining with latest modified
            if (dynamicDocs.size < remainingLimit) {
                val remainingFiles = collectOpenableFiles(getRootNotesDirectory())
                    .sortedByDescending { it.lastModified() }
                for (file in remainingFiles) {
                    val canonical = canonicalOf(file)
                    if (!seenPaths.contains(file.absolutePath) && !seenPaths.contains(canonical)) {
                        seenPaths.add(file.absolutePath)
                        seenPaths.add(canonical)
                        buildNoteDocument(file, cache)?.let { doc ->
                            dynamicDocs.add(doc)
                        }
                    }
                    if (dynamicDocs.size >= remainingLimit) break
                }
            }
        }

        (pinnedDocs + dynamicDocs).take(limit)
    }
}

