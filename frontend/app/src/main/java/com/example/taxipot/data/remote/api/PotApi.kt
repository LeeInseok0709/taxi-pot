package com.example.taxipot.data.remote.api

import com.example.taxipot.data.remote.dto.common.PageResponse
import com.example.taxipot.data.remote.dto.pot.JoinRequestCreateRequest
import com.example.taxipot.data.remote.dto.pot.JoinRequestResponse
import com.example.taxipot.data.remote.dto.pot.PotArrivalRequest
import com.example.taxipot.data.remote.dto.pot.PotArrivalResponse
import com.example.taxipot.data.remote.dto.pot.PotCreateRequest
import com.example.taxipot.data.remote.dto.pot.PotDetailResponse
import com.example.taxipot.data.remote.dto.pot.PotJoinRequest
import com.example.taxipot.data.remote.dto.pot.PotJoinResponse
import com.example.taxipot.data.remote.dto.pot.PotSummaryResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.PATCH
import com.example.taxipot.data.remote.dto.pot.PotUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
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

    /** 방 수정 (PATCH /api/pots/{potId}, 방장) — 제목·오픈채팅 링크만 */
    @PATCH("api/pots/{potId}")
    suspend fun updatePot(
        @Path("potId") potId: Long,
        @Body request: PotUpdateRequest
    ): Response<PotDetailResponse>

    /** 방 만들기 (POST /api/pots) — 성공하면 만든 방 상세가 옴, 방장 예치금도 이때 빠짐 */
    @POST("api/pots")
    suspend fun createPot(@Body request: PotCreateRequest): Response<PotDetailResponse>

    /** 택시팟 참여 (POST /api/pots/{potId}/members) — 참여하면서 예치금이 빠짐 */
    @POST("api/pots/{potId}/members")
    suspend fun join(
        @Path("potId") potId: Long,
        @Body request: PotJoinRequest
    ): Response<PotJoinResponse>

    // ===== 대기실 (RD-01) =====

    /** 도착 확인 (POST /api/pots/{potId}/members/me/arrival) — 내 GPS 위치, 출발지 50m 안 */
    @POST("api/pots/{potId}/members/me/arrival")
    suspend fun arrive(
        @Path("potId") potId: Long,
        @Body request: PotArrivalRequest
    ): Response<PotArrivalResponse>

    /** 탑승 시작 (POST /api/pots/{potId}/start, 방장) — 전원 도착 + 2명 이상 */
    @POST("api/pots/{potId}/start")
    suspend fun start(@Path("potId") potId: Long): Response<PotDetailResponse>

    /** 미도착자 내보내기 (POST /api/pots/{potId}/members/{userId}/kick, 방장) — 204 */
    @POST("api/pots/{potId}/members/{userId}/kick")
    suspend fun kick(
        @Path("potId") potId: Long,
        @Path("userId") userId: Long
    ): Response<Unit>

    /** 방 취소 (DELETE /api/pots/{potId}, 방장) — 204 */
    @DELETE("api/pots/{potId}")
    suspend fun cancelPot(@Path("potId") potId: Long): Response<Unit>

    /** 참여 취소 (DELETE /api/pots/{potId}/members/me, 동승자) — 204 */
    @DELETE("api/pots/{potId}/members/me")
    suspend fun leave(@Path("potId") potId: Long): Response<Unit>

    // ===== 입장 요청 (RD-03) =====

    /** 입장 요청 보내기 (참여 마감 후, 출발지 50m 안) — 예치는 방장이 수락할 때 */
    @POST("api/pots/{potId}/join-requests")
    suspend fun requestJoin(
        @Path("potId") potId: Long,
        @Body request: JoinRequestCreateRequest
    ): Response<JoinRequestResponse>

    /** 입장 요청 대기 목록 (방장 전용, 요청 시각 순) */
    @GET("api/pots/{potId}/join-requests")
    suspend fun getJoinRequests(@Path("potId") potId: Long): Response<List<JoinRequestResponse>>

    /** 내 입장 요청 철회 — 204 */
    @DELETE("api/pots/{potId}/join-requests/me")
    suspend fun withdrawJoinRequest(@Path("potId") potId: Long): Response<Unit>

    /** 입장 요청 수락 (방장) — 요청자 예치 + 바로 도착 처리, 요청자의 참여 결과가 옴 */
    @POST("api/pots/{potId}/join-requests/{userId}/accept")
    suspend fun acceptJoinRequest(
        @Path("potId") potId: Long,
        @Path("userId") userId: Long
    ): Response<PotJoinResponse>

    /** 입장 요청 거절 (방장) — 204, 거절된 사람은 같은 방에 다시 요청 불가 */
    @POST("api/pots/{potId}/join-requests/{userId}/reject")
    suspend fun rejectJoinRequest(
        @Path("potId") potId: Long,
        @Path("userId") userId: Long
    ): Response<Unit>

    // ===== 실요금 청구 (RD-05) =====

    /** 실요금 청구 (방장) — 결제내역 사진 + 실제 요금, 정산은 운영자 승인 때 */
    @Multipart
    @POST("api/pots/{potId}/fare-claim")
    suspend fun claimFare(
        @Path("potId") potId: Long,
        @Part("actualFare") actualFare: RequestBody,
        @Part receiptImage: MultipartBody.Part
    ): Response<Unit>
}