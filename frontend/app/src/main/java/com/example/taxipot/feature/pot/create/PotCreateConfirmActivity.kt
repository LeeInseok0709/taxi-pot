package com.example.taxipot.feature.pot.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.data.remote.dto.pot.PotCreateRequest
import com.example.taxipot.data.remote.dto.pot.PotDetailResponse
import com.example.taxipot.databinding.ActivityPotCreateConfirmBinding
import com.example.taxipot.feature.pot.data.FakePotData
import com.example.taxipot.feature.pot.data.PotRepository
import com.example.taxipot.feature.pot.data.clockOf
import com.example.taxipot.feature.pot.main.PotColors
import com.example.taxipot.feature.pot.main.PotMainActivity
import com.example.taxipot.feature.pot.main.roundedBg
import com.example.taxipot.feature.pot.room.WaitingRoomActivity
import com.example.taxipot.feature.pot.room.showActivePotDialog
import com.example.taxipot.feature.wallet.WalletBalance
import kotlinx.coroutines.launch

/**
 * CR-04 방 만들기 확인
 * CR-01에서 입력한 내용을 보여주고, "예치하고 방 만들기"를 누르면 POST /api/pots
 */
class PotCreateConfirmActivity : ComponentActivity() {

    private lateinit var binding: ActivityPotCreateConfirmBinding
    private lateinit var request: PotCreateRequest

    // 방 만들기 담당
    private val viewModel: PotCreateViewModel by viewModels()

    // 성공 후 화면 이동은 한 번만
    private var moved = false

    companion object {
        private const val EXTRA_REQUEST = "create_request"

        /** 입력한 내용(요청)을 담아서 확인 화면 열기 */
        fun newIntent(context: Context, request: PotCreateRequest): Intent =
            Intent(context, PotCreateConfirmActivity::class.java)
                .putExtra(EXTRA_REQUEST, ApiClient.gson.toJson(request))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) CR-01에서 넘겨준 입력 내용 꺼내기
        val json = intent.getStringExtra(EXTRA_REQUEST)
        if (json == null) {
            finish()
            return
        }
        request = ApiClient.gson.fromJson(json, PotCreateRequest::class.java)

        // 2) XML 화면 붙이기
        binding = ActivityPotCreateConfirmBinding.inflate(layoutInflater)
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

        // 4) 디자인
        binding.root.setBackgroundColor(PotColors.BACKGROUND)
        binding.summaryCard.background = roundedBg(PotColors.CARD, 14f, PotColors.LINE)
        binding.moneyCard.background = roundedBg(PotColors.CARD, 14f, PotColors.LINE)
        binding.tvInfo.background = roundedBg(PotColors.CARD, 10f, PotColors.LINE)
        binding.btnCreate.background = roundedBg(PotColors.ACCENT, 12f)

        // 5) 버튼
        binding.btnBack.setOnClickListener { finish() }
        binding.btnCreate.setOnClickListener { viewModel.create(request) }

        // 6) 내용 채우기 + 결과 기다리기
        showSummary()
        observeViewModel()
    }

    // 입력한 내용을 화면에 채우기
    private fun showSummary() {
        binding.tvRouteTitle.text = "${request.departureName} → ${request.destinationName}"
        binding.tvTimeValue.text = clockOf(request.departureTime)
        binding.tvMembersValue.text = "${request.maxMembers}명 · 방장 포함"
        binding.tvGenderValue.text = when (request.genderOption) {
            "FEMALE" -> "여성전용"
            "MALE" -> "남성전용"
            else -> "제한없음"
        }
        binding.tvFareValue.text = "%,d원".format(request.expectedFare)
        binding.tvSeatValue.text = when (request.preferredSeat) {
            "FRONT" -> "조수석"
            "BACK_LEFT" -> "뒷좌석 왼쪽"
            "BACK_MID" -> "뒷좌석 가운데"
            "BACK_RIGHT" -> "뒷좌석 오른쪽"
            else -> "상관없음"
        }

        // 예치액과 현재 잔액 (잔액은 지갑 API 연결 전까지 가짜 값)
        val hold = PotRepository.holdAmount(request.expectedFare, request.maxMembers)
        val balance = WalletBalance.latest ?: 0
        binding.tvHoldValue.text = "%,dP".format(hold)
        binding.tvBalanceValue.text = "%,dP".format(balance)

        if (hold > balance) {
            binding.tvInfo.text = "ⓘ 잔액이 예치액보다 적어요. 충전한 뒤 방을 만들 수 있어요."
        }
    }

    // 버튼 상태, 실패 메시지, 성공하면 화면 이동
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.btnCreate.text = if (state.isLoading) "방 만드는 중..." else "예치하고 방 만들기"
                    binding.btnCreate.isEnabled = !state.isLoading

                    state.errorMessage?.let {
                        showToast(it)
                        viewModel.messageShown()
                    }

                    // 이미 다른 방에 참여 중 → 그 방으로 가는 안내 창
                    state.activePotId?.let {
                        showActivePotDialog(it)
                        viewModel.activePotShown()
                    }

                    val created = state.created
                    if (created != null && !moved) {
                        moved = true
                        goNext(created)
                    }
                }
            }
        }
    }

    // 방이 만들어지면: 방 만들기 화면들을 닫고 (홈만 남기고) 대기실(RD-01)로
    private fun goNext(created: PotDetailResponse) {
        showToast("방이 만들어졌어요! 예치금 %,dP".format(created.holdAmount))

        val home = Intent(this, PotMainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(home)
        startActivity(WaitingRoomActivity.newIntent(this, created.id))
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}