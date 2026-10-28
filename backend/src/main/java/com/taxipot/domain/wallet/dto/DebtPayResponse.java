package com.taxipot.domain.wallet.dto;

import com.taxipot.global.code.DebtStatus;

/**
 * DebtPayResponse — API 명세서 3.9.0 components.schemas.DebtPayResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record DebtPayResponse(
        Long settlementId,
        // 이번에 납부한 금액
        Integer paidAmount,
        DebtStatus debtStatus,
        // 납부 후 남은 금액 (0이면 PAID)
        Integer debtAmount,
        Integer balanceAfter
) {
}
