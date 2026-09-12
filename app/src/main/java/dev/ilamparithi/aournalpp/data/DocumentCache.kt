package dev.ilamparithi.aournalpp.data

import dev.ilamparithi.aournalpp.model.FolderItem
import dev.ilamparithi.aournalpp.model.NoteDocument
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory cache layer for DocumentRepository.
 * Manages directory listings, note lists, metadata caches, and cache invalidation.
 */
class DocumentCache {
    val directoryCache = ConcurrentHashMap<String, Pair<List<FolderItem>, List<NoteDocument>>>()
    val homeNotesCache = ConcurrentHashMap<Int, List<NoteDocument>>()
    val recentNotesCache = ConcurrentHashMap<Int, List<NoteDocument>>()
    val folderMetaCache = ConcurrentHashMap<String, Pair<Long, FolderMetaData>>()

    @Volatile var cachedContinueNote: NoteDocument? = null
    @Volatile var cachedTotalNotesCount: Int? = null
    @Volatile var cachedTotalFoldersCount: Int? = null
    @Volatile var cachedPinnedNotes: List<String>? = null
    @Volatile var cachedPinnedNotesSet: Set<String>? = null
    @Volatile var cachedPinnedFolders: List<String>? = null
    @Volatile var cachedOpenedNotesHistory: List<String>? = null
    @Volatile var cachedOpenedNotesTimestamps: Map<String, Long>? = null

    fun invalidateAll() {
        directoryCache.clear()
        homeNotesCache.clear()
        recentNotesCache.clear()
        folderMetaCache.clear()
        cachedContinueNote = null
        cachedTotalNotesCount = null
        cachedTotalFoldersCount = null
        cachedPinnedNotes = null
        cachedPinnedNotesSet = null
        cachedPinnedFolders = null
        cachedOpenedNotesHistory = null
        cachedOpenedNotesTimestamps = null
    }

    fun getDirectory(targetDir: File, query: String = "", showHidden: Boolean = false): Pair<List<FolderItem>, List<NoteDocument>>? {
        val key = "${targetDir.absolutePath}_${query.trim()}_$showHidden"
        return directoryCache[key]
    }

    fun putDirectory(targetDir: File, query: String = "", showHidden: Boolean = false, data: Pair<List<FolderItem>, List<NoteDocument>>) {
        val key = "${targetDir.absolutePath}_${query.trim()}_$showHidden"
        directoryCache[key] = data
    }
}
