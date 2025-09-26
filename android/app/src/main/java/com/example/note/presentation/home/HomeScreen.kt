package com.example.note.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.note.domain.entity.NoteEntity
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onClickCreate: () -> Unit,
    onClickItem: (NoteEntity) -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val paging = vm.notes.collectAsLazyPagingItems()
    val refreshing = paging.loadState.refresh is LoadState.Loading
    val swipe = rememberSwipeRefreshState(isRefreshing = refreshing)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onClickCreate) { Text("+") }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        SwipeRefresh(
            state = swipe,
            onRefresh = { paging.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                paging.loadState.refresh is LoadState.Error -> {
                    val e = (paging.loadState.refresh as LoadState.Error).error
                    ErrorView(message = e.message ?: "에러", onRetry = { paging.retry() })
                }
                paging.itemCount == 0 && !refreshing -> EmptyView(onClickCreate)
                else -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(paging.itemCount) { index ->
                            val note = paging[index]
                            if (note != null) {
                                NoteRow(
                                    note = note,
                                    onClick = onClickItem,
                                    onEdit = { edited ->
                                        vm.update(edited)
                                    },
                                    onDelete = { toDelete ->
                                        vm.delete(toDelete)
                                    }
                                )
                            }
                        }
                        item {
                            when (paging.loadState.append) {
                                is LoadState.Loading -> LoadingRow()
                                is LoadState.Error -> RetryRow { paging.retry() }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteRow(
    note: NoteEntity,
    onClick: (NoteEntity) -> Unit,
    onEdit: (NoteEntity) -> Unit = {},
    onDelete: (NoteEntity) -> Unit = {},
) {
    var showEdit by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 편집 다이얼로그용 로컬 상태 (아이템이 바뀌면 원본으로 리셋)
    var editTitle by remember(note.id) { mutableStateOf(note.title) }
    var editContent by remember(note.id) { mutableStateOf(note.content) }

    Card(
        onClick = { onClick(note) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("#${note.id}", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(note.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )

            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = {
                    // 기존 값으로 초기화하고 다이얼로그 오픈
                    editTitle = note.title
                    editContent = note.content
                    showEdit = true
                }) { Text("수정") }

                Spacer(Modifier.width(8.dp))

                OutlinedButton(onClick = { showDeleteConfirm = true }) { Text("삭제") }
            }
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("메모 삭제") },
            text = { Text("정말로 이 메모를 삭제할까요? 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(note)
                }) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("취소") }
            }
        )
    }

    // 수정 다이얼로그
    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("메모 수정") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        singleLine = true,
                        label = { Text("제목") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("내용") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                val canSave = editTitle.isNotBlank() && editContent.isNotBlank()
                TextButton(
                    enabled = canSave,
                    onClick = {
                        showEdit = false
                        onEdit(
                            note.copy(
                                title = editTitle.trim(),
                                content = editContent.trim()
                            )
                        )
                    }
                ) { Text("저장") }
            },
            dismissButton = {
                TextButton(onClick = { showEdit = false }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun EmptyView(onClickCreate: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("노트가 없습니다.")
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onClickCreate) { Text("새 노트 작성") }
    }
}

@Composable
private fun LoadingRow() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) { CircularProgressIndicator() }
}

@Composable
private fun RetryRow(onRetry: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) { OutlinedButton(onClick = onRetry) { Text("더 불러오기 실패, 다시 시도") } }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRetry) { Text("다시 시도") }
    }
}
