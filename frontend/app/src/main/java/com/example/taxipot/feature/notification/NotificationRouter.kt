package com.example.taxipot.feature.notification

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.taxipot.data.remote.dto.notification.NotificationResponse
import com.example.taxipot.feature.auth.verification.VerificationStatusActivity
import com.example.taxipot.feature.mypage.report.MyReportActivity
import com.example.taxipot.feature.pot.chat.ChatActivity
import com.example.taxipot.feature.pot.detail.PotDetailActivity
import com.example.taxipot.feature.pot.main.PotMainActivity
import com.example.taxipot.feature.rating.RatingActivity
import com.example.taxipot.feature.settlement.SettlementActivity
import com.example.taxipot.feature.wallet.WalletActivity

/**
 * 알림(목록에서 누름 · 푸시에서 누름) → 이동할 화면. 기성님과 확정한 type 8가지 기준.
 * POT·LATE_WARNING → 방 상세 / RATING_REQUEST → 평가 / PAYMENT → 정산 내역(방 없으면 지갑)
 * CHAT → 채팅 / KICKED → 홈 / REPORT_RESULT → 신고 내역 / SYSTEM → 알림 목록(인증 결과면 인증 상태)
 */
object NotificationRouter {

    fun intentFor(
        context: Context,
        type: String,
        potId: Long?,
        title: String? = null,
        fromPush: Boolean = false
    ): Intent? = when (type) {
        "POT", "LATE_WARNING" -> potId?.let { PotDetailActivity.newIntent(context, it) }
        "RATING_REQUEST" -> potId?.let { RatingActivity.newIntent(context, it) }
        "PAYMENT" -> if (potId != null) SettlementActivity.newIntent(context, potId) else WalletActivity.newIntent(context)
        "CHAT" -> potId?.let { ChatActivity.newIntent(context, it) }
        "KICKED" -> Intent(context, PotMainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        "REPORT_RESULT" -> Intent(context, MyReportActivity::class.java)
        "SYSTEM" -> when {
            title.orEmpty().contains("인증") -> Intent(context, VerificationStatusActivity::class.java)
            fromPush -> Intent(context, NotificationActivity::class.java)
            else -> null // 알림 목록 안에서 누른 공지는 그대로 둠
        }
        else -> if (fromPush) Intent(context, NotificationActivity::class.java) else null
    }

    /** 알림 목록에서 눌렀을 때. 이동할 화면이 있으면 열고 true */
    fun open(activity: Activity, n: NotificationResponse): Boolean {
        val intent = intentFor(activity, n.type, n.potId, n.title) ?: return false
        activity.startActivity(intent)
        return true
    }
}
