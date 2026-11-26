package com.example.taxipot.feature.auth.verification

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.taxipot.databinding.ActivityCaptureVerificationBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** AU-03 학생 정보 인증 (2차 인증: 캡처본 제출, 반려 후 재제출 포함) */
class CaptureVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaptureVerificationBinding
    private val viewModel: CaptureVerificationViewModel by viewModels()

    // 안드로이드 사진 선택기 (권한 필요 없음)
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) loadCapture(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCaptureVerificationBinding.inflate(layoutInflater)
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

        binding.appBar.tvAppBarTitle.text = "학생 정보 인증"
        binding.appBar.btnBack.setOnClickListener { finish() }

        // 반려돼서 다시 제출하는 경우 반려 사유 표시
        val rejectReason = intent.getStringExtra(EXTRA_REJECT_REASON)
        if (rejectReason.isNullOrBlank()) {
            binding.boxReject.visibility = View.GONE
        } else {
            binding.boxReject.visibility = View.VISIBLE
            binding.tvRejectReason.text = "“$rejectReason”\n정보가 보이도록 캡처본을 다시 제출해주세요."
        }

        binding.boxUpload.setOnClickListener {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.etStudentNo.doAfterTextChanged { viewModel.clearStudentNoError() }
        binding.btnSubmit.setOnClickListener {
            viewModel.submit(binding.etStudentNo.text?.toString().orEmpty())
        }

        observeViewModel()
    }

    /** 사진 읽기 → 긴 변 1600px로 줄이기 → JPEG (서버 제한 10MB 안으로) */
    private fun loadCapture(uri: Uri) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val original = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                        ?: return@withContext null
                    val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(original.width, original.height))
                    val bitmap = if (scale < 1f) {
                        Bitmap.createScaledBitmap(
                            original,
                            (original.width * scale).toInt(),
                            (original.height * scale).toInt(),
                            true
                        )
                    } else {
                        original
                    }
                    val out = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                    bitmap to out.toByteArray()
                } catch (e: Exception) {
                    null
                }
            }
            if (result == null) {
                viewModel.showImageError("사진을 읽지 못했어요. 다른 사진을 골라 주세요.")
            } else {
                viewModel.setCapture(result.first, result.second)
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.events.collect { handleEvent(it) } }
            }
        }
    }

    private fun render(state: CaptureUiState) {
        // 미리보기
        if (state.preview != null) {
            binding.ivCapture.setImageBitmap(state.preview)
            binding.ivCapture.visibility = View.VISIBLE
            binding.uploadHint.visibility = View.GONE
        } else {
            binding.ivCapture.visibility = View.GONE
            binding.uploadHint.visibility = View.VISIBLE
        }

        showErrorText(binding.tvImageError, state.imageError)
        binding.tilStudentNo.error = state.studentNoError
        showErrorText(binding.tvError, state.errorMessage)

        binding.btnSubmit.isEnabled = !state.isSubmitting
        binding.btnSubmit.text = if (state.isSubmitting) "" else "제출하기"
        binding.progress.visibility = if (state.isSubmitting) View.VISIBLE else View.GONE
    }

    private fun showErrorText(view: TextView, message: String?) {
        view.text = message
        view.visibility = if (message != null) View.VISIBLE else View.GONE
    }

    private fun handleEvent(event: CaptureEvent) {
        when (event) {
            CaptureEvent.Submitted -> {
                startActivity(VerificationStatusActivity.newIntent(this))
                finish()
            }
            CaptureEvent.NeedEmailVerification -> {
                Toast.makeText(this, "이메일 인증을 먼저 완료해 주세요.", Toast.LENGTH_SHORT).show()
                startActivity(EmailVerificationActivity.newIntent(this, email = null, justSent = false))
                finish()
            }
        }
    }

    companion object {
        private const val EXTRA_REJECT_REASON = "rejectReason"
        private const val MAX_SIDE = 1600
        private const val JPEG_QUALITY = 85

        /** @param rejectReason 반려 후 재제출이면 반려 사유 */
        fun newIntent(context: Context, rejectReason: String? = null): Intent =
            Intent(context, CaptureVerificationActivity::class.java)
                .putExtra(EXTRA_REJECT_REASON, rejectReason)
    }
}
