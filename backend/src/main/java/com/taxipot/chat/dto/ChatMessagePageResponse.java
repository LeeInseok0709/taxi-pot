package com.taxipot.chat.dto;

import java.util.List;

/**
 * 명세 3.9.0 스키마 ChatMessagePageResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record ChatMessagePageResponse(
        List<ChatMessageResponse> messages,
        /** 다음 조회용 커서, 더 없으면 null; null 가능 */
        Long nextCursor,
        Boolean hasNext
) {}
