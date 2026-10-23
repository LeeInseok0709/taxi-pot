package com.taxipot.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 명세 3.9.0 스키마 PaymentConfirmRequest (Request — @Valid로 검증).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record PaymentConfirmRequest(
        @NotBlank @Size(max = 64) String orderId,
        /** PG사 거래번호 (필수) */
        @NotBlank @Size(max = 100) String pgTid
) {}
