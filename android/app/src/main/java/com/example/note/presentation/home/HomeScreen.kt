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


@Composable
fun HomeScreen(
    onClickCreate: () -> Unit,
    onClickItem: (NoteEntity) -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val paging = vm.notes.collectAsLazyPagingItems()
    val refreshing = paging.loadState.refresh is LoadState.Loading
    val swipe = rememberSwipeRefreshState(isRefreshing = refreshing)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onClickCreate) { Text("+") }
        }
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
                            if (note != null) NoteRow(note, onClickItem)
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

@Composable private fun NoteRow(
    note: NoteEntity,
    onClick: (NoteEntity) -> Unit,
    onEdit: (NoteEntity) -> Unit = {},
    onDelete: (NoteEntity) -> Unit = {}
) {
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
            Text(note.content, style = MaterialTheme.typography.bodyMedium, maxLines = 3)

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onEdit(note) }) { Text("수정") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { onDelete(note) }) { Text("삭제") }
            }
        }
    }
}

@Composable private fun EmptyView(onClickCreate: () -> Unit) {
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

@Composable private fun LoadingRow() {
    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
@Composable private fun RetryRow(onRetry: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        OutlinedButton(onClick = onRetry) { Text("더 불러오기 실패, 다시 시도") }
    }
}
@Composable private fun ErrorView(message: String, onRetry: () -> Unit) {
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
