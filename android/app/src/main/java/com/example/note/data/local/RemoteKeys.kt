package com.example.note.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

@Entity(tableName = "remote_keys")
data class RemoteKeys(
    @PrimaryKey val feed: String,  // "main" 등
    val nextCursor: String?
)

@Dao
interface RemoteKeysDao {
    @Query("SELECT * FROM remote_keys WHERE feed = :feed")
    suspend fun get(feed: String): RemoteKeys?

    @Upsert
    suspend fun upsert(keys: RemoteKeys)

    @Query("DELETE FROM remote_keys WHERE feed = :feed")
    suspend fun clear(feed: String)
}