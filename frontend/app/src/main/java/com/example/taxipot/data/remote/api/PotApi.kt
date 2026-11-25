package com.example.taxipot.data.remote.api

import com.example.taxipot.data.remote.dto.common.PageResponse
import com.example.taxipot.data.remote.dto.pot.PotCreateRequest
import com.example.taxipot.data.remote.dto.pot.PotDetailResponse
import com.example.taxipot.data.remote.dto.pot.PotSummaryResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** 택시팟 API (로그인 필요 → 토큰 자동 첨부) */
interface PotApi {

    /**
     * 택시팟 목록 (GET /api/pots)
     * 값이 null인 조건은 서버로 보내지 않음
     */
    @GET("api/pots")
    suspend fun getPots(
        @Query("lat") lat: Double? = null,                    // 기준 위도 (거리 계산·거리순에 필요)
        @Query("lng") lng: Double? = null,                    // 기준 경도
        @Query("radius") radius: Int? = null,                 // 반경 (m)
        @Query("departureName") departureName: String? = null,
        @Query("destinationName") destinationName: String? = null,
        @Query("from") from: String? = null,                  // 출발 시각 시작
        @Query("to") to: String? = null,                      // 출발 시각 끝
        @Query("genderOption") genderOption: String? = null,  // ALL / MALE / FEMALE
        @Query("minHostRating") minHostRating: Double? = null,// 방장 최소 평점
        @Query("sort") sort: String? = null,                  // DEPARTURE_TIME / DISTANCE
        @Query("page") page: Int? = null,                     // 0부터
        @Query("size") size: Int? = null                      // 1 ~ 100
    ): Response<PageResponse<PotSummaryResponse>>

    /** 택시팟 상세 (GET /api/pots/{potId}) */
    @GET("api/pots/{potId}")
    suspend fun getPot(@Path("potId") potId: Long): Response<PotDetailResponse>

    /** 방 만들기 (POST /api/pots) — 성공하면 만든 방 상세가 옴, 방장 예치금도 이때 빠짐 */
    @POST("api/pots")
    suspend fun createPot(@Body request: PotCreateRequest): Response<PotDetailResponse>
}