package com.example.note.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.note.data.local.NoteDatabase
import com.example.note.data.local._NoteEntity
import com.example.note.data.paging.NotesRemoteMediator
import com.example.note.data.remote.api.NoteApi
import com.example.note.data.remote.api.NoteRequest
import com.example.note.domain.entity.NoteEntity
import com.example.note.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import androidx.paging.map
import com.example.note.data.mapper.toDomain
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class NoteRepositoryImpl@Inject constructor(
    private val api: NoteApi,
    private val db: NoteDatabase
) : NoteRepository {

    override fun pager(): Flow<PagingData<NoteEntity>> =
        Pager(
            config = PagingConfig(pageSize = 10, enablePlaceholders = false),
            remoteMediator = NotesRemoteMediator(api, db),
            pagingSourceFactory = { db.getDao().pagingSource() }
        ).flow.map { paging ->
            paging.map { it.toDomain() }
        }

    override suspend fun create(note: NoteEntity): Result<Unit> = runCatching {
        val dto = api.create(NoteRequest(title = note.title, content = note.content))
        // 생성 응답을 로컬 반영(즉시 리스트에 보이게)
        db.getDao().upsertAll(listOf(
            _NoteEntity(
            id = dto.id,
            userId = dto.userId,
            userName = dto.userName,
            title = dto.title,
            content = dto.content,
            summarize = dto.summarize,
            sentiment = dto.sentiment,
            createdAt = dto.createdAt ?: ""
        )
        ))
        Unit
    }

    override suspend fun update(note: NoteEntity): Result<Unit> = runCatching {
        val dto = api.update(id = note.id, body = NoteRequest(title = note.title, content = note.content))

        db.getDao().upsertAll(listOf(_NoteEntity(
            id = dto.id,
            userId = dto.userId,
            userName = dto.userName,
            title = dto.title,
            content = dto.content,
            summarize = dto.summarize,
            sentiment = dto.sentiment,
            createdAt = dto.createdAt ?: ""
        )))
        Unit
    }

    override suspend fun delete(note: NoteEntity): Result<Unit> = runCatching {
        api.delete(note.id)
        db.getDao().deleteById(note.id)
    }
}

