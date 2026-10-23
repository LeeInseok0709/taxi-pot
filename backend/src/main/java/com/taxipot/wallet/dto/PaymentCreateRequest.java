package com.taxipot.wallet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 명세 3.9.0 스키마 PaymentCreateRequest (Request — @Valid로 검증).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record PaymentCreateRequest(
        /** 1회 충전 금액 (1,000 ~ 500,000원) */
        @NotNull @Min(1000) @Max(500000) Integer amount,
        /** PORTONE만 허용 (DUMMY는 400 DUMMY_CHARGE_NOT_ALLOWED, 3.3.0) */
        @NotNull PgProvider pgProvider
) {

    public enum PgProvider { PORTONE }
}
