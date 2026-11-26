package com.example.taxipot.data.remote.dto.kakao

import com.google.gson.annotations.SerializedName

// ===== 카카오 장소 검색 (키워드로 장소 검색) =====

data class KakaoPlaceResponse(
    val documents: List<KakaoPlace>
)

data class KakaoPlace(
    @SerializedName("place_name") val placeName: String,              // 장소 이름
    @SerializedName("address_name") val addressName: String?,         // 지번 주소
    @SerializedName("road_address_name") val roadAddressName: String?,// 도로명 주소
    val x: String,                                                    // 경도 (lng)
    val y: String                                                     // 위도 (lat)
)

// ===== 카카오 좌표 → 주소 =====

data class KakaoAddressResponse(
    val documents: List<KakaoAddressDocument>
)

data class KakaoAddressDocument(
    @SerializedName("road_address") val roadAddress: KakaoRoadAddress?,
    val address: KakaoAddress?
)

data class KakaoRoadAddress(
    @SerializedName("address_name") val addressName: String?,
    @SerializedName("building_name") val buildingName: String?
)

data class KakaoAddress(
    @SerializedName("address_name") val addressName: String?
)

// ===== 카카오모빌리티 길찾기 (예상 택시 요금) =====

data class KakaoDirectionsResponse(
    val routes: List<KakaoRoute>
)

data class KakaoRoute(
    @SerializedName("result_code") val resultCode: Int,   // 0이면 길찾기 성공
    @SerializedName("result_msg") val resultMsg: String?,
    val summary: KakaoRouteSummary?
)

data class KakaoRouteSummary(
    val fare: KakaoFare?,
    val distance: Int,      // 총 거리 (m)
    val duration: Int       // 걸리는 시간 (초)
)

data class KakaoFare(
    val taxi: Int,          // 택시 요금 (원)
    val toll: Int           // 통행료 (원)
)