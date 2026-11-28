package com.example.taxipot.data.remote.dto.notification

/** 알림 한 건 */
data class NotificationResponse(
    val id: Long,
    /** 누르면 이동할 방 (없으면 null) */
    val potId: Long?,
    /** POT / PAYMENT / LATE_WARNING / KICKED / RATING_REQUEST / REPORT_RESULT / SYSTEM */
    val type: String,
    val title: String?,
    val content: String?,
    val isRead: Boolean,
    val createdAt: String?
)

/** GET /api/notifications/unread-count */
data class UnreadCountResponse(
    val unreadCount: Int
)
