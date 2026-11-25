package com.example.taxipot.feature.pot.chat

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.taxipot.BuildConfig
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.PotApi
import com.example.taxipot.data.remote.dto.chat.ChatMessageResponse
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * 방 실시간 연결 (명세서 '실시간 통신': WebSocket + STOMP, SockJS 없음)
 * - 연결: ws://서버/ws, CONNECT 헤더 Authorization: Bearer {accessToken}, heart-beat:0,0
 * - 구독: /topic/pots/{potId} (id 헤더 필수)
 * - 받는 것: CHAT_MESSAGE(새 채팅) / POT_UPDATED(인원·도착·상태 변경 → 상세를 다시 조회)
 * - 보내기는 REST(POST /messages)로만 함 (STOMP SEND 안 씀)
 * - 연결할 때마다 먼저 API를 한 번 호출 → 토큰이 만료됐으면 TokenAuthenticator가 재발급한 뒤 연결
 * 라이브러리 없이 OkHttp WebSocket으로 STOMP 글자를 직접 주고받음 (build.gradle 수정 없음)
 * 콜백(onChat, onPotUpdated)은 화면(메인) 스레드에서 불림
 */
class PotRealtime(
    private val potId: Long,
    private val onChat: (ChatMessageResponse) -> Unit,
    private val onPotUpdated: () -> Unit
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val potApi by lazy { ApiClient.create(PotApi::class.java) }

    private var socket: WebSocket? = null
    private var openJob: Job? = null

    @Volatile
    private var active = false

    // 서버 ERROR가 연달아 온 횟수 (구독 성공하면 0으로)
    @Volatile
    private var errorCount = 0

    companion object {
        private const val TAG = "PotRealtime"
        private const val RETRY_MS = 3_000L
        private const val MAX_ERROR_RETRY = 3
    }

    /** 연결 시작 (이미 연결 중이면 무시) */
    fun connect() {
        if (active) return
        active = true
        errorCount = 0
        open()
    }

    /** 연결 끊기 (화면이 안 보일 때) */
    fun disconnect() {
        active = false
        mainHandler.removeCallbacksAndMessages(null)
        openJob?.cancel()
        socket?.let {
            it.send(frame("DISCONNECT", emptyMap()))
            it.close(1000, null)
        }
        socket = null
    }

    private fun open() {
        openJob?.cancel()
        openJob = scope.launch {
            // 1) 토큰 갱신: 로그인이 필요한 API를 한 번 호출
            //    (만료된 토큰이면 401 → TokenAuthenticator가 재발급 → accessToken이 새 값으로 바뀜)
            safeApiCall { potApi.getPot(potId) }
            if (!active) return@launch

            // 2) 예전 연결이 남아 있으면 정리하고 새로 연결
            socket?.cancel()
            val request = Request.Builder().url(wsUrl()).build()
            socket = client.newWebSocket(request, listener)
        }
    }

    private val listener = object : WebSocketListener() {

        // 3) 연결되면 STOMP CONNECT (방금 갱신된 토큰 사용)
        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (webSocket != socket) return
            val token = ApiClient.tokenManager.accessToken ?: ""
            webSocket.send(
                frame(
                    "CONNECT", mapOf(
                        "accept-version" to "1.2",
                        "host" to host(),
                        "heart-beat" to "0,0",
                        "Authorization" to "Bearer $token"
                    )
                )
            )
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (webSocket != socket) return
            handleFrame(webSocket, text)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (webSocket != socket) return   // 이미 바뀐 예전 연결의 알림은 무시
            Log.w(TAG, "실시간 연결 끊김: ${t.message}")
            retryLater()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket != socket) return
            retryLater()
        }
    }

    // 서버에서 온 STOMP 글자 처리
    private fun handleFrame(webSocket: WebSocket, text: String) {
        val frame = text.trimStart('\n', '\r')
        if (frame.isEmpty()) return   // 하트비트(빈 줄)

        when (frame.substringBefore('\n').trim()) {
            // 4) CONNECT 성공 → 이 방 구독
            "CONNECTED" -> {
                errorCount = 0
                webSocket.send(
                    frame(
                        "SUBSCRIBE", mapOf(
                            "id" to "pot-$potId",
                            "destination" to "/topic/pots/$potId",
                            "ack" to "auto"
                        )
                    )
                )
            }

            // 5) 방송 메시지 {"type": "...", "potId": 3, "payload": {...}}
            "MESSAGE" -> {
                val body = frame.substringAfter("\n\n", "").substringBefore('\u0000')
                handleEvent(body)
            }

            // 인증 실패 등 → 서버가 연결을 끊으면 토큰 갱신 후 다시 시도 (3번까지)
            "ERROR" -> {
                errorCount++
                Log.w(TAG, "실시간 오류($errorCount): ${frame.take(300)}")
                if (errorCount > MAX_ERROR_RETRY) {
                    Log.w(TAG, "실시간 오류가 계속돼서 연결을 멈춰요 (참여자가 아니거나 로그인 만료)")
                    active = false
                }
            }
        }
    }

    private fun handleEvent(body: String) {
        try {
            val json = JsonParser.parseString(body).asJsonObject
            when (json.get("type")?.asString) {
                "CHAT_MESSAGE" -> {
                    val message = ApiClient.gson.fromJson(json.get("payload"), ChatMessageResponse::class.java)
                    mainHandler.post { onChat(message) }
                }
                "POT_UPDATED" -> mainHandler.post { onPotUpdated() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "실시간 메시지 해석 실패: ${e.message}")
        }
    }

    // 끊기면 3초 뒤 다시 연결 (화면이 보이는 동안만, 예약은 하나만)
    private fun retryLater() {
        if (!active) return
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({ if (active) open() }, RETRY_MS)
    }

    // STOMP 글자 만들기: 명령 + 헤더 + 빈 줄 + 끝 표시(\u0000)
    private fun frame(command: String, headers: Map<String, String>): String = buildString {
        append(command).append('\n')
        headers.forEach { (key, value) -> append(key).append(':').append(value).append('\n') }
        append('\n')
        append('\u0000')
    }

    // "http://13.209.40.139:8080/" → "ws://13.209.40.139:8080/ws"
    private fun wsUrl(): String = BuildConfig.BASE_URL
        .replaceFirst("https://", "wss://")
        .replaceFirst("http://", "ws://")
        .trimEnd('/') + "/ws"

    private fun host(): String = Uri.parse(BuildConfig.BASE_URL).host ?: "localhost"
}