package com.example.taxipot.feature.pot.data

import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.PotApi
import com.example.taxipot.data.remote.dto.pot.PotArrivalRequest
import com.example.taxipot.data.remote.dto.pot.PotArrivalResponse
import com.example.taxipot.data.remote.dto.pot.PotCreateRequest
import com.example.taxipot.data.remote.dto.pot.PotDetailResponse
import com.example.taxipot.data.remote.dto.pot.PotJoinRequest
import com.example.taxipot.data.remote.dto.pot.PotJoinResponse
import com.example.taxipot.data.remote.dto.pot.PotMemberResponse
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

    /** 가짜 데이터에서 "나"의 회원 번호 */
    const val FAKE_MY_ID = 0L

    // ===== 가짜 데이터 상태 (대기실 시험용, 앱을 끄면 처음으로 돌아감) =====
    private val fakeCreated = mutableMapOf<Long, PotDetailResponse>()   // 가짜로 만든 방 (내가 방장)
    private val fakeJoinedSeat = mutableMapOf<Long, String>()            // 가짜로 참여한 방 → 고른 좌석 (내가 동승자)
    private var fakeActivePotId: Long? = null                            // 지금 참여 중인 방 (한 번에 하나만)
    private val fakeArrived = mutableSetOf<Pair<Long, Long>>()           // (방, 회원) 도착 확인
    private val fakeRemoved = mutableSetOf<Pair<Long, Long>>()           // (방, 회원) 내보냄·참여 취소
    private val fakeStatus = mutableMapOf<Long, String>()                // 방 상태 변경 (RUNNING, CANCELED)
    private var fakeNextId = 900L

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
            val pot = fakeDetail(potId)
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

    // 가짜 방 만들기: 예치액이 가짜 잔액보다 크면 잔액 부족, 아니면 성공 (만든 방은 대기실에서 볼 수 있게 저장)
    private fun fakeCreate(r: PotCreateRequest): ApiResult<PotDetailResponse> {
        val hold = holdAmount(r.expectedFare, r.maxMembers)
        if (hold > FakePotData.FAKE_BALANCE) {
            return ApiResult.Failure(409, "INSUFFICIENT_BALANCE", "지갑 잔액이 부족합니다.", null)
        }
        fakeActivePotId?.let { return activePotExists(it) }

        val id = fakeNextId++
        val me = PotMemberResponse(
            id = id * 10, memberId = FAKE_MY_ID, nickname = "나", ratingAvg = 5.0, role = "HOST",
            preferredSeat = r.preferredSeat ?: "NONE", status = "JOINED",
            arrivedAt = null, joinedAt = r.departureTime, canceledAt = null
        )
        val pot = PotDetailResponse(
            id = id, title = r.title, hostId = FAKE_MY_ID, hostNickname = "나", hostRatingAvg = 5.0,
            departureName = r.departureName, departureLat = r.departureLat, departureLng = r.departureLng,
            destinationName = r.destinationName, destinationLat = r.destinationLat, destinationLng = r.destinationLng,
            departureTime = r.departureTime, arrivalDeadline = r.departureTime, cancelDeadline = r.departureTime,
            maxMembers = r.maxMembers, currentMembers = 1, genderOption = r.genderOption ?: "ALL",
            expectedFare = r.expectedFare, holdAmount = hold, actualFare = null,
            status = "RECRUITING", hasCanceled = false, openChatUrl = r.openChatUrl,
            isJoined = true, myMemberStatus = "JOINED", hasReceipt = false,
            fareClaimStatus = "NONE", fareRejectReason = null, members = listOf(me) + fakeTestGuests(id, r.departureTime),
            createdAt = r.departureTime, updatedAt = r.departureTime
        )
        fakeCreated[id] = pot
        fakeActivePotId = id
        return ApiResult.Success(pot)
    }

    // 가짜 상세: 원래 가짜 데이터 + 대기실에서 바뀐 것(도착·내보냄·상태) 반영
    // 처음부터 첫 번째 동승자는 "도착"한 것으로 보여줌 (도착·미도착을 같이 시험하려고)
    private fun fakeDetail(potId: Long): PotDetailResponse? {
        val base = fakeCreated[potId] ?: FakePotDetailData.find(potId) ?: return null
        val status = fakeStatus[potId] ?: base.status
        val firstGuestId = base.members.firstOrNull { it.role == "GUEST" }?.memberId
        // 내가 동승자로 참여한 방이면 참여자 목록 끝에 "나" 추가
        val mySeat = fakeJoinedSeat[potId]
        val withMe = if (mySeat != null) {
            base.members + PotMemberResponse(
                id = potId * 10 + 9, memberId = FAKE_MY_ID, nickname = "나", ratingAvg = 5.0, role = "GUEST",
                preferredSeat = mySeat, status = "JOINED", arrivedAt = null,
                joinedAt = base.createdAt, canceledAt = null
            )
        } else {
            base.members
        }
        val members = withMe
            .filter { (potId to it.memberId) !in fakeRemoved }
            .map { m ->
                val arrived = (potId to m.memberId) in fakeArrived || m.memberId == firstGuestId
                m.copy(
                    arrivedAt = if (arrived) (m.arrivedAt ?: base.arrivalDeadline) else null,
                    status = if (status == "RUNNING") "BOARDED" else m.status
                )
            }
        return base.copy(
            members = members,
            currentMembers = members.size,
            status = status,
            isJoined = base.isJoined || potId in fakeJoinedSeat,
            myMemberStatus = if (base.isJoined || potId in fakeJoinedSeat) "JOINED" else null
        )
    }

    // 시험용 가짜 동승자 2명 (만든 방에서 도착·미도착·내보내기를 시험하려고)
    private fun fakeTestGuests(potId: Long, time: String): List<PotMemberResponse> = listOf(
        PotMemberResponse(potId * 10 + 1, 901, "새내기", 5.5, "GUEST", "BACK_LEFT", "JOINED", null, time, null),
        PotMemberResponse(potId * 10 + 2, 902, "천안러", 6.0, "GUEST", "BACK_RIGHT", "JOINED", null, time, null)
    )

    // 가짜: 이미 다른 방에 참여 중 (서버와 같은 detail 모양)
    private fun <T> activePotExists(activePotId: Long): ApiResult<T> = ApiResult.Failure(
        409, "ACTIVE_POT_EXISTS", "이미 참여 중인 택시팟이 있습니다.",
        mapOf("activePotId" to activePotId.toDouble())
    )

    /** ACTIVE_POT_EXISTS 실패면 이미 참여 중인 방 번호 (아니면 null) */
    fun activePotIdOf(result: ApiResult<*>): Long? {
        if (result !is ApiResult.Failure || result.code != "ACTIVE_POT_EXISTS") return null
        return (result.detail?.get("activePotId") as? Number)?.toLong()
    }

    /** 택시팟 참여 (희망 좌석과 함께) */
    suspend fun join(potId: Long, preferredSeat: String): ApiResult<PotJoinResponse> {
        if (USE_FAKE) return fakeJoin(potId, preferredSeat)
        return safeApiCall { api.join(potId, PotJoinRequest(preferredSeat)) }
    }

    // 가짜 참여: 예치액이 가짜 잔액보다 크면 잔액 부족(서버와 같은 detail 모양), 아니면 성공
    private fun fakeJoin(potId: Long, seat: String): ApiResult<PotJoinResponse> {
        val pot = FakePotDetailData.find(potId)
            ?: return ApiResult.Failure(404, "POT_NOT_FOUND", "택시팟을 찾을 수 없어요.", null)
        if (potId in fakeJoinedSeat) {
            return ApiResult.Failure(409, "ALREADY_JOINED", "이미 참여한 택시팟입니다.", null)
        }
        fakeActivePotId?.let { return activePotExists(it) }
        val balance = FakePotData.FAKE_BALANCE
        if (pot.holdAmount > balance) {
            return ApiResult.Failure(
                409, "INSUFFICIENT_BALANCE", "지갑 잔액이 부족합니다.",
                mapOf(
                    "requiredAmount" to pot.holdAmount.toDouble(),
                    "balance" to balance.toDouble(),
                    "shortageAmount" to (pot.holdAmount - balance).toDouble()
                )
            )
        }
        fakeJoinedSeat[potId] = seat
        fakeActivePotId = potId
        val status = if (pot.currentMembers + 1 >= pot.maxMembers) "CLOSED" else "RECRUITING"
        return ApiResult.Success(PotJoinResponse(potId, null, pot.holdAmount, balance - pot.holdAmount, status))
    }

    // ===== 대기실 (RD-01) =====

    /**
     * 도착 확인 (내 GPS 위치를 보내면 서버가 출발지 50m 안인지 판정)
     * fakeMyId: 가짜 데이터일 때 "나"로 볼 회원 번호
     */
    suspend fun arrive(potId: Long, lat: Double, lng: Double, fakeMyId: Long?): ApiResult<PotArrivalResponse> {
        if (!USE_FAKE) return safeApiCall { api.arrive(potId, PotArrivalRequest(lat, lng)) }

        val pot = fakeDetail(potId) ?: return ApiResult.Failure(404, "POT_NOT_FOUND", "택시팟을 찾을 수 없어요.", null)
        if (fakeMyId != null) fakeArrived.add(potId to fakeMyId)
        val after = fakeDetail(potId) ?: pot
        val arrivedCount = after.members.count { it.arrivedAt != null }
        val meters = FloatArray(1)
        android.location.Location.distanceBetween(lat, lng, pot.departureLat, pot.departureLng, meters)
        return ApiResult.Success(
            PotArrivalResponse(
                potId = potId,
                arrivedAt = pot.arrivalDeadline,
                distanceMeters = meters[0].toInt(),
                arrivedCount = arrivedCount,
                totalMembers = after.members.size,
                allArrived = arrivedCount == after.members.size
            )
        )
    }

    /** 탑승 시작 (방장) — 남은 참여자 전원 도착 + 2명 이상이어야 함 */
    suspend fun start(potId: Long): ApiResult<PotDetailResponse> {
        if (!USE_FAKE) return safeApiCall { api.start(potId) }

        val pot = fakeDetail(potId) ?: return ApiResult.Failure(404, "POT_NOT_FOUND", "택시팟을 찾을 수 없어요.", null)
        if (pot.members.any { it.arrivedAt == null }) {
            return ApiResult.Failure(409, "NOT_ALL_ARRIVED", "아직 도착하지 않은 참여자가 있습니다.", null)
        }
        if (pot.members.size < 2) {
            return ApiResult.Failure(409, "NOT_ENOUGH_MEMBERS", "탑승 인원이 부족합니다.", null)
        }
        fakeStatus[potId] = "RUNNING"
        return ApiResult.Success(fakeDetail(potId) ?: pot)
    }

    /** 미도착자 내보내기 (방장) */
    suspend fun kick(potId: Long, userId: Long): ApiResult<Unit> {
        if (!USE_FAKE) return safeApiCall { api.kick(potId, userId) }

        val pot = fakeDetail(potId) ?: return ApiResult.Failure(404, "POT_NOT_FOUND", "택시팟을 찾을 수 없어요.", null)
        if (pot.members.any { it.memberId == userId && it.arrivedAt != null }) {
            return ApiResult.Failure(409, "MEMBER_ALREADY_ARRIVED", "도착한 참여자는 내보낼 수 없습니다.", null)
        }
        fakeRemoved.add(potId to userId)
        return ApiResult.Success(Unit)
    }

    /** 방 취소 (방장) — 전원 예치금 환불 */
    suspend fun cancelPot(potId: Long): ApiResult<Unit> {
        if (!USE_FAKE) return safeApiCall { api.cancelPot(potId) }
        fakeStatus[potId] = "CANCELED"
        if (fakeActivePotId == potId) fakeActivePotId = null
        return ApiResult.Success(Unit)
    }

    /** 참여 취소 (동승자) — 예치금 환불, 출발 10분 전부터는 불가 */
    suspend fun leave(potId: Long, fakeMyId: Long?): ApiResult<Unit> {
        if (!USE_FAKE) return safeApiCall { api.leave(potId) }
        fakeJoinedSeat.remove(potId)
        if (fakeActivePotId == potId) fakeActivePotId = null
        return ApiResult.Success(Unit)
    }

    /** 실패 결과를 화면에 보여줄 문장으로 바꾸기 */
    fun errorMessage(result: ApiResult<*>): String = when (result) {
        is ApiResult.Failure -> when (result.code) {
            "POT_NOT_FOUND" -> "택시팟을 찾을 수 없어요."
            "INSUFFICIENT_BALANCE" -> "잔액이 부족해요. 충전한 뒤 다시 시도해 주세요."
            "DEPARTURE_TIME_TOO_SOON" -> "출발 시각은 지금부터 15분 이후여야 해요. 시각을 다시 골라 주세요."
            "DEPARTURE_TIME_TOO_FAR" -> "출발 시각은 24시간 이내여야 해요."
            "ACTIVE_POT_EXISTS" -> "이미 참여 중인 택시팟이 있어요. 한 번에 하나의 방만 참여할 수 있어요."
            "NOT_VERIFIED" -> "학생 인증을 먼저 완료해 주세요."
            "SUSPENDED_USER" -> "이용 정지 중이라 방을 만들 수 없어요."
            "GENDER_RESTRICTED" -> "성별 제한 때문에 할 수 없어요."
            "POT_FULL" -> "모집 인원이 모두 찼어요."
            "JOIN_DEADLINE_PASSED" -> "참여 마감 시각이 지났어요. 출발지 근처에서 입장 요청을 보내 주세요."
            "ALREADY_JOINED" -> "이미 참여한 택시팟이에요."
            "KICKED_FROM_POT" -> "내보내진 방에는 다시 참여할 수 없어요."
            "INVALID_POT_STATUS" -> "지금은 할 수 없는 상태의 택시팟이에요."
            "OUT_OF_ARRIVAL_RANGE" -> "출발지 50m 안에서만 도착 확인을 할 수 있어요."
            "ARRIVAL_DEADLINE_PASSED" -> "집결 기한이 지나 도착 확인을 할 수 없어요."
            "ALREADY_ARRIVED" -> "이미 도착 확인을 했어요."
            "NOT_ALL_ARRIVED" -> "아직 도착하지 않은 참여자가 있어요. 전원 도착해야 탑승을 시작할 수 있어요."
            "NOT_ENOUGH_MEMBERS" -> "탑승 인원이 2명 이상이어야 출발할 수 있어요."
            "MEMBER_ALREADY_ARRIVED" -> "도착한 참여자는 내보낼 수 없어요."
            "CANCEL_TIME_EXPIRED" -> "출발 10분 전부터는 취소할 수 없어요."
            "HOST_CANNOT_LEAVE" -> "방장은 참여 취소 대신 방 취소를 해 주세요."
            "NOT_POT_HOST" -> "방장만 할 수 있어요."
            "NOT_POT_MEMBER" -> "이 택시팟의 참여자가 아니에요."
            else -> result.message
        }
        is ApiResult.NetworkError -> "서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요."
        is ApiResult.Success -> ""
    }
}