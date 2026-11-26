package com.taxipot.domain.payment.dto;

import com.taxipot.global.code.PgProvider;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * PaymentCreateRequest — API 명세서 3.9.0 components.schemas.PaymentCreateRequest
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record PaymentCreateRequest(
        // 1회 충전 금액 (10,000 ~ 50,000원)
        @NotNull @Min(10000) @Max(50000) Integer amount,
        // 허용 값: PORTONE (그 외는 서비스에서 INVALID_INPUT) / PORTONE만 허용 (DUMMY는 400 DUMMY_CHARGE_NOT_ALLOWED, 3.3.0)
        @NotNull PgProvider pgProvider
) {
}
