package com.taxipot.settlement.dto;

import com.taxipot.global.code.FareClaimStatus;
import com.taxipot.global.code.PotStatus;
import java.time.OffsetDateTime;

/**
 * 명세 3.9.0 스키마 FareClaimResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 실요금 청구 현황 (3.5.0)
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record FareClaimResponse(
        Long potId,
        String title,
        Long hostId,
        String hostNickname,
        PotStatus potStatus,
        FareClaimStatus fareClaimStatus,
        Integer expectedFare,
        /** 청구 금액 (청구 전 null); null 가능 */
        Integer actualFare,
        /** actualFare ÷ expectedFare (소수 둘째 자리, 청구 전 null) — 운영자 확인 참고용; null 가능 */
        Double fareRatio,
        Integer boardedCount,
        /** 영수증 이미지 조회 API 경로 (운영자는 `/api/admin/pots/{potId}/receipt`); null 가능 */
        String receiptUrl,
        /** null 가능 */
        OffsetDateTime startedAt,
        /** 청구 기한 = startedAt + 24시간; null 가능 */
        OffsetDateTime claimDeadline,
        /** null 가능 */
        OffsetDateTime fareClaimedAt,
        /** null 가능 */
        OffsetDateTime fareReviewedAt,
        /** null 가능 */
        String fareRejectReason
) {}
