package com.taxipot.pot.dto;

import jakarta.validation.constraints.Size;

/**
 * 명세 3.9.0 스키마 PotUpdateRequest (Request — @Valid로 검증).
 * 변경할 항목만 전달
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record PotUpdateRequest(
        /** 선택 */
        @Size(max = 100) String title,
        /** 선택 */
        @Size(max = 500) String openChatUrl
) {}
