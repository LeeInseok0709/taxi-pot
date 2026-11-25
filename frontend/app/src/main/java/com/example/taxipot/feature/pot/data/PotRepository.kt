package com.example.taxipot.feature.pot.data

import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.PotApi
import com.example.taxipot.data.remote.dto.pot.PotCreateRequest
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

    /** 1인 예치액 = floor(예상 요금 × 1.2 ÷ 최대 인원) + 1,000 (명세서 공식) */
    fun holdAmount(expectedFare: Int, maxMembers: Int): Int =
        expectedFare * 12 / (maxMembers * 10) + 1000

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

    /** 방 만들기 (성공하면 만든 방 상세) */
    suspend fun createPot(request: PotCreateRequest): ApiResult<PotDetailResponse> {
        if (USE_FAKE) return fakeCreate(request)
        return safeApiCall { api.createPot(request) }
    }

    // 가짜 방 만들기: 예치액이 가짜 잔액보다 크면 잔액 부족, 아니면 성공
    private fun fakeCreate(r: PotCreateRequest): ApiResult<PotDetailResponse> {
        val hold = holdAmount(r.expectedFare, r.maxMembers)
        if (hold > FakePotData.FAKE_BALANCE) {
            return ApiResult.Failure(409, "INSUFFICIENT_BALANCE", "지갑 잔액이 부족합니다.", null)
        }
        return ApiResult.Success(
            PotDetailResponse(
                id = 999, title = r.title, hostId = 0, hostNickname = "나", hostRatingAvg = 0.0,
                departureName = r.departureName, departureLat = r.departureLat, departureLng = r.departureLng,
                destinationName = r.destinationName, destinationLat = r.destinationLat, destinationLng = r.destinationLng,
                departureTime = r.departureTime, arrivalDeadline = r.departureTime, cancelDeadline = r.departureTime,
                maxMembers = r.maxMembers, currentMembers = 1, genderOption = r.genderOption ?: "ALL",
                expectedFare = r.expectedFare, holdAmount = hold, actualFare = null,
                status = "RECRUITING", hasCanceled = false, openChatUrl = r.openChatUrl,
                isJoined = true, myMemberStatus = "JOINED", hasReceipt = false,
                fareClaimStatus = "NONE", fareRejectReason = null, members = emptyList(),
                createdAt = r.departureTime, updatedAt = r.departureTime
            )
        )
    }

    /** 실패 결과를 화면에 보여줄 문장으로 바꾸기 */
    fun errorMessage(result: ApiResult<*>): String = when (result) {
        is ApiResult.Failure -> when (result.code) {
            "POT_NOT_FOUND" -> "택시팟을 찾을 수 없어요."
            "INSUFFICIENT_BALANCE" -> "잔액이 부족해요. 충전한 뒤 다시 시도해 주세요."
            "DEPARTURE_TIME_TOO_SOON" -> "출발 시각은 지금부터 15분 이후여야 해요. 시각을 다시 골라 주세요."
            "DEPARTURE_TIME_TOO_FAR" -> "출발 시각은 24시간 이내여야 해요."
            "ACTIVE_POT_EXISTS" -> "이미 참여 중인 택시팟이 있어요."
            "NOT_VERIFIED" -> "학생 인증을 먼저 완료해 주세요."
            "SUSPENDED_USER" -> "이용 정지 중이라 방을 만들 수 없어요."
            "GENDER_RESTRICTED" -> "내 성별로는 고를 수 없는 성별 옵션이에요."
            else -> result.message
        }
        is ApiResult.NetworkError -> "서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요."
        is ApiResult.Success -> ""
    }
}