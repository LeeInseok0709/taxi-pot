package com.example.taxipot.data.remote.dto.chat

/** 채팅 메시지 1개 — 명세서 ChatMessageResponse */
data class ChatMessageResponse(
    val id: Long,               // 메시지 번호 (중복 거르기·커서에 사용)
    val potId: Long,
    val senderId: Long,         // 보낸 사람 회원 번호
    val senderNickname: String,
    val msgType: String,        // TEXT(일반) / QUICK(퀵블록)
    val content: String,
    val sentAt: String          // "2026-11-25T21:25:00+09:00"
)

/** 채팅 메시지 페이지 — 명세서 ChatMessagePageResponse (최신순) */
data class ChatMessagePageResponse(
    val messages: List<ChatMessageResponse>,
    val nextCursor: Long?,      // 더 오래된 메시지를 부를 때 쓰는 값 (없으면 null)
    val hasNext: Boolean        // 더 오래된 메시지가 있는지
)

/** 채팅 보내기 요청 — 명세서 ChatMessageRequest */
data class ChatMessageRequest(
    val msgType: String,        // TEXT / QUICK
    val content: String         // 1,000자 이하 (QUICK이면 퀵블록 문구 그대로)
)

/** 퀵블록 문구 — 명세서 QuickBlockResponse */
data class QuickBlockResponse(
    val code: String,           // 예: LATE
    val content: String         // 예: 조금 늦어요
)