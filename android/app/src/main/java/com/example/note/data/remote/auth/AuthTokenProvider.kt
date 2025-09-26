package com.example.note.data.remote.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthTokenProvider(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun currentIdToken(forceRefresh: Boolean = false): String? =
        auth.currentUser?.getIdToken(forceRefresh)?.await()?.token
}
