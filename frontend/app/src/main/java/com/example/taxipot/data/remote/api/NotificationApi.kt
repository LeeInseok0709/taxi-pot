package com.example.taxipot.data.remote.api

import com.example.taxipot.data.remote.dto.common.PageResponse
import com.example.taxipot.data.remote.dto.notification.NotificationResponse
import com.example.taxipot.data.remote.dto.notification.UnreadCountResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

/** 알림 API (로그인 토큰 필요) */
interface NotificationApi {

    @GET("api/notifications")
    suspend fun getNotifications(
        @Query("unreadOnly") unreadOnly: Boolean = false,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<NotificationResponse>>

    @GET("api/notifications/unread-count")
    suspend fun getUnreadCount(): Response<UnreadCountResponse>

    /** 읽음 처리 (204) */
    @PATCH("api/notifications/{notificationId}/read")
    suspend fun markRead(@Path("notificationId") notificationId: Long): Response<Unit>

    /** 모두 읽음 (204) */
    @PATCH("api/notifications/read-all")
    suspend fun markAllRead(): Response<Unit>
}
