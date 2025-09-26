package com.example.note.presentation.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    data class AuthState(
        val uid: String? = null,
        val displayName: String? = null,
        val email: String? = null,
        val isLoggedIn: Boolean = false,
        val idToken: String? = null
    )

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val listener = FirebaseAuth.AuthStateListener { fb ->
        val u = fb.currentUser
        _state.value = _state.value.copy(
            uid = u?.uid,
            displayName = u?.displayName,
            email = u?.email,
            isLoggedIn = (u != null)
        )
    }

    init { auth.addAuthStateListener(listener) }
    override fun onCleared() { auth.removeAuthStateListener(listener); super.onCleared() }

    // 구글 ID 토큰으로 파이어베이스 로그인
    fun signInWithGoogleIdToken(
        idToken: String,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val cred = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(cred).addOnCompleteListener { task ->
            if (task.isSuccessful) onSuccess() else onError(task.exception ?: RuntimeException("signIn failed"))
        }
    }

    // displayName 업데이트 (username 저장)
    fun updateDisplayName(
        username: String,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val u = auth.currentUser ?: return onError(IllegalStateException("Not logged in"))
        val req = userProfileChangeRequest { displayName = username }
        u.updateProfile(req).addOnCompleteListener { t ->
            if (t.isSuccessful) {
                // 최신 토큰 받아서 상태에 보관 (서버 Authorization에 사용)
                u.getIdToken(true).addOnSuccessListener { result ->
                    _state.value = _state.value.copy(displayName = username, idToken = result.token)
                    onSuccess()
                }.addOnFailureListener(onError)
            } else onError(t.exception ?: RuntimeException("update profile failed"))
        }
    }

    suspend fun forceRefreshIdToken(): String? =
        auth.currentUser?.getIdToken(true)?.await()?.token

    fun signOut() { auth.signOut() }
}
