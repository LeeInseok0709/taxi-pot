package com.example.taxipot.fcm

import android.util.Log
import com.example.taxipot.BuildConfig
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.UserApi
import com.example.taxipot.data.remote.dto.user.FcmTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 이 기기의 FCM 토큰을 서버에 맞춰 둔다.
 * 로그인한 상태에서만 보내고, 실패해도 앱 사용에는 영향이 없다 (다음 실행 때 다시 보냄).
 */
object FcmTokenSync {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val api by lazy { ApiClient.create(UserApi::class.java) }

    /** 앱 시작·로그인·회원가입 직후에 호출: 지금 토큰을 받아 서버로 */
    fun syncCurrentToken() {
        if (!ApiClient.tokenManager.isLoggedIn()) return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> send(token) }
    }

    /** 토큰이 바뀌었을 때 (TaxiPotFirebaseMessagingService.onNewToken) */
    fun send(token: String) {
        // 테스트용: Logcat에서 'FCM'으로 검색하면 Firebase 콘솔 테스트 메시지에 넣을 토큰을 볼 수 있음
        if (BuildConfig.DEBUG) Log.d("FCM", "token: $token")
        if (!ApiClient.tokenManager.isLoggedIn() || token.isBlank() || token.length > 255) return
        scope.launch { safeApiCall { api.updateFcmToken(FcmTokenRequest(token)) } }
    }
}
