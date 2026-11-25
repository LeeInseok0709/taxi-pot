package com.example.taxipot.feature.pot.create

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taxipot.databinding.ActivityPotCreateBinding
import com.example.taxipot.feature.pot.data.Place
import com.example.taxipot.feature.pot.main.PotColors
import com.example.taxipot.feature.pot.main.roundedBg
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * CR-01 방 만들기 (입력 화면)
 * 출발지·목적지는 CR-02/03(PlacePickActivity)에서 골라 오고, "다음"을 누르면 CR-04(확인)로 이동 예정
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
            binding.tvInfo, binding.etFare, binding.tvSeat, binding.etOpenChat
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
        binding.optGenderFemale.setOnClickListener { genderOption = "FEMALE"; render() }
        binding.optGenderMale.setOnClickListener { genderOption = "MALE"; render() }

        binding.btnNext.setOnClickListener { onNext() }

        // 5) 처음 화면 그리기
        render()
    }

    // 고른 값에 맞춰 글자·선택 표시 다시 그리기
    private fun render() {
        showValue(binding.tvDeparture, departure?.name, "출발지 선택 (지도 핀)  ›")
        showValue(binding.tvDestination, destination?.name, "목적지 선택 (지도 핀)  ›")
        showValue(binding.tvTime, departureTime?.let { timeText(it) }, "출발 시각 선택  ›")
        showValue(binding.tvSeat, seatLabels[seatCodes.indexOf(preferredSeat)], "희망 좌석 선택  ›")

        selectOption(listOf(binding.optMembers3, binding.optMembers4),
            if (maxMembers == 3) binding.optMembers3 else binding.optMembers4)
        selectOption(listOf(binding.optGenderAll, binding.optGenderFemale, binding.optGenderMale),
            when (genderOption) {
                "FEMALE" -> binding.optGenderFemale
                "MALE" -> binding.optGenderMale
                else -> binding.optGenderAll
            })
    }

    // 선택칸 글자: 값이 있으면 진한 글자, 없으면 회색 안내 글자
    private fun showValue(view: TextView, value: String?, placeholder: String) {
        view.text = value ?: placeholder
        view.setTextColor(if (value != null) PotColors.TEXT else 0xFF9A9A9A.toInt())
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

    // 희망 좌석 고르기
    private fun pickSeat() {
        AlertDialog.Builder(this)
            .setTitle("내 희망 좌석")
            .setItems(seatLabels) { _, which ->
                preferredSeat = seatCodes[which]
                render()
            }
            .show()
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

    // 다음: 입력 확인 (명세서 규칙과 같게)
    private fun onNext() {
        val title = binding.etTitle.text.toString().trim()
        val fare = binding.etFare.text.toString().toIntOrNull()
        val time = departureTime
        val minutesLeft = time?.let { (it.timeInMillis - System.currentTimeMillis()) / 60_000 }

        val error = when {
            title.isEmpty() -> "방 제목을 입력해 주세요"
            departure == null -> "출발지를 선택해 주세요"
            destination == null -> "목적지를 선택해 주세요"
            time == null || minutesLeft == null -> "출발 시각을 선택해 주세요"
            minutesLeft < 15 -> "출발 시각은 지금부터 15분 이후여야 해요"
            minutesLeft > 24 * 60 -> "출발 시각은 24시간 이내여야 해요"
            fare == null -> "예상 총 요금을 입력해 주세요"
            fare > 500_000 -> "예상 총 요금은 50만 원 이하로 입력해 주세요"
            else -> null
        }
        if (error != null) {
            showToast(error)
            return
        }

        // CR-04(방 만들기 확인)는 다음 단계에서 연결
        showToast("입력 완료! ${departure?.name} → ${destination?.name}, ${isoText(time!!)}")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}