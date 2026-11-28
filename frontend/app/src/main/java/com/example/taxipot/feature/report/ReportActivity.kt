package com.example.taxipot.feature.report

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.example.taxipot.databinding.ActivityReportBinding
import kotlinx.coroutines.launch

/** ST-03 신고하기: 노쇼 / 불쾌한 언행 / 기타 + 사유 → 운영자 확인 후 누적 반영 */
class ReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportBinding
    private val viewModel: ReportViewModel by viewModels()

    private var potId = -1L
    private var reportedId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityReportBinding.inflate(layoutInflater)
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

        potId = intent.getLongExtra(EXTRA_POT_ID, -1L)
        reportedId = intent.getLongExtra(EXTRA_REPORTED_ID, -1L)
        val nickname = intent.getStringExtra(EXTRA_NICKNAME) ?: "동승자"

        binding.appBar.tvAppBarTitle.text = "신고하기"
        binding.appBar.btnBack.setOnClickListener { finish() }
        binding.tvTarget.text = "신고 대상: $nickname"

        binding.optNoShow.setOnClickListener { viewModel.selectType(ReportType.NO_SHOW) }
        binding.optAbusive.setOnClickListener { viewModel.selectType(ReportType.ABUSIVE) }
        binding.optEtc.setOnClickListener { viewModel.selectType(ReportType.ETC) }
        binding.etReason.doAfterTextChanged { viewModel.clearReasonError() }
        binding.btnSubmit.setOnClickListener {
            viewModel.submit(potId, reportedId, binding.etReason.text?.toString().orEmpty())
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch {
                    viewModel.events.collect {
                        Toast.makeText(this@ReportActivity, "신고가 접수됐어요. 운영자 확인 후 반영돼요.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun render(state: ReportUiState) {
        renderOption(binding.optNoShow, state.type == ReportType.NO_SHOW)
        renderOption(binding.optAbusive, state.type == ReportType.ABUSIVE)
        renderOption(binding.optEtc, state.type == ReportType.ETC)

        showText(binding.tvTypeError, state.typeError)
        binding.tilReason.error = state.reasonError
        showText(binding.tvError, state.errorMessage)

        binding.btnSubmit.isEnabled = !state.isSubmitting
        binding.btnSubmit.text = if (state.isSubmitting) "접수 중..." else "신고 접수"
    }

    private fun renderOption(view: TextView, selected: Boolean) {
        view.setBackgroundResource(if (selected) R.drawable.bg_taxipot_option_selected else R.drawable.bg_taxipot_option)
        view.paint.isFakeBoldText = selected
        view.invalidate()
    }

    private fun showText(view: TextView, message: String?) {
        view.text = message
        view.visibility = if (message != null) View.VISIBLE else View.GONE
    }

    companion object {
        private const val EXTRA_POT_ID = "potId"
        private const val EXTRA_REPORTED_ID = "reportedId"
        private const val EXTRA_NICKNAME = "nickname"

        fun newIntent(context: Context, potId: Long, reportedId: Long, nickname: String?): Intent =
            Intent(context, ReportActivity::class.java)
                .putExtra(EXTRA_POT_ID, potId)
                .putExtra(EXTRA_REPORTED_ID, reportedId)
                .putExtra(EXTRA_NICKNAME, nickname)
    }
}
