package com.example.taxipot.feature.wallet.withdraw

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taxipot.R
import com.example.taxipot.data.remote.dto.wallet.WithdrawalResponse
import com.example.taxipot.databinding.ActivityWithdrawResultBinding
import com.example.taxipot.feature.wallet.WalletActivity
import com.example.taxipot.feature.wallet.WalletFormat

/** WD-02 출금 완료 / WD-03 출금 실패 (결과에 따라 한 화면에서 나눠 보여줌) */
class WithdrawResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWithdrawResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWithdrawResultBinding.inflate(layoutInflater)
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

        val success = intent.getStringExtra(EXTRA_STATUS) == "COMPLETED"
        val amount = intent.getIntExtra(EXTRA_AMOUNT, 0)
        val balanceAfter = if (intent.hasExtra(EXTRA_BALANCE_AFTER)) intent.getIntExtra(EXTRA_BALANCE_AFTER, 0) else null

        binding.appBar.tvAppBarTitle.text = if (success) "출금 완료" else "출금 실패"
        binding.appBar.btnBack.setOnClickListener { if (success) goWallet() else finish() }

        if (success) {
            binding.ivResult.setImageResource(R.drawable.ic_check)
            binding.ivResult.setBackgroundResource(R.drawable.bg_taxipot_circle_primary)
            binding.tvTitle.text = "출금이 완료되었습니다"

            binding.boxSuccess.visibility = View.VISIBLE
            binding.tvAmount.text = WalletFormat.point(amount)
            binding.tvAccount.text = intent.getStringExtra(EXTRA_ACCOUNT).orEmpty()
            binding.tvBalanceAfter.text = balanceAfter?.let { WalletFormat.point(it) } ?: "-"
            binding.tvDate.text = WalletFormat.shortDateTime(intent.getStringExtra(EXTRA_COMPLETED_AT))

            binding.btnRetry.visibility = View.GONE
        } else {
            binding.ivResult.setImageResource(R.drawable.ic_close)
            binding.ivResult.setBackgroundResource(R.drawable.bg_taxipot_circle)
            binding.tvTitle.text = "출금에 실패했습니다"

            binding.boxFail.visibility = View.VISIBLE
            binding.tvFailReason.text = intent.getStringExtra(EXTRA_FAIL_REASON) ?: "송금하지 못했습니다."
            binding.tvRestored.text = "출금 신청 금액 ${WalletFormat.point(amount)}는 지갑에 다시 돌려드렸어요."
            binding.tvRestoredBalance.text = balanceAfter?.let { WalletFormat.point(it) } ?: "-"
            binding.boxCannot.visibility = View.VISIBLE

            binding.btnRetry.visibility = View.VISIBLE
            binding.btnRetry.setOnClickListener { finish() } // 출금하기 화면으로 돌아감
        }

        binding.btnWallet.setOnClickListener { goWallet() }

        // 성공 후 뒤로가기는 (이미 닫힌) 출금 화면 대신 지갑으로
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(success) {
            override fun handleOnBackPressed() = goWallet()
        })
    }

    /** 지갑으로: 지갑 위에 쌓인 출금 화면들은 정리 */
    private fun goWallet() {
        startActivity(
            Intent(this, WalletActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    companion object {
        private const val EXTRA_STATUS = "status"
        private const val EXTRA_AMOUNT = "amount"
        private const val EXTRA_ACCOUNT = "account"
        private const val EXTRA_BALANCE_AFTER = "balanceAfter"
        private const val EXTRA_COMPLETED_AT = "completedAt"
        private const val EXTRA_FAIL_REASON = "failReason"

        fun newIntent(context: Context, result: WithdrawalResponse): Intent =
            Intent(context, WithdrawResultActivity::class.java).apply {
                putExtra(EXTRA_STATUS, result.status)
                putExtra(EXTRA_AMOUNT, result.amount)
                putExtra(
                    EXTRA_ACCOUNT,
                    WithdrawFormat.account(result.bankName, result.maskedAccountNumber, result.accountHolder)
                )
                result.balanceAfter?.let { putExtra(EXTRA_BALANCE_AFTER, it) }
                putExtra(EXTRA_COMPLETED_AT, result.completedAt ?: result.requestedAt)
                putExtra(EXTRA_FAIL_REASON, result.failReason)
            }
    }
}
