package com.example.note.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.note.presentation.auth.AuthViewModel
import com.example.note.presentation.auth.LoginScreen
import com.example.note.presentation.create.CreateNoteScreen
import com.example.note.presentation.home.HomeScreen
import com.example.note.presentation.nav.Routes
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val nav = rememberNavController()
            val authVm: AuthViewModel = hiltViewModel()
            val authState by authVm.state.collectAsState() // ← isLoggedIn 여기서 사용

            MaterialTheme {
                NavHost(navController = nav, startDestination = Routes.Splash) {

                    // 1) 스플래시: isLoggedIn 변화를 관찰해서 단방향 라우팅
                    composable(Routes.Splash) {
                        LaunchedEffect(authState.isLoggedIn) {
                            if (authState.isLoggedIn) {
                                nav.navigate(Routes.Home) {
                                    popUpTo(Routes.Splash) { inclusive = true }
                                }
                            } else {
                                nav.navigate(Routes.Login) {
                                    popUpTo(Routes.Splash) { inclusive = true }
                                }
                            }
                        }
                        // 간단한 로딩 UI (선택)
                        Text("Loading…")
                    }

                    // 2) 로그인 화면: 구글 로그인 + 같은 화면에서 username 입력/저장 후 바로 홈 이동
                    composable(Routes.Login) {
                        LoginScreen(
                            onGoogleSignedIn = {
                                nav.navigate(Routes.Home) {
                                    popUpTo(Routes.Login) { inclusive = true }
                                }
                            }
                        )
                    }

                    // 3) 홈 (최신순 10개 페이징 + 무한스크롤)
                    composable(Routes.Home) {
                        HomeScreen(
                            onClickCreate = { nav.navigate(Routes.Create) },
                            onClickItem = { /* 상세/수정 이동 필요 시 */ }
                        )
                    }

                    // 4) 생성 화면
                    composable(Routes.Create) {
                        CreateNoteScreen(
                            onCancel = { nav.popBackStack() },
                            onCreated = { nav.popBackStack() } // 성공 후 홈으로 복귀
                        )
                    }
                }
            }
        }
    }
}
