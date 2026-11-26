package com.example.taxipot.feature.wallet.charge

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.taxipot.databinding.ActivityWalletChargeBinding
import com.example.taxipot.feature.wallet.WalletFormat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * WL-02 충전하기.
 * 앱 결제(PortOne)는 캡스톤 범위 밖이라 서버에 구현되지 않았고(명세서 3.7.0), 시연 충전은 운영자 잔액 조정으로 한다.
 * 그래서 금액 입력·미수금 차감 계산까지 보여주고, 결제하기는 시연 충전 안내로 대신한다. (Figma에도 '정적 결제 화면'으로 표시됨)
 * 다른 화면에서 이름(feature.wallet.charge.WalletChargeActivity)으로 여니 패키지·이름을 바꾸지 말 것.
 */
class WalletChargeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWalletChargeBinding
    private val viewModel: WalletChargeViewModel by viewModels()

    /** 버튼으로 금액을 바꿀 때 입력칸 변경을 다시 처리하지 않도록 */
    private var updatingText = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWalletChargeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        binding.appBar.tvAppBarTitle.text = "충전하기"
        binding.appBar.btnBack.setOnClickListener { finish() }

        binding.tilAmount.helperText = String.format(
            Locale.KOREA, "1회 %,d~%,d원 · 보관 한도 %,dP",
            WalletChargeViewModel.MIN_CHARGE, WalletChargeViewModel.MAX_CHARGE, WalletChargeViewModel.WALLET_LIMIT
        )

        binding.etAmount.doAfterTextChanged { text ->
            if (updatingText) return@doAfterTextChanged
            val amount = text?.toString()?.filter { it.isDigit() }?.take(7)?.toIntOrNull() ?: 0
            viewModel.setAmount(amount)
        }
        binding.btnAdd1.setOnClickListener { addAmount(10_000) }
        binding.btnAdd2.setOnClickListener { addAmount(20_000) }
        binding.btnAdd3.setOnClickListener { addAmount(30_000) }
        binding.btnAdd5.setOnClickListener { addAmount(50_000) }
        binding.btnPay.setOnClickListener { submit() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }

        if (savedInstanceState == null) {
            setAmountText(DEFAULT_AMOUNT)
            viewModel.setAmount(DEFAULT_AMOUNT)
        }
        viewModel.loadWallet()
    }

    private fun addAmount(add: Int) {
        viewModel.addAmount(add)
        setAmountText(viewModel.uiState.value.amount)
    }

    private fun setAmountText(amount: Int) {
        updatingText = true
        binding.etAmount.setText(if (amount > 0) amount.toString() else "")
        binding.etAmount.setSelection(binding.etAmount.text?.length ?: 0)
        updatingText = false
    }

    private fun render(state: WalletChargeUiState) {
        binding.tilAmount.error = state.amountError
        binding.tvSumAmount.text = WalletFormat.point(state.amount)
        binding.tvSumDebt.text = if (state.debtDeduction > 0) "−" + WalletFormat.point(state.debtDeduction) else "0P"
        binding.tvSumActual.text = WalletFormat.point(state.actualCharge)
        binding.rowDebt.visibility = if ((state.wallet?.debtAmount ?: 0) > 0) View.VISIBLE else View.GONE
    }

    private fun submit() {
        if (!viewModel.validate()) return
        val state = viewModel.uiState.value
        MaterialAlertDialogBuilder(this)
            .setTitle("시연 충전 안내")
            .setMessage(
                "앱 결제(PortOne)는 아직 연동되지 않았어요.\n" +
                    "시연 기간에는 운영자가 잔액 조정으로 충전해 드려요.\n\n" +
                    "요청 금액: ${WalletFormat.point(state.amount)}" +
                    if (state.debtDeduction > 0) "\n(미수금 ${WalletFormat.point(state.debtDeduction)} 먼저 차감)" else ""
            )
            .setPositiveButton("확인") { _, _ -> finish() }
            .show()
    }

    companion object {
        private const val DEFAULT_AMOUNT = 10_000
    }
}
