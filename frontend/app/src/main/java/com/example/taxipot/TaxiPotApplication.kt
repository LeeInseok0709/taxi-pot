package com.example.taxipot

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.taxipot.core.event.AppEvent
import com.example.taxipot.core.event.AppEventBus
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.fcm.FcmTokenSync
import com.example.taxipot.fcm.PushNotifier
import com.example.taxipot.feature.auth.login.LoginActivity
import com.example.taxipot.feature.wallet.WalletActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class TaxiPotApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** 지금 화면에 떠 있는 화면 (앱이 백그라운드면 null) */
    private var resumedActivity: WeakReference<Activity>? = null
    private var lastDebtNoticeAt = 0L

    override fun onCreate() {
        super.onCreate()
        // 앱 시작 시 저장된 토큰을 불러오고 Retrofit을 준비한다
        ApiClient.init(this)

        // 푸시 알림 채널 만들고, 로그인 상태면 이 기기 FCM 토큰을 서버에 맞춰 둔다
        PushNotifier.createChannel(this)
        FcmTokenSync.syncCurrentToken()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                resumedActivity = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                if (resumedActivity?.get() === activity) resumedActivity = null
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })

        // 어느 화면에서든 받아야 하는 공통 이벤트 (네트워크 쪽에서 보냄)
        appScope.launch {
            AppEventBus.events.collect { event ->
                val activity = resumedActivity?.get() ?: return@collect
                when (event) {
                    AppEvent.UnpaidDebt -> openWalletForDebt(activity)
                    AppEvent.ForceLogout -> openLogin(activity)
                }
            }
        }
    }

    /** 409 UNPAID_DEBT_EXISTS → 내 지갑의 미수금 칸으로 (연달아 오면 한 번만) */
    private fun openWalletForDebt(activity: Activity) {
        val now = System.currentTimeMillis()
        if (activity is WalletActivity || now - lastDebtNoticeAt < DEBT_NOTICE_INTERVAL_MS) return
        lastDebtNoticeAt = now
        activity.startActivity(WalletActivity.newIntent(activity, focusDebt = true))
    }

    /** 토큰 무효·재발급 실패 → 로그인 화면으로 (이전 화면은 정리) */
    private fun openLogin(activity: Activity) {
        if (activity is LoginActivity) return
        Toast.makeText(activity, "로그인이 만료됐어요. 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show()
        activity.startActivity(
            Intent(activity, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    companion object {
        private const val DEBT_NOTICE_INTERVAL_MS = 3_000L
    }
}
