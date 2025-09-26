package com.example.note.domain.repository

import androidx.paging.PagingData
import com.example.note.domain.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun pager(): Flow<PagingData<NoteEntity>>
    suspend fun create(note: NoteEntity): Result<Unit>
    suspend fun update(note: NoteEntity): Result<Unit>
    suspend fun delete(note: NoteEntity): Result<Unit>
}