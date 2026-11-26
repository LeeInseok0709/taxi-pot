package com.example.taxipot.feature.rating

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.taxipot.R
import com.example.taxipot.databinding.ActivityRatingBinding
import com.example.taxipot.feature.report.ReportActivity
import kotlinx.coroutines.launch

/** ST-02 동승자 평가: 동승자별 4항목 별점(1~5), 평균 3점 미만이면 사유 필수 */
class RatingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRatingBinding
    private val viewModel: RatingViewModel by viewModels()
    private var potId = -1L

    /** 항목별 별 5개 (화면에 한 번만 만들어 두고 상태만 바꿈) */
    private val starViews = mutableMapOf<RatingCategory, List<TextView>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRatingBinding.inflate(layoutInflater)
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
        binding.appBar.tvAppBarTitle.text = "동승자 평가"
        binding.appBar.btnBack.setOnClickListener { finish() }

        buildStarRows()
        binding.etReason.doAfterTextChanged { binding.tilReason.error = null }
        binding.btnReport.setOnClickListener {
            val target = viewModel.uiState.value.selected ?: return@setOnClickListener
            startActivity(ReportActivity.newIntent(this, potId, target.memberId, target.nickname))
        }
        binding.btnSubmit.setOnClickListener {
            viewModel.submit(binding.etReason.text?.toString().orEmpty())
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is RatingEvent.ShowMessage -> showToast(event.message)
                            RatingEvent.AllDone -> {
                                showToast("모든 동승자 평가를 마쳤어요. 고마워요!")
                                finish()
                            }
                        }
                    }
                }
            }
        }

        if (potId > 0) viewModel.load(potId) else showToast("택시팟 정보를 찾을 수 없어요.")
    }

    /** 항목 4줄 × 별 5개 */
    private fun buildStarRows() {
        val density = resources.displayMetrics.density
        RatingCategory.entries.forEach { category ->
            val label = TextView(this).apply {
                text = category.label
                setTextColor(ContextCompat.getColor(context, R.color.taxipot_text))
                textSize = 13f
                paint.isFakeBoldText = true
                setPadding(0, (if (category.ordinal == 0) 0 else 14 * density).toInt(), 0, (8 * density).toInt())
            }
            binding.starRows.addView(label)

            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val stars = (1..5).map { score ->
                TextView(this).apply {
                    gravity = Gravity.CENTER
                    textSize = 20f
                    setTextColor(ContextCompat.getColor(context, R.color.taxipot_text))
                    contentDescription = "${category.label} ${score}점"
                    setOnClickListener { viewModel.setScore(category, score) }
                    layoutParams = LinearLayout.LayoutParams(0, (40 * density).toInt(), 1f).apply {
                        if (score < 5) marginEnd = (8 * density).toInt()
                    }
                }
            }
            stars.forEach { row.addView(it) }
            binding.starRows.addView(row)
            starViews[category] = stars
        }
    }

    private fun render(state: RatingUiState) {
        binding.progress.visibility = if (state.isLoading && state.targets.isEmpty()) View.VISIBLE else View.GONE
        binding.tvError.text = state.errorMessage
        binding.tvError.visibility = if (state.errorMessage != null) View.VISIBLE else View.GONE

        val target = state.selected
        binding.content.visibility = if (target != null) View.VISIBLE else View.GONE
        binding.tvEmpty.visibility =
            if (!state.isLoading && state.errorMessage == null && state.targets.isEmpty()) View.VISIBLE else View.GONE
        renderTabs(state)
        if (target == null) return

        // 별점
        val scores = state.scoresOf(target.memberId)
        starViews.forEach { (category, stars) ->
            val score = scores.getValue(category)
            stars.forEachIndexed { index, view ->
                val on = index < score
                view.text = if (on) "★" else "☆"
                view.setBackgroundResource(if (on) R.drawable.bg_taxipot_option_selected else 0)
                view.isEnabled = !target.isRated
            }
        }

        // 3점 미만 사유
        val needsReason = state.needsReason(target.memberId)
        binding.reasonGroup.visibility = if (needsReason && !target.isRated) View.VISIBLE else View.GONE
        binding.tilReason.error = state.reasonError

        binding.btnSubmit.isEnabled = !target.isRated && !state.isSubmitting
        binding.btnSubmit.text = when {
            target.isRated -> "평가 완료"
            state.isSubmitting -> "보내는 중..."
            else -> "평가 제출"
        }
    }

    /** 동승자 탭 (평가한 사람은 ✓) */
    private fun renderTabs(state: RatingUiState) {
        binding.tabs.removeAllViews()
        val density = resources.displayMetrics.density
        state.targets.forEachIndexed { index, t ->
            val selected = t.memberId == state.selectedId
            val tab = TextView(this).apply {
                text = (t.nickname ?: "동승자") + if (t.isRated) " ✓" else ""
                gravity = Gravity.CENTER
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.taxipot_text))
                paint.isFakeBoldText = selected
                setBackgroundResource(if (selected) R.drawable.bg_taxipot_option_selected else R.drawable.bg_taxipot_option)
                setOnClickListener {
                    viewModel.select(t.memberId)
                    binding.etReason.setText("")
                }
                layoutParams = LinearLayout.LayoutParams(0, (44 * density).toInt(), 1f).apply {
                    if (index < state.targets.size - 1) marginEnd = (8 * density).toInt()
                }
            }
            binding.tabs.addView(tab)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val EXTRA_POT_ID = "potId"

        fun newIntent(context: Context, potId: Long): Intent =
            Intent(context, RatingActivity::class.java).putExtra(EXTRA_POT_ID, potId)
    }
}
