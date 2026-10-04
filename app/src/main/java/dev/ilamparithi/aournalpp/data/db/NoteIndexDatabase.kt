package dev.ilamparithi.aournalpp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [IndexedNoteEntity::class, IndexedNoteFtsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NoteIndexDatabase : RoomDatabase() {

    abstract fun noteIndexDao(): NoteIndexDao

    companion object {
        @Volatile
        private var INSTANCE: NoteIndexDatabase? = null

        fun getInstance(context: Context): NoteIndexDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteIndexDatabase::class.java,
                    "aournal_notes_index.db"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
