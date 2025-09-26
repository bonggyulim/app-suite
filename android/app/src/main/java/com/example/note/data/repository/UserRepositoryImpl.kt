package com.example.note.data.repository

import com.example.note.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
) : UserRepository {

    override suspend fun isLoggedIn(): Boolean = auth.currentUser != null

    override suspend fun signInWithGoogleIdToken(idToken: String) {
        val cred = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(cred).await()
    }

    override suspend fun updateDisplayName(username: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: error("Not logged in")

        val updates = UserProfileChangeRequest.Builder()
            .setDisplayName(username)
            .build()

        user.updateProfile(updates).await()
    }

    override suspend fun currentIdToken(forceRefresh: Boolean): String? =
        auth.currentUser?.getIdToken(forceRefresh)?.await()?.token

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun deleteAccount(): Boolean {
        val u = auth.currentUser ?: return false
        u.delete().await()
        return true
    }
}