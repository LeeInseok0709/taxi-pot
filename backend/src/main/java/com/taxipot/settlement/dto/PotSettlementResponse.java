package com.taxipot.settlement.dto;

import java.util.List;

/**
 * 명세 3.9.0 스키마 PotSettlementResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record PotSettlementResponse(
        Long potId,
        Integer expectedFare,
        /** null 가능 */
        Integer actualFare,
        /** 탑승 인원(BOARDED) */
        Integer boardedCount,
        /** 방장에게 입금된 정산금(HOST_PAYOUT = actualFare), 정산 전 null (3.3.0); null 가능 */
        Integer hostPayoutAmount,
        /** 영수증 이미지 조회 API 경로 `/api/pots/{potId}/receipt` (토큰 필요), 없으면 null (3.3.0); null 가능 */
        String receiptUrl,
        List<SettlementResponse> settlements
) {}
