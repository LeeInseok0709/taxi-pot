package com.example.taxipot.feature.pot.chat

import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.ChatApi
import com.example.taxipot.data.remote.dto.chat.ChatMessagePageResponse
import com.example.taxipot.data.remote.dto.chat.ChatMessageRequest
import com.example.taxipot.data.remote.dto.chat.ChatMessageResponse
import com.example.taxipot.data.remote.dto.chat.QuickBlockResponse
import com.example.taxipot.feature.pot.data.PotRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 채팅 데이터 창구 (가짜 데이터 ↔ 서버는 PotRepository.USE_FAKE 를 같이 따름)
 */
object ChatRepository {

    // 한 번에 받아올 메시지 수 (명세서 기본 30)
    private const val PAGE_SIZE = 30

    private val api by lazy { ApiClient.create(ChatApi::class.java) }

    // ===== 가짜 데이터 (앱을 끄면 처음으로 돌아감) =====
    private val fakeMessages = mutableMapOf<Long, MutableList<ChatMessageResponse>>()
    private var fakeNextId = 1000L

    // 서버 QuickBlockCatalog 와 같은 4개
    private val fakeQuickBlocks = listOf(
        QuickBlockResponse("ARRIVED", "도착했어요"),
        QuickBlockResponse("LATE", "조금 늦어요"),
        QuickBlockResponse("ARRIVING_SOON", "곧 도착해요"),
        QuickBlockResponse("DEPARTING_SOON", "곧 출발해요")
    )

    /** 퀵블록 문구 목록 */
    suspend fun quickBlocks(): ApiResult<List<QuickBlockResponse>> {
        if (PotRepository.USE_FAKE) return ApiResult.Success(fakeQuickBlocks)
        return safeApiCall { api.getQuickBlocks() }
    }

    /** 메시지 조회 (최신순, cursor 가 있으면 그보다 오래된 것) */
    suspend fun messages(potId: Long, cursor: Long?): ApiResult<ChatMessagePageResponse> {
        if (!PotRepository.USE_FAKE) return safeApiCall { api.getMessages(potId, cursor, PAGE_SIZE) }

        val older = fakeList(potId)
            .filter { cursor == null || it.id < cursor }
            .sortedByDescending { it.id }
        val page = older.take(PAGE_SIZE)
        val hasNext = older.size > PAGE_SIZE
        return ApiResult.Success(ChatMessagePageResponse(page, if (hasNext) page.last().id else null, hasNext))
    }

    /** 메시지 보내기 (msgType: TEXT / QUICK) */
    suspend fun send(potId: Long, msgType: String, content: String): ApiResult<ChatMessageResponse> {
        if (!PotRepository.USE_FAKE) {
            return safeApiCall { api.sendMessage(potId, ChatMessageRequest(msgType, content)) }
        }
        val message = ChatMessageResponse(
            id = fakeNextId++, potId = potId, senderId = PotRepository.FAKE_MY_ID, senderNickname = "나",
            msgType = msgType, content = content, sentAt = nowText()
        )
        fakeList(potId).add(message)
        return ApiResult.Success(message)
    }

    /** 실패 결과를 화면 문장으로 */
    fun errorMessage(result: ApiResult<*>): String = when {
        result is ApiResult.Failure && result.code == "INVALID_QUICK_MESSAGE" -> "보낼 수 없는 퀵블록이에요."
        result is ApiResult.Failure && result.code == "NOT_POT_MEMBER" -> "이 택시팟 참여자만 채팅할 수 있어요."
        result is ApiResult.Failure && result.code == "INVALID_POT_STATUS" -> "끝난 택시팟에는 메시지를 보낼 수 없어요."
        else -> PotRepository.errorMessage(result)
    }

    // 가짜: 방마다 처음 들어가면 예시 대화 3개를 만들어 둠
    private suspend fun fakeList(potId: Long): MutableList<ChatMessageResponse> {
        fakeMessages[potId]?.let { return it }

        val pot = (PotRepository.getPot(potId) as? ApiResult.Success)?.data
        // 상대방: 내가 아닌 첫 번째 참여자
        val other = pot?.members?.firstOrNull { it.memberId != PotRepository.FAKE_MY_ID }
        val otherId = other?.memberId ?: 999L
        val otherName = other?.nickname ?: "동승자"
        val time = pot?.createdAt ?: nowText()

        val list = mutableListOf(
            ChatMessageResponse(1, potId, otherId, otherName, "TEXT", "저 출구 앞이에요", time),
            ChatMessageResponse(2, potId, PotRepository.FAKE_MY_ID, "나", "TEXT", "금방 가요!", time),
            ChatMessageResponse(3, potId, otherId, otherName, "QUICK", "조금 늦어요", time)
        )
        fakeMessages[potId] = list
        return list
    }

    private fun nowText(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA)
            .apply { timeZone = TimeZone.getTimeZone("Asia/Seoul") }
            .format(Date()) + "+09:00"
}