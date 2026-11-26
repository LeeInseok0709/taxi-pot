package com.example.taxipot.feature.wallet.withdraw

import android.os.Bundle
import android.view.View
import android.widget.Toast
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
import com.example.taxipot.databinding.ActivityWithdrawBinding
import com.example.taxipot.feature.wallet.WalletFormat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** WD-01 출금하기: 등록 계좌로 출금 (시연은 더미 송금이라 결과가 바로 나옴) */
class WithdrawActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWithdrawBinding
    private val viewModel: WithdrawViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWithdrawBinding.inflate(layoutInflater)
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

        binding.appBar.tvAppBarTitle.text = "출금하기"
        binding.appBar.btnBack.setOnClickListener { finish() }

        // TODO: AC-01 내 정보 수정(계좌 변경) 만들면 연결
        binding.rowChangeAccount.setOnClickListener {
            Toast.makeText(this, "계좌 변경은 내 정보 수정에서 할 수 있어요 (준비 중)", Toast.LENGTH_SHORT).show()
        }
        binding.etAmount.doAfterTextChanged { viewModel.clearAmountError() }
        binding.tvAll.setOnClickListener {
            val balance = viewModel.uiState.value.wallet?.balance ?: 0
            binding.etAmount.setText(if (balance > 0) balance.toString() else "")
            binding.etAmount.setSelection(binding.etAmount.text?.length ?: 0)
        }
        binding.btnWithdraw.setOnClickListener { submit() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is WithdrawEvent.Finished -> openResult(event)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 출금 실패 화면에서 '다시 시도'로 돌아오면 복구된 잔액으로 다시 표시
        viewModel.load()
    }

    private fun render(state: WithdrawUiState) {
        val account = state.account
        binding.tvAccount.text = if (account?.maskedAccountNumber.isNullOrBlank()) {
            "등록된 계좌가 없어요"
        } else {
            WithdrawFormat.account(account?.bankName, account?.maskedAccountNumber, account?.accountHolder)
        }

        val balance = state.wallet?.balance ?: 0
        binding.tilAmount.helperText = "사용 가능 잔액 ${WalletFormat.point(balance)} 이내"
        binding.tilAmount.error = state.amountError

        binding.tvError.text = state.errorMessage
        binding.tvError.visibility = if (state.errorMessage != null) View.VISIBLE else View.GONE

        binding.btnWithdraw.isEnabled = !state.isSubmitting
        binding.btnWithdraw.text = if (state.isSubmitting) "" else "출금 신청"
        binding.progress.visibility = if (state.isSubmitting) View.VISIBLE else View.GONE
    }

    private fun submit() {
        val amount = binding.etAmount.text?.toString()?.filter { it.isDigit() }?.take(9)?.toIntOrNull()
        if (!viewModel.validate(amount) || amount == null) return

        val account = viewModel.uiState.value.account
        MaterialAlertDialogBuilder(this)
            .setTitle("출금 신청")
            .setMessage(
                "${WalletFormat.point(amount)}을\n" +
                    "${WithdrawFormat.account(account?.bankName, account?.maskedAccountNumber, account?.accountHolder)}" +
                    "\n으로 출금할까요?"
            )
            .setNegativeButton("취소", null)
            .setPositiveButton("출금") { _, _ -> viewModel.withdraw(amount) }
            .show()
    }

    private fun openResult(event: WithdrawEvent.Finished) {
        startActivity(WithdrawResultActivity.newIntent(this, event.result))
        binding.etAmount.setText("")
        // 성공이면 출금 화면은 닫고, 실패면 '다시 시도'로 돌아올 수 있게 남겨 둔다
        if (event.result.status == "COMPLETED") finish()
    }
}
