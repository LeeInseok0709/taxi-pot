package com.example.taxipot.data.remote.api

import com.example.taxipot.data.remote.dto.chat.ChatMessagePageResponse
import com.example.taxipot.data.remote.dto.chat.ChatMessageRequest
import com.example.taxipot.data.remote.dto.chat.ChatMessageResponse
import com.example.taxipot.data.remote.dto.chat.QuickBlockResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** 채팅 API (로그인 필요 → 토큰 자동 첨부). 실시간 받기는 STOMP(PotRealtime) */
interface ChatApi {

    /** 채팅 메시지 조회 (최신순, cursor 보다 오래된 메시지) */
    @GET("api/pots/{potId}/messages")
    suspend fun getMessages(
        @Path("potId") potId: Long,
        @Query("cursor") cursor: Long? = null,
        @Query("size") size: Int? = null       // 1 ~ 100, 기본 30
    ): Response<ChatMessagePageResponse>

    /** 채팅·퀵블록 보내기 */
    @POST("api/pots/{potId}/messages")
    suspend fun sendMessage(
        @Path("potId") potId: Long,
        @Body request: ChatMessageRequest
    ): Response<ChatMessageResponse>

    /** 퀵블록 문구 목록 (서버가 관리하는 4개) */
    @GET("api/quick-blocks")
    suspend fun getQuickBlocks(): Response<List<QuickBlockResponse>>
}