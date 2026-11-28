package com.example.taxipot.feature.pot.create

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.UserApi
import com.example.taxipot.data.remote.dto.pot.PotCreateRequest
import com.example.taxipot.databinding.ActivityPotCreateBinding
import com.example.taxipot.feature.pot.data.FakePotData
import com.example.taxipot.feature.pot.data.Place
import com.example.taxipot.feature.pot.data.PlaceRepository
import com.example.taxipot.feature.pot.data.PotRepository
import com.example.taxipot.feature.pot.main.PotColors
import com.example.taxipot.feature.pot.main.roundedBg
import com.example.taxipot.feature.wallet.WalletBalance
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * CR-01 방 만들기 (입력 화면)
 * 출발지·목적지는 CR-02/03(PlacePickActivity), 좌석은 SeatPickActivity에서 골라 오고,
 * "다음"을 누르면 CR-04(PotCreateConfirmActivity)로 이동
 */
class PotCreateActivity : ComponentActivity() {

    private lateinit var binding: ActivityPotCreateBinding

    // ===== 입력한 값들 =====
    private var departure: Place? = null          // 출발지
    private var destination: Place? = null        // 목적지
    private var departureTime: Calendar? = null   // 출발 시각
    private var maxMembers = 3                    // 모집 인원 (3 또는 4, 기본 3 = 권장)
    private var genderOption = "ALL"              // ALL / FEMALE / MALE
    private var preferredSeat = "FRONT"           // 내 희망 좌석
    private var expectedFare: Int? = null         // 예상 총 요금 (카카오 자동 계산, 직접 수정 불가)
    private var myGender: String? = null          // 내 성별 M / F (모르면 null → 성별 옵션 제한 안 함)
    private var fareJob: Job? = null              // 진행 중인 요금 계산 (장소를 다시 고르면 이전 계산 취소)

    // 좌석 선택지 (화면 글자 ↔ 명세서 값)
    private val seatLabels = arrayOf("조수석", "뒷좌석 왼쪽", "뒷좌석 가운데", "뒷좌석 오른쪽", "상관없음")
    private val seatCodes = arrayOf("FRONT", "BACK_LEFT", "BACK_MID", "BACK_RIGHT", "NONE")

    // 한국 시간
    private val seoul = TimeZone.getTimeZone("Asia/Seoul")

