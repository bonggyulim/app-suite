package com.example.note.data.repository

import com.example.note.domain.entity.AuthUser
import com.example.note.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : UserRepository {

    private val _authState = MutableStateFlow<AuthUser?>(null)
    override val authState: StateFlow<AuthUser?> = _authState.asStateFlow()

    private val listener = FirebaseAuth.AuthStateListener { fb ->
        val u = fb.currentUser
        _authState.value = u?.let { AuthUser(it.uid, it.displayName, it.email) }
    }

    init {
        auth.addAuthStateListener(listener)
    }

    override fun isLoggedIn(): Boolean = auth.currentUser != null

    override suspend fun signInWithGoogleIdToken(idToken: String) {
        val cred = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(cred).await()
        // 성공 시 listener가 authState 갱신
    }

    override suspend fun updateDisplayName(username: String) {
        val user = auth.currentUser ?: error("Not logged in")
        val updates = UserProfileChangeRequest.Builder()
            .setDisplayName(username)
            .build()
        user.updateProfile(updates).await()
        // 프로필 반영은 listener가 다음 토큰 갱신 시점 또는 즉시 반영될 수 있음
        _authState.value = AuthUser(user.uid, username, user.email)
    }

    override suspend fun currentIdToken(forceRefresh: Boolean): String? =
        auth.currentUser?.getIdToken(forceRefresh)?.await()?.token

    override fun signOut() {
        auth.signOut()
        // listener가 null로 내려줌
    }

    override suspend fun deleteAccount(): Boolean {
        val u = auth.currentUser ?: return false
        try {
            u.delete().await()
            return true
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            throw e
        }
    }
}