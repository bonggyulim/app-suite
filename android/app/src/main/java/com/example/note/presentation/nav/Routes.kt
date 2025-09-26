package com.example.note.presentation.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.note.presentation.auth.LoginScreen
import com.example.note.presentation.home.HomeScreen
import com.example.note.presentation.create.CreateNoteScreen
import com.google.firebase.auth.FirebaseAuth

object Routes {
    const val Splash = "splash"
    const val Login  = "login"
    const val Home   = "home"
    const val Create = "create"
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = Routes.Splash
    ) {
        // 0) 스플래시/라우터: 로그인 여부에 따라 분기
        composable(Routes.Splash) {
            SplashRouter(
                onResult = { loggedIn ->
                    if (loggedIn) nav.navigateAndClear(Routes.Home)
                    else nav.navigateAndClear(Routes.Login)
                }
            )
        }

        // 1) 로그인 화면 (구글 로그인 후 onSignedIn 호출)
        composable(Routes.Login) {
            LoginScreen(
                onGoogleSignedIn = { nav.navigateAndClear(Routes.Home) }
            )
        }

        // 2) 홈 화면: 여기서 HomeScreen 붙임
        composable(Routes.Home) {
            HomeScreen(
                onClickCreate = { nav.navigate(Routes.Create) },
                onClickItem   = { note ->
                    // 상세 화면이 생기면 여기서 navigate("detail/${note.id}")
                }
            )
        }

        // 3) 생성 화면
        composable(Routes.Create) {
            CreateNoteScreen(
                onCancel  = { nav.popBackStack() },
                onCreated = { nav.popBackStack() } // 생성 성공 후 홈으로 복귀
            )
        }
    }
}

@Composable
private fun SplashRouter(onResult: (Boolean) -> Unit) {
    LaunchedEffect(Unit) {
        val loggedIn = FirebaseAuth.getInstance().currentUser != null
        onResult(loggedIn)
    }
    // 로고/로딩 UI 원하면 여기 그려도 됨
}

// 편의 확장: 그래프 시작지점까지 클리어하고 이동
private fun NavController.navigateAndClear(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { inclusive = true }
        launchSingleTop = true
    }
}
