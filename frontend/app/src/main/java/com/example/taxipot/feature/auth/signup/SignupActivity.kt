package com.example.taxipot.feature.auth.signup

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
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
import com.example.taxipot.R
import com.example.taxipot.databinding.ActivitySignupBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

/**
 * AU-01 회원가입 (Figma 01 회원가입 · 학생 인증)
 * 가입하면 학교 메일로 인증코드가 발송되고, 다음 화면은 AU-02 이메일 인증
 */
class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val viewModel: SignupViewModel by viewModels()

    /** 항목별 에러를 보여줄 입력 상자 */
    private val fieldLayouts: Map<SignupField, TextInputLayout> by lazy {
        mapOf(
            SignupField.EMAIL to binding.tilEmail,
            SignupField.PASSWORD to binding.tilPassword,
            SignupField.PASSWORD_CONFIRM to binding.tilPasswordConfirm,
            SignupField.NAME to binding.tilName,
            SignupField.NICKNAME to binding.tilNickname,
            SignupField.PHONE to binding.tilPhone,
            SignupField.UNIVERSITY to binding.tilUniversity,
            SignupField.STUDENT_NO to binding.tilStudentNo,
            SignupField.BANK to binding.tilBank,
            SignupField.ACCOUNT_NUMBER to binding.tilAccountNumber
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 상태바·하단바·키보드 여백, 밝은 배경이라 상태바 글자는 어둡게
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

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // 입력을 고치면 그 칸의 에러 지우기
        clearErrorOnChange(binding.etEmail, SignupField.EMAIL)
        clearErrorOnChange(binding.etPassword, SignupField.PASSWORD)
        clearErrorOnChange(binding.etPasswordConfirm, SignupField.PASSWORD_CONFIRM)
        clearErrorOnChange(binding.etPhone, SignupField.PHONE)
        clearErrorOnChange(binding.etStudentNo, SignupField.STUDENT_NO)
        clearErrorOnChange(binding.etAccountNumber, SignupField.ACCOUNT_NUMBER)

        // 예금주는 이름과 같아야 해서 이름을 그대로 따라 쓴다
        binding.etName.doAfterTextChanged {
            binding.etAccountHolder.setText(it?.toString().orEmpty().trim())
            viewModel.clearError(SignupField.NAME)
        }

        // 닉네임을 바꾸면 중복확인 다시
        binding.etNickname.doAfterTextChanged {
            viewModel.onNicknameChanged(it?.toString().orEmpty().trim())
            viewModel.clearError(SignupField.NICKNAME)
        }
        binding.tvNicknameCheck.setOnClickListener {
            viewModel.checkNickname(binding.etNickname.text?.toString().orEmpty())
        }

        binding.optFemale.setOnClickListener { viewModel.selectGender(GENDER_FEMALE) }
        binding.optMale.setOnClickListener { viewModel.selectGender(GENDER_MALE) }

        binding.etUniversity.setOnClickListener { showUniversityPicker() }
        binding.tilUniversity.setEndIconOnClickListener { showUniversityPicker() }
        binding.etBank.setOnClickListener { showBankPicker() }
        binding.tilBank.setEndIconOnClickListener { showBankPicker() }

        binding.cbRequired.setOnCheckedChangeListener { _, _ -> viewModel.clearError(SignupField.AGREE) }

        binding.btnSignup.setOnClickListener { submit() }
    }

    private fun clearErrorOnChange(editText: EditText, field: SignupField) {
        editText.doAfterTextChanged { viewModel.clearError(field) }
    }

    private fun submit() {
        viewModel.signup(
            SignupInput(
                email = binding.etEmail.text?.toString().orEmpty(),
                password = binding.etPassword.text?.toString().orEmpty(),
                passwordConfirm = binding.etPasswordConfirm.text?.toString().orEmpty(),
                name = binding.etName.text?.toString().orEmpty(),
                nickname = binding.etNickname.text?.toString().orEmpty(),
                phone = binding.etPhone.text?.toString().orEmpty(),
                studentNo = binding.etStudentNo.text?.toString().orEmpty(),
                accountNumber = binding.etAccountNumber.text?.toString().orEmpty(),
                agreeRequired = binding.cbRequired.isChecked,
                agreeMarketing = binding.cbMarketing.isChecked
            )
        )
    }

    private fun showUniversityPicker() {
        val universities = viewModel.uiState.value.universities
        if (universities.isEmpty()) {
            viewModel.loadOptions()
            showToast("대학교 목록을 불러오는 중이에요. 잠시 후 다시 눌러 주세요.")
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("대학교 선택")
            .setItems(universities.map { it.name }.toTypedArray()) { _, which ->
                viewModel.selectUniversity(universities[which])
            }
            .show()
    }

    private fun showBankPicker() {
        val banks = viewModel.uiState.value.banks
        if (banks.isEmpty()) {
            viewModel.loadOptions()
            showToast("은행 목록을 불러오는 중이에요. 잠시 후 다시 눌러 주세요.")
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("은행 선택")
            .setItems(banks.map { it.name }.toTypedArray()) { _, which ->
                viewModel.selectBank(banks[which])
            }
            .show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.events.collect { handleEvent(it) } }
            }
        }
    }

    private fun render(state: SignupUiState) {
        // 입력칸 에러
        fieldLayouts.forEach { (field, layout) -> layout.error = state.errors[field] }
        showErrorText(binding.tvGenderError, state.errors[SignupField.GENDER])
        showErrorText(binding.tvAgreeError, state.errors[SignupField.AGREE])
        showErrorText(binding.tvError, state.errorMessage)

        // 닉네임 중복확인
        binding.tvNicknameCheck.text = when (state.nicknameCheck) {
            NicknameCheck.CHECKING -> "확인 중"
            NicknameCheck.AVAILABLE -> "확인완료"
            NicknameCheck.NONE -> "중복확인"
        }
        binding.tilNickname.helperText =
            if (state.nicknameCheck == NicknameCheck.AVAILABLE) "사용할 수 있는 닉네임이에요." else null

        // 성별
        renderOption(binding.optFemale, "여자", state.gender == GENDER_FEMALE)
        renderOption(binding.optMale, "남자", state.gender == GENDER_MALE)

        // 선택한 학교·은행
        setTextIfChanged(binding.etUniversity, state.university?.name.orEmpty())
        setTextIfChanged(binding.etBank, state.bank?.name.orEmpty())

        // 가입 중에는 버튼 잠금 + 로딩 표시
        binding.btnSignup.isEnabled = !state.isLoading
        binding.btnSignup.text = if (state.isLoading) "" else "가입하고 인증메일 받기"
        binding.progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
    }

    private fun renderOption(view: TextView, label: String, selected: Boolean) {
        view.text = if (selected) "✓ $label" else label
        view.setBackgroundResource(
            if (selected) R.drawable.bg_taxipot_option_selected else R.drawable.bg_taxipot_option
        )
        view.paint.isFakeBoldText = selected
        view.invalidate()
    }

    private fun showErrorText(view: TextView, message: String?) {
        view.text = message
        view.visibility = if (message != null) View.VISIBLE else View.GONE
    }

    private fun setTextIfChanged(editText: EditText, text: String) {
        if (editText.text?.toString() != text) editText.setText(text)
    }

    private fun handleEvent(event: SignupEvent) {
        when (event) {
            // TODO: AU-02 이메일 인증 화면 만들면 그 화면으로 이동
            SignupEvent.Done -> {
                showToast("가입 완료! 학교 메일로 인증코드를 보냈어요 (인증 화면 준비 중)")
                finish()
            }
            is SignupEvent.ScrollTo -> scrollTo(event.field)
            is SignupEvent.ShowMessage -> showToast(event.message)
        }
    }

    /** 에러가 난 항목이 보이도록 스크롤 */
    private fun scrollTo(field: SignupField) {
        val target: View = when (field) {
            SignupField.GENDER -> binding.optFemale
            SignupField.AGREE -> binding.cbRequired
            else -> fieldLayouts[field] ?: return
        }
        binding.scrollView.post {
            val rect = Rect()
            target.getDrawingRect(rect)
            binding.scrollView.offsetDescendantRectToMyCoords(target, rect)
            val margin = (48 * resources.displayMetrics.density).toInt()
            binding.scrollView.smoothScrollTo(0, (rect.top - margin).coerceAtLeast(0))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val GENDER_FEMALE = "FEMALE"
        private const val GENDER_MALE = "MALE"
    }
}
