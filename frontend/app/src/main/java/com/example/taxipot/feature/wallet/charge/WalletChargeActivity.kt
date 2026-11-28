package com.example.taxipot.feature.wallet.charge

import android.content.Intent
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
import com.example.taxipot.feature.account.AccountEditActivity
import com.example.taxipot.feature.wallet.WalletFormat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * WL-02 충전하기: 등록한 본인 계좌(충전 계좌)에서 지갑으로 충전하는 화면.
 * 외부 결제창(PortOne) 연동은 캡스톤 범위 밖이라(명세서 3.7.0) 서버 충전 API가 아직 없고, 시연 충전은 운영자 잔액 조정으로 한다.
 * 그래서 지금은 금액·미수금 차감 계산과 충전 계좌까지 보여주고, 충전하기는 시연 충전 안내로 대신한다.
 * (서버에 즉시 충전이 생기면 submit()에서 그 API만 부르면 됨)
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
        // 충전 계좌 카드를 누르면 계좌 변경
        binding.cardAccount.setOnClickListener {
            startActivity(Intent(this, AccountEditActivity::class.java))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }

        if (savedInstanceState == null) {
            setAmountText(DEFAULT_AMOUNT)
            viewModel.setAmount(DEFAULT_AMOUNT)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load() // 계좌를 바꾸고 돌아와도 최신으로
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

        // 충전 계좌 카드
        val account = state.account
        if (state.hasAccount && account != null) {
            binding.tvAccount.text = "${account.bankName.orEmpty()} ${account.maskedAccountNumber}"
            binding.chipDefault.visibility = View.VISIBLE
            binding.tvAccountHint.text = "출금 계좌와 같은 본인 계좌예요 · 눌러서 변경"
        } else {
            binding.tvAccount.text = if (state.accountLoaded) "등록된 계좌가 없어요" else "불러오는 중..."
            binding.chipDefault.visibility = View.GONE
            binding.tvAccountHint.text = "눌러서 충전할 본인 계좌를 등록해 주세요"
        }
    }

    private fun submit() {
        if (!viewModel.validate()) return
        val state = viewModel.uiState.value
        val account = state.account
        MaterialAlertDialogBuilder(this)
            .setTitle("시연 충전 안내")
            .setMessage(
                "지금은 앱에서 바로 충전되지 않아요.\n" +
                    "시연 기간에는 운영자가 지갑에 충전해 드려요.\n\n" +
                    "충전 계좌: ${account?.bankName.orEmpty()} ${account?.maskedAccountNumber.orEmpty()}\n" +
                    "충전 금액: ${WalletFormat.point(state.amount)}" +
                    if (state.debtDeduction > 0) "\n(미수금 ${WalletFormat.point(state.debtDeduction)} 먼저 차감)" else ""
            )
            .setPositiveButton("확인") { _, _ -> finish() }
            .show()
    }

    companion object {
        private const val DEFAULT_AMOUNT = 10_000
    }
}
