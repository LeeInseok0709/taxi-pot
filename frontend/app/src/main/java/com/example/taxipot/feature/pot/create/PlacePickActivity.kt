package com.example.taxipot.feature.pot.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taxipot.BuildConfig
import com.example.taxipot.databinding.ActivityPlacePickBinding
import com.example.taxipot.feature.pot.data.FakePotData
import com.example.taxipot.feature.pot.data.Place
import com.example.taxipot.feature.pot.data.PlacePresets
import com.example.taxipot.feature.pot.main.PotColors
import com.example.taxipot.feature.pot.main.dp
import com.example.taxipot.feature.pot.main.roundedBg
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraUpdateFactory

/**
 * CR-02 출발지 설정 / CR-03 목적지 설정 (같은 화면을 제목만 바꿔서 사용)
 * 지도 가운데 핀 위치 또는 검색 결과로 장소를 고르고, 고른 장소를 이전 화면으로 돌려줌
 */
class PlacePickActivity : ComponentActivity() {

    private lateinit var binding: ActivityPlacePickBinding
    private var kakaoMap: KakaoMap? = null

    // 지금 고른 위치 (처음엔 백석대학교)
    private var selectedLat = FakePotData.BAEKSEOK_LAT
    private var selectedLng = FakePotData.BAEKSEOK_LNG

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_NAME = "place_name"
        private const val EXTRA_LAT = "place_lat"
        private const val EXTRA_LNG = "place_lng"

        /** 장소 고르기 화면 열기 (place: 이미 고른 장소가 있으면 그 위치에서 시작) */
        fun newIntent(context: Context, title: String, place: Place?): Intent =
            Intent(context, PlacePickActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                if (place != null) {
                    putExtra(EXTRA_NAME, place.name)
                    putExtra(EXTRA_LAT, place.lat)
                    putExtra(EXTRA_LNG, place.lng)
                }
            }

        /** 돌려받은 결과에서 고른 장소 꺼내기 (없으면 null) */
        fun placeFrom(data: Intent?): Place? {
            val name = data?.getStringExtra(EXTRA_NAME) ?: return null
            return Place(
                name = name,
                address = "",
                lat = data.getDoubleExtra(EXTRA_LAT, 0.0),
                lng = data.getDoubleExtra(EXTRA_LNG, 0.0)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) 카카오맵 준비
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        // 2) XML 화면 붙이기
        binding = ActivityPlacePickBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 3) 상태바·하단바·키보드 여백 + 상태바 글자를 어두운 색으로
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // 4) 디자인
        binding.root.setBackgroundColor(PotColors.BACKGROUND)
        binding.searchBox.background = roundedBg(PotColors.CARD, 10f, PotColors.LINE)
        binding.resultList.background = roundedBg(PotColors.CARD, 12f, PotColors.LINE)
        binding.mapCard.background = roundedBg(PotColors.CARD, 14f, PotColors.LINE)
        binding.selectedCard.background = roundedBg(PotColors.CARD, 12f, PotColors.LINE)
        binding.tvGuide.background = roundedBg(PotColors.CARD, 10f, PotColors.LINE)
        binding.btnConfirm.background = roundedBg(PotColors.ACCENT, 12f)

        // 5) 제목 (출발지 설정 / 목적지 설정)
        binding.tvTitle.text = intent.getStringExtra(EXTRA_TITLE) ?: "장소 설정"

        // 6) 처음 위치: 이미 고른 장소가 있으면 그곳, 없으면 백석대학교
        val initialName = intent.getStringExtra(EXTRA_NAME)
        if (initialName != null) {
            selectedLat = intent.getDoubleExtra(EXTRA_LAT, selectedLat)
            selectedLng = intent.getDoubleExtra(EXTRA_LNG, selectedLng)
            binding.etPlaceName.setText(initialName)
            binding.tvSelectedAddress.text = "선택한 핀의 장소명"
        } else {
            updateSelected(selectedLat, selectedLng)
        }

