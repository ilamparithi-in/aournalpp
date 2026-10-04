package dev.ilamparithi.aournalpp.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "indexed_notes",
    indices = [
        Index(value = ["folderPath"]),
        Index(value = ["extension"]),
        Index(value = ["lastModifiedMs"])
    ]
)
data class IndexedNoteEntity(
    @PrimaryKey
    val path: String,
    val title: String,
    val folderPath: String,
    val extension: String,
    val lastModifiedMs: Long,
    val sizeBytes: Long,
    val tags: String = ""
)

data class PathTimestampTuple(
    val path: String,
    val lastModifiedMs: Long
)
