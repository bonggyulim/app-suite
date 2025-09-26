package com.example.note.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface NoteDao {
    // PagingSource 추가 (최신순: createdAt DESC, id DESC)
    @Query("""
    SELECT * FROM _NoteEntity
    WHERE summarize IS NOT NULL AND sentiment IS NOT NULL
    ORDER BY datetime(COALESCE(createdAt,'1970-01-01T00:00:00Z')) DESC, id DESC""")
    fun pagingSource(): androidx.paging.PagingSource<Int, _NoteEntity>

    // 단건/유틸은 유지
    @Query("SELECT * FROM _NoteEntity WHERE id = :id")
    fun read(id: Int): kotlinx.coroutines.flow.Flow<_NoteEntity>

    @Upsert suspend fun upsertAll(items: List<_NoteEntity>)
    @Upsert suspend fun upsert(item: _NoteEntity)

    @Query("DELETE FROM _NoteEntity WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM _NoteEntity")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<_NoteEntity>) {
        clear(); upsertAll(items)
    }

    @Query("SELECT COUNT(*) FROM _NoteEntity")
    suspend fun count(): Int

    @Query("DELETE FROM _NoteEntity WHERE id NOT IN (:ids)")
    suspend fun deleteAllExcept(ids: Set<Int>)
}