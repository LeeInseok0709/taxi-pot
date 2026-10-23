package com.taxipot.chat.dto;

import com.taxipot.global.code.MsgType;
import java.time.OffsetDateTime;

/**
 * 명세 3.9.0 스키마 ChatMessageResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record ChatMessageResponse(
        Long id,
        Long potId,
        Long senderId,
        String senderNickname,
        MsgType msgType,
        String content,
        OffsetDateTime sentAt
) {}
