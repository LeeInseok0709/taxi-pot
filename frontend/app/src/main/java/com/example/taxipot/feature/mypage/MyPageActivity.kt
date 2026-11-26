package com.example.taxipot.feature.mypage

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.taxipot.core.ui.TaxiPotBottomNav
import com.example.taxipot.databinding.ActivityMyPageBinding
import com.example.taxipot.feature.auth.login.LoginActivity
import com.example.taxipot.feature.auth.verification.VerificationStatusActivity
import com.example.taxipot.feature.mypage.claim.MyFareClaimActivity
import com.example.taxipot.feature.mypage.history.MyPotHistoryActivity
import com.example.taxipot.feature.mypage.report.MyReportActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** MY-01 마이페이지 */
class MyPageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyPageBinding
    private val viewModel: MyPageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMyPageBinding.inflate(layoutInflater)
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

        TaxiPotBottomNav.setup(this, binding.bottomNav, TaxiPotBottomNav.Tab.MY)
        binding.btnBell.setOnClickListener { TaxiPotBottomNav.openNotifications(this) }

        binding.menuHistory.setOnClickListener { startActivity(Intent(this, MyPotHistoryActivity::class.java)) }
        binding.menuClaims.setOnClickListener { startActivity(Intent(this, MyFareClaimActivity::class.java)) }
        binding.menuNotifications.setOnClickListener { TaxiPotBottomNav.openNotifications(this) }
        binding.menuReports.setOnClickListener { startActivity(Intent(this, MyReportActivity::class.java)) }
        // TODO: AC-01 내 정보 수정, AC-02 회원 탈퇴 만들면 연결
        binding.menuEdit.setOnClickListener { showToast("내 정보 수정 (준비 중)") }
        binding.menuWithdraw.setOnClickListener { showToast("회원 탈퇴 (준비 중)") }
        binding.menuLogout.setOnClickListener { confirmLogout() }
        binding.chipVerified.setOnClickListener {
            if (viewModel.uiState.value.profile?.isVerified != true) {
                startActivity(VerificationStatusActivity.newIntent(this))
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            MyPageEvent.LoggedOut -> {
                                startActivity(
                                    Intent(this@MyPageActivity, LoginActivity::class.java)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                )
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

    private fun render(state: MyPageUiState) {
        binding.tvError.text = state.errorMessage
        binding.tvError.visibility = if (state.errorMessage != null) View.VISIBLE else View.GONE

        val profile = state.profile ?: return
        binding.tvNickname.text = profile.nickname
        binding.tvSchool.text = listOfNotNull(profile.university, "평점 " + MyPageFormat.rating(profile.ratingAvg))
            .joinToString(" · ")

        val verified = profile.isVerified == true
        binding.chipVerified.text = if (verified) "✓ 인증완료" else "인증 필요"

        // 신고 누적 · 정지 안내
        val reportCount = profile.reportCount ?: 0
        val suspended = profile.status == "SUSPENDED"
        binding.boxReport.visibility = if (reportCount > 0 || suspended) View.VISIBLE else View.GONE
        binding.tvReportCount.text = "누적 신고 ${reportCount}회"
        binding.tvReportNext.text = when {
            suspended && profile.suspendedUntil == null -> "영구 이용 정지 상태예요"
            suspended -> "${MyPageFormat.monthDay(profile.suspendedUntil)}까지 이용이 정지됐어요"
            else -> MyPageFormat.nextSuspension(reportCount, profile.nextSuspensionIn) ?: ""
        }
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("로그아웃")
            .setMessage("로그아웃할까요?")
            .setNegativeButton("취소", null)
            .setPositiveButton("로그아웃") { _, _ -> viewModel.logout() }
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
