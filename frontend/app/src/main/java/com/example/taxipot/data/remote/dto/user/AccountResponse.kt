package com.example.taxipot.data.remote.dto.user

/** GET /api/users/me/account 내 출금 계좌 */
data class AccountResponse(
    val bankCode: String?,
    val bankName: String?,
    /** 앞 3자리·뒤 4자리만 보이게 가려진 계좌번호 */
    val maskedAccountNumber: String?,
    val accountHolder: String?
)
