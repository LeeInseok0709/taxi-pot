package com.example.taxipot.core.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.taxipot.R
import com.example.taxipot.databinding.ViewTaxipotBottomNavBinding
import com.example.taxipot.feature.mypage.MyPageActivity
import com.example.taxipot.feature.notification.NotificationActivity
import com.example.taxipot.feature.pot.main.PotMainActivity
import com.example.taxipot.feature.wallet.WalletActivity

/**
 * 공통 하단 메뉴 (홈 화면 · 내 지갑 · 마이페이지).
 * 레이아웃에 <include android:id="@+id/bottomNav" layout="@layout/view_taxipot_bottom_nav" />를 넣고
 * TaxiPotBottomNav.setup(this, binding.bottomNav, TaxiPotBottomNav.Tab.WALLET) 한 줄로 연결한다.
 */
object TaxiPotBottomNav {

    enum class Tab { HOME, WALLET, MY }

    fun setup(activity: Activity, nav: ViewTaxipotBottomNavBinding, current: Tab) {
        render(activity, nav.ivNavHome, nav.tvNavHome, current == Tab.HOME)
        render(activity, nav.ivNavWallet, nav.tvNavWallet, current == Tab.WALLET)
        render(activity, nav.ivNavMy, nav.tvNavMy, current == Tab.MY)

        nav.navHome.setOnClickListener { if (current != Tab.HOME) openHome(activity) }
        nav.navWallet.setOnClickListener { if (current != Tab.WALLET) openWallet(activity) }
        nav.navMy.setOnClickListener { if (current != Tab.MY) openMyPage(activity) }
    }

    /** 다른 화면(홈·목록 등)의 '내 지갑' 버튼에서도 이걸 쓰면 지갑 화면이 여러 개 쌓이지 않는다 */
    fun openWallet(activity: Activity) = openTab(activity, WalletActivity::class.java, Tab.WALLET)

    fun openMyPage(activity: Activity) = openTab(activity, MyPageActivity::class.java, Tab.MY)

    fun openHome(activity: Activity) = openTab(activity, PotMainActivity::class.java, Tab.HOME)

    /** 홈·지갑·마이페이지 상단의 종 아이콘 */
    fun openNotifications(activity: Activity) {
        activity.startActivity(Intent(activity, NotificationActivity::class.java))
    }

    /**
     * 탭 이동 + 방향에 맞는 밀기 효과.
     * 메뉴 순서(홈 → 내 지갑 → 마이페이지) 기준으로 오른쪽 탭으로 가면 지금 화면이 왼쪽으로 빠지고,
     * 왼쪽 탭으로 가면 오른쪽으로 빠진다.
     */
    private fun openTab(activity: Activity, target: Class<out Activity>, targetTab: Tab) {
        activity.startActivity(Intent(activity, target).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        val from = tabOf(activity) ?: return // 탭 화면이 아닌 곳에서 열면 기본 효과
        if (from == targetTab) return
        @Suppress("DEPRECATION")
        if (targetTab.ordinal > from.ordinal) {
            activity.overridePendingTransition(R.anim.taxipot_slide_in_right, R.anim.taxipot_slide_out_left)
        } else {
            activity.overridePendingTransition(R.anim.taxipot_slide_in_left, R.anim.taxipot_slide_out_right)
        }
    }

    private fun tabOf(activity: Activity): Tab? = when (activity) {
        is PotMainActivity -> Tab.HOME
        is WalletActivity -> Tab.WALLET
        is MyPageActivity -> Tab.MY
        else -> null
    }

    private fun render(activity: Activity, icon: ImageView, label: TextView, selected: Boolean) {
        val text = ContextCompat.getColor(activity, R.color.taxipot_text)
        val sub = ContextCompat.getColor(activity, R.color.taxipot_sub_text)
        icon.setBackgroundResource(if (selected) R.drawable.bg_taxipot_nav_selected else 0)
        icon.setColorFilter(if (selected) text else sub)
        label.setTextColor(if (selected) text else sub)
        label.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
    }
}
