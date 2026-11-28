package com.example.taxipot.data.remote.dto.user

/** PUT /api/users/me/fcm-token (빈 문자열 불가, 255자 이하) */
data class FcmTokenRequest(
    val fcmToken: String
)
