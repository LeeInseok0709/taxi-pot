package com.taxipot.chat.dto;

import com.taxipot.global.code.MsgType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 명세 3.9.0 스키마 ChatMessageRequest (Request — @Valid로 검증).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record ChatMessageRequest(
        /** 선택 */
        MsgType msgType,
        @NotBlank @Size(max = 1000) String content
) {}
