package com.example.taxipot.feature.pot.data

import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.PotApi
import com.example.taxipot.data.remote.dto.pot.PotDetailResponse
import com.example.taxipot.data.remote.dto.pot.PotSummaryResponse

/**
 * 택시팟 데이터 창구
 * 화면(ViewModel)은 여기만 부르면 되고, 가짜 데이터인지 서버인지는 여기서 정함
 */
object PotRepository {

    /**
     * true  = 가짜 데이터 사용 (서버·계정 없이 화면 확인용)
     * false = 실제 서버 사용 (테스트 계정으로 로그인한 뒤)
     */
    const val USE_FAKE = true

    // 한 번에 받아올 방 개수 (명세서: 1 ~ 100)
    private const val PAGE_SIZE = 50

    private val api by lazy { ApiClient.create(PotApi::class.java) }

    /** 택시팟 목록: 필터·정렬 조건과 기준 위치(lat, lng)로 조회 */
    suspend fun getPots(
        filter: PotFilter,
        lat: Double?,
        lng: Double?
    ): ApiResult<List<PotSummaryResponse>> {
        if (USE_FAKE) return ApiResult.Success(filter.apply(FakePotData.pots))

        val result = safeApiCall {
            api.getPots(
                lat = lat,
                lng = lng,
                // "전체"는 조건을 안 보냄
                genderOption = filter.genderOption.takeIf { it != "ALL" },
                minHostRating = filter.minHostRating.takeIf { it > 1.0 },
                sort = filter.sort,
                page = 0,
                size = PAGE_SIZE
            )
        }
        // 페이지 응답에서 방 목록(content)만 꺼내서 돌려줌
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.data.content)
            is ApiResult.Failure -> result
            is ApiResult.NetworkError -> result
        }
    }

    /** 택시팟 상세 */
    suspend fun getPot(potId: Long): ApiResult<PotDetailResponse> {
        if (USE_FAKE) {
            val pot = FakePotDetailData.find(potId)
                ?: return ApiResult.Failure(404, "POT_NOT_FOUND", "택시팟을 찾을 수 없어요.", null)
            return ApiResult.Success(pot)
        }
        return safeApiCall { api.getPot(potId) }
    }

    /** 실패 결과를 화면에 보여줄 문장으로 바꾸기 */
    fun errorMessage(result: ApiResult<*>): String = when (result) {
        is ApiResult.Failure -> when (result.code) {
            "POT_NOT_FOUND" -> "택시팟을 찾을 수 없어요."
            else -> result.message
        }
        is ApiResult.NetworkError -> "서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요."
        is ApiResult.Success -> ""
    }
}
