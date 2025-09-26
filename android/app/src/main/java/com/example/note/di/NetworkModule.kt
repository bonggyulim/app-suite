package com.example.note.di

import com.example.note.data.remote.auth.AuthTokenProvider
import com.example.note.data.remote.auth.FirebaseIdTokenInterceptor
import com.example.note.data.remote.auth.FirebaseTokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** BaseUrl 주입: 개발 환경에 맞춰 교체 (에뮬레이터=10.0.2.2) */
    @Provides @Singleton @Named("BaseUrl")
    fun provideBaseUrl(): String = "http://192.168.219.102:8080/"

    @Provides @Singleton @Named("Auth")
    fun provideAuthInterceptor(
        tokenProvider: AuthTokenProvider
    ): Interceptor = FirebaseIdTokenInterceptor(tokenProvider)

    @Provides @Singleton
    fun provideAuthenticator(
        tokenProvider: AuthTokenProvider
    ): FirebaseTokenAuthenticator = FirebaseTokenAuthenticator(tokenProvider)

    // --- 로깅 ---
    @Provides @Singleton @Named("Logging")
    fun provideLoggingInterceptor(): Interceptor =
        HttpLoggingInterceptor().apply {
            // 프로덕션에서는 Level.BASIC 이하 권장
            level = HttpLoggingInterceptor.Level.BODY
            // 민감 헤더는 마스킹
            redactHeader("Authorization")
        }

    // --- OkHttp ---
    @Provides @Singleton
    fun provideOkHttpClient(
        @Named("Logging") logging: Interceptor,
        @Named("Auth") auth: Interceptor,
        authenticator: FirebaseTokenAuthenticator
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(auth)         // 1) 토큰 주입
            .addInterceptor(logging)      // 2) 로깅(마지막에)
            .authenticator(authenticator) // 401 시 재발급 재시도
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

    // --- Retrofit ---
    @Provides @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        @Named("BaseUrl") baseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}
