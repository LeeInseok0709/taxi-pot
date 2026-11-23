package com.example.taxipot.feature.pot.list

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taxipot.databinding.ActivityPotListBinding
import com.example.taxipot.feature.pot.data.FakePotData
import com.example.taxipot.feature.pot.data.PotSummaryResponse
import com.example.taxipot.feature.pot.data.isVacancy
import com.example.taxipot.feature.pot.main.PotColors
import com.example.taxipot.feature.pot.main.PotListAdapter
import com.example.taxipot.feature.pot.main.roundedBg

/**
 * EX-02 내 주변 택시팟 (전체 목록)
 */
class PotListActivity : ComponentActivity() {

    private lateinit var binding: ActivityPotListBinding

    // 카드를 눌렀을 때 (나중에 EX-04 택시팟 상세 화면으로 이동)
    private val adapter = PotListAdapter { pot ->
        showToast("${pot.departureName} → ${pot.destinationName} (상세 화면 준비 중)")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) XML 화면 붙이기
        binding = ActivityPotListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2) 상태바·하단바 여백 + 상태바 글자를 어두운 색으로
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // 3) 디자인 적용 (둥근 배경, 테두리)
        applyDesign()

        // 4) 카드 목록 설정
        binding.rvPots.layoutManager = LinearLayoutManager(this)
        binding.rvPots.adapter = adapter

        // 5) 성별·인원·정렬 버튼 (다음 단계 EX-03 필터·정렬 화면에서 연결)
        binding.btnGender.setOnClickListener { showToast("필터·정렬 화면 (다음 단계)") }
        binding.btnMembers.setOnClickListener { showToast("필터·정렬 화면 (다음 단계)") }
        binding.btnSort.setOnClickListener { showToast("필터·정렬 화면 (다음 단계)") }

        // 6) 그 외 버튼
        binding.btnBell.setOnClickListener { showToast("알림 (준비 중)") }
        binding.navHome.setOnClickListener { finish() }   // 홈 화면으로 돌아가기
        binding.navWallet.setOnClickListener { showToast("내 지갑 (준비 중)") }
        binding.navMy.setOnClickListener { showToast("마이페이지 (준비 중)") }

        // 7) 가짜 데이터로 목록 보여주기 (서버 연결 후 이 줄만 바꾸면 됨)
        showPots(FakePotData.pots)
    }

    // 결원 방은 맨 위에 고정, 나머지는 출발 시각 순서로 보여주기
    // (서버 GET /api/pots 도 같은 규칙으로 정렬해서 주지만, 지금은 가짜 데이터라 앱에서 정렬)
    private fun showPots(source: List<PotSummaryResponse>) {
        val sorted = source.sortedWith(
            compareByDescending<PotSummaryResponse> { it.isVacancy }
                .thenBy { it.departureTime }
        )
        adapter.submitList(sorted)
    }

    // 둥근 배경과 테두리 입히기 (색은 PotStyle.kt 에서 관리)
    private fun applyDesign() {
        binding.root.setBackgroundColor(PotColors.BACKGROUND)
        binding.btnGender.background = roundedBg(PotColors.CARD, 10f, PotColors.LINE)
        binding.btnMembers.background = roundedBg(PotColors.CARD, 10f, PotColors.LINE)
        binding.btnSort.background = roundedBg(PotColors.ACCENT, 10f, PotColors.LINE)
        binding.tvNotice.background = roundedBg(PotColors.CARD, 10f, PotColors.LINE)
        binding.ivNavHome.background = roundedBg(PotColors.ACCENT, 14f)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}