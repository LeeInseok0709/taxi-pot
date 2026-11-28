package com.example.taxipot.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.taxipot.MainActivity
import com.example.taxipot.R

/** 상단 알림 만들기 (앱이 켜져 있을 때 온 푸시, 또는 data만 온 푸시) */
object PushNotifier {

    /** Manifest의 default_notification_channel_id와 같은 값 */
    const val CHANNEL_ID = "taxipot_default"

    /** 푸시 data 키 (서버 명세: 모든 값은 문자열) */
    const val KEY_TYPE = "type"
    const val KEY_POT_ID = "potId"
    const val KEY_NOTIFICATION_ID = "notificationId"

    /** 앱 시작 시 1회 (Android 8 이상은 채널이 있어야 알림이 뜸) */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "택시팟 알림", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "택시팟 진행·정산·채팅 알림"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(context: Context, title: String, body: String, data: Map<String, String>) {
        // 누르면 시작 화면(MainActivity)이 받아서 알맞은 화면으로 보낸다 (앱이 꺼져 있을 때 온 푸시와 같은 경로)
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data[KEY_TYPE]?.let { putExtra(KEY_TYPE, it) }
            data[KEY_POT_ID]?.let { putExtra(KEY_POT_ID, it) }
            data[KEY_NOTIFICATION_ID]?.let { putExtra(KEY_NOTIFICATION_ID, it) }
        }
        val requestCode = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val pendingIntent = PendingIntent.getActivity(
            context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_taxi)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return // 알림 권한을 안 줬으면 앱 안 알림 목록으로만 확인
        try {
            manager.notify(requestCode, notification)
        } catch (e: SecurityException) {
            // Android 13+ 알림 권한 거부
        }
    }
}
