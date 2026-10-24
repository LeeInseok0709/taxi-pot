package com.taxipot.domain.wallet.dto;

import java.time.OffsetDateTime;

/**
 * WalletResponse — API 명세서 3.9.0 components.schemas.WalletResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record WalletResponse(
        Long id,
        Long memberId,
        // 즉시 사용 가능한 잔액 (방장 정산금 입금으로 1,000,000원을 넘을 수 있음)
        Integer balance,
        // 남은 미수금 합계 (UNPAID·OVERDUE 행의 debt_amount 합, 없으면 0, 3.3.0)
        Integer debtAmount,
        // 선결제로 예치(홀딩)된 금액
        Integer heldBalance,
        OffsetDateTime updatedAt
) {
}
