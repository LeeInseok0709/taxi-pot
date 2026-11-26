package com.example.taxipot.feature.mypage.report

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
import com.example.taxipot.data.remote.dto.report.ReportResponse
import com.example.taxipot.databinding.ActivityTaxipotListBinding
import com.example.taxipot.databinding.ItemTaxipotCardBinding
import com.example.taxipot.feature.mypage.TaxiPotListScreen
import com.example.taxipot.feature.wallet.WalletFormat
import kotlinx.coroutines.launch

class MyReportViewModel : PagedListViewModel<ReportResponse>() {
    private val api = ApiClient.create(UserApi::class.java)
    override suspend fun fetch(page: Int, size: Int): ApiResult<PageResponse<ReportResponse>> =
        safeApiCall { api.getMyReports(page = page, size = size) }
}

/** 내가 접수한 신고 내역 (Figma 화면 없음, 마이페이지 메뉴) */
class MyReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaxipotListBinding
    private val viewModel: MyReportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaxipotListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        TaxiPotListScreen.setup(this, binding, "내가 접수한 신고 내역")
        binding.tvMore.setOnClickListener { viewModel.loadMore() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    TaxiPotListScreen.render(binding, state, "접수한 신고가 없어요.") { card, report -> bind(card, report) }
                }
            }
        }
        viewModel.refresh()
    }

    private fun bind(card: ItemTaxipotCardBinding, report: ReportResponse) {
        val type = when (report.type) {
            "NO_SHOW" -> "노쇼"
            "ABUSIVE" -> "불쾌한 언행"
            else -> "기타"
        }
        card.tvTitle.text = "${report.reportedNickname ?: "동승자"} · $type"
        card.tvChip.text = when (report.status) {
            "APPROVED" -> "✓ 승인"
            "REJECTED" -> "반려"
            else -> "접수됨 · 운영자 확인 중"
        }
        card.tvChip.visibility = View.VISIBLE
        card.tvDetail.text = listOfNotNull(
            report.reason,
            WalletFormat.shortDateTime(report.createdAt).ifEmpty { null }?.let { "접수 $it" }
        ).joinToString("\n")
        card.tvDetail.visibility = View.VISIBLE
    }
}
