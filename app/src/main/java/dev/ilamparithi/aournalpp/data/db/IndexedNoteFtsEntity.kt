package dev.ilamparithi.aournalpp.data.db

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = IndexedNoteEntity::class)
@Entity(tableName = "indexed_notes_fts")
data class IndexedNoteFtsEntity(
    val title: String,
    val tags: String
)
