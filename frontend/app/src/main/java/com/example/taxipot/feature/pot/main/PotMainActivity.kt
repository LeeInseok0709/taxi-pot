package com.example.taxipot.feature.pot.main

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taxipot.BuildConfig
import com.example.taxipot.databinding.ActivityPotMainBinding
import com.example.taxipot.feature.pot.data.FakePotData
import com.example.taxipot.feature.pot.data.PotSummaryResponse
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import java.security.MessageDigest

class PotMainActivity : ComponentActivity() {

    private lateinit var binding: ActivityPotMainBinding
    private var kakaoMap: KakaoMap? = null                      // 지도가 준비되면 여기 들어옴
    private var pots: List<PotSummaryResponse> = emptyList()   // 지금 화면에 보여주는 방 목록

    // 카드를 눌렀을 때: 지도를 그 방 위치로 이동 (나중에 방 상세 화면으로 이동)
    private val adapter = PotListAdapter { pot ->
        moveCamera(pot.departureLat, pot.departureLng)
        showToast("${pot.departureName} → ${pot.destinationName} (상세 화면 준비 중)")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) 카카오맵에 내 키 알려주기
        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        // 2) 키 해시 출력 (팀원 등록이 끝나면 삭제)
        logKeyHash()

        // 3) XML 화면 붙이기
        binding = ActivityPotMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 4) 상태바·하단바 여백 + 상태바 글자를 어두운 색으로 (밝은 배경이라서)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // 5) 디자인 적용 (둥근 배경, 테두리)
        applyDesign()

        // 6) 잔액 표시 (지금은 가짜 값)
        binding.tvBalance.text = "%,dP".format(FakePotData.FAKE_BALANCE)

        // 7) 방 카드 목록 설정
        binding.rvPots.layoutManager = LinearLayoutManager(this)
        binding.rvPots.adapter = adapter

        // 8) 버튼들 (아직 화면이 없는 것은 "준비 중" 메시지)
        binding.btnMyLocation.setOnClickListener {
            moveCamera(FakePotData.BAEKSEOK_LAT, FakePotData.BAEKSEOK_LNG)
        }
        binding.btnBell.setOnClickListener { showToast("알림 (준비 중)") }
        binding.tvSearch.setOnClickListener { showToast("장소 검색 (준비 중)") }
        binding.btnCreatePot.setOnClickListener { showToast("방 만들기 (준비 중)") }
        binding.navWallet.setOnClickListener { showToast("내 지갑 (준비 중)") }
        binding.navMy.setOnClickListener { showToast("마이페이지 (준비 중)") }

        // 9) 지도 시작
        startMap()

        // 10) 가짜 데이터로 목록 보여주기 (서버 연결 후 이 줄만 바꾸면 됨)
        showPots(FakePotData.pots.sortedBy { it.departureTime })
    }

    // 둥근 배경과 테두리 입히기 (색은 PotStyle.kt 에서 관리)
    private fun applyDesign() {
        binding.root.setBackgroundColor(PotColors.BACKGROUND)
        binding.tvBalance.background = roundedBg(PotColors.ACCENT, 12f)
        binding.tvSearch.background = roundedBg(PotColors.CARD, 10f, PotColors.LINE)
        binding.mapCard.background = roundedBg(PotColors.CARD, 14f, PotColors.LINE)
        binding.btnMyLocation.background = roundedBg(PotColors.WHITE, 20f, PotColors.LINE)
        binding.btnCreatePot.background = roundedBg(PotColors.ACCENT, 12f)
        binding.ivNavHome.background = roundedBg(PotColors.ACCENT, 14f)
    }

    // 지도 켜기
    private fun startMap() {
        binding.mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {}
            override fun onMapError(error: Exception) {
                Log.e("KakaoMap", "지도 오류: ${error.message}")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                Log.d("KakaoMap", "지도 준비 완료!")
                kakaoMap = map
                drawMarkers()   // 지도가 준비되면 말풍선 핀 찍기

                // 핀을 눌렀을 때: 목록에서 그 방으로 스크롤
                map.setOnLabelClickListener { _, _, label ->
                    val potId = label.tag as? Long
                    val index = pots.indexOfFirst { it.id == potId }
                    if (index >= 0) {
                        val pot = pots[index]
                        binding.rvPots.smoothScrollToPosition(index)
                        showToast("${pot.departureName} → ${pot.destinationName}")
                    }
                    true
                }
            }

            // 지도 처음 위치: 백석대학교
            override fun getPosition(): LatLng =
                LatLng.from(FakePotData.BAEKSEOK_LAT, FakePotData.BAEKSEOK_LNG)

            // 확대 정도 (숫자가 클수록 더 확대)
            override fun getZoomLevel(): Int = 16
        })
    }

    // 목록, 제목 숫자, 핀을 새 데이터로 다시 그리기
    private fun showPots(newPots: List<PotSummaryResponse>) {
        pots = newPots
        adapter.submitList(newPots)
        binding.tvNearbyTitle.text = "내 주변 모집 중 택시팟 ${newPots.size}"
        drawMarkers()
    }

    // ===== 핀 크기 설정 (숫자만 바꿔서 조절) =====
    // 보통 말풍선 크기 (크면 줄이고, 작으면 키우기)
    private val bubbleScale = 0.55f
    // 작은 말풍선 크기 (중간 줌일 때)
    private val smallBubbleScale = 0.38f
    // 이 줌 레벨부터 "작은 말풍선" (이보다 더 축소하면 점만 보임)
    private val smallBubbleZoom = 14
    // 이 줌 레벨부터 "보통 말풍선"
    private val normalBubbleZoom = 16

    // 방마다 출발지 위치에 핀 찍기 (지도 확대 정도에 따라 모양이 자동으로 바뀜)
    private fun drawMarkers() {
        val map = kakaoMap ?: return                  // 지도가 아직 준비 안 됐으면 그냥 끝
        val labelManager = map.labelManager ?: return
        val layer = labelManager.layer ?: return

        layer.removeAll()   // 예전 핀 지우고
        pots.forEach { pot ->
            val shortName = pot.departureName.replace("대학교", "대")
            val text = "$shortName → ${pot.destinationName} ${"%,d".format(pot.expectedFare)}원"

            // 줌 레벨별 모양 3가지: 0부터 점 / smallBubbleZoom부터 작은 말풍선 / normalBubbleZoom부터 보통 말풍선
            val styles = labelManager.addLabelStyles(
                LabelStyles.from(
                    LabelStyle.from(createDotBitmap()).setZoomLevel(0),
                    LabelStyle.from(createBubbleBitmap(text, smallBubbleScale)).setZoomLevel(smallBubbleZoom),
                    LabelStyle.from(createBubbleBitmap(text, bubbleScale)).setZoomLevel(normalBubbleZoom)
                )
            )
            layer.addLabel(
                LabelOptions.from("pot_${pot.id}", LatLng.from(pot.departureLat, pot.departureLng))
                    .setStyles(styles)
                    .setTag(pot.id)   // 핀에 방 번호 달아두기 (눌렀을 때 사용)
            )
        }
    }

    // 많이 축소했을 때 보이는 작은 점 (흰 테두리 + 베이지 원 + 진한 테두리)
    private fun createDotBitmap(): Bitmap {
        val size = (dp(14f) * bubbleScale).toInt().coerceAtLeast(8)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PotColors.ACCENT }
        canvas.drawCircle(center, center, center - 1f, fill)

        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PotColors.TEXT
            style = Paint.Style.STROKE
            strokeWidth = (dp(1.5f) * bubbleScale).coerceAtLeast(1f)
        }
        canvas.drawCircle(center, center, center - stroke.strokeWidth, stroke)

        return bitmap
    }

    // 말풍선 핀 이미지를 코드로 그리기 (scale: 크기 비율)
    private fun createBubbleBitmap(text: String, scale: Float): Bitmap {
        fun s(value: Float) = dp(value) * scale   // 비율을 곱한 크기

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PotColors.TEXT
            textSize = s(12f)
            isFakeBoldText = true
        }
        val padH = s(8f)
        val padV = s(6f)
        val icon = s(10f)
        val gap = s(5f)
        val fm = textPaint.fontMetrics
        val textHeight = fm.descent - fm.ascent

        val width = (padH + icon + gap + textPaint.measureText(text) + padH).toInt()
        val height = (padV + textHeight + padV).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rect = RectF(1f, 1f, width - 1f, height - 1f)

        // 배경
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PotColors.ACCENT }
        canvas.drawRoundRect(rect, s(6f), s(6f), fill)

        // 테두리
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PotColors.LINE
            style = Paint.Style.STROKE
            strokeWidth = s(1f).coerceAtLeast(1f)
        }
        canvas.drawRoundRect(rect, s(6f), s(6f), stroke)

        // 왼쪽 작은 동그라미 (위치 표시)
        val pin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PotColors.TEXT
            style = Paint.Style.STROKE
            strokeWidth = s(1.5f).coerceAtLeast(1f)
        }
        canvas.drawCircle(padH + icon / 2, height / 2f, icon / 2 - s(1f), pin)

        // 글자
        val baseline = height / 2f - (fm.ascent + fm.descent) / 2
        canvas.drawText(text, padH + icon + gap, baseline, textPaint)

        return bitmap
    }

    // 말풍선 핀 이미지를 코드로 그리기 (베이지 배경 + 테두리 + 동그라미 + 글자)
    private fun createBubbleBitmap(text: String): Bitmap {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PotColors.TEXT
            textSize = dp(12f)
            isFakeBoldText = true
        }
        val padH = dp(8f)
        val padV = dp(6f)
        val icon = dp(10f)
        val gap = dp(5f)
        val fm = textPaint.fontMetrics
        val textHeight = fm.descent - fm.ascent

        val width = (padH + icon + gap + textPaint.measureText(text) + padH).toInt()
        val height = (padV + textHeight + padV).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rect = RectF(1f, 1f, width - 1f, height - 1f)

        // 배경
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PotColors.ACCENT }
        canvas.drawRoundRect(rect, dp(6f), dp(6f), fill)

        // 테두리
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PotColors.LINE
            style = Paint.Style.STROKE
            strokeWidth = dp(1f)
        }
        canvas.drawRoundRect(rect, dp(6f), dp(6f), stroke)

        // 왼쪽 작은 동그라미 (위치 표시)
        val pin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PotColors.TEXT
            style = Paint.Style.STROKE
            strokeWidth = dp(1.5f)
        }
        canvas.drawCircle(padH + icon / 2, height / 2f, icon / 2 - dp(1f), pin)

        // 글자
        val baseline = height / 2f - (fm.ascent + fm.descent) / 2
        canvas.drawText(text, padH + icon + gap, baseline, textPaint)

        return bitmap
    }

    // 지도를 원하는 위치로 이동
    private fun moveCamera(lat: Double, lng: Double) {
        kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(lat, lng)))
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

    // 키 해시 출력 함수 (팀원 등록이 끝나면 함수째 삭제)
    private fun logKeyHash() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        info.signingInfo?.apkContentsSigners?.forEach { sig ->
            val md = MessageDigest.getInstance("SHA")
            md.update(sig.toByteArray())
            Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.NO_WRAP))
        }
    }
}