package com.example.note.presentation.create.model

import com.example.note.domain.entity.NoteEntity

fun NoteEntity.toModel(): NoteModel {
    return NoteModel(
        id,
        userId,
        userName,
        title,
        content,
        summarize,
        sentiment,
        createdAt ?: ""
    )
}

fun NoteModel.toEntity(): NoteEntity {
    return NoteEntity(
        title = title,
        content = content
    )
}
