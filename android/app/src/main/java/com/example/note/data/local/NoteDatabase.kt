package com.example.note.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [_NoteEntity::class, RemoteKeys::class],
    exportSchema = false,
    version = 1
)
abstract class NoteDatabase: RoomDatabase() {
    abstract fun getDao(): NoteDao
    abstract fun remoteKeys(): RemoteKeysDao
}