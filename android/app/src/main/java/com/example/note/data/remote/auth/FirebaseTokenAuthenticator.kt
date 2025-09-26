package com.example.note.data.remote.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/** 401 발생 시 토큰 재발급 후 1회 재시도 */
class FirebaseTokenAuthenticator(
    private val tokenProvider: AuthTokenProvider
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // Authorization 헤더 없는 요청은 재시도 안 함
        if (response.request.header("Authorization") == null) return null

        val newToken = runBlocking { tokenProvider.currentIdToken(true) } ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }
}
