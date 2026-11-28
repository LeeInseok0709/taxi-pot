package com.example.taxipot.feature.account

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
import com.example.taxipot.data.remote.api.CommonApi
import com.example.taxipot.data.remote.api.UserApi
import com.example.taxipot.data.remote.dto.common.BankResponse
import com.example.taxipot.data.remote.dto.user.AccountUpdateRequest
import com.example.taxipot.databinding.ActivityAccountEditBinding
import com.example.taxipot.feature.wallet.withdraw.WithdrawFormat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * 출금 계좌 관리 (Figma엔 AC-01의 메뉴만 있음).
 * 예금주는 가입한 이름과 같아야 해서 이름을 그대로 쓰고, 바꾸려면 현재 비밀번호가 필요하다.
 */
class AccountEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountEditBinding
    private val userApi by lazy { ApiClient.create(UserApi::class.java) }
    private val commonApi by lazy { ApiClient.create(CommonApi::class.java) }

    private var banks: List<BankResponse> = emptyList()
    private var selectedBank: BankResponse? = null
    private var holderName: String = ""
    private var saving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAccountEditBinding.inflate(layoutInflater)
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

        binding.appBar.tvAppBarTitle.text = "출금 계좌 관리"
        binding.appBar.btnBack.setOnClickListener { finish() }
        binding.etBank.setOnClickListener { pickBank() }
        binding.tilBank.setEndIconOnClickListener { pickBank() }
        binding.etAccountNumber.doAfterTextChanged { binding.tilAccountNumber.error = null }
        binding.etPassword.doAfterTextChanged { binding.tilPassword.error = null }
        binding.btnSave.setOnClickListener { submit() }

        load()
    }

    private fun load() {
        lifecycleScope.launch {
            val profileJob = async { safeApiCall { userApi.getMyProfile() } }
            val accountJob = async { safeApiCall { userApi.getMyAccount() } }
            val banksJob = async { safeApiCall { commonApi.getBanks() } }

            val profile = profileJob.await()
            if (profile is ApiResult.Success) {
                holderName = profile.data.name
                binding.etHolder.setText(holderName)
            }
            val accountResult = accountJob.await()
            val account = if (accountResult is ApiResult.Success) accountResult.data else null
            binding.tvCurrent.text = if (account?.maskedAccountNumber.isNullOrBlank()) {
                "등록된 계좌가 없어요"
            } else {
                WithdrawFormat.account(account?.bankName, account?.maskedAccountNumber, account?.accountHolder)
            }
            val bankResult = banksJob.await()
            if (bankResult is ApiResult.Success) banks = bankResult.data
        }
    }

    private fun pickBank() {
        if (banks.isEmpty()) {
            toast("은행 목록을 불러오는 중이에요. 잠시 후 다시 눌러 주세요.")
            load()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("은행 선택")
            .setItems(banks.map { it.name }.toTypedArray()) { _, which ->
                selectedBank = banks[which]
                binding.etBank.setText(banks[which].name)
                binding.tilBank.error = null
            }
            .show()
    }

    private fun submit() {
        if (saving) return
        val bank = selectedBank
        val accountNumber = binding.etAccountNumber.text?.toString()?.trim().orEmpty()
        val digits = accountNumber.filter { it.isDigit() }
        val password = binding.etPassword.text?.toString().orEmpty()

        var ok = true
        if (bank == null) { binding.tilBank.error = "은행을 선택해 주세요."; ok = false }
        if (digits.length !in 10..14 || !accountNumber.all { it.isDigit() || it == '-' }) {
            binding.tilAccountNumber.error = "계좌번호는 숫자 10~14자리로 입력해 주세요."; ok = false
        }
        if (password.isEmpty()) { binding.tilPassword.error = "현재 비밀번호를 입력해 주세요."; ok = false }
        if (holderName.isEmpty()) { toast("내 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요."); ok = false }
        if (!ok || bank == null) return

        setSaving(true)
        lifecycleScope.launch {
            val result = safeApiCall {
                userApi.updateMyAccount(AccountUpdateRequest(password, bank.code, accountNumber, holderName))
            }
            setSaving(false)
            when (result) {
                is ApiResult.Success -> {
                    toast("출금 계좌를 바꿨어요.")
                    finish()
                }
                is ApiResult.Failure -> when (result.code) {
                    "INVALID_CREDENTIALS" -> binding.tilPassword.error = "현재 비밀번호가 맞지 않아요."
                    "ACCOUNT_HOLDER_MISMATCH" -> toast("예금주가 가입한 이름과 달라요.")
                    else -> toast(result.message)
                }
                is ApiResult.NetworkError -> toast(ProfileEditViewModel.NETWORK_ERROR_MESSAGE)
            }
        }
    }

    private fun setSaving(value: Boolean) {
        saving = value
        binding.btnSave.isEnabled = !value
        binding.btnSave.text = if (value) "저장 중..." else "계좌 변경"
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
