package com.example.note.presentation.create

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNoteScreen(
    onCancel: () -> Unit,
    onCreated: () -> Unit,
    vm: CreateNoteViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val canSubmit = state.title.isNotBlank() && state.content.isNotBlank() && !state.isSubmitting

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("새 노트") },
                navigationIcon = {
                    TextButton(onClick = onCancel, enabled = !state.isSubmitting) {
                        Text("취소")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val ok = vm.create()
                                if (ok) onCreated()
                            }
                        },
                        enabled = canSubmit
                    ) {
                        Text(if (state.isSubmitting) "생성중…" else "생성")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.Top,
        ) {
            // 제목
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("제목") },
                singleLine = true,
                enabled = !state.isSubmitting,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            Spacer(Modifier.height(16.dp))

            // 내용
            OutlinedTextField(
                value = state.content,
                onValueChange = vm::onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp),
                label = { Text("내용") },
                enabled = !state.isSubmitting,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (canSubmit) {
                            scope.launch {
                                val ok = vm.create()
                                if (ok) onCreated()
                            }
                        }
                    }
                ),
                minLines = 8,
            )

            if (state.errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = state.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 하단 여백
            Spacer(Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = onCancel,
                    label = { Text("취소") },
                    enabled = !state.isSubmitting
                )
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val ok = vm.create()
                            if (ok) onCreated()
                        }
                    },
                    enabled = canSubmit
                ) { Text("생성") }
            }
        }
    }
}
