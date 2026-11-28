package com.example.taxipot.feature.pot.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.data.remote.dto.chat.ChatMessageResponse
import com.example.taxipot.data.remote.dto.chat.QuickBlockResponse
import com.example.taxipot.feature.pot.data.MyInfo
import com.example.taxipot.feature.pot.data.PotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 채팅 화면에 그릴 상태 */
data class ChatUiState(
    /** "백석대 → 천안역 · 동승 채팅" */
    val subtitle: String = "",
    /** 이 방에서 "나"의 회원 번호 (내 메시지는 오른쪽) */
    val myUserId: Long? = null,
    /** 메시지 (오래된 것 → 최신, 화면 위 → 아래) */
    val messages: List<ChatMessageResponse> = emptyList(),
    val quickBlocks: List<QuickBlockResponse> = emptyList(),
    /** 더 오래된 메시지가 있는지 */
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val sending: Boolean = false,
    /** 한 번 보여주고 지울 문장 */
    val message: String? = null,
    /** 이 숫자가 바뀔 때마다 맨 아래로 스크롤 */
    val scrollToBottom: Int = 0,
    /** 전송 실패한 일반 메시지 (입력칸에 되돌림) */
    val failedText: String? = null
)

/** RD-02 채팅 */
class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var potId = -1L
    private var nextCursor: Long? = null
    private var realtime: PotRealtime? = null

    /** 처음 한 번: 방 정보 → 퀵블록 → 최근 메시지 */
    fun start(potId: Long) {
        if (this.potId == potId) return
        this.potId = potId

        viewModelScope.launch {
            when (val result = PotRepository.getPot(potId)) {
                is ApiResult.Success -> _uiState.update {
                    val pot = result.data
                    it.copy(
                        subtitle = "${pot.departureName.replace("대학교", "대")} → ${pot.destinationName} · 동승 채팅",
                        myUserId = MyInfo.myUserId(pot)
                    )
                }
                is ApiResult.Failure, is ApiResult.NetworkError -> showMessage(PotRepository.errorMessage(result))
            }

            when (val result = ChatRepository.quickBlocks()) {
                is ApiResult.Success -> _uiState.update { it.copy(quickBlocks = result.data) }
                is ApiResult.Failure, is ApiResult.NetworkError -> Unit   // 퀵블록이 없어도 채팅은 가능
            }

            loadLatest(scroll = true)
        }
    }

    /** 최근 메시지 다시 받기 (처음, 실시간 연결이 다시 됐을 때) */
    private suspend fun loadLatest(scroll: Boolean) {
        when (val result = ChatRepository.messages(potId, null)) {
            is ApiResult.Success -> {
                val page = result.data
                if (_uiState.value.messages.isEmpty()) {
                    nextCursor = page.nextCursor
                    _uiState.update { it.copy(hasMore = page.hasNext) }
                }
                merge(page.messages, scroll)
            }
            is ApiResult.Failure, is ApiResult.NetworkError -> showMessage(ChatRepository.errorMessage(result))
        }
    }

    /** 위로 끝까지 올리면 더 오래된 메시지 */
    fun loadMore() {
        val state = _uiState.value
        if (state.loadingMore || !state.hasMore || potId < 0) return
        _uiState.update { it.copy(loadingMore = true) }

        viewModelScope.launch {
            when (val result = ChatRepository.messages(potId, nextCursor)) {
                is ApiResult.Success -> {
                    nextCursor = result.data.nextCursor
                    _uiState.update { it.copy(hasMore = result.data.hasNext) }
                    merge(result.data.messages, scroll = false)
                }
                is ApiResult.Failure, is ApiResult.NetworkError -> showMessage(ChatRepository.errorMessage(result))
            }
            _uiState.update { it.copy(loadingMore = false) }
        }
    }

    /** 일반 메시지 보내기 */
    fun sendText(text: String) {
        val content = text.trim()
        if (content.isEmpty()) return
        if (content.length > 1000) {
            showMessage("메시지는 1,000자까지 보낼 수 있어요")
            return
        }
        send("TEXT", content)
    }

    /** 퀵블록 보내기 (문구 그대로) */
    fun sendQuick(block: QuickBlockResponse) = send("QUICK", block.content)

    private fun send(msgType: String, content: String) {
        if (_uiState.value.sending || potId < 0) return
        _uiState.update { it.copy(sending = true) }

        viewModelScope.launch {
            when (val result = ChatRepository.send(potId, msgType, content)) {
                is ApiResult.Success -> merge(listOf(result.data), scroll = true)
                is ApiResult.Failure, is ApiResult.NetworkError -> _uiState.update {
                    it.copy(
                        message = "전송하지 못했어요. 다시 눌러 주세요",
                        failedText = if (msgType == "TEXT") content else null
                    )
                }
            }
            _uiState.update { it.copy(sending = false) }
        }
    }

    /**
     * 메시지 합치기: 같은 id는 한 번만 (내 메시지는 REST 응답 + 실시간 방송으로 두 번 오므로)
     * 항상 오래된 것 → 최신 순서로
     */
    private fun merge(newMessages: List<ChatMessageResponse>, scroll: Boolean) {
        _uiState.update { state ->
            val merged = (state.messages + newMessages).distinctBy { it.id }.sortedBy { it.id }
            state.copy(
                messages = merged,
                scrollToBottom = if (scroll) state.scrollToBottom + 1 else state.scrollToBottom
            )
        }
    }

    /** 화면이 보일 때: 실시간 연결 (가짜 데이터일 때는 연결 안 함) */
    fun startRealtime() {
        if (PotRepository.USE_FAKE || potId < 0) return
        if (realtime == null) {
            realtime = PotRealtime(
                potId = potId,
                onChat = { merge(listOf(it), scroll = true) },
                onPotUpdated = { }   // 채팅 화면에서는 방 정보 변경은 따로 안 그림
            )
        }
        realtime?.connect()
        // 끊겨 있던 동안 온 메시지 채우기
        if (_uiState.value.messages.isNotEmpty()) {
            viewModelScope.launch { loadLatest(scroll = false) }
        }
    }

    /** 화면이 안 보일 때: 실시간 끊기 */
    fun stopRealtime() {
        realtime?.disconnect()
    }

    override fun onCleared() {
        realtime?.disconnect()
    }

    private fun showMessage(text: String) = _uiState.update { it.copy(message = text) }

    /** 되돌린 뒤 지우기 */
    fun failedTextShown() = _uiState.update { it.copy(failedText = null) }

    /** 문장을 보여준 뒤 지우기 */
    fun messageShown() = _uiState.update { it.copy(message = null) }
}