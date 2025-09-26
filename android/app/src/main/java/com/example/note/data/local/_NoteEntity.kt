package com.example.note.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "_NoteEntity")
data class _NoteEntity(
    @PrimaryKey val id: Int,
    val userId: String,
    val userName: String,
    val title: String,
    val content: String,
    val summarize: String?,
    val sentiment: Double?,
    val createdAt: String
)