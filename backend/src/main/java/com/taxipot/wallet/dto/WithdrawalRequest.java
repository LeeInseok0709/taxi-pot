package com.taxipot.wallet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 명세 3.9.0 스키마 WithdrawalRequest (Request — @Valid로 검증).
 * 출금 신청 (3.5.0)
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record WithdrawalRequest(
        /** 출금 금액 (1,000원 이상, 사용 가능 잔액 이하) */
        @NotNull @Min(1000) @Max(1000000) Integer amount
) {}
