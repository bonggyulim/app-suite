package com.example.note.presentation.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.note.domain.repository.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: UserRepository
) : ViewModel() {

    data class AuthState(
        val uid: String? = null,
        val displayName: String? = null,
        val email: String? = null,
        val isLoggedIn: Boolean = false,
        val idToken: String? = null,
        val loading: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        // 레포가 내보내는 authState를 관찰하여 UI 상태 갱신
        viewModelScope.launch {
            repo.authState.collect { u ->
                _state.update {
                    it.copy(
                        uid = u?.uid,
                        displayName = u?.displayName,
                        email = u?.email,
                        isLoggedIn = (u != null)
                    )
                }
            }
        }
    }

    private fun userMessage(t: Throwable): String =
        when (t) {
            is ApiException -> "구글 로그인 실패: ${GoogleSignInStatusCodes.getStatusCodeString(t.statusCode)} (${t.statusCode})"
            else -> t.localizedMessage ?: "알 수 없는 오류가 발생했어요"
        }

    private fun postError(t: Throwable) {
        val msg = userMessage(t)
        Log.e("AuthViewModel", msg, t)
        _state.update { it.copy(error = msg, loading = false) }
    }

    fun clearError() = _state.update { it.copy(error = null) }
    private fun setLoading(v: Boolean) = _state.update { it.copy(loading = v) }

    fun signInWithGoogleIdToken(
        idToken: String,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            setLoading(true)
            runCatching { repo.signInWithGoogleIdToken(idToken) }
                .onSuccess {
                    val token = runCatching { repo.currentIdToken(true) }.getOrNull()
                    _state.update { it.copy(idToken = token, loading = false) }
                    onSuccess()
                }
                .onFailure { e -> postError(e); onError(e) }
        }
    }

    fun updateDisplayName(
        username: String,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            setLoading(true)
            runCatching {
                repo.updateDisplayName(username)
                repo.currentIdToken(true)
            }.onSuccess { token ->
                _state.update { it.copy(displayName = username, idToken = token, loading = false) }
                onSuccess()
            }.onFailure { e ->
                postError(e); onError(e)
            }
        }
    }
}
