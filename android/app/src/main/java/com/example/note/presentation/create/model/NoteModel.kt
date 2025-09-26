package com.example.note.presentation.create.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NoteModel(
    val id: Int,
    val userId: String,
    val userName: String,
    val title: String,
    val content: String,
    val summarize: String,
    val sentiment: Double,
    val createdAt: String
): Parcelable