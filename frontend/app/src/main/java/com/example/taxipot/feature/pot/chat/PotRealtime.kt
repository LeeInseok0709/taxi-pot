package com.example.taxipot.feature.pot.chat

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.taxipot.BuildConfig
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.data.remote.dto.chat.ChatMessageResponse
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * 방 실시간 연결 (명세서 '실시간 통신': WebSocket + STOMP)
 * - 연결: ws://서버/ws, CONNECT 헤더 Authorization: Bearer {accessToken}
 * - 구독: /topic/pots/{potId}
 * - 받는 것: CHAT_MESSAGE(새 채팅) / POT_UPDATED(인원·도착·상태 변경 → 상세를 다시 조회)
 * - 보내기는 REST(POST /messages)로만 함 (STOMP SEND 안 씀)
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
    private var socket: WebSocket? = null

    @Volatile
    private var active = false

    companion object {
        private const val TAG = "PotRealtime"
        private const val RETRY_MS = 3_000L
    }

    /** 연결 시작 (이미 연결 중이면 무시) */
    fun connect() {
        if (active) return
        active = true
        open()
    }

    /** 연결 끊기 (화면이 안 보일 때) */
    fun disconnect() {
        active = false
        mainHandler.removeCallbacksAndMessages(null)
        socket?.let {
            it.send(frame("DISCONNECT", emptyMap()))
            it.close(1000, null)
        }
        socket = null
    }

    private fun open() {
        val request = Request.Builder().url(wsUrl()).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {

            // 1) 연결되면 STOMP CONNECT (토큰 포함)
            override fun onOpen(webSocket: WebSocket, response: Response) {
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
                handleFrame(webSocket, text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "실시간 연결 끊김: ${t.message}")
                retryLater()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                retryLater()
            }
        })
    }

    // 서버에서 온 STOMP 글자 처리
    private fun handleFrame(webSocket: WebSocket, text: String) {
        val frame = text.trimStart('\n', '\r')
        if (frame.isEmpty()) return   // 하트비트(빈 줄)

        when (frame.substringBefore('\n').trim()) {
            // 2) CONNECT 성공 → 이 방 구독
            "CONNECTED" -> webSocket.send(
                frame(
                    "SUBSCRIBE", mapOf(
                        "id" to "pot-$potId",
                        "destination" to "/topic/pots/$potId",
                        "ack" to "auto"
                    )
                )
            )

            // 3) 방송 메시지 {"type": "...", "potId": 3, "payload": {...}}
            "MESSAGE" -> {
                val body = frame.substringAfter("\n\n", "").substringBefore('\u0000')
                handleEvent(body)
            }

            // 인증 실패·참여자 아님 등 → 다시 연결해도 같으니 멈춤
            "ERROR" -> {
                Log.w(TAG, "실시간 오류: ${frame.take(300)}")
                active = false
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

    // 끊기면 3초 뒤 다시 연결 (화면이 보이는 동안만)
    private fun retryLater() {
        if (!active) return
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