        // 7) 버튼
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSearch.setOnClickListener { search() }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search()
                true
            } else {
                false
            }
        }
        binding.btnConfirm.setOnClickListener { confirm() }

        // 8) 지도 시작
        startMap()
    }

    // 지도 켜기: 지도를 움직이고 멈추면 가운데 위치를 "고른 위치"로
    private fun startMap() {
        binding.mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {}
            override fun onMapError(error: Exception) {
                Log.e("KakaoMap", "장소 고르기 지도 오류: ${error.message}")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map
                map.setOnCameraMoveEndListener { _, cameraPosition, _ ->
                    updateSelected(cameraPosition.position.latitude, cameraPosition.position.longitude)
                }
            }

            override fun getPosition(): LatLng = LatLng.from(selectedLat, selectedLng)

            override fun getZoomLevel(): Int = 16
        })
    }

    // 고른 위치가 바뀌었을 때: 가까운 장소가 있으면 그 이름, 없으면 "지도에서 선택한 위치"
    private fun updateSelected(lat: Double, lng: Double) {
        selectedLat = lat
        selectedLng = lng

        val near = PlacePresets.nearest(lat, lng)
        if (near != null) {
            binding.etPlaceName.setText(near.name)
            binding.tvSelectedAddress.text = near.address
        } else {
            binding.etPlaceName.setText("지도에서 선택한 위치")
            binding.tvSelectedAddress.text = "직접 고른 위치 · 위 이름을 눌러 고칠 수 있어요"
        }
    }

    // 검색: 검색어에 맞는 장소를 최대 3개 보여주기
    private fun search() {
        hideKeyboard()
        val results = PlacePresets.search(binding.etSearch.text.toString()).take(3)

        binding.resultList.removeAllViews()
        if (results.isEmpty()) {
            binding.resultList.visibility = android.view.View.GONE
            showToast("검색 결과가 없어요. 지도를 움직여 핀을 맞춰 주세요")
            return
        }

        results.forEachIndexed { index, place ->
            if (index > 0) binding.resultList.addView(divider())
            binding.resultList.addView(resultRow(place))
        }
        binding.resultList.visibility = android.view.View.VISIBLE
    }

    // 검색 결과 한 줄: [장소 이름] ......... [주소] ›
    private fun resultRow(place: Place): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            val padH = dp(14f).toInt()
            val padV = dp(12f).toInt()
            setPadding(padH, padV, padH, padV)
            setOnClickListener { selectPlace(place) }
        }
        row.addView(TextView(this).apply {
            text = place.name
            setTextColor(PotColors.TEXT)
            textSize = 14f
            paint.isFakeBoldText = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(this).apply {
            text = "${place.address}  ›"
            setTextColor(PotColors.SUB_TEXT)
            textSize = 11f
        })
        return row
    }

    // 결과 사이 구분선
    private fun divider() = android.view.View(this).apply {
        setBackgroundColor(PotColors.LINE)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
    }

    // 검색 결과를 눌렀을 때: 그 장소로 지도 이동 + 고른 장소로 설정
    private fun selectPlace(place: Place) {
        selectedLat = place.lat
        selectedLng = place.lng
        binding.etPlaceName.setText(place.name)
        binding.tvSelectedAddress.text = place.address
        binding.resultList.visibility = android.view.View.GONE
        kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(place.lat, place.lng)))
    }

    // 이 위치로 설정: 이름과 좌표를 이전 화면(CR-01)으로 돌려주고 닫기
    private fun confirm() {
        val name = binding.etPlaceName.text.toString().trim()
        if (name.isEmpty()) {
            showToast("장소 이름을 입력해 주세요")
            return
        }
        val result = Intent()
            .putExtra(EXTRA_NAME, name)
            .putExtra(EXTRA_LAT, selectedLat)
            .putExtra(EXTRA_LNG, selectedLng)
        setResult(RESULT_OK, result)
        finish()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
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