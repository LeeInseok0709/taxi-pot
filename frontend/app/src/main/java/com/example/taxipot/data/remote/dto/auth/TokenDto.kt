package com.example.taxipot.data.remote.dto.auth

data class RefreshRequest(
    val refreshToken: String
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    /** 액세스 토큰 만료까지 남은 초 */
    val expiresIn: Int
)
