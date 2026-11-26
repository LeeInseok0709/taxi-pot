package com.example.taxipot.feature.mypage.history

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.core.ui.PagedListViewModel
import com.example.taxipot.data.remote.api.UserApi
import com.example.taxipot.data.remote.dto.common.PageResponse
import com.example.taxipot.data.remote.dto.user.MyPotHistoryResponse
import com.example.taxipot.databinding.ActivityTaxipotListBinding
import com.example.taxipot.databinding.ItemTaxipotCardBinding
import com.example.taxipot.feature.mypage.MyPageFormat
import com.example.taxipot.feature.mypage.TaxiPotListScreen
import com.example.taxipot.feature.pot.detail.PotDetailActivity
import com.example.taxipot.feature.rating.RatingActivity
import com.example.taxipot.feature.settlement.SettlementActivity
import com.example.taxipot.feature.wallet.WalletFormat
import kotlinx.coroutines.launch
import java.util.Locale

class MyPotHistoryViewModel : PagedListViewModel<MyPotHistoryResponse>() {
    private val api = ApiClient.create(UserApi::class.java)
    override suspend fun fetch(page: Int, size: Int): ApiResult<PageResponse<MyPotHistoryResponse>> =
        safeApiCall { api.getMyPots(page = page, size = size) }
}

/** MY-02 내 택시 이용 기록: 정산 완료 → 평가하기 / 취소·노쇼·퇴장 기록 */
class MyPotHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaxipotListBinding
    private val viewModel: MyPotHistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaxipotListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        TaxiPotListScreen.setup(this, binding, "내 택시 이용 기록")
        binding.tvMore.setOnClickListener { viewModel.loadMore() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    TaxiPotListScreen.render(binding, state, "아직 택시 이용 기록이 없어요.") { card, pot -> bind(card, pot) }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh() // 평가하고 돌아오면 '평가 완료'로
    }

    private fun bind(card: ItemTaxipotCardBinding, pot: MyPotHistoryResponse) {
        card.tvTitle.text = "${MyPageFormat.monthDay(pot.departureTime)} ${pot.departureName.orEmpty()} → ${pot.destinationName.orEmpty()}"

        val (chip, detail) = when {
            pot.myStatus == "NO_SHOW" -> "노쇼 퇴장" to "집결 기한까지 도착하지 않아 퇴장 처리됐어요"
            pot.myStatus == "KICKED" -> "퇴장" to "방장이 내보냈어요 · 예치금 환불"
            pot.myStatus == "CANCELED" -> "참여 취소" to "예치금 환불"
            pot.myStatus == "REJECTED" -> "입장 거절" to "예치금 환불"
            pot.myStatus == "REQUESTED" -> "입장 요청 중" to "방장의 수락을 기다리고 있어요"
            pot.potStatus == "CANCELED" -> "취소" to "전액 환불"
            pot.potStatus == "COMPLETED" -> "정산 완료" to buildString {
                append("분담 ")
                append(pot.fareShare?.let { won(it) } ?: "-")
                if ((pot.uncoveredAmount ?: 0) > 0) append(" · 미수금 ${WalletFormat.point(pot.uncoveredAmount ?: 0)}")
            }
            pot.potStatus == "RUNNING" -> "탑승 중" to "운영자가 실요금을 확인하면 정산돼요"
            pot.potStatus == "CLOSED" -> "마감" to "곧 출발해요"
            else -> "모집 중" to "출발 전이에요"
        }
        card.tvChip.text = chip
        card.tvChip.visibility = View.VISIBLE
        card.tvDetail.text = detail
        card.tvDetail.visibility = View.VISIBLE

        if (pot.potStatus == "COMPLETED" && pot.myStatus == "BOARDED") {
            // 정산 완료: 평가하기 / 평가 완료, 카드를 누르면 정산 내역
            if (pot.isRated == true) {
                card.btnOutline.text = "평가 완료"
                card.btnOutline.isEnabled = false
                card.btnOutline.visibility = View.VISIBLE
            } else {
                card.btnPrimary.text = "평가하기"
                card.btnPrimary.visibility = View.VISIBLE
                card.btnPrimary.setOnClickListener { startActivity(RatingActivity.newIntent(this, pot.potId)) }
            }
            card.root.setOnClickListener { startActivity(SettlementActivity.newIntent(this, pot.potId)) }
        } else if (pot.potStatus in listOf("RECRUITING", "CLOSED", "RUNNING") &&
            pot.myStatus in listOf("JOINED", "BOARDED", "REQUESTED")
        ) {
            // 진행 중인 방은 방 상세로
            card.root.setOnClickListener { startActivity(PotDetailActivity.newIntent(this, pot.potId)) }
        }
    }

    private fun won(amount: Int) = String.format(Locale.KOREA, "%,d원", amount)
}
