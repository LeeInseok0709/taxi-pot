package com.taxipot.domain.settlement.dto;

import com.taxipot.global.code.FareClaimStatus;
import com.taxipot.global.code.PotStatus;

import java.time.OffsetDateTime;

/**
 * FareClaimResponse — API 명세서 3.9.0 components.schemas.FareClaimResponse
 * <p>실요금 청구 현황 (3.5.0)
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record FareClaimResponse(
        Long potId,
        String title,
        Long hostId,
        String hostNickname,
        PotStatus potStatus,
        FareClaimStatus fareClaimStatus,
        Integer expectedFare,
        // nullable / 청구 금액 (청구 전 null)
        Integer actualFare,
        // nullable / actualFare ÷ expectedFare (소수 둘째 자리, 청구 전 null) — 운영자 확인 참고용
        Double fareRatio,
        Integer boardedCount,
        // nullable / 영수증 이미지 조회 API 경로 (운영자는 `/api/admin/pots/{potId}/receipt`)
        String receiptUrl,
        // nullable
        OffsetDateTime startedAt,
        // nullable / 청구 기한 = startedAt + 24시간
        OffsetDateTime claimDeadline,
        // nullable
        OffsetDateTime fareClaimedAt,
        // nullable
        OffsetDateTime fareReviewedAt,
        // nullable
        String fareRejectReason
) {
}
