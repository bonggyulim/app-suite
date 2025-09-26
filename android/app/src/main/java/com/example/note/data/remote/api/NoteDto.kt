package com.example.note.data.remote.api

import com.google.gson.annotations.SerializedName

data class NoteDto(
    val id: Int,
    val userId: String,
    val userName: String,
    val title: String,
    val content: String,
    val summarize: String?,
    val sentiment: Double?,
    val createdAt: String?
)

data class PagedNotesDto(
    val items: List<NoteDto>,
    @SerializedName("next_cursor") val nextCursor: String?,
    @SerializedName("has_more") val hasMore: Boolean
)