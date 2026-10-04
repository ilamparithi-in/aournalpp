package dev.ilamparithi.aournalpp.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface NoteIndexDao {

    @Query("""
        SELECT n.* FROM indexed_notes n
        JOIN indexed_notes_fts fts ON n.rowid = fts.rowid
        WHERE (n.folderPath = :folderPath OR n.folderPath LIKE :folderPathPrefix || '%')
          AND n.extension IN (:allowedExtensions)
          AND indexed_notes_fts MATCH :ftsQuery
        ORDER BY n.lastModifiedMs DESC
    """)
    suspend fun searchNotesFts(
        folderPath: String,
        folderPathPrefix: String,
        ftsQuery: String,
        allowedExtensions: Set<String>
    ): List<IndexedNoteEntity>

    @Query("""
        SELECT * FROM indexed_notes
        WHERE (folderPath = :folderPath OR folderPath LIKE :folderPathPrefix || '%')
          AND extension IN (:allowedExtensions)
          AND (title LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
        ORDER BY lastModifiedMs DESC
    """)
    suspend fun searchNotesLike(
        folderPath: String,
        folderPathPrefix: String,
        query: String,
        allowedExtensions: Set<String>
    ): List<IndexedNoteEntity>

    @Upsert
    suspend fun upsertNotes(notes: List<IndexedNoteEntity>)

    @Query("DELETE FROM indexed_notes WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM indexed_notes WHERE folderPath = :folderPath OR folderPath LIKE :folderPathPrefix || '%'")
    suspend fun deleteByFolderPath(folderPath: String, folderPathPrefix: String)

    @Query("SELECT path, lastModifiedMs FROM indexed_notes WHERE folderPath = :folderPath")
    suspend fun getFolderTimestamps(folderPath: String): List<PathTimestampTuple>

    @Query("SELECT COUNT(*) FROM indexed_notes")
    suspend fun getCount(): Int

    @Query("DELETE FROM indexed_notes")
    suspend fun clearAll()
}
