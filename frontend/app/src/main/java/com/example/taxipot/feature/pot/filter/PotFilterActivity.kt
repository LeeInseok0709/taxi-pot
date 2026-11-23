package com.example.taxipot.feature.pot.filter

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taxipot.BuildConfig
import com.example.taxipot.databinding.ActivityPotFilterBinding
import com.example.taxipot.feature.pot.data.FakePotData
import com.example.taxipot.feature.pot.data.PotFilter
import com.example.taxipot.feature.pot.main.PotColors
import com.example.taxipot.feature.pot.main.roundedBg
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import kotlin.math.roundToInt

/**
 * EX-03 필터 · 정렬
 * 고른 조건은 "적용"을 누르면 EX-02 목록으로 돌려보냄
 */
class PotFilterActivity : ComponentActivity() {

    private lateinit var binding: ActivityPotFilterBinding

    // 지금 화면에서 고른 조건 (명세서 GET /api/pots 조건값과 같은 이름)
    private var genderOption = "ALL"
    private var minHostRating = 1.0
    private var sort = "DEPARTURE_TIME"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) 카카오맵 준비 (이 화면에도 지도가 있어서)
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        // 2) XML 화면 붙이기
        binding = ActivityPotFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 3) 상태바·하단바 여백 + 상태바 글자를 어두운 색으로
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // 4) EX-02에서 보내 준 지금 조건 꺼내기
        val current = PotFilter.from(intent)
        genderOption = current.genderOption
        minHostRating = current.minHostRating
        sort = current.sort

        // 5) 뒤로 가기
        binding.btnBack.setOnClickListener { finish() }

        // 6) 기준 위치 (지금은 현재 위치 = 백석대 고정, 지하철역은 팀에서 정한 뒤 추가)
        binding.optStation.setOnClickListener { showToast("지하철역 선택 (준비 중)") }

        // 7) 성별 옵션
        binding.optGenderAll.setOnClickListener { genderOption = "ALL"; refresh() }
        binding.optGenderFemale.setOnClickListener { genderOption = "FEMALE"; refresh() }
        binding.optGenderMale.setOnClickListener { genderOption = "MALE"; refresh() }

        // 8) 방장 최소 평점: 슬라이더 0~90 → 평점 1.0~10.0
        binding.seekRating.max = 90
        binding.seekRating.progress = ((minHostRating - 1.0) * 10).roundToInt()
        binding.seekRating.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                minHostRating = 1.0 + progress / 10.0
                refresh()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 9) 정렬
        binding.optSortTime.setOnClickListener { sort = "DEPARTURE_TIME"; refresh() }
        binding.optSortDistance.setOnClickListener { sort = "DISTANCE"; refresh() }
        binding.optSortRating.setOnClickListener { sort = "HOST_RATING"; refresh() }

        // 10) 초기화: 처음 상태로
        binding.btnReset.setOnClickListener {
            genderOption = "ALL"
            minHostRating = 1.0
            sort = "DEPARTURE_TIME"
            binding.seekRating.progress = 0
            refresh()
        }

        // 11) 적용: 고른 조건을 EX-02로 돌려보내고 닫기
        binding.btnApply.setOnClickListener {
            val result = PotFilter(genderOption, minHostRating, sort).putInto(Intent())
            setResult(RESULT_OK, result)
            finish()
        }

        // 12) 고정 디자인 + 지도
        binding.root.setBackgroundColor(PotColors.BACKGROUND)
        binding.mapCard.background = roundedBg(PotColors.CARD, 14f, PotColors.LINE)
        binding.ratingCard.background = roundedBg(PotColors.CARD, 12f, PotColors.LINE)
        binding.btnReset.background = roundedBg(PotColors.CARD, 12f, PotColors.LINE)
        binding.btnApply.background = roundedBg(PotColors.ACCENT, 12f)
        startMap()

        refresh()
    }

    // 고른 버튼은 베이지, 나머지는 흰색 + 테두리로 다시 칠하기
    private fun refresh() {
        select(binding.optCurrent, true)
        select(binding.optStation, false)

        select(binding.optGenderAll, genderOption == "ALL")
        select(binding.optGenderFemale, genderOption == "FEMALE")
        select(binding.optGenderMale, genderOption == "MALE")

        binding.tvRatingValue.text =
            if (minHostRating <= 1.0) "전체" else "%.1f 이상".format(minHostRating)

        select(binding.optSortTime, sort == "DEPARTURE_TIME")
        select(binding.optSortDistance, sort == "DISTANCE")
        select(binding.optSortRating, sort == "HOST_RATING")
    }

    private fun select(view: TextView, selected: Boolean) {
        view.background = if (selected) {
            roundedBg(PotColors.ACCENT, 10f, PotColors.LINE)
        } else {
            roundedBg(PotColors.CARD, 10f, PotColors.LINE)
        }
        view.paint.isFakeBoldText = selected
        view.invalidate()
    }

    // 지도 켜기 (기준 위치: 백석대학교)
    private fun startMap() {
        binding.mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {}
            override fun onMapError(error: Exception) {
                Log.e("KakaoMap", "필터 화면 지도 오류: ${error.message}")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {}
            override fun getPosition(): LatLng =
                LatLng.from(FakePotData.BAEKSEOK_LAT, FakePotData.BAEKSEOK_LNG)
            override fun getZoomLevel(): Int = 15
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.pause()
    }
}