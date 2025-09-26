package com.example.note.data.mapper

import com.example.note.data.local._NoteEntity
import com.example.note.data.remote.api.NoteDto
import com.example.note.data.remote.api.NoteRequest
import com.example.note.domain.entity.NoteEntity

// 도메인 -> 서버
fun NoteEntity.toRequest(): NoteRequest = NoteRequest(title = title, content = content)

// 서버 -> 도메인
fun NoteDto.toDomain(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    summarize = summarize ?: "",
    sentiment = sentiment ?: 0.0,
    createdAt = createdAt ?: ""
)

// 도메인 -> 로컬
private const val EPOCH = "1970-01-01T00:00:00Z"
fun NoteEntity.toLocal(): _NoteEntity = _NoteEntity(
    id = id,
    userId = userId,
    userName = userName,
    title = title,
    content = content,
    summarize = summarize.ifBlank { null },
    sentiment = sentiment.takeIf { it in 0.0..1.0 },
    createdAt = createdAt ?: EPOCH
)

// 로컬 -> 도메인
fun _NoteEntity.toDomain(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    summarize = summarize ?: "",
    sentiment = sentiment ?: 0.0,
    createdAt = createdAt
)
