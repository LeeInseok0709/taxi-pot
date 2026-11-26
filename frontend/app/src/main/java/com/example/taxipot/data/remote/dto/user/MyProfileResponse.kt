package com.example.taxipot.data.remote.dto.user

/**
 * 내 정보 (API 명세서 MyProfileResponse).
 * 날짜는 ISO 문자열, enum은 문자열로 받는다.
 */
data class MyProfileResponse(
    val id: Long,
    val email: String,
    val name: String,
    val nickname: String,
    val phone: String?,
    /** MALE / FEMALE */
    val gender: String?,
    val university: String?,
    val studentNo: String?,
    /** 학생 인증 완료 여부. false면 인증 화면으로 */
    val isVerified: Boolean?,
    val ratingAvg: Double?,
    val noShowCount: Int?,
    val reportCount: Int?,
    val cleanStreak: Int?,
    /** USER / ADMIN */
    val role: String?,
    /** ACTIVE / SUSPENDED / WITHDRAWN */
    val status: String?,
    /** SUSPENDED일 때만 값 있음. null이면 영구 정지 */
    val suspendedUntil: String?,
    val agreeMarketing: Boolean?,
    val createdAt: String?,
    val updatedAt: String?,
    /** 다음 정지까지 남은 신고 횟수 (영구 정지면 null) */
    val nextSuspensionIn: Int?
)
