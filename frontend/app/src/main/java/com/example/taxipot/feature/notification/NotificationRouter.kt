package com.example.taxipot.feature.notification

import android.app.Activity
import android.content.Intent
import com.example.taxipot.data.remote.dto.notification.NotificationResponse
import com.example.taxipot.feature.auth.verification.VerificationStatusActivity
import com.example.taxipot.feature.mypage.report.MyReportActivity
import com.example.taxipot.feature.pot.detail.PotDetailActivity
import com.example.taxipot.feature.rating.RatingActivity
import com.example.taxipot.feature.settlement.SettlementActivity
import com.example.taxipot.feature.wallet.WalletActivity

/**
 * 알림을 눌렀을 때 갈 화면 (Figma MY-05 흐름)
 * 정산 완료 → ST-01 / 평가 요청 → ST-02 / 지각·방 관련 → 방 상세 / 인증 결과 → AU-04 / 신고 처리 → 신고 내역
 */
object NotificationRouter {

    /** 이동할 화면이 있으면 열고 true */
    fun open(activity: Activity, n: NotificationResponse): Boolean {
        val potId = n.potId
        val intent: Intent? = when (n.type) {
            "RATING_REQUEST" -> potId?.let { RatingActivity.newIntent(activity, it) }
            "PAYMENT" -> if (potId != null) {
                SettlementActivity.newIntent(activity, potId)
            } else {
                WalletActivity.newIntent(activity)
            }
            "POT", "LATE_WARNING", "KICKED" -> potId?.let { PotDetailActivity.newIntent(activity, it) }
            "REPORT_RESULT" -> Intent(activity, MyReportActivity::class.java)
            "SYSTEM" -> if (n.title.orEmpty().contains("인증")) {
                Intent(activity, VerificationStatusActivity::class.java)
            } else {
                null
            }
            else -> null
        }
        intent ?: return false
        activity.startActivity(intent)
        return true
    }
}
