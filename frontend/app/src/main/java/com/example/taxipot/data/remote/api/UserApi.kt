package com.example.taxipot.data.remote.api

import com.example.taxipot.data.remote.dto.user.AccountResponse
import retrofit2.Response
import retrofit2.http.GET

/** 내 정보 API (로그인 토큰 필요). 마이페이지 만들 때 여기에 이어서 추가 */
interface UserApi {

    /** 내 출금 계좌 (계좌번호는 가려져서 옴) */
    @GET("api/users/me/account")
    suspend fun getMyAccount(): Response<AccountResponse>
}
