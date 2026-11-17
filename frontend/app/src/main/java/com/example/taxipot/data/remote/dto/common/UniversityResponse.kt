package com.example.taxipot.data.remote.dto.common

data class UniversityResponse(
    val id: Long,
    /** SignupRequest.university에 그대로 보낼 학교명 */
    val name: String,
    /** 학교 이메일의 '@' 뒤 도메인 */
    val emailDomain: String
)
