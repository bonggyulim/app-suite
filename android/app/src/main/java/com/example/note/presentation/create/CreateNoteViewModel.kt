package com.example.note.presentation.create

import androidx.lifecycle.ViewModel
import com.example.note.domain.entity.NoteEntity
import com.example.note.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class CreateNoteUiState(
    val title: String = "",
    val content: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)

class CreateNoteViewModel @Inject constructor(
    private val repo: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateNoteUiState())
    val uiState = _uiState.asStateFlow()

    fun onTitleChange(v: String) {
        _uiState.value = _uiState.value.copy(title = v, errorMessage = null)
    }

    fun onContentChange(v: String) {
        _uiState.value = _uiState.value.copy(content = v, errorMessage = null)
    }

    suspend fun create(): Boolean {
        val s = _uiState.value
        if (s.title.isBlank() || s.content.isBlank() || s.isSubmitting) return false

        _uiState.value = s.copy(isSubmitting = true, errorMessage = null)

        return try {
            // 🔽 레포 시그니처에 맞춰 호출
            // 예1) repo.create(title, content)
            // 예2) repo.create(NoteRequest(title, content))
            // 예3) repo.createAndReturnId(title, content)

            repo.create(NoteEntity(title = s.title.trim(), content = s.content.trim()))
            _uiState.value = _uiState.value.copy(isSubmitting = false)
            true
        } catch (t: Throwable) {
            _uiState.value = _uiState.value.copy(
                isSubmitting = false,
                errorMessage = t.message ?: "생성에 실패했습니다"
            )
            false
        }
    }
}
