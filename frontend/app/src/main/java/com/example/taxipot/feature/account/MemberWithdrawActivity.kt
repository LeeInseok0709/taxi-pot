package com.example.taxipot.feature.account

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.UserApi
import com.example.taxipot.data.remote.api.WalletApi
import com.example.taxipot.data.remote.dto.user.MemberWithdrawRequest
import com.example.taxipot.data.remote.dto.wallet.WalletResponse
import com.example.taxipot.databinding.ActivityMemberWithdrawBinding
import com.example.taxipot.feature.wallet.WalletActivity
import com.example.taxipot.feature.wallet.WalletFormat
import com.example.taxipot.feature.wallet.withdraw.WithdrawActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * AC-02 회원 탈퇴 → 잔액 남음이면 AC-03 안내(출금하러 가기), 0원이면 AC-04 확인 → AC-05 완료.
 * Figma 정책대로 지갑 잔액이 0원일 때만 탈퇴시킨다 (서버는 잔액 소멸 동의로도 허용하지만, 돈이 사라지지 않게 앱에서 막음).
 */
class MemberWithdrawActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemberWithdrawBinding
    private val userApi by lazy { ApiClient.create(UserApi::class.java) }
    private val walletApi by lazy { ApiClient.create(WalletApi::class.java) }
    private var wallet: WalletResponse? = null
    private var working = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMemberWithdrawBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        binding.appBar.tvAppBarTitle.text = "회원 탈퇴"
        binding.appBar.btnBack.setOnClickListener { finish() }
        binding.etPassword.doAfterTextChanged { binding.tilPassword.error = null }
        binding.btnWithdraw.setOnClickListener { submit() }
    }

    override fun onResume() {
        super.onResume()
        // 출금하고 돌아오면 잔액 다시 확인
        lifecycleScope.launch {
            val result = safeApiCall { walletApi.getWallet() }
            if (result is ApiResult.Success) wallet = result.data
            renderBalance()
        }
    }

    private fun renderBalance() {
        val w = wallet
        binding.tvBalanceInfo.text = when {
            w == null -> "지갑 잔액을 확인하고 있어요."
            w.balance > 0 -> "지갑 잔액 ${WalletFormat.point(w.balance)} · 잔액이 0원일 때만 탈퇴할 수 있어요."
            else -> "지갑 잔액 0원 · 탈퇴가 가능합니다."
        }
    }

    private fun submit() {
        if (working) return
        val password = binding.etPassword.text?.toString().orEmpty()
        if (password.isEmpty()) {
            binding.tilPassword.error = "비밀번호를 입력해 주세요."
            return
        }
        val w = wallet
        when {
            w == null -> toast("지갑 잔액을 확인하지 못했어요. 잠시 후 다시 시도해 주세요.")
            w.debtAmount > 0 -> showDebtDialog(w.debtAmount)
            w.balance > 0 -> showBalanceDialog()          // AC-03
            else -> showConfirmDialog(password)          // AC-04
        }
    }

    /** AC-03 탈퇴 불가 · 잔액 남음 */
    private fun showBalanceDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("탈퇴할 수 없습니다")
            .setMessage("지갑에 아직 돈이 남아 있습니다.\n잔액을 모두 출금한 뒤 다시 시도해주세요.")
            .setNegativeButton("닫기", null)
            .setPositiveButton("출금하러 가기") { _, _ -> startActivity(Intent(this, WithdrawActivity::class.java)) }
            .show()
    }

    private fun showDebtDialog(debt: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("탈퇴할 수 없습니다")
            .setMessage("미수금 ${WalletFormat.point(debt)}이 남아 있어요.\n내 지갑에서 먼저 납부해 주세요.")
            .setNegativeButton("닫기", null)
            .setPositiveButton("내 지갑으로") { _, _ -> startActivity(WalletActivity.newIntent(this, focusDebt = true)) }
            .show()
    }

    /** AC-04 탈퇴 확인 · 잔액 0원 */
    private fun showConfirmDialog(password: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("정말 탈퇴하시겠습니까?")
            .setMessage("탈퇴 후에는 되돌릴 수 없습니다.")
            .setNegativeButton("취소", null)
            .setPositiveButton("확인") { _, _ -> withdraw(password) }
            .show()
    }

    private fun withdraw(password: String) {
        setWorking(true)
        lifecycleScope.launch {
            val result = safeApiCall { userApi.withdraw(MemberWithdrawRequest(password)) }
            setWorking(false)
            when (result) {
                is ApiResult.Success -> {
                    ApiClient.tokenManager.clear()
                    // AC-05 탈퇴 완료 (이전 화면은 모두 정리)
                    startActivity(
                        Intent(this@MemberWithdrawActivity, MemberWithdrawDoneActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                }
                is ApiResult.Failure -> when (result.code) {
                    "INVALID_CREDENTIALS" -> binding.tilPassword.error = "비밀번호가 맞지 않아요."
                    "ACTIVE_POT_EXISTS" -> toast("참여 중이거나 정산 전인 택시팟이 있어요. 끝난 뒤 탈퇴할 수 있어요.")
                    "UNPAID_DEBT_EXISTS" -> toast("미수금이 남아 있어요. 내 지갑에서 먼저 납부해 주세요.")
                    else -> toast(result.message)
                }
                is ApiResult.NetworkError -> toast(ProfileEditViewModel.NETWORK_ERROR_MESSAGE)
            }
        }
    }

    private fun setWorking(value: Boolean) {
        working = value
        binding.btnWithdraw.isEnabled = !value
        binding.btnWithdraw.text = if (value) "처리 중..." else "탈퇴하기"
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
