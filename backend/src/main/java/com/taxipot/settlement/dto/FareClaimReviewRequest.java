package com.taxipot.settlement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 명세 3.9.0 스키마 FareClaimReviewRequest (Request — @Valid로 검증).
 * 실요금 청구 승인·반려 (운영자, 3.5.0)
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record FareClaimReviewRequest(
        @NotNull Decision decision,
        /** REJECTED면 필수; null 가능 */
        @Size(max = 255) String rejectReason
) {

    public enum Decision { APPROVED, REJECTED }
}
