package com.example.note.presentation.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

@Composable
fun LoginScreen(
    onGoogleSignedIn: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    var stage by remember { mutableStateOf("ready") } // ready / pick_name
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }

    // 구글 런처는 그대로
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        loading = false
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching { task.result }.onSuccess { account ->
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                error = "Google ID Token is null"
            } else {
                loading = true
                vm.signInWithGoogleIdToken(
                    idToken,
                    onSuccess = { loading = false; stage = "pick_name" },
                    onError = { t -> loading = false; error = t.message }
                )
            }
        }.onFailure { t -> error = t.message }
    }

    fun launchGoogle() {
        error = null
        loading = true
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(ctx.getString(com.example.note.R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(ctx, gso)
        launcher.launch(client.signInIntent)
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (stage) {
            "ready" -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = ::launchGoogle, enabled = !loading) {
                    Text(if (loading) "로그인 중..." else "구글로 계속하기")
                }
                Spacer(Modifier.height(12.dp))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            "pick_name" -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text("닉네임을 입력하세요")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = username, onValueChange = { username = it },
                    singleLine = true, label = { Text("Username") }
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        loading = true
                        vm.updateDisplayName(
                            username.trim(),
                            onSuccess = { loading = false; onGoogleSignedIn() },
                            onError = { t -> loading = false; error = t.message }
                        )
                    },
                    enabled = username.isNotBlank() && !loading
                ) { Text(if (loading) "저장 중..." else "시작하기") }
                Spacer(Modifier.height(8.dp))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
