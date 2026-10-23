package com.taxipot.settlement.dto;

import com.taxipot.global.code.DebtStatus;
import com.taxipot.global.code.SettlementStatus;
import java.time.OffsetDateTime;

/**
 * 명세 3.9.0 스키마 SettlementResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record SettlementResponse(
        Long id,
        Long potId,
        Long memberId,
        String nickname,
        Integer heldAmount,
        /** 정산 전 null, 노쇼·취소 등 환불(REFUNDED) 행도 null (3.3.0, 0 → null); null 가능 */
        Integer fareShare,
        /** 정산 시점에 기록 (정산 전·환불 행은 null, 3.3.0); null 가능 */
        Integer feeAmount,
        /** heldAmount − (fareShare + feeAmount), 양수: 환불 / 음수: 추가 차감 대상액; null 가능 */
        Integer diffAmount,
        /** 정산 시 잔액 부족으로 못 걷은 금액 (기본 0). 택시팟이 방장에게 대신 지급하고 회원 미수금으로 전환 */
        Integer uncoveredAmount,
        DebtStatus debtStatus,
        /** 남은 미수 납부액 (OVERDUE여도 금액 변동 없음, 3.4.0) */
        Integer debtAmount,
        /** 미수금 납부 기한 (정산 + 3일, 3.4.0), 미수 없으면 null; null 가능 */
        OffsetDateTime debtDueAt,
        SettlementStatus status,
        /** null 가능 */
        OffsetDateTime settledAt,
        OffsetDateTime createdAt
) {}
