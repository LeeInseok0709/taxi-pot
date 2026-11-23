package com.example.taxipot.feature.pot.list

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taxipot.databinding.ActivityPotListBinding
import com.example.taxipot.feature.pot.data.FakePotData
import com.example.taxipot.feature.pot.data.PotFilter
import com.example.taxipot.feature.pot.filter.PotFilterActivity
import com.example.taxipot.feature.pot.main.PotColors
import com.example.taxipot.feature.pot.main.PotListAdapter
import com.example.taxipot.feature.pot.main.roundedBg

/**
 * EX-02 내 주변 택시팟 (전체 목록)
 */
class PotListActivity : ComponentActivity() {

    private lateinit var binding: ActivityPotListBinding

    // 지금 적용된 필터·정렬 조건 (처음엔 기본값)
    private var filter = PotFilter()

    // 카드를 눌렀을 때: 택시팟 상세(EX-04)로 이동
    private val adapter = PotListAdapter { pot ->
        startActivity(com.example.taxipot.feature.pot.detail.PotDetailActivity.newIntent(this, pot.id))
    }

    // EX-03 필터 화면을 열고, "적용"을 누르면 결과를 받아서 목록에 반영
    private val filterLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                filter = PotFilter.from(result.data)
                showPots()
            }
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

        // 3) 고정 디자인
        binding.root.setBackgroundColor(PotColors.BACKGROUND)
        binding.btnMembers.background = roundedBg(PotColors.CARD, 10f, PotColors.LINE)
        binding.tvNotice.background = roundedBg(PotColors.CARD, 10f, PotColors.LINE)
        binding.ivNavHome.background = roundedBg(PotColors.ACCENT, 14f)

        // 4) 카드 목록 설정
        binding.rvPots.layoutManager = LinearLayoutManager(this)
        binding.rvPots.adapter = adapter

        // 5) 성별·정렬 버튼 → EX-03 필터·정렬 화면 열기
        binding.btnGender.setOnClickListener { openFilter() }
        binding.btnSort.setOnClickListener { openFilter() }
        // 인원은 명세서 GET /api/pots 에 조건값이 없어서 팀에서 정한 뒤 추가
        binding.btnMembers.setOnClickListener { showToast("인원 필터는 팀에서 정한 뒤 추가 예정") }

        // 6) 그 외 버튼
        binding.btnBell.setOnClickListener { showToast("알림 (준비 중)") }
        binding.navHome.setOnClickListener { finish() }   // 홈 화면으로 돌아가기
        binding.navWallet.setOnClickListener { showToast("내 지갑 (준비 중)") }
        binding.navMy.setOnClickListener { showToast("마이페이지 (준비 중)") }

        // 7) 목록 보여주기
        showPots()
    }

    // 지금 조건을 담아서 필터 화면 열기
    private fun openFilter() {
        filterLauncher.launch(filter.putInto(Intent(this, PotFilterActivity::class.java)))
    }

    // 조건을 적용해서 목록과 버튼 글자 다시 그리기
    // (지금은 가짜 데이터라 앱에서 거르고 정렬, 서버 연결 후에는 조건을 GET /api/pots 로 보냄)
    private fun showPots() {
        val result = filter.apply(FakePotData.pots)
        adapter.submitList(result)

        // 버튼 글자: 고른 조건 보여주기
        binding.btnGender.text = "${filter.genderText} ▾"
        binding.btnSort.text = "정렬: ${filter.sortText}"

        // 조건이 걸린 버튼은 베이지로 강조
        binding.btnGender.background = if (filter.genderOption != "ALL") {
            roundedBg(PotColors.ACCENT, 10f, PotColors.LINE)
        } else {
            roundedBg(PotColors.CARD, 10f, PotColors.LINE)
        }
        binding.btnSort.background = roundedBg(PotColors.ACCENT, 10f, PotColors.LINE)

        if (result.isEmpty()) showToast("조건에 맞는 택시팟이 없어요")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}