package com.example.taxipot.fcm

import android.content.Context
import android.content.Intent
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.NotificationApi
import com.example.taxipot.feature.notification.NotificationRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 푸시를 눌러 앱이 열렸을 때: data(type·potId·notificationId)를 보고 갈 화면을 정한다 */
object PushRoute {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 푸시로 열린 게 아니면 null */
    fun targetIntent(context: Context, intent: Intent?): Intent? {
        val type = intent?.getStringExtra(PushNotifier.KEY_TYPE) ?: return null
        val potId = intent.getStringExtra(PushNotifier.KEY_POT_ID)?.toLongOrNull()
        // CHAT 푸시엔 notificationId가 없음 → 있을 때만 읽음 처리
        intent.getStringExtra(PushNotifier.KEY_NOTIFICATION_ID)?.toLongOrNull()?.let { markRead(it) }
        return NotificationRouter.intentFor(context, type, potId, title = null, fromPush = true)
    }

    private fun markRead(notificationId: Long) {
        scope.launch { safeApiCall { ApiClient.create(NotificationApi::class.java).markRead(notificationId) } }
    }
}
