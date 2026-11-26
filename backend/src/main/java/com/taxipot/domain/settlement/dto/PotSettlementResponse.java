package com.taxipot.domain.settlement.dto;

import java.util.List;

/**
 * PotSettlementResponse — API 명세서 3.9.0 components.schemas.PotSettlementResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record PotSettlementResponse(
        Long potId,
        Integer expectedFare,
        // nullable
        Integer actualFare,
        // 탑승 인원(BOARDED)
        Integer boardedCount,
        // nullable / 방장에게 입금된 정산금(HOST_PAYOUT = actualFare), 정산 전 null (3.3.0)
        Integer hostPayoutAmount,
        // nullable / 영수증 이미지 조회 API 경로 `/api/pots/{potId}/receipt` (토큰 필요), 없으면 null (3.3.0)
        String receiptUrl,
        List<SettlementResponse> settlements
) {
}
