package com.example.note.presentation.home

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.note.domain.entity.NoteEntity
import com.example.note.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    repo: NoteRepository
) : ViewModel() {
    val notes: Flow<PagingData<NoteEntity>> =
        repo.pager().cachedIn(viewModelScope)
}