package com.example.note.domain.entity

data class NoteEntity(
    val id: Int = 0,
    val userId: String = "",
    val userName: String = "",
    val title: String = "",
    val content: String = "",
    val summarize: String = "",
    val sentiment: Double = 0.5,
    val createdAt: String? = ""
)