package com.example.taxipot.data.remote.api

import com.example.taxipot.data.remote.dto.common.UniversityResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers

/** 로그인 없이 쓰는 공통 조회 API */
interface CommonApi {

    /** 가입 가능 대학교 목록 */
    @Headers("No-Auth: true")
    @GET("api/universities")
    suspend fun getUniversities(): Response<List<UniversityResponse>>
}
