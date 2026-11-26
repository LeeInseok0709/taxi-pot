package com.example.taxipot.data.remote.api

import com.example.taxipot.BuildConfig
import com.example.taxipot.data.remote.dto.kakao.KakaoAddressResponse
import com.example.taxipot.data.remote.dto.kakao.KakaoDirectionsResponse
import com.example.taxipot.data.remote.dto.kakao.KakaoPlaceResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/** 카카오 로컬 API (dapi.kakao.com) — 장소 검색, 좌표 → 주소 */
interface KakaoLocalApi {

    /** 키워드로 장소 검색 (x = 경도, y = 위도 기준으로 가까운 곳 위주) */
    @GET("v2/local/search/keyword.json")
    suspend fun searchKeyword(
        @Query("query") query: String,
        @Query("x") x: Double? = null,
        @Query("y") y: Double? = null,
        @Query("radius") radius: Int? = null,   // 반경 m (최대 20,000)
        @Query("size") size: Int? = null        // 결과 개수 (1 ~ 15)
    ): Response<KakaoPlaceResponse>

    /** 좌표 → 주소 */
    @GET("v2/local/geo/coord2address.json")
    suspend fun coordToAddress(
        @Query("x") x: Double,
        @Query("y") y: Double
    ): Response<KakaoAddressResponse>
}

/** 카카오모빌리티 길찾기 API (apis-navi.kakaomobility.com) — 예상 택시 요금 */
interface KakaoNaviApi {

    /** 자동차 길찾기 (origin, destination = "경도,위도") */
    @GET("v1/directions")
    suspend fun directions(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("summary") summary: Boolean = true    // 요약 정보만 받기
    ): Response<KakaoDirectionsResponse>
}

/**
 * 카카오 전용 Retrofit
 * 우리 서버(ApiClient)와 완전히 분리: 우리 서버 토큰을 붙이지 않고, 401 재발급도 돌지 않음
 * 헤더는 Authorization: KakaoAK {REST 키} 하나만
 */
object KakaoApiClient {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Authorization", "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}")
                        .build()
                )
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private fun retrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val local: KakaoLocalApi by lazy {
        retrofit("https://dapi.kakao.com/").create(KakaoLocalApi::class.java)
    }

    val navi: KakaoNaviApi by lazy {
        retrofit("https://apis-navi.kakaomobility.com/").create(KakaoNaviApi::class.java)
    }
}