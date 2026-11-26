package com.example.taxipot.feature.settlement

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.taxipot.databinding.ActivitySettlementBinding
import com.example.taxipot.databinding.ItemTaxipotRowBinding
import com.example.taxipot.feature.rating.RatingActivity
import com.example.taxipot.feature.wallet.WalletFormat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Locale

/** ST-01 정산 내역: 운영자가 실요금을 승인하면 정산 결과 확인 → 동승자 평가로 */
class SettlementActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettlementBinding
    private val viewModel: SettlementViewModel by viewModels()
    private var potId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettlementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        potId = intent.getLongExtra(EXTRA_POT_ID, -1L)
        binding.appBar.tvAppBarTitle.text = "정산 내역"
        binding.appBar.btnBack.setOnClickListener { finish() }
        binding.btnReceipt.setOnClickListener { viewModel.loadReceipt(potId) }
        binding.btnRate.setOnClickListener { startActivity(RatingActivity.newIntent(this, potId)) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is SettlementEvent.ShowReceipt -> showReceipt(event)
                            is SettlementEvent.ShowMessage -> showToast(event.message)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (potId > 0) viewModel.load(potId) else showToast("택시팟 정보를 찾을 수 없어요.")
    }

    private fun render(state: SettlementUiState) {
        binding.progress.visibility = if (state.isLoading && state.settlement == null) View.VISIBLE else View.GONE
        binding.tvError.text = state.errorMessage
        binding.tvError.visibility = if (state.errorMessage != null) View.VISIBLE else View.GONE

        val pot = state.settlement
        binding.content.visibility = if (pot != null) View.VISIBLE else View.INVISIBLE
        if (pot == null) return
        val mine = state.mine

        // 상태 칩 + 요금 카드
        binding.tvChip.text = if (state.isSettled) "✓ 정산 완료" else "정산 대기 중"
        binding.tvFare.text = if (pot.actualFare != null) {
            "실제 요금 ${won(pot.actualFare)}"
        } else {
            "예상 요금 ${won(pot.expectedFare)}"
        }
        binding.tvFareSub.text = if (state.isSettled && mine?.fareShare != null) {
            "탑승 ${pot.boardedCount}명 · 1인 분담금 ${won(mine.fareShare)}"
        } else {
            "탑승 ${pot.boardedCount}명 · 운영자가 실요금을 확인하면 정산돼요"
        }

        // 내 정산 상세
        binding.rows.removeAllViews()
        if (mine == null) {
            addRow("내 정산 정보가 없어요", "")
        } else if (mine.status == "REFUNDED") {
            addRow("예치액", WalletFormat.point(mine.heldAmount))
            addRow("전액 환불", "+" + WalletFormat.point(mine.heldAmount), bold = true)
        } else {
            addRow("예치액", WalletFormat.point(mine.heldAmount))
            mine.fareShare?.let { addRow("분담금", WalletFormat.point(it)) }
            mine.feeAmount?.let { addRow("수수료", WalletFormat.point(it)) }
            mine.diffAmount?.let { diff ->
                when {
                    diff > 0 -> addRow("예치금 환불", "+" + WalletFormat.point(diff), bold = true)
                    diff < 0 -> addRow("추가 차감", "−" + WalletFormat.point(-diff), bold = true)
                    else -> addRow("추가 차감·환불", "없음")
                }
            }
            if (mine.uncoveredAmount > 0) {
                addRow("미수금 발생", WalletFormat.point(mine.uncoveredAmount), bold = true)
            }
        }

        // 미수금 안내 (내게 미수금이 생겼을 때만)
        val hasDebt = (mine?.uncoveredAmount ?: 0) > 0
        binding.boxDebt.visibility = if (hasDebt) View.VISIBLE else View.GONE
        if (hasDebt && mine != null) {
            val due = WalletFormat.shortDateTime(mine.debtDueAt)
            binding.tvDebtInfo.text = buildString {
                append("다음 충전 시 자동 차감됩니다.")
                if (due.isNotEmpty()) append("\n$due 까지 내 지갑에서 납부해 주세요.")
                append("\n기한이 지나면 택시팟 이용이 제한돼요.")
            }
        }

        binding.btnReceipt.visibility = if (pot.receiptUrl != null) View.VISIBLE else View.GONE
        binding.btnReceipt.isEnabled = !state.isLoadingReceipt
        binding.btnReceipt.text = if (state.isLoadingReceipt) "불러오는 중..." else "영수증 사진 보기"

        // 평가는 정산 후에만
        binding.btnRate.isEnabled = state.isSettled
        binding.btnRate.text = if (state.isSettled) "동승자 평가하기" else "정산 후 평가할 수 있어요"
    }

    private fun addRow(label: String, value: String, bold: Boolean = false) {
        val row = ItemTaxipotRowBinding.inflate(LayoutInflater.from(this), binding.rows, false)
        row.tvLabel.text = label
        row.tvValue.text = value
        row.tvValue.paint.isFakeBoldText = bold
        binding.rows.addView(row.root)
    }

    private fun showReceipt(event: SettlementEvent.ShowReceipt) {
        val image = ImageView(this).apply {
            adjustViewBounds = true
            setImageBitmap(event.bitmap)
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, 0)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("영수증 사진")
            .setView(image)
            .setPositiveButton("닫기", null)
            .show()
    }

    private fun won(amount: Int): String = String.format(Locale.KOREA, "%,d원", amount)

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val EXTRA_POT_ID = "potId"

        fun newIntent(context: Context, potId: Long): Intent =
            Intent(context, SettlementActivity::class.java).putExtra(EXTRA_POT_ID, potId)
    }
}