    // CR-02 출발지 설정 결과 받기
    private val departureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                PlacePickActivity.placeFrom(result.data)?.let {
                    departure = it
                    render()
                    estimateFareIfReady()
                }
            }
        }

    // CR-03 목적지 설정 결과 받기
    private val destinationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                PlacePickActivity.placeFrom(result.data)?.let {
                    destination = it
                    render()
                    estimateFareIfReady()
                }
            }
        }

    // 좌석 선택 결과 받기
    private val seatLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                SeatPickActivity.seatFrom(result.data)?.let {
                    preferredSeat = it
                    render()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) XML 화면 붙이기
        binding = ActivityPotCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2) 상태바·하단바·키보드 여백 + 상태바 글자를 어두운 색으로
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // 3) 고정 디자인 (입력칸 둥근 테두리)
        binding.root.setBackgroundColor(PotColors.BACKGROUND)
        listOf(
            binding.etTitle, binding.tvDeparture, binding.tvDestination, binding.tvTime,
            binding.tvInfo, binding.tvFare, binding.tvSeat
        ).forEach { it.background = roundedBg(PotColors.CARD, 10f, PotColors.LINE) }
        binding.btnNext.background = roundedBg(PotColors.ACCENT, 12f)

        // 4) 버튼
        binding.btnBack.setOnClickListener { finish() }
        binding.tvDeparture.setOnClickListener {
            departureLauncher.launch(PlacePickActivity.newIntent(this, "출발지 설정", departure))
        }
        binding.tvDestination.setOnClickListener {
            destinationLauncher.launch(PlacePickActivity.newIntent(this, "목적지 설정", destination))
        }
        binding.tvTime.setOnClickListener { pickTime() }
        binding.tvSeat.setOnClickListener { pickSeat() }

        binding.optMembers3.setOnClickListener { maxMembers = 3; render() }
        binding.optMembers4.setOnClickListener { maxMembers = 4; render() }
        binding.optGenderAll.setOnClickListener { genderOption = "ALL"; render() }
        // 내 성별과 반대인 전용 옵션은 비활성화 (render 에서 처리, 여기선 한 번 더 막기)
        binding.optGenderFemale.setOnClickListener {
            if (myGender != "M") { genderOption = "FEMALE"; render() }
        }
        binding.optGenderMale.setOnClickListener {
            if (myGender != "F") { genderOption = "MALE"; render() }
        }

        // 예상 총 요금: 직접 입력 불가. 계산에 실패했으면 눌러서 다시 계산
        binding.tvFare.setOnClickListener {
            when {
                departure == null || destination == null ->
                    showToast("출발지와 목적지를 고르면 요금이 자동으로 계산돼요")
                expectedFare == null && fareJob?.isActive != true -> estimateFareIfReady()
            }
        }

        binding.btnNext.setOnClickListener { onNext() }

        // 5) 처음 화면 그리기 + 내 성별 불러오기 (성별 옵션 제한용)
        render()
        loadMyGender()
    }

    // 고른 값에 맞춰 글자·선택 표시 다시 그리기
    private fun render() {
        showValue(binding.tvDeparture, departure?.name, "출발지 선택 (지도 핀)  ›")
        showValue(binding.tvDestination, destination?.name, "목적지 선택 (지도 핀)  ›")
        showValue(binding.tvTime, departureTime?.let { timeText(it) }, "출발 시각 선택  ›")
        showValue(binding.tvSeat, seatLabels[seatCodes.indexOf(preferredSeat)], "희망 좌석 선택  ›")
        showValue(binding.tvFare, expectedFare?.let { "%,d원".format(it) }, "출발지·목적지를 고르면 자동 계산돼요")

        // 여자 회원은 남성전용, 남자 회원은 여성전용을 고를 수 없음
        val femaleLocked = myGender == "M"
        val maleLocked = myGender == "F"
        if ((genderOption == "FEMALE" && femaleLocked) || (genderOption == "MALE" && maleLocked)) {
            genderOption = "ALL"
        }

        selectOption(listOf(binding.optMembers3, binding.optMembers4),
            if (maxMembers == 3) binding.optMembers3 else binding.optMembers4)
        selectOption(listOf(binding.optGenderAll, binding.optGenderFemale, binding.optGenderMale),
            when (genderOption) {
                "FEMALE" -> binding.optGenderFemale
                "MALE" -> binding.optGenderMale
                else -> binding.optGenderAll
            })
        setLocked(binding.optGenderFemale, femaleLocked)
        setLocked(binding.optGenderMale, maleLocked)
    }

    // 고를 수 없는 옵션은 흐리게 + 누를 수 없게
    private fun setLocked(view: TextView, locked: Boolean) {
        view.isEnabled = !locked
        view.alpha = if (locked) 0.35f else 1f
    }

    // 내 정보(GET /api/users/me)에서 성별 가져오기. 실패하면 제한 없이 두고 서버 검증(GENDER_RESTRICTED)에 맡김
    private fun loadMyGender() {
        if (PotRepository.USE_FAKE) return
        lifecycleScope.launch {
            val result = safeApiCall { ApiClient.create(UserApi::class.java).getMyProfile() }
            if (result is ApiResult.Success) {
                myGender = when (result.data.gender) {
                    "M", "MALE" -> "M"
                    "F", "FEMALE" -> "F"
                    else -> null
                }
                render()
            }
        }
    }

    // 선택칸 글자: 값이 있으면 진한 글자, 없으면 회색 안내 글자
    private fun showValue(view: TextView, value: String?, placeholder: String) {
        view.text = value ?: placeholder
        view.setTextColor(if (value != null) PotColors.TEXT else PotColors.SUB_TEXT)
    }

    // 버튼 묶음 중 고른 것만 베이지로
    private fun selectOption(all: List<TextView>, selected: TextView) {
        all.forEach { view ->
            val isSelected = view == selected
            view.background = if (isSelected) {
                roundedBg(PotColors.ACCENT, 10f, PotColors.LINE)
            } else {
                roundedBg(PotColors.CARD, 10f, PotColors.LINE)
            }
            view.paint.isFakeBoldText = isSelected
            view.invalidate()
        }
    }

    // 출발 시각 고르기 (고른 시각이 이미 지났으면 내일로)
    private fun pickTime() {
        val now = Calendar.getInstance(seoul)
        val start = departureTime ?: (now.clone() as Calendar).apply { add(Calendar.MINUTE, 30) }

        TimePickerDialog(this, { _, hour, minute ->
            val picked = Calendar.getInstance(seoul).apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (picked.before(now)) picked.add(Calendar.DAY_OF_MONTH, 1)
            departureTime = picked
            render()
        }, start.get(Calendar.HOUR_OF_DAY), start.get(Calendar.MINUTE), true).show()
    }

    // 희망 좌석 고르기 → 좌석 선택 화면 (요금을 넣었으면 예치액도 보여줌)
    private fun pickSeat() {
        val hold = expectedFare?.let { PotRepository.holdAmount(it, maxMembers) }
        seatLauncher.launch(
            SeatPickActivity.newIntent(this, preferredSeat, hold, WalletBalance.latest ?: 0)
        )
    }

    // 출발지·목적지를 둘 다 골랐으면 카카오 예상 택시 요금을 자동으로 넣기 (직접 수정 불가)
    private fun estimateFareIfReady() {
        val dep = departure ?: return
        val dest = destination ?: return

        // 장소가 바뀌었으니 이전 요금은 버리고 다시 계산
        expectedFare = null
        render()
        binding.tvFareHint.text = "예상 요금 계산 중..."
        fareJob?.cancel()
        fareJob = lifecycleScope.launch {
            when (val result = PlaceRepository.estimateFare(dep, dest)) {
                is ApiResult.Success -> {
                    val estimate = result.data
                    expectedFare = estimate.fare
                    render()
                    val minutes = (estimate.durationSeconds + 59) / 60
                    val km = estimate.distanceMeters / 1000.0
                    binding.tvFareHint.text = "카카오 예상 요금 · 약 ${minutes}분 · %.1fkm".format(km)
                }
                is ApiResult.Failure, is ApiResult.NetworkError -> {
                    binding.tvFareHint.text = "예상 요금을 계산하지 못했어요. 요금 칸을 눌러 다시 시도해 주세요"
                }
            }
        }
    }

    // "오늘 21:30" / "내일 07:00"
    private fun timeText(time: Calendar): String {
        val today = Calendar.getInstance(seoul)
        val day = if (time.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) "오늘" else "내일"
        val clock = SimpleDateFormat("HH:mm", Locale.KOREA).apply { timeZone = seoul }.format(time.time)
        return "$day $clock"
    }

    // 서버로 보낼 시각 모양 "2026-11-25T21:30:00+09:00"
    private fun isoText(time: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA).apply { timeZone = seoul }
            .format(time.time) + "+09:00"

    // 다음: 입력 확인 (명세서 규칙과 같게) → CR-04 방 만들기 확인으로 이동
    private fun onNext() {
        val title = binding.etTitle.text.toString().trim()
        val fare = expectedFare
        val dep = departure
        val dest = destination
        val time = departureTime
        val minutesLeft = time?.let { (it.timeInMillis - System.currentTimeMillis()) / 60_000 }

        val error = when {
            title.isEmpty() -> "방 제목을 입력해 주세요"
            dep == null -> "출발지를 선택해 주세요"
            dest == null -> "목적지를 선택해 주세요"
            time == null || minutesLeft == null -> "출발 시각을 선택해 주세요"
            minutesLeft < 15 -> "출발 시각은 지금부터 15분 이후여야 해요"
            minutesLeft > 24 * 60 -> "출발 시각은 24시간 이내여야 해요"
            fare == null && fareJob?.isActive == true -> "예상 요금을 계산하는 중이에요. 잠시만 기다려 주세요"
            fare == null -> "예상 요금을 계산하지 못했어요. 요금 칸을 눌러 다시 시도해 주세요"
            fare > 500_000 -> "예상 요금이 50만 원을 넘어서 방을 만들 수 없어요"
            else -> null
        }
        if (error != null || dep == null || dest == null || time == null || fare == null) {
            if (error != null) showToast(error)
            return
        }

        // 서버로 보낼 요청 만들기 (오픈채팅 링크는 실시간 채팅으로 대체해 입력받지 않음)
        val request = PotCreateRequest(
            title = title,
            departureName = dep.name,
            departureLat = dep.lat,
            departureLng = dep.lng,
            destinationName = dest.name,
            destinationLat = dest.lat,
            destinationLng = dest.lng,
            departureTime = isoText(time),
            maxMembers = maxMembers,
            genderOption = genderOption,
            expectedFare = fare,
            openChatUrl = null,
            preferredSeat = preferredSeat
        )

        // CR-04 방 만들기 확인으로 이동
        startActivity(PotCreateConfirmActivity.newIntent(this, request))
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}