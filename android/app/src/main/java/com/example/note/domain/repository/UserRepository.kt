package com.example.note.domain.repository

interface UserRepository {
    suspend fun isLoggedIn(): Boolean
    suspend fun signInWithGoogleIdToken(idToken: String)
    suspend fun updateDisplayName(username: String)
    suspend fun signOut()
    suspend fun currentIdToken(forceRefresh: Boolean): String?
    suspend fun deleteAccount(): Boolean
}