package com.example.taxipot.feature.account

import android.content.Intent
import android.os.Bundle
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
import com.example.taxipot.databinding.ActivityProfileEditBinding
import kotlinx.coroutines.launch

/** AC-01 내 정보 수정: 이메일·이름·학교·성별·학번(보기만) / 닉네임·휴대폰·마케팅 동의 / 비밀번호 변경 / 출금 계좌 관리 */
class ProfileEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileEditBinding
    private val viewModel: ProfileEditViewModel by viewModels()
    private var filled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
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

        binding.appBar.tvAppBarTitle.text = "내 정보 수정"
        binding.appBar.btnBack.setOnClickListener { finish() }

        binding.etNickname.doAfterTextChanged { viewModel.onNicknameChanged() }
        binding.etPhone.doAfterTextChanged { viewModel.onPhoneChanged() }
        binding.tvNicknameCheck.setOnClickListener {
            viewModel.checkNickname(binding.etNickname.text?.toString().orEmpty())
        }
        binding.rowPassword.setOnClickListener { startActivity(Intent(this, PasswordChangeActivity::class.java)) }
        binding.rowAccount.setOnClickListener { startActivity(Intent(this, AccountEditActivity::class.java)) }
        binding.btnSave.setOnClickListener {
            viewModel.save(
                binding.etNickname.text?.toString().orEmpty(),
                binding.etPhone.text?.toString().orEmpty(),
                binding.cbMarketing.isChecked
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is ProfileEditEvent.ShowMessage -> toast(event.message)
                            ProfileEditEvent.Saved -> {
                                toast("내 정보를 저장했어요.")
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    private fun render(state: ProfileEditUiState) {
        val profile = state.profile
        // 처음 한 번만 입력칸 채우기 (고치던 내용이 덮이지 않게)
        if (profile != null && !filled) {
            filled = true
            // 이메일·이름은 보여주기만 (서버에서 바꿀 수 없는 정보)
            binding.etEmail.setText(profile.email)
            binding.etName.setText(profile.name)
            binding.etUniversity.setText(profile.university.orEmpty())
            binding.etGender.setText(
                when (profile.gender) {
                    "M" -> "남자"
                    "F" -> "여자"
                    else -> ""
                }
            )
            binding.etStudentNo.setText(profile.studentNo.orEmpty())
            binding.etNickname.setText(profile.nickname)
            binding.etPhone.setText(AccountFormat.phone(profile.phone))
            binding.cbMarketing.isChecked = profile.agreeMarketing == true
        }

        binding.tilNickname.error = state.nicknameError
        val nickname = binding.etNickname.text?.toString()?.trim()
        binding.tilNickname.helperText =
            if (state.checkedNickname != null && state.checkedNickname == nickname) "사용할 수 있는 닉네임이에요." else null
        binding.tvNicknameCheck.text = if (state.isCheckingNickname) "확인 중" else "중복확인"
        binding.tilPhone.error = state.phoneError

        val account = state.account
        binding.tvAccountValue.text = if (account?.maskedAccountNumber.isNullOrBlank()) {
            "미등록"
        } else {
            "${account?.bankName.orEmpty()} ${account?.maskedAccountNumber}"
        }

        binding.btnSave.isEnabled = !state.isSaving
        binding.btnSave.text = if (state.isSaving) "저장 중..." else "저장"
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
