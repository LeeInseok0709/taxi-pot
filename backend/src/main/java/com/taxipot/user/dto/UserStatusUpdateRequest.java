package com.taxipot.user.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * 명세 3.9.0 스키마 UserStatusUpdateRequest (Request — @Valid로 검증).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record UserStatusUpdateRequest(
        @NotNull Status status,
        /** SUSPENDED일 때 해제 예정 시각, 미지정 시 영구 정지. 기존 정지가 더 길면 기존 값 유지 (3.3.0); null 가능 */
        OffsetDateTime suspendedUntil
) {

    public enum Status { ACTIVE, SUSPENDED }
}
