package dev.ilamparithi.aournalpp.testutils

import dev.ilamparithi.aournalpp.data.db.IndexedNoteEntity
import dev.ilamparithi.aournalpp.data.db.NoteIndexDao
import dev.ilamparithi.aournalpp.data.db.PathTimestampTuple
import java.util.concurrent.ConcurrentHashMap

class FakeNoteIndexDao : NoteIndexDao {

    private val storage = ConcurrentHashMap<String, IndexedNoteEntity>()

    override suspend fun searchNotesFts(
        folderPath: String,
        folderPathPrefix: String,
        ftsQuery: String,
        allowedExtensions: Set<String>
    ): List<IndexedNoteEntity> {
        val cleanQuery = ftsQuery.removeSuffix("*").replace("\"", "").trim()
        return searchNotesLike(folderPath, folderPathPrefix, cleanQuery, allowedExtensions)
    }

    override suspend fun searchNotesLike(
        folderPath: String,
        folderPathPrefix: String,
        query: String,
        allowedExtensions: Set<String>
    ): List<IndexedNoteEntity> {
        val q = query.lowercase()
        return storage.values
            .filter { entity ->
                val inFolder = entity.folderPath == folderPath || entity.folderPath.startsWith(folderPathPrefix)
                val matchingExt = allowedExtensions.contains(entity.extension.lowercase())
                val matchesQuery = entity.title.lowercase().contains(q) || entity.tags.lowercase().contains(q)
                inFolder && matchingExt && matchesQuery
            }
            .sortedByDescending { it.lastModifiedMs }
    }

    override suspend fun upsertNotes(notes: List<IndexedNoteEntity>) {
        notes.forEach { storage[it.path] = it }
    }

    override suspend fun deleteByPath(path: String) {
        storage.remove(path)
    }

    override suspend fun deleteByFolderPath(folderPath: String, folderPathPrefix: String) {
        storage.keys.removeIf { path ->
            val entity = storage[path]
            entity != null && (entity.folderPath == folderPath || entity.folderPath.startsWith(folderPathPrefix))
        }
    }

    override suspend fun getFolderTimestamps(folderPath: String): List<PathTimestampTuple> {
        return storage.values
            .filter { it.folderPath == folderPath }
            .map { PathTimestampTuple(it.path, it.lastModifiedMs) }
    }

    override suspend fun getCount(): Int {
        return storage.size
    }

    override suspend fun clearAll() {
        storage.clear()
    }
}
