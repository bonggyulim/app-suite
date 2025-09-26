package com.example.note.di

import com.example.note.data.remote.api.NoteApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides @Singleton
    fun provideNoteApi(retrofit: Retrofit): NoteApi =
        retrofit.create(NoteApi::class.java)
}
