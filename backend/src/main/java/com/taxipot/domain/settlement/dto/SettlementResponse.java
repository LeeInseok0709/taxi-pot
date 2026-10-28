package com.taxipot.domain.settlement.dto;

import com.taxipot.global.code.DebtStatus;
import com.taxipot.global.code.SettlementStatus;

import java.time.OffsetDateTime;

/**
 * SettlementResponse — API 명세서 3.9.0 components.schemas.SettlementResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record SettlementResponse(
        Long id,
        Long potId,
        Long memberId,
        String nickname,
        Integer heldAmount,
        // nullable / 정산 전 null, 노쇼·취소 등 환불(REFUNDED) 행도 null (3.3.0, 0 → null)
        Integer fareShare,
        // nullable / 정산 시점에 기록 (정산 전·환불 행은 null, 3.3.0)
        Integer feeAmount,
        // nullable / heldAmount − (fareShare + feeAmount), 양수: 환불 / 음수: 추가 차감 대상액
        Integer diffAmount,
        // 정산 시 잔액 부족으로 못 걷은 금액 (기본 0). 택시팟이 방장에게 대신 지급하고 회원 미수금으로 전환
        Integer uncoveredAmount,
        DebtStatus debtStatus,
        // 남은 미수 납부액 (OVERDUE여도 금액 변동 없음, 3.4.0)
        Integer debtAmount,
        // nullable / 미수금 납부 기한 (정산 + 3일, 3.4.0), 미수 없으면 null
        OffsetDateTime debtDueAt,
        SettlementStatus status,
        // nullable
        OffsetDateTime settledAt,
        OffsetDateTime createdAt
) {
}
