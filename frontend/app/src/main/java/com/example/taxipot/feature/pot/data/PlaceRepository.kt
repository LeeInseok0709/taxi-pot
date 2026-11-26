package com.example.taxipot.feature.pot.data

import com.example.taxipot.BuildConfig
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.KakaoApiClient

/** 예상 택시 요금 결과 */
data class FareEstimate(
    val fare: Int,              // 택시 요금 + 통행료 (원)
    val distanceMeters: Int,    // 거리 (m)
    val durationSeconds: Int    // 걸리는 시간 (초)
)

/**
 * 장소·요금 데이터 창구 (카카오 API)
 * local.properties 에 KAKAO_REST_API_KEY 가 없으면 앱 안 장소 목록(PlacePresets)으로 대신함
 */
object PlaceRepository {

    /** REST 키가 들어 있는지 */
    private val hasKey: Boolean
        get() = BuildConfig.KAKAO_REST_API_KEY.isNotBlank() && BuildConfig.KAKAO_REST_API_KEY != "null"

    /** 장소 검색: 지금 지도 위치(lat, lng) 기준 20km 안에서 */
    suspend fun search(query: String, lat: Double, lng: Double): ApiResult<List<Place>> {
        if (!hasKey) return ApiResult.Success(PlacePresets.search(query))

        val result = safeApiCall {
            KakaoApiClient.local.searchKeyword(query = query, x = lng, y = lat, radius = 20_000, size = 15)
        }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(
                result.data.documents.mapNotNull { doc ->
                    val placeLat = doc.y.toDoubleOrNull() ?: return@mapNotNull null
                    val placeLng = doc.x.toDoubleOrNull() ?: return@mapNotNull null
                    val address = doc.roadAddressName?.takeIf { it.isNotBlank() } ?: doc.addressName ?: ""
                    Place(doc.placeName, address, placeLat, placeLng)
                }
            )
            is ApiResult.Failure -> result
            is ApiResult.NetworkError -> result
        }
    }

    /**
     * 지도 핀 위치의 이름 찾기
     * 1) 자주 쓰는 장소(150m 이내)면 그 이름  2) 아니면 카카오 주소(건물 이름 → 도로명 → 지번)
     * 못 찾으면 null
     */
    suspend fun nameAt(lat: Double, lng: Double): Place? {
        PlacePresets.nearest(lat, lng)?.let { return it.copy(lat = lat, lng = lng) }
        if (!hasKey) return null

        val result = safeApiCall { KakaoApiClient.local.coordToAddress(x = lng, y = lat) }
        if (result !is ApiResult.Success) return null

        val doc = result.data.documents.firstOrNull() ?: return null
        val road = doc.roadAddress?.addressName?.takeIf { it.isNotBlank() }
        val jibun = doc.address?.addressName?.takeIf { it.isNotBlank() }
        val building = doc.roadAddress?.buildingName?.takeIf { it.isNotBlank() }
        val name = building ?: road ?: jibun ?: return null
        return Place(name, road ?: jibun ?: "", lat, lng)
    }

    /** 출발지 → 목적지 예상 택시 요금 (카카오모빌리티 길찾기) */
    suspend fun estimateFare(from: Place, to: Place): ApiResult<FareEstimate> {
        if (!hasKey) return ApiResult.Failure(0, "NO_KEY", "카카오 REST 키가 없어요.", null)

        val result = safeApiCall {
            KakaoApiClient.navi.directions(
                origin = "${from.lng},${from.lat}",
                destination = "${to.lng},${to.lat}"
            )
        }
        return when (result) {
            is ApiResult.Success -> {
                val route = result.data.routes.firstOrNull()
                val summary = route?.summary
                val fare = summary?.fare
                if (route == null || route.resultCode != 0 || summary == null || fare == null) {
                    ApiResult.Failure(0, "NO_ROUTE", route?.resultMsg ?: "경로를 찾지 못했어요.", null)
                } else {
                    ApiResult.Success(FareEstimate(fare.taxi + fare.toll, summary.distance, summary.duration))
                }
            }
            is ApiResult.Failure -> result
            is ApiResult.NetworkError -> result
        }
    }

    /** 실패 결과를 화면 문장으로 */
    fun errorMessage(result: ApiResult<*>): String = when (result) {
        is ApiResult.Failure -> when {
            result.httpStatus == 401 || result.httpStatus == 403 ->
                "카카오 키 설정을 확인해 주세요 (REST 키, 카카오맵 사용 설정)"
            result.code == "NO_ROUTE" -> "경로를 찾지 못했어요."
            else -> result.message
        }
        is ApiResult.NetworkError -> "인터넷 연결을 확인해 주세요."
        is ApiResult.Success -> ""
    }
}