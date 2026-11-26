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
    fun openWallet(activity: Activity) {
        activity.startActivity(
            Intent(activity, WalletActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
    }

    fun openMyPage(activity: Activity) {
        activity.startActivity(
            Intent(activity, MyPageActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
    }

    /** 홈·지갑·마이페이지 상단의 종 아이콘 */
    fun openNotifications(activity: Activity) {
        activity.startActivity(Intent(activity, NotificationActivity::class.java))
    }

    fun openHome(activity: Activity) {
        activity.startActivity(
            Intent(activity, PotMainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
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
