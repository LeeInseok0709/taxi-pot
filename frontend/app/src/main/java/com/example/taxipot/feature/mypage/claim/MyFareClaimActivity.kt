package com.example.taxipot.feature.mypage.claim

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.taxipot.R
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
import com.example.taxipot.feature.settlement.SettlementActivity
import com.example.taxipot.feature.wallet.WalletFormat
import kotlinx.coroutines.launch
import java.util.Locale

/** 방장으로 연 방 중 정산 완료(COMPLETED) / 처리 중(RUNNING) */
class MyFareClaimViewModel : PagedListViewModel<MyPotHistoryResponse>() {
    private val api = ApiClient.create(UserApi::class.java)

    var completedTab = true
        private set

    fun selectTab(completed: Boolean) {
        if (completedTab == completed) return
        completedTab = completed
        refresh()
    }

    override suspend fun fetch(page: Int, size: Int): ApiResult<PageResponse<MyPotHistoryResponse>> =
        safeApiCall {
            api.getMyPots(role = "HOST", status = if (completedTab) "COMPLETED" else "RUNNING", page = page, size = size)
        }
}

/** MY-03 · MY-04 내 실요금 청구 내역 (방장): 완료된 건 / 처리중 */
class MyFareClaimActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaxipotListBinding
    private val viewModel: MyFareClaimViewModel by viewModels()
    private lateinit var tabDone: TextView
    private lateinit var tabPending: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaxipotListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        TaxiPotListScreen.setup(this, binding, "내 청구 내역")
        binding.tvMore.setOnClickListener { viewModel.loadMore() }

        tabDone = addTab("완료된 건", first = true) { viewModel.selectTab(true); renderTabs() }
        tabPending = addTab("처리중", first = false) { viewModel.selectTab(false); renderTabs() }
        binding.tabs.visibility = View.VISIBLE
        renderTabs()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    val empty = if (viewModel.completedTab) "정산까지 끝난 청구가 없어요." else "처리 중인 청구가 없어요."
                    TaxiPotListScreen.render(binding, state, empty) { card, pot -> bind(card, pot) }
                    binding.boxFooter.visibility = if (viewModel.completedTab && state.items.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.tvFooter.text = "운영자가 승인해 정산까지 끝난 청구입니다."
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun bind(card: ItemTaxipotCardBinding, pot: MyPotHistoryResponse) {
        card.tvTitle.text = "${MyPageFormat.monthDay(pot.departureTime)} ${pot.departureName.orEmpty()} → ${pot.destinationName.orEmpty()}"
        card.tvChip.visibility = View.VISIBLE
        card.tvDetail.visibility = View.VISIBLE

        if (pot.potStatus == "COMPLETED") {
            card.tvChip.text = "✓ 정산 완료"
            card.tvDetail.text = "승인 ${pot.actualFare?.let { won(it) } ?: "-"}"
            // 방장 정산금 = 승인된 실요금 전액 (명세서 HOST_PAYOUT)
            pot.actualFare?.let {
                card.tvStrong.text = "정산금 +${WalletFormat.point(it)} 지급"
                card.tvStrong.visibility = View.VISIBLE
            }
            card.btnOutline.text = "영수증 · 정산 내역"
            card.btnOutline.visibility = View.VISIBLE
            card.btnOutline.setOnClickListener { startActivity(SettlementActivity.newIntent(this, pot.potId)) }
        } else {
            card.tvChip.text = "처리 중"
            card.tvDetail.text = "실요금을 청구했다면 운영자 확인을 기다리는 중이에요.\n아직이라면 탑승 화면에서 청구해 주세요."
            card.root.setOnClickListener { startActivity(PotDetailActivity.newIntent(this, pot.potId)) }
        }
    }

    private fun addTab(label: String, first: Boolean, onClick: () -> Unit): TextView {
        val density = resources.displayMetrics.density
        val tab = TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.taxipot_text))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(0, (44 * density).toInt(), 1f).apply {
                if (first) marginEnd = (8 * density).toInt()
            }
        }
        binding.tabs.addView(tab)
        return tab
    }

    private fun renderTabs() {
        listOf(tabDone to viewModel.completedTab, tabPending to !viewModel.completedTab).forEach { (tab, selected) ->
            tab.setBackgroundResource(if (selected) R.drawable.bg_taxipot_option_selected else R.drawable.bg_taxipot_option)
            tab.paint.isFakeBoldText = selected
            tab.invalidate()
        }
    }

    private fun won(amount: Int) = String.format(Locale.KOREA, "%,d원", amount)
}
