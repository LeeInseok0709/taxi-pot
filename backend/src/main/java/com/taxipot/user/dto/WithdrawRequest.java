package com.taxipot.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 명세 3.9.0 스키마 WithdrawRequest (Request — @Valid로 검증).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record WithdrawRequest(
        @NotBlank String password,
        /** 남은 지갑 잔액 소멸 동의 (true만 허용, 3.3.0) */
        @NotNull Boolean agreeBalanceForfeit
) {}
