package com.example.note.presentation.home

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.note.domain.entity.NoteEntity
import com.example.note.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: NoteRepository
) : ViewModel() {

    val notes: Flow<PagingData<NoteEntity>> =
        repo.pager().cachedIn(viewModelScope)

    private val _opError = MutableStateFlow<String?>(null)
    val opError: StateFlow<String?> = _opError

    fun clearError() { _opError.value = null }

    fun delete(note: NoteEntity) = viewModelScope.launch {
        val r = repo.delete(note)
        if (r.isFailure) {
            _opError.value = r.exceptionOrNull()?.message ?: "삭제에 실패했어요"
        }
    }

    fun update(note: NoteEntity) = viewModelScope.launch {
        val r = repo.update(note)
        if (r.isFailure) {
            _opError.value = r.exceptionOrNull()?.message ?: "수정에 실패했어요"
        }
    }
}