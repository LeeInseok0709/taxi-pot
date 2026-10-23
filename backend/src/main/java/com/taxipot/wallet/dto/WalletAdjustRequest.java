package com.taxipot.wallet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 명세 3.9.0 스키마 WalletAdjustRequest (Request — @Valid로 검증).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record WalletAdjustRequest(
        /** 더할 금액 (음수면 차감). 조정 후 balance 0 이상, balance + held_balance 1,000,000원 이하 */
        @NotNull @Min(-1000000) @Max(1000000) Integer amount
) {}
