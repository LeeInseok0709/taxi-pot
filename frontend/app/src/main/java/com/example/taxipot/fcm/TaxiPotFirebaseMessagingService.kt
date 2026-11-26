package com.example.taxipot.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FCM 수신.
 * - 앱이 켜져 있을 때: 여기로 와서 직접 상단 알림을 만든다
 * - 앱이 꺼져 있고 notification이 같이 온 푸시: 시스템이 알림을 띄우고, 누르면 MainActivity가 data를 받는다
 */
class TaxiPotFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        FcmTokenSync.send(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "택시팟"
        val body = message.notification?.body ?: data["body"] ?: data["content"] ?: ""
        PushNotifier.show(this, title, body, data)
    }
}
