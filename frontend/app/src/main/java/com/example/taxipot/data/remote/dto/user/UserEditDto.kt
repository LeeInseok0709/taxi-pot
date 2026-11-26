package com.example.taxipot.data.remote.dto.user

/** PATCH /api/users/me 바꿀 것만 넣는다 (null이면 그대로) */
data class UserUpdateRequest(
    val nickname: String? = null,
    val phone: String? = null,
    val agreeMarketing: Boolean? = null
)

/** PATCH /api/users/me/password (성공하면 이 기기용 새 토큰이 옴) */
data class PasswordChangeRequest(
    val currentPassword: String,
    /** 8자 이상 */
    val newPassword: String
)

/** PUT /api/users/me/account 출금 계좌 변경 */
data class AccountUpdateRequest(
    /** 현재 비밀번호 */
    val password: String,
    val bankCode: String,
    /** 숫자 10~14자리 */
    val accountNumber: String,
    /** 예금주 = 가입한 이름 */
    val accountHolder: String
)

/** POST /api/users/me/withdraw 회원 탈퇴 */
data class MemberWithdrawRequest(
    val password: String,
    /** 남은 잔액 소멸 동의 (true만 허용, 앱은 잔액 0원일 때만 탈퇴시킴) */
    val agreeBalanceForfeit: Boolean = true
)
