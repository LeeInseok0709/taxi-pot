package com.example.taxipot.data.remote.dto.auth

import com.example.taxipot.data.remote.dto.user.MyProfileResponse

/** POST /api/auth/login 요청 */
data class LoginRequest(
    val email: String,
    val password: String,
    /** 로그인 기기의 FCM 토큰 (FCM 연동 전까지 null) */
    val fcmToken: String? = null
)

/** 로그인·가입 공용 응답: 토큰 + 내 정보 */
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Int,
    val user: MyProfileResponse
)
