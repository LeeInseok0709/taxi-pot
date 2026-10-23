package com.taxipot.wallet.dto;

import java.time.OffsetDateTime;

/**
 * 명세 3.9.0 스키마 WalletResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record WalletResponse(
        Long id,
        Long memberId,
        /** 즉시 사용 가능한 잔액 (방장 정산금 입금으로 1,000,000원을 넘을 수 있음) */
        Integer balance,
        /** 남은 미수금 합계 (UNPAID·OVERDUE 행의 debt_amount 합, 없으면 0, 3.3.0) */
        Integer debtAmount,
        /** 선결제로 예치(홀딩)된 금액 */
        Integer heldBalance,
        OffsetDateTime updatedAt
) {}
