package com.example.taxipot.feature.wallet

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.taxipot.R
import com.example.taxipot.core.ui.TaxiPotBottomNav
import com.example.taxipot.data.remote.dto.wallet.DebtResponse
import com.example.taxipot.databinding.ActivityWalletBinding
import com.example.taxipot.databinding.ItemWalletDebtBinding
import com.example.taxipot.databinding.ItemWalletHistoryBinding
import com.example.taxipot.feature.wallet.charge.WalletChargeActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** WL-01 내 지갑: 잔액 · 미수금 납부 · 포인트/충전/출금 내역 */
class WalletActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWalletBinding
    private val viewModel: WalletViewModel by viewModels()

    /** 미수금 때문에 들어왔으면 불러온 뒤 미수금 칸으로 스크롤 */
    private var focusDebtPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWalletBinding.inflate(layoutInflater)
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

        TaxiPotBottomNav.setup(this, binding.bottomNav, TaxiPotBottomNav.Tab.WALLET)
        focusDebtPending = intent.getBooleanExtra(EXTRA_FOCUS_DEBT, false)

        // TODO: MY-05 알림 화면 만들면 연결
        binding.btnBell.setOnClickListener { showToast("알림 (준비 중)") }
        // WL-02 충전하기 (시연 충전 안내까지, 실제 충전은 운영자 잔액 조정)
        binding.btnCharge.setOnClickListener {
            startActivity(Intent(this, WalletChargeActivity::class.java))
        }
        // TODO: WD-01 출금하기
        binding.btnWithdraw.setOnClickListener { showToast("출금하기 (준비 중)") }

        binding.tabPoint.setOnClickListener { viewModel.selectTab(WalletTab.POINT) }
        binding.tabCharge.setOnClickListener { viewModel.selectTab(WalletTab.CHARGE) }
        binding.tabWithdraw.setOnClickListener { viewModel.selectTab(WalletTab.WITHDRAW) }
        binding.tvMore.setOnClickListener { viewModel.loadMoreHistory() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.events.collect { handleEvent(it) } }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_FOCUS_DEBT, false)) focusDebtPending = true
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun render(state: WalletUiState) {
        binding.progress.visibility = if (state.isLoading && state.wallet == null) View.VISIBLE else View.GONE
        showText(binding.tvError, state.errorMessage)

        // 잔액
        val wallet = state.wallet
        binding.tvBalance.text = WalletFormat.point(wallet?.balance ?: 0)
        binding.tvBalanceSub.text = "예치 중 ${WalletFormat.point(wallet?.heldBalance ?: 0)} · " +
            "미수금 ${WalletFormat.point(wallet?.debtAmount ?: 0)}"

        renderDebts(state)
        renderTabs(state.tab)
        renderHistory(state)

        // 미수금 때문에 들어왔으면 한 번만 안내하고 미수금 칸으로 이동
        if (focusDebtPending && !state.isLoading && wallet != null) {
            focusDebtPending = false
            if (state.debts.isNotEmpty()) {
                showToast("미수금이 있어 택시팟 이용이 제한돼요. 먼저 납부해 주세요.")
                scrollTo(binding.boxDebt)
            }
        }
    }

    private fun renderDebts(state: WalletUiState) {
        binding.boxDebt.visibility = if (state.debts.isEmpty()) View.GONE else View.VISIBLE
        if (state.debts.isEmpty()) return

        val total = state.debts.sumOf { it.debtAmount }
        binding.tvDebtTitle.text = "미수금 ${state.debts.size}건 · ${WalletFormat.point(total)}"

        binding.debtList.removeAllViews()
        state.debts.forEach { debt ->
            val item = ItemWalletDebtBinding.inflate(LayoutInflater.from(this), binding.debtList, false)
            item.tvDebtPot.text = debt.potTitle ?: "택시팟 #${debt.potId}"
            val due = WalletFormat.shortDateTime(debt.debtDueAt)
            item.tvDebtDue.text = when {
                debt.debtStatus == "OVERDUE" -> "납부 기한 지남 · 이용이 제한돼요"
                due.isNotEmpty() -> "$due 까지 납부"
                else -> ""
            }
            item.tvDebtAmount.text = WalletFormat.point(debt.debtAmount)

            val paying = state.payingSettlementId == debt.settlementId
            item.btnPay.isEnabled = state.payingSettlementId == null
            item.btnPay.text = if (paying) "납부 중..." else "납부"
            item.btnPay.setOnClickListener { confirmPay(debt) }
            binding.debtList.addView(item.root)
        }
    }

    private fun confirmPay(debt: DebtResponse) {
        val balance = viewModel.uiState.value.wallet?.balance ?: 0
        MaterialAlertDialogBuilder(this)
            .setTitle("미수금 납부")
            .setMessage(
                "${debt.potTitle ?: "택시팟"} 미수금 ${WalletFormat.point(debt.debtAmount)}을 잔액에서 납부할까요?\n" +
                    "(현재 잔액 ${WalletFormat.point(balance)}, 모자라면 있는 만큼만 납부돼요)"
            )
            .setNegativeButton("취소", null)
            .setPositiveButton("납부") { _, _ -> viewModel.payDebt(debt) }
            .show()
    }

    private fun renderTabs(tab: WalletTab) {
        renderTab(binding.tabPoint, tab == WalletTab.POINT)
        renderTab(binding.tabCharge, tab == WalletTab.CHARGE)
        renderTab(binding.tabWithdraw, tab == WalletTab.WITHDRAW)
    }

    private fun renderTab(view: TextView, selected: Boolean) {
        view.setBackgroundResource(
            if (selected) R.drawable.bg_taxipot_option_selected else R.drawable.bg_taxipot_option
        )
        view.paint.isFakeBoldText = selected
        view.invalidate()
    }

    private fun renderHistory(state: WalletUiState) {
        binding.historyList.removeAllViews()
        state.history.forEach { history ->
            val item = ItemWalletHistoryBinding.inflate(LayoutInflater.from(this), binding.historyList, false)
            item.tvType.text = WalletFormat.typeLabel(history.type)
            item.tvAmount.text = WalletFormat.signedPoint(history.amount)
            item.tvAmount.setTextColor(
                ContextCompat.getColor(this, if (history.amount < 0) R.color.taxipot_sub_text else R.color.taxipot_text)
            )
            item.tvDetail.text = listOfNotNull(
                history.potTitle,
                WalletFormat.shortDateTime(history.createdAt).ifEmpty { null }
            ).joinToString(" · ")
            binding.historyList.addView(item.root)
        }

        val empty = state.history.isEmpty() && !state.isLoadingHistory
        binding.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.tvEmpty.text = when (state.tab) {
            WalletTab.POINT -> "아직 포인트 내역이 없어요."
            WalletTab.CHARGE -> "아직 충전 내역이 없어요."
            WalletTab.WITHDRAW -> "아직 출금 내역이 없어요."
        }
        binding.tvMore.visibility = if (state.hasMoreHistory) View.VISIBLE else View.GONE
        binding.tvMore.text = if (state.isLoadingHistory) "불러오는 중..." else "더 보기"
    }

    private fun handleEvent(event: WalletEvent) {
        when (event) {
            is WalletEvent.ShowMessage -> showToast(event.message)
        }
    }

    private fun scrollTo(target: View) {
        binding.scrollView.post {
            val rect = Rect()
            target.getDrawingRect(rect)
            binding.scrollView.offsetDescendantRectToMyCoords(target, rect)
            binding.scrollView.smoothScrollTo(0, (rect.top - 24).coerceAtLeast(0))
        }
    }

    private fun showText(view: TextView, message: String?) {
        view.text = message
        view.visibility = if (message != null) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val EXTRA_FOCUS_DEBT = "focusDebt"

        /** @param focusDebt 미수금 때문에 들어온 경우 true (미수금 칸으로 스크롤) */
        fun newIntent(context: Context, focusDebt: Boolean = false): Intent =
            Intent(context, WalletActivity::class.java)
                .putExtra(EXTRA_FOCUS_DEBT, focusDebt)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
    }
}
