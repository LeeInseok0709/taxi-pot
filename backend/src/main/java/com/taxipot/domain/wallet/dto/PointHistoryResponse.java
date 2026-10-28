package com.taxipot.domain.wallet.dto;

import com.taxipot.global.code.PointType;

import java.time.OffsetDateTime;

/**
 * PointHistoryResponse — API 명세서 3.9.0 components.schemas.PointHistoryResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record PointHistoryResponse(
        Long id,
        // nullable
        Long potId,
        // nullable
        String potTitle,
        // nullable
        Long paymentId,
        PointType type,
        // +충전·환불 / −차감
        Integer amount,
        Integer balanceAfter,
        OffsetDateTime createdAt
) {
}
