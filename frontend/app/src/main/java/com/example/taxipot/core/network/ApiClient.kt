package com.example.taxipot.core.network

import android.content.Context
import com.example.taxipot.BuildConfig
import com.example.taxipot.data.local.TokenManager
import com.example.taxipot.data.remote.api.AuthApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit 생성 담당. TaxiPotApplication에서 init() 1회 호출.
 * 사용법: val api = ApiClient.create(PotApi::class.java)
 */
object ApiClient {

    val gson: Gson = GsonBuilder().create()

    lateinit var tokenManager: TokenManager
        private set

    private lateinit var retrofit: Retrofit

    fun init(context: Context) {
        tokenManager = TokenManager(context)
        runBlocking { tokenManager.load() }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }

        // 재발급 전용 클라이언트: 인증 헤더·Authenticator를 달지 않아 무한 반복을 막는다
        val refreshApi = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(OkHttpClient.Builder().addInterceptor(logging).build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(AuthApi::class.java)

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(DebtCheckInterceptor(gson))
            .addInterceptor(logging)
            .authenticator(TokenAuthenticator(tokenManager, refreshApi, gson))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun <T> create(service: Class<T>): T = retrofit.create(service)
}
