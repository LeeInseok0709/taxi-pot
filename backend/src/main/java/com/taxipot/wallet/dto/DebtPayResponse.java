package com.taxipot.wallet.dto;

import com.taxipot.global.code.DebtStatus;

/**
 * 명세 3.9.0 스키마 DebtPayResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record DebtPayResponse(
        Long settlementId,
        /** 이번에 납부한 금액 */
        Integer paidAmount,
        DebtStatus debtStatus,
        /** 납부 후 남은 금액 (0이면 PAID) */
        Integer debtAmount,
        Integer balanceAfter
) {}
