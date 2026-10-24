package com.taxipot.domain.payment.dto;

import com.taxipot.global.code.PaymentStatus;
import com.taxipot.global.code.PgProvider;

import java.time.OffsetDateTime;

/**
 * PaymentResponse — API 명세서 3.9.0 components.schemas.PaymentResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record PaymentResponse(
        Long id,
        String orderId,
        PgProvider pgProvider,
        // nullable
        String pgTid,
        Integer amount,
        PaymentStatus status,
        // nullable
        OffsetDateTime paidAt,
        // nullable / 승인·환불 직후 지갑 잔액 (주문 생성 시 null)
        Integer balanceAfter,
        OffsetDateTime createdAt
) {
}
