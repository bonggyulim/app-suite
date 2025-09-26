package com.example.note.domain.repository

import com.example.note.domain.entity.AuthUser
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val authState: StateFlow<AuthUser?>
    fun isLoggedIn(): Boolean
    suspend fun signInWithGoogleIdToken(idToken: String)
    suspend fun updateDisplayName(username: String)
    fun signOut()
    suspend fun currentIdToken(forceRefresh: Boolean): String?
    suspend fun deleteAccount(): Boolean
}