package com.example.taxipot.feature.pot.data

import android.location.Location

/** 장소 1곳 (이름 + 주소 + 좌표) */
data class Place(
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double
)

/**
 * 장소 검색용 목록 (백석대 주변에서 자주 쓰는 곳)
 * 지금은 앱 안 목록에서 찾고, 나중에 카카오 장소 검색 API로 바꿀 예정
 * 좌표는 대략적인 위치
 */
object PlacePresets {

    val places = listOf(
        Place("백석대학교 정문", "충남 천안시 동남구 안서동", 36.84021, 127.18322),
        Place("백석문화대학교", "충남 천안시 동남구 안서동", 36.83780, 127.18246),
        Place("천안역 동부광장", "충남 천안시 동남구 대흥로", 36.81010, 127.14700),
        Place("천안역 서부광장", "충남 천안시 동남구 대흥로", 36.80960, 127.14490),
        Place("천안역 1호선", "충남 천안시 동남구", 36.81010, 127.14640),
        Place("천안아산역", "충남 아산시 배방읍", 36.79450, 127.10460),
        Place("천안터미널", "충남 천안시 동남구 신부동", 36.81970, 127.15660),
        Place("두정역", "충남 천안시 서북구 두정동", 36.83350, 127.14910)
    )

    /** 검색어가 이름이나 주소에 들어간 장소 찾기 (띄어쓰기 무시) */
    fun search(query: String): List<Place> {
        val q = query.replace(" ", "")
        if (q.isEmpty()) return places
        return places.filter {
            it.name.replace(" ", "").contains(q) || it.address.replace(" ", "").contains(q)
        }
    }

    /** 이 좌표에서 가까운(150m 이내) 장소 찾기 (없으면 null) */
    fun nearest(lat: Double, lng: Double): Place? {
        val result = FloatArray(1)
        return places
            .map { place ->
                Location.distanceBetween(lat, lng, place.lat, place.lng, result)
                place to result[0]
            }
            .filter { it.second <= 150f }
            .minByOrNull { it.second }
            ?.first
    }
}