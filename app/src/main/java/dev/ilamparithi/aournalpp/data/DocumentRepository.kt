package dev.ilamparithi.aournalpp.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.content.FileProvider
import dev.ilamparithi.aournalpp.model.AutosaveInfo
import dev.ilamparithi.aournalpp.model.FolderItem
import dev.ilamparithi.aournalpp.model.NoteDocument
import dev.ilamparithi.aournalpp.runtime.LinuxEnvironment
import dev.ilamparithi.aournalpp.runtime.PdfExportManager
import dev.ilamparithi.aournalpp.R
import dev.ilamparithi.aournalpp.utils.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPOutputStream

class DocumentRepository internal constructor(private val context: Context) {

    companion object {
        val SUPPORTED_EXTENSIONS = setOf("xopp", "xoj", "pdf")
        private const val FOLDER_META_FILE = FolderMetadataManager.FOLDER_META_FILE
        const val TRASH_DIR_NAME = TrashRepository.TRASH_DIR_NAME
        const val TRASH_MANIFEST_FILE = TrashRepository.TRASH_MANIFEST_FILE
        const val EMERGENCY_SAVES_DEFAULT_COLOR = "#F44336"
        const val EMERGENCY_SAVES_DEFAULT_ICON = "emergency"

        private val cache = DocumentCache()
        private val repoScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

        @Volatile
        private var INSTANCE: DocumentRepository? = null

        fun init(appContext: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = DocumentRepository(appContext.applicationContext)
                    }
                }
            }
        }

        fun getInstance(context: Context): DocumentRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DocumentRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun invalidateAllCaches() {
            cache.invalidateAll()
        }

        // Internal accessors delegating to DocumentCache
        private var cachedPinnedNotes: List<String>?
            get() = cache.cachedPinnedNotes
            set(value) { cache.cachedPinnedNotes = value }

        private var cachedPinnedNotesSet: Set<String>?
            get() = cache.cachedPinnedNotesSet
            set(value) { cache.cachedPinnedNotesSet = value }

        private var cachedPinnedFolders: List<String>?
            get() = cache.cachedPinnedFolders
            set(value) { cache.cachedPinnedFolders = value }

        private var cachedOpenedNotesHistory: List<String>?
            get() = cache.cachedOpenedNotesHistory
            set(value) { cache.cachedOpenedNotesHistory = value }

        private var cachedOpenedNotesTimestamps: Map<String, Long>?
            get() = cache.cachedOpenedNotesTimestamps
            set(value) { cache.cachedOpenedNotesTimestamps = value }

        private var cachedContinueNote: NoteDocument?
            get() = cache.cachedContinueNote
            set(value) { cache.cachedContinueNote = value }

        private var cachedTotalNotesCount: Int?
            get() = cache.cachedTotalNotesCount
            set(value) { cache.cachedTotalNotesCount = value }

        private var cachedTotalFoldersCount: Int?
            get() = cache.cachedTotalFoldersCount
            set(value) { cache.cachedTotalFoldersCount = value }

        private val directoryCache get() = cache.directoryCache
        private val homeNotesCache get() = cache.homeNotesCache
        private val recentNotesCache get() = cache.recentNotesCache
        private val folderMetaCache get() = cache.folderMetaCache
    }

    fun getCachedDirectory(
        targetDir: File,
        query: String = "",
        showHidden: Boolean = false
    ): Pair<List<FolderItem>, List<NoteDocument>>? = cache.getDirectory(targetDir, query, showHidden)

    fun getCachedHomeNotes(limit: Int = 16): List<NoteDocument>? = cache.homeNotesCache[limit]

    fun getCachedRecentNotes(limit: Int = 10): List<NoteDocument>? = cache.recentNotesCache[limit]

    fun getCachedContinueNote(): NoteDocument? = cache.cachedContinueNote

    fun getCachedTotalNotesCount(): Int? = cache.cachedTotalNotesCount

    fun getCachedTotalFoldersCount(): Int? = cache.cachedTotalFoldersCount

    private val env = LinuxEnvironment(context)
    private val prefs = AppPreferences.getDocumentHub(context)

    private val rootNotesDirCanonicalPath: String by lazy { canonicalOf(getRootNotesDirectory()) }
    private val rootNotesDirAbsolutePath: String by lazy { getRootNotesDirectory().absolutePath }
    private val emergencySavesCanonical: String by lazy { canonicalOf(env.getEmergencySavesDirectory()) }
    private val importedCanonical: String by lazy { canonicalOf(env.getImportedDirectory()) }
    private val audioCanonical: String by lazy { canonicalOf(env.getAudioDirectory()) }

    fun getLinuxEnvironment(): LinuxEnvironment = env

    fun getRootNotesDirectory(): File = env.getNotesDirectory()

    fun isRootNotesDirectory(dir: File): Boolean {
        val path = dir.absolutePath
        return path == rootNotesDirAbsolutePath
    }

    val folderMetadataManager = FolderMetadataManager(
        folderMetaCache = cache.folderMetaCache,
        isEmergencySavesFolder = { isEmergencySavesFolder(it) },
        getImportedCanonical = { importedCanonical },
        getAudioCanonical = { audioCanonical },
        onInvalidateCaches = { invalidateAllCaches() }
    )

    val noteHistoryTracker = NoteHistoryTracker(
        context = context,
        prefs = prefs,
        cache = cache,
        scope = repoScope,
        isExcludedFromRecents = { file -> isExcludedFromRecents(file) },
        isWithinRootDirectory = { file -> isWithinRootDirectory(file) }
    )

    val trashRepository = TrashRepository(
        notesDirectoryProvider = { env.getNotesDirectory() },
        findAssociatedFiles = { file -> findAssociatedAutosaveAndBackupFiles(file) },
        onRemoveOpenedNoteHistory = { path -> removeOpenedNoteHistory(path) },
        onInvalidateCaches = { invalidateAllCaches() },
        isOpenableFile = { file -> isOpenableFile(file) }
    )

    fun getTrashDirectory(): File = trashRepository.getTrashDirectory()

    private fun isOpenableFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return SUPPORTED_EXTENSIONS.contains(ext)
    }

    fun isWithinRootDirectory(file: File): Boolean {
        val target = file.absolutePath
        val root = rootNotesDirAbsolutePath
        if (target == root || target.startsWith("$root/") || target.startsWith("$root\\")) return true
        return try {
            val rootCanon = rootNotesDirCanonicalPath
            val targetCanon = canonicalOf(file)
            targetCanon == rootCanon || targetCanon.startsWith("$rootCanon/") || targetCanon.startsWith("$rootCanon\\")
        } catch (e: Exception) {
            false
        }
    }

    // Pinned Notes Persistence
    fun getPinnedNotePaths(strictlyWithinRoot: Boolean = true): List<String> {
        var list = cachedPinnedNotes
        if (list == null) {
            val raw = prefs.getString("pref_pinned_notes_order_json", null)
            list = if (raw != null) {
                try {
                    val array = org.json.JSONArray(raw)
                    val l = mutableListOf<String>()
                    for (i in 0 until array.length()) {
                        val p = array.optString(i)
                        if (p.isNotBlank()) {
                            l.add(p)
                        }
                    }
                    l
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
            cachedPinnedNotes = list
            cachedPinnedNotesSet = list.toSet()
        }

        return if (strictlyWithinRoot) {
            list.filter { isWithinRootDirectory(File(it)) }
        } else {
            list
        }
    }

    fun isNotePinned(path: String): Boolean {
        var set = cachedPinnedNotesSet
        if (set == null) {
            val list = getPinnedNotePaths()
            set = list.toSet()
            cachedPinnedNotesSet = set
        }
        return set.contains(path)
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
        cachedPinnedNotes = paths
        val array = org.json.JSONArray()
        paths.forEach { array.put(it) }
        prefs.edit().putString("pref_pinned_notes_order_json", array.toString()).apply()
        invalidateAllCaches()
    }

    // Pinned Folders Persistence
    fun getPinnedFolderPaths(): List<String> {
        var list = cachedPinnedFolders
        if (list == null) {
            val raw = prefs.getString("pref_pinned_folders_order_json", null)
            list = if (raw != null) {
                try {
                    val array = org.json.JSONArray(raw)
                    val l = mutableListOf<String>()
                    for (i in 0 until array.length()) {
                        val p = array.optString(i)
                        if (p.isNotBlank()) l.add(p)
                    }
                    l
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
            cachedPinnedFolders = list
        }
        return list
    }

    fun isFolderPinned(path: String): Boolean {
        val file = File(path)
        return folderMetadataManager.getFolderMeta(file).isPinned
    }

    fun pinFolder(path: String) {
        val file = File(path)
        folderMetadataManager.setFolderPinned(file, true)
        val current = getPinnedFolderPaths().toMutableList()
        current.remove(path)
        current.add(0, path)
        savePinnedFolders(current)
    }

    fun unpinFolder(path: String) {
        val file = File(path)
        folderMetadataManager.setFolderPinned(file, false)
        val current = getPinnedFolderPaths().toMutableList()
        current.remove(path)
        savePinnedFolders(current)
    }

    fun togglePinFolder(folder: FolderItem): Boolean {
        val path = folder.file.absolutePath
        val willPin = !folder.isPinned
        if (willPin) {
            pinFolder(path)
        } else {
            unpinFolder(path)
        }
        return willPin
    }

    private fun savePinnedFolders(paths: List<String>) {
        cachedPinnedFolders = paths
        val array = org.json.JSONArray()
        paths.forEach { array.put(it) }
        prefs.edit().putString("pref_pinned_folders_order_json", array.toString()).apply()
        invalidateAllCaches()
    }

    private fun canonicalOf(file: File): String =
        try { file.canonicalPath } catch (e: Exception) { file.absolutePath }

    fun getOpenedNotesTimestamps(): Map<String, Long> = noteHistoryTracker.getOpenedNotesTimestamps()

    /** Per-scan cache for the values every file in a scan shares. */
    private inner class ScanCache {
        val pinnedPaths: Set<String> by lazy { getPinnedNotePaths().toSet() }
        val pinnedFolderOrder: List<String> by lazy { getPinnedFolderPaths() }
        val pinnedFolderPaths: Set<String> by lazy { pinnedFolderOrder.toSet() }
        val pinnedFolderOrderMap: Map<String, Int> by lazy { pinnedFolderOrder.withIndex().associate { it.value to it.index } }
        val openedTimestamps: Map<String, Long> by lazy { getOpenedNotesTimestamps() }

        val rootCanonical: String by lazy { rootNotesDirCanonicalPath }
        val rootAbsolute: String by lazy { rootNotesDirAbsolutePath }

        fun folderMeta(dir: File): FolderMetaData = readFolderMeta(dir)

        fun isRoot(dir: File): Boolean {
            val path = dir.absolutePath
            return path == rootAbsolute || canonicalOf(dir) == rootCanonical
        }
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

        // Single-pass partitioning of target directory entries
        val directories = mutableListOf<File>()
        val mainFiles = mutableListOf<File>()
        val hiddenOrBackupFiles = if (showHidden) mutableListOf<File>() else null
        val fileMapByName = HashMap<String, File>(allFiles.size)

        for (f in allFiles) {
            if (f.isDirectory) {
                if (f.name != TRASH_DIR_NAME) {
                    directories.add(f)
                }
            } else if (f.isFile) {
                fileMapByName[f.name] = f
                val name = f.name
                val isHiddenOrBackup = name.startsWith(".") || name.endsWith("~") || name.contains(".autosave.", ignoreCase = true)
                if (!isHiddenOrBackup && isOpenableFile(f)) {
                    mainFiles.add(f)
                } else if (showHidden && isHiddenOrBackup && isOpenableCandidate(f)) {
                    hiddenOrBackupFiles?.add(f)
                }
            }
        }

        // 1. Scan Subfolders
        val folderItems = mutableListOf<FolderItem>()

        for (dir in directories) {
            val isHidden = dir.name.startsWith(".")
            if (isHidden && !showHidden) continue

            if (trimmedQuery.isNotEmpty() && !dir.name.contains(trimmedQuery, ignoreCase = true)) {
                continue
            }

            val meta = cache.folderMeta(dir)
            val isEmergency = meta.role == "emergency" || isEmergencySavesFolder(dir)
            val role = meta.role
            val isPinned = meta.isPinned
            val itemCount = dir.list { _, name ->
                !name.startsWith(".") && (name.endsWith(".xopp", ignoreCase = true) || name.endsWith(".xoj", ignoreCase = true) || name.endsWith(".pdf", ignoreCase = true))
            }?.size ?: 0

            folderItems.add(
                FolderItem(
                    file = dir,
                    name = dir.name,
                    colorHex = meta.colorHex,
                    iconEmoji = meta.iconEmoji,
                    iconType = meta.iconType,
                    isEmergencyFolder = isEmergency,
                    isPinned = isPinned,
                    role = role,
                    isExcludedFromRecents = meta.excludeFromRecents,
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

        val isRoot = cache.isRoot(targetDir)
        val targetFolderMeta = if (!isRoot) cache.folderMeta(targetDir) else FolderMetaData()

        for (file in mainFiles) {
            if (!seenMainPaths.add(file.absolutePath)) continue

            if (trimmedQuery.isNotEmpty() && !file.name.contains(trimmedQuery, ignoreCase = true)) {
                continue
            }

            val autosaveCandidate = findMatchingAutosave(fileMapByName, file)
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
                    lastModifiedFormatted = FormatUtils.formatDateTimeMedium(file.lastModified()),
                    sizeFormatted = FormatUtils.formatFileSize(file.length()),
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
        if (showHidden && hiddenOrBackupFiles != null) {
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
                        lastModifiedFormatted = FormatUtils.formatDateTimeMedium(file.lastModified()),
                        sizeFormatted = FormatUtils.formatFileSize(file.length()),
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

        // Sort folder items:
        // 1. User pinned folders in user pin order
        // 2. Virtually pinned special folders in alphabetical order
        // 3. Regular unpinned folders in alphabetical order
        val sortedFolders = folderItems.sortedWith { a, b ->
            val aPinned = a.isPinned
            val bPinned = b.isPinned
            when {
                aPinned && bPinned -> {
                    val aIndex = cache.pinnedFolderOrderMap[a.file.absolutePath] ?: Int.MAX_VALUE
                    val bIndex = cache.pinnedFolderOrderMap[b.file.absolutePath] ?: Int.MAX_VALUE
                    if (aIndex != bIndex) {
                        aIndex.compareTo(bIndex)
                    } else {
                        a.name.lowercase().compareTo(b.name.lowercase())
                    }
                }
                aPinned -> -1
                bPinned -> 1
                else -> a.name.lowercase().compareTo(b.name.lowercase())
            }
        }

        val result = Pair(
            sortedFolders,
            resultNotes.sortedByDescending { it.lastModifiedMs }
        )
        val cacheKey = "${targetDir.absolutePath}_${trimmedQuery}_$showHidden"
        directoryCache[cacheKey] = result
        if (isRoot && trimmedQuery.isEmpty()) {
            cachedTotalFoldersCount = sortedFolders.size
        }
        result
    }

    private fun isOpenableCandidate(file: File): Boolean {
        val lower = file.name.lowercase()
        return lower.endsWith(".xopp") || lower.endsWith(".xoj") || lower.endsWith(".pdf") ||
               lower.endsWith(".xopp~") || lower.endsWith(".xoj~") || lower.endsWith(".pdf~") ||
               lower.contains(".autosave.xopp") || lower.contains(".autosave.xoj")
    }

    private fun findMatchingAutosave(fileMap: Map<String, File>, mainFile: File): File? {
        val base = mainFile.nameWithoutExtension
        val ext = mainFile.extension
        val candidateNames = listOf(
            ".${mainFile.name}.autosave.$ext",
            ".$base.autosave.$ext",
            ".${mainFile.name}~",
            "${mainFile.name}~",
            ".$base.$ext~"
        )
        for (name in candidateNames) {
            val candidate = fileMap[name]
            if (candidate != null && candidate.isFile && candidate.length() > 0) {
                return candidate
            }
        }
        return null
    }

    fun findMatchingAutosave(parentDir: File, mainFile: File): File? {
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
               folderDir.absolutePath == env.getEmergencySavesDirectory().absolutePath ||
               try { canonicalOf(folderDir) == emergencySavesCanonical } catch (e: Exception) { false }
    }

    // Folder Management
    fun createFolder(
        parentDir: File,
        name: String,
        colorHex: String? = null,
        iconEmoji: String? = null,
        iconType: String? = null,
        role: String? = null,
        excludeFromRecents: Boolean = false
    ): Result<File> {
        val cleanName = name.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_")
        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Folder name cannot be blank"))

        val newDir = File(parentDir, cleanName)
        if (newDir.exists()) return Result.failure(IllegalArgumentException("Folder '$cleanName' already exists"))

        if (!newDir.mkdirs()) {
            return Result.failure(IllegalStateException("Failed to create folder '$cleanName'"))
        }

        if (colorHex != null || iconEmoji != null || iconType != null || role != null || excludeFromRecents) {
            writeFolderMeta(newDir, colorHex, iconEmoji, iconType, role, excludeFromRecents)
        }
        invalidateAllCaches()
        return Result.success(newDir)
    }

    fun setFolderColor(folderDir: File, colorHex: String?): Result<Unit> =
        folderMetadataManager.setFolderColor(folderDir, colorHex)

    fun setFolderEmoji(folderDir: File, emoji: String?): Result<Unit> =
        folderMetadataManager.setFolderEmoji(folderDir, emoji)

    fun setFolderIcon(folderDir: File, iconType: String?): Result<Unit> =
        folderMetadataManager.setFolderIcon(folderDir, iconType)

    fun setFolderExcludeFromRecents(folderDir: File, exclude: Boolean): Result<Unit> =
        folderMetadataManager.setFolderExcludeFromRecents(folderDir, exclude)

    fun updateFolderMeta(
        folderDir: File,
        colorHex: String?,
        iconEmoji: String?,
        iconType: String? = null,
        role: String? = null,
        excludeFromRecents: Boolean? = null
    ): Result<Unit> = folderMetadataManager.updateFolderMeta(folderDir, colorHex, iconEmoji, iconType, role, excludeFromRecents)

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

            val oldMeta = readFolderMeta(folderDir)
            val wasPinned = isFolderPinned(folderDir.absolutePath)

            if (!folderDir.renameTo(targetDir)) {
                error("Failed to rename folder to '$cleanName'")
            }

            // If folder had a special role, update LinuxEnvironment special path
            if (!oldMeta.role.isNullOrBlank()) {
                env.setSpecialDirectoryPath(oldMeta.role, targetDir.absolutePath)
            }

            // If folder was pinned, update the pinned path list
            if (wasPinned) {
                val currentPinned = getPinnedFolderPaths().toMutableList()
                val index = currentPinned.indexOf(folderDir.absolutePath)
                if (index != -1) {
                    currentPinned[index] = targetDir.absolutePath
                    savePinnedFolders(currentPinned)
                }
            }

            invalidateAllCaches()
            targetDir
        }
    }

    fun getFolderMeta(folderDir: File): FolderMetaData = folderMetadataManager.getFolderMeta(folderDir)

    private fun readFolderMeta(folderDir: File): FolderMetaData = folderMetadataManager.readFolderMeta(folderDir)

    private fun writeFolderMeta(
        folderDir: File,
        colorHex: String?,
        iconEmoji: String?,
        iconType: String? = null,
        role: String? = null,
        excludeFromRecents: Boolean? = null
    ): Result<Unit> = folderMetadataManager.writeFolderMeta(folderDir, colorHex, iconEmoji, iconType, role, excludeFromRecents)

    fun getFolderItem(dir: File): FolderItem {
        val cache = ScanCache()
        val meta = cache.folderMeta(dir)
        val isEmergency = meta.role == "emergency" || isEmergencySavesFolder(dir)
        val role = meta.role
        val isPinned = meta.isPinned
        val count = dir.listFiles { f -> f.isFile && isOpenableFile(f) && !f.name.startsWith(".") }?.size ?: 0
        return FolderItem(
            file = dir,
            name = dir.name,
            colorHex = meta.colorHex,
            iconEmoji = meta.iconEmoji,
            iconType = meta.iconType,
            isEmergencyFolder = isEmergency,
            isPinned = isPinned,
            role = role,
            isExcludedFromRecents = meta.excludeFromRecents,
            itemCount = count,
            lastModifiedMs = dir.lastModified(),
            isHidden = dir.name.startsWith(".")
        )
    }

    suspend fun getAllFolders(root: File = getRootNotesDirectory()): List<FolderItem> = withContext(Dispatchers.IO) {
        val cache = ScanCache()
        val list = mutableListOf<FolderItem>()
        fun recurse(dir: File) {
            val subdirs = dir.listFiles { f -> f.isDirectory && f.name != TRASH_DIR_NAME && !f.name.startsWith(".") } ?: return
            for (sub in subdirs) {
                val meta = cache.folderMeta(sub)
                val isEmergency = meta.role == "emergency" || isEmergencySavesFolder(sub)
                val role = meta.role
                val isPinned = meta.isPinned
                val count = sub.listFiles { f -> f.isFile && isOpenableFile(f) && !f.name.startsWith(".") }?.size ?: 0
                list.add(
                    FolderItem(
                        file = sub,
                        name = sub.name,
                        colorHex = meta.colorHex,
                        iconEmoji = meta.iconEmoji,
                        iconType = meta.iconType,
                        isEmergencyFolder = isEmergency,
                        isPinned = isPinned,
                        role = role,
                        isExcludedFromRecents = meta.excludeFromRecents,
                        itemCount = count,
                        lastModifiedMs = sub.lastModified()
                    )
                )
                recurse(sub)
            }
        }
        recurse(root)

        list.sortedWith { a, b ->
            val aPinned = a.isPinned
            val bPinned = b.isPinned
            when {
                aPinned && bPinned -> {
                    val aIndex = cache.pinnedFolderOrder.indexOf(a.file.absolutePath).takeIf { it >= 0 } ?: Int.MAX_VALUE
                    val bIndex = cache.pinnedFolderOrder.indexOf(b.file.absolutePath).takeIf { it >= 0 } ?: Int.MAX_VALUE
                    if (aIndex != bIndex) {
                        aIndex.compareTo(bIndex)
                    } else {
                        a.name.lowercase().compareTo(b.name.lowercase())
                    }
                }
                aPinned -> -1
                bPinned -> 1
                else -> a.name.lowercase().compareTo(b.name.lowercase())
            }
        }
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

            for ((file) in notes) {
                if (!file.exists()) continue
                var destFile = File(destFolder, file.name)
                if (destFile.exists() && destFile.canonicalPath != file.canonicalPath) {
                    val nameWithoutExt = file.nameWithoutExtension
                    val ext = file.extension
                    var counter = 1
                    while (destFile.exists()) {
                        destFile = File(destFolder, "${nameWithoutExt}_$counter.$ext")
                        counter++
                    }
                }

                val srcAssociated = findAssociatedAutosaveAndBackupFiles(file)

                if (file.renameTo(destFile)) {
                    movedCount++

                    // Move all associated autosave and backup files into destFolder as well
                    for (assoc in srcAssociated) {
                        if (assoc.exists()) {
                            val newAssocName = if (assoc.name.contains(file.name)) {
                                assoc.name.replace(file.name, destFile.name)
                            } else if (assoc.name.contains(file.nameWithoutExtension)) {
                                assoc.name.replace(file.nameWithoutExtension, destFile.nameWithoutExtension)
                            } else {
                                assoc.name
                            }
                            val destAssocFile = File(destFolder, newAssocName)
                            assoc.renameTo(destAssocFile)
                        }
                    }
                }
            }
            movedCount.also { invalidateAllCaches() }
        }
    }

    // Trashcan Operations delegated to TrashRepository
    suspend fun moveToTrash(notes: List<NoteDocument>): Result<TrashReceipt> =
        trashRepository.moveToTrash(notes)

    suspend fun moveFolderToTrash(folder: File): Result<String> =
        trashRepository.moveFolderToTrash(folder)

    suspend fun restoreTrashItems(trashFileNames: List<String>): Result<Int> =
        trashRepository.restoreTrashItems(trashFileNames)

    suspend fun restoreFolderFromTrash(trashFolderName: String): Result<File> =
        trashRepository.restoreFolderFromTrash(trashFolderName)

    suspend fun scanTrash(): List<NoteDocument> =
        trashRepository.scanTrash()

    suspend fun restoreFromTrash(note: NoteDocument): Result<File> =
        trashRepository.restoreFromTrash(note)

    suspend fun restoreMultipleFromTrash(notes: List<NoteDocument>): Result<Int> =
        trashRepository.restoreMultipleFromTrash(notes)

    suspend fun deletePermanently(notes: List<NoteDocument>): Result<Int> =
        trashRepository.deletePermanently(notes)

    suspend fun emptyTrash(): Result<Unit> =
        trashRepository.emptyTrash()

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

            invalidateAllCaches()
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
            invalidateAllCaches()
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
    fun shareNoteAsXopp(context: Context, doc: NoteDocument, customName: String? = null) {
        if (!customName.isNullOrBlank() && customName != doc.title) {
            val shareStagingDir = File(context.cacheDir, "shared_notes").apply { mkdirs() }
            val ext = if (doc.file.extension.equals("pdf", ignoreCase = true)) "pdf" else "xopp"
            val stagedFile = File(shareStagingDir, "$customName.$ext")
            try {
                doc.file.copyTo(stagedFile, overwrite = true)
                val fileUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    stagedFile
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = if (ext == "pdf") "application/pdf" else "application/x-xopp"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, customName)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Note"))
                return
            } catch (e: Exception) {
                Log.w("DocumentRepository", "Failed to stage custom named file for share, falling back to original", e)
            }
        }
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
            for ((file) in docs) {
                uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserTitle = context.resources.getQuantityString(R.plurals.title_share_notes, docs.size, docs.size)
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        }
    }

    suspend fun shareNoteAsPdf(
        context: Context,
        doc: NoteDocument,
        pdfExportManager: PdfExportManager,
        customName: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val pdfFile = if (doc.file.extension.equals("pdf", ignoreCase = true)) {
                if (!customName.isNullOrBlank() && customName != doc.title) {
                    val shareStagingDir = File(context.cacheDir, "shared_notes").apply { mkdirs() }
                    val staged = File(shareStagingDir, "$customName.pdf")
                    doc.file.copyTo(staged, overwrite = true)
                    staged
                } else {
                    doc.file
                }
            } else {
                val basePdf = pdfExportManager.renderPdfForSharing(context, doc.file).getOrThrow()
                if (!customName.isNullOrBlank() && customName != doc.title) {
                    val shareStagingDir = File(context.cacheDir, "shared_notes").apply { mkdirs() }
                    val staged = File(shareStagingDir, "$customName.pdf")
                    basePdf.copyTo(staged, overwrite = true)
                    staged
                } else {
                    basePdf
                }
            }

            val pdfUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val subjectName = customName ?: doc.title

            withContext(Dispatchers.Main) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    putExtra(Intent.EXTRA_SUBJECT, "$subjectName.pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share PDF"))
            }
        }
    }

    suspend fun shareMultipleNotesAsPdf(
        context: Context,
        docs: List<NoteDocument>,
        pdfExportManager: PdfExportManager
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (docs.isEmpty()) return@runCatching

            val pdfUris = ArrayList<Uri>()
            for ((file) in docs) {
                val pdfFile = if (file.extension.equals("pdf", ignoreCase = true)) {
                    file
                } else {
                    pdfExportManager.renderPdfForSharing(context, file).getOrThrow()
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
                    val chooserTitle = context.resources.getQuantityString(R.plurals.title_share_pdfs, docs.size, docs.size)
                    context.startActivity(Intent.createChooser(intent, chooserTitle))
                }
            }
        }
    }

    enum class ShareExportFormat {
        PDF,
        XOPP,
        ORIGINAL
    }

    suspend fun exportDocumentToUri(
        context: Context,
        doc: NoteDocument,
        format: ShareExportFormat,
        destUri: Uri,
        pdfExportManager: PdfExportManager
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            when (format) {
                ShareExportFormat.PDF -> {
                    if (doc.file.extension.equals("pdf", ignoreCase = true)) {
                        context.contentResolver.openOutputStream(destUri)?.use { out ->
                            doc.file.inputStream().use { it.copyTo(out) }
                        } ?: error("Failed to open destination URI for PDF export")
                    } else {
                        pdfExportManager.exportPdfToUri(context, doc.file, destUri).getOrThrow()
                    }
                }
                ShareExportFormat.XOPP,
                ShareExportFormat.ORIGINAL -> {
                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                        doc.file.inputStream().use { it.copyTo(out) }
                    } ?: error("Failed to open destination URI for note export")
                }
            }
            Unit
        }
    }

    suspend fun exportDocumentsToDirectory(
        context: Context,
        docs: List<NoteDocument>,
        format: ShareExportFormat,
        treeUri: Uri,
        pdfExportManager: PdfExportManager,
        onProgress: ((current: Int, total: Int, name: String) -> Unit)? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
            var count = 0

            docs.forEachIndexed { index, doc ->
                onProgress?.invoke(index + 1, docs.size, doc.title)
                val targetExt = when (format) {
                    ShareExportFormat.PDF -> "pdf"
                    ShareExportFormat.XOPP -> "xopp"
                    ShareExportFormat.ORIGINAL -> doc.file.extension.ifEmpty { "xopp" }
                }
                val mimeType = when (targetExt.lowercase()) {
                    "pdf" -> "application/pdf"
                    "xopp" -> "application/x-xopp"
                    "xoj" -> "application/x-xoj"
                    else -> "application/octet-stream"
                }
                val cleanFileName = "${doc.title}.$targetExt"
                val newDocUri = DocumentsContract.createDocument(
                    context.contentResolver,
                    parentDocUri,
                    mimeType,
                    cleanFileName
                ) ?: error("Failed to create document $cleanFileName in destination folder")

                exportDocumentToUri(context, doc, format, newDocUri, pdfExportManager).getOrThrow()
                count++
            }
            count
        }
    }

    suspend fun shareUnifiedDocuments(
        context: Context,
        docs: List<NoteDocument>,
        format: ShareExportFormat,
        customNameForSingle: String? = null,
        pdfExportManager: PdfExportManager
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (docs.isEmpty()) return@runCatching

            if (docs.size == 1) {
                val doc = docs.first()
                when (format) {
                    ShareExportFormat.PDF -> {
                        shareNoteAsPdf(context, doc, pdfExportManager, customNameForSingle).getOrThrow()
                    }
                    ShareExportFormat.XOPP,
                    ShareExportFormat.ORIGINAL -> {
                        shareNoteAsXopp(context, doc, customNameForSingle)
                    }
                }
            } else {
                when (format) {
                    ShareExportFormat.PDF -> {
                        shareMultipleNotesAsPdf(context, docs, pdfExportManager).getOrThrow()
                    }
                    ShareExportFormat.XOPP -> {
                        shareMultipleNotesAsXopp(context, docs.filter { it.file.extension.equals("xopp", ignoreCase = true) })
                    }
                    ShareExportFormat.ORIGINAL -> {
                        val uris = ArrayList<Uri>()
                        for ((file) in docs) {
                            uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
                        }
                        withContext(Dispatchers.Main) {
                            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = "*/*"
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val chooserTitle = context.resources.getQuantityString(R.plurals.title_share_notes, docs.size, docs.size)
                            context.startActivity(Intent.createChooser(intent, chooserTitle))
                        }
                    }
                }
            }
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
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
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
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
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
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
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
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
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

    /** Returns true if the file is in staged_imports, cache, trash, or in a folder marked excludeFromRecents. */
    fun isExcludedFromRecents(file: File): Boolean = isExcludedFromRecents(file, null)

    private fun isExcludedFromRecents(file: File, cache: ScanCache? = null): Boolean {
        val path = file.absolutePath
        if (path.contains("staged_imports") || path.contains("/cache/") || path.contains("/.Trash/")) return true
        val rootPath = rootNotesDirAbsolutePath
        val rootCanonical = rootNotesDirCanonicalPath
        var curr: File? = file.parentFile
        while (curr != null) {
            val meta = cache?.folderMeta(curr) ?: readFolderMeta(curr)
            if (meta.excludeFromRecents) return true
            val currPath = curr.absolutePath
            if (currPath == rootPath) break
            if (canonicalOf(curr) == rootCanonical) break
            curr = curr.parentFile
        }
        return false
    }

    /** Collects openable files under [root], skipping trash, hidden dirs, backups and autosaves. */
    private fun collectOpenableFiles(root: File, cache: ScanCache? = null, skipRecentsExcluded: Boolean = false): List<File> {
        val found = mutableListOf<File>()
        fun scan(dir: File) {
            if (skipRecentsExcluded) {
                val meta = cache?.folderMeta(dir) ?: readFolderMeta(dir)
                if (meta.excludeFromRecents) return
            }
            val children = dir.listFiles() ?: return
            for (c in children) {
                if (c.isDirectory && c.name != TRASH_DIR_NAME && !c.name.startsWith(".")) {
                    scan(c)
                } else if (c.isFile && isOpenableFile(c) && !c.name.startsWith(".") && !c.name.endsWith("~") && !c.name.contains(".autosave.")) {
                    if (!skipRecentsExcluded || !isExcludedFromRecents(c, cache)) {
                        found.add(c)
                    }
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
            lastModifiedFormatted = FormatUtils.formatDateTimeMedium(file.lastModified()),
            sizeFormatted = FormatUtils.formatFileSize(file.length()),
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
    fun recordNoteOpened(path: String) = noteHistoryTracker.recordNoteOpened(path)

    private fun getRecentlyOpenedHistoryFromPrefs(): List<String> = noteHistoryTracker.getRecentlyOpenedHistoryFromPrefs()

    fun removeOpenedNoteHistory(path: String) = noteHistoryTracker.removeOpenedNoteHistory(path)

    fun updateOpenedNotePath(oldPath: String, newPath: String) = noteHistoryTracker.updateOpenedNotePath(oldPath, newPath)

    fun getRecentlyOpenedPaths(strictlyWithinRoot: Boolean = true): List<String> = noteHistoryTracker.getRecentlyOpenedPaths(strictlyWithinRoot)

    suspend fun getLastOpenedOrModifiedNote(): NoteDocument? = withContext(Dispatchers.IO) {
        val cache = ScanCache()
        // 1. Check open history first (the most recently opened valid note)
        for (path in getRecentlyOpenedPaths()) {
            val file = File(path)
            if (file.exists() && file.isFile && isOpenableFile(file) && !isExcludedFromRecents(file, cache)) {
                val doc = buildNoteDocument(file, cache)
                if (doc != null) {
                    cachedContinueNote = doc
                    return@withContext doc
                }
            }
        }
        // 2. Fallback to latest modified file
        val doc = collectOpenableFiles(getRootNotesDirectory(), cache, skipRecentsExcluded = true)
            .maxByOrNull { it.lastModified() }
            ?.let { buildNoteDocument(it, cache) }
        cachedContinueNote = doc
        doc
    }

    suspend fun getNoteDocumentForFile(file: File): NoteDocument? = withContext(Dispatchers.IO) {
        buildNoteDocument(file, ScanCache())
    }

    suspend fun findNoteDocumentByTitle(title: String): NoteDocument? = withContext(Dispatchers.IO) {
        val cleanTitle = title.removePrefix("*").removeSuffix("*").trim()
        if (cleanTitle.isBlank() || cleanTitle.equals("New Note", ignoreCase = true) ||
            cleanTitle.equals("Unsaved Document", ignoreCase = true) ||
            cleanTitle.equals("Preferences", ignoreCase = true) ||
            cleanTitle.equals("Xournal++", ignoreCase = true)) {
            return@withContext null
        }
        val root = getRootNotesDirectory()
        val candidates = listOf(
            cleanTitle,
            "$cleanTitle.xopp",
            "$cleanTitle.pdf",
            "$cleanTitle.xoj"
        )
        for (cand in candidates) {
            val f = File(root, cand)
            if (f.exists() && f.isFile) {
                return@withContext getNoteDocumentForFile(f)
            }
        }
        val matched = collectOpenableFiles(root).firstOrNull {
            it.nameWithoutExtension.equals(cleanTitle, ignoreCase = true) ||
            it.name.equals(cleanTitle, ignoreCase = true)
        }
        matched?.let { getNoteDocumentForFile(it) }
    }

    suspend fun countAllNotes(): Int = withContext(Dispatchers.IO) {
        val count = collectOpenableFiles(getRootNotesDirectory()).size
        cachedTotalNotesCount = count
        count
    }

    suspend fun getAllRecentNotes(limit: Int = 10): List<NoteDocument> = withContext(Dispatchers.IO) {
        val cache = ScanCache()
        val seenPaths = mutableSetOf<String>()
        val result = mutableListOf<NoteDocument>()

        // 1. Prioritize recently opened notes
        for (path in getRecentlyOpenedPaths()) {
            val file = File(path)
            if (file.exists() && file.isFile && isOpenableFile(file) && !isExcludedFromRecents(file, cache)) {
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
            val remainingFiles = collectOpenableFiles(getRootNotesDirectory(), cache, skipRecentsExcluded = true)
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

        val res = result.take(limit)
        recentNotesCache[limit] = res
        res
    }

    data class HomeDataResult(
        val notes: List<NoteDocument>,
        val totalNotesCount: Int,
        val totalFoldersCount: Int,
        val continueNote: NoteDocument?
    )

    suspend fun getHomeData(limit: Int = 16): HomeDataResult = withContext(Dispatchers.IO) {
        val cache = ScanCache()
        val rootDir = getRootNotesDirectory()
        val allOpenableFiles = collectOpenableFiles(rootDir, cache, skipRecentsExcluded = false)
        val totalNotes = allOpenableFiles.size
        cachedTotalNotesCount = totalNotes

        val rootFolders = scanDirectory(rootDir).first.size
        cachedTotalFoldersCount = rootFolders

        val seenPaths = mutableSetOf<String>()
        val pinnedDocs = mutableListOf<NoteDocument>()

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

        // 1. Find continueNote from recently opened or latest modified
        var continueDoc: NoteDocument? = null
        for (path in getRecentlyOpenedPaths()) {
            val file = File(path)
            if (file.exists() && file.isFile && isOpenableFile(file) && !isExcludedFromRecents(file, cache)) {
                continueDoc = buildNoteDocument(file, cache)
                if (continueDoc != null) break
            }
        }
        if (continueDoc == null) {
            val latestFile = allOpenableFiles
                .filter { !isExcludedFromRecents(it, cache) }
                .maxByOrNull { it.lastModified() }
            if (latestFile != null) {
                continueDoc = buildNoteDocument(latestFile, cache)
            }
        }
        cachedContinueNote = continueDoc

        // 2. Build home notes
        val remainingLimit = limit - pinnedDocs.size
        val dynamicDocs = mutableListOf<NoteDocument>()
        if (remainingLimit > 0) {
            for (path in getRecentlyOpenedPaths()) {
                val file = File(path)
                if (file.exists() && file.isFile && isOpenableFile(file) && !isExcludedFromRecents(file, cache)) {
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

            if (dynamicDocs.size < remainingLimit) {
                val remainingFiles = allOpenableFiles
                    .filter { !isExcludedFromRecents(it, cache) }
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

        val homeNotes = (pinnedDocs + dynamicDocs).take(limit)
        homeNotesCache[limit] = homeNotes

        HomeDataResult(
            notes = homeNotes,
            totalNotesCount = totalNotes,
            totalFoldersCount = rootFolders,
            continueNote = continueDoc
        )
    }

    suspend fun getHomeNotes(limit: Int = 16): List<NoteDocument> = withContext(Dispatchers.IO) {
        getHomeData(limit).notes
    }
}

