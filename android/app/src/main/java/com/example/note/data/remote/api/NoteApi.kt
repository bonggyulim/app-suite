package com.example.note.data.remote.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.Response
import retrofit2.http.Query

interface NoteApi {
    @GET("notes")
    suspend fun listNotes(
        @Query("limit") limit: Int = 10,
        @Query("cursor") cursor: String? = null,
        @Query("order") order: String = "desc"
    ): PagedNotesDto

    @GET("notes/{id}") suspend fun get(@Path("id") id: Int): NoteDto
    @POST("notes") suspend fun create(@Body body: NoteRequest): NoteDto
    @PUT("notes/{id}") suspend fun update(@Path("id") id: Int, @Body body: NoteRequest): NoteDto
    @DELETE("notes/{id}") suspend fun delete(@Path("id") id: Int): Response<Unit>
}
