package com.taxipot.wallet.dto;

import com.taxipot.global.code.DebtStatus;
import java.time.OffsetDateTime;

/**
 * 명세 3.9.0 스키마 DebtResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 미수금 1건 (SETTLEMENT 행 기준, 3.3.0)
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record DebtResponse(
        Long settlementId,
        Long potId,
        String potTitle,
        OffsetDateTime settledAt,
        /** 정산 당시 못 걷은 금액 */
        Integer uncoveredAmount,
        DebtStatus debtStatus,
        /** 지금 내야 할 남은 금액 (OVERDUE여도 금액 변동 없음, 3.4.0) */
        Integer debtAmount,
        OffsetDateTime debtDueAt
) {}
