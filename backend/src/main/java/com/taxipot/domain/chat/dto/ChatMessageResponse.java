package com.taxipot.domain.chat.dto;

import com.taxipot.global.code.MsgType;

import java.time.OffsetDateTime;

/**
 * ChatMessageResponse — API 명세서 3.9.0 components.schemas.ChatMessageResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record ChatMessageResponse(
        Long id,
        Long potId,
        Long senderId,
        String senderNickname,
        MsgType msgType,
        String content,
        OffsetDateTime sentAt
) {
}
