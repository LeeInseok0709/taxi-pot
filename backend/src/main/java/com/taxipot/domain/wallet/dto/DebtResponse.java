package com.taxipot.domain.wallet.dto;

import com.taxipot.global.code.DebtStatus;

import java.time.OffsetDateTime;

/**
 * DebtResponse — API 명세서 3.9.0 components.schemas.DebtResponse
 * <p>미수금 1건 (SETTLEMENT 행 기준, 3.3.0)
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record DebtResponse(
        Long settlementId,
        Long potId,
        String potTitle,
        OffsetDateTime settledAt,
        // 정산 당시 못 걷은 금액
        Integer uncoveredAmount,
        DebtStatus debtStatus,
        // 지금 내야 할 남은 금액 (OVERDUE여도 금액 변동 없음, 3.4.0)
        Integer debtAmount,
        OffsetDateTime debtDueAt
) {
}
