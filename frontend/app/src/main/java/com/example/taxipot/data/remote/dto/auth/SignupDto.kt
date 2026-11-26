package com.example.taxipot.data.remote.dto.auth

/** POST /api/auth/signup 요청 (응답은 로그인과 같은 LoginResponse) */
data class SignupRequest(
    val email: String,
    /** 8자 이상 */
    val password: String,
    val name: String,
    /** 30자 이하, '탈퇴회원_'으로 시작 불가 */
    val nickname: String,
    /** 하이픈 있어도 됨 (서버가 숫자만 남김) */
    val phone: String,
    /** MALE / FEMALE */
    val gender: String,
    /** GET /api/universities 응답의 name. 이메일 도메인이 이 학교 emailDomain과 같아야 함 */
    val university: String,
    val studentNo: String,
    /** GET /api/banks 응답의 code */
    val bankCode: String,
    /** 숫자 10~14자리 (하이픈 허용) */
    val accountNumber: String,
    /** 예금주: name과 같아야 함 */
    val accountHolder: String,
    /** 필수 약관 동의 (true만 허용) */
    val agreeRequired: Boolean,
    val agreeMarketing: Boolean = false
)

/** GET /api/auth/nickname-check 응답 */
data class AvailabilityResponse(
    val available: Boolean,
    /** 사용 불가 사유: DUPLICATE(중복) / RESERVED('탈퇴회원_'으로 시작). 사용 가능하면 null */
    val reason: String?
)
