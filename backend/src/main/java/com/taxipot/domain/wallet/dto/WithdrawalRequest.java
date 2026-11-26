package com.taxipot.domain.wallet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * WithdrawalRequest — API 명세서 3.9.0 components.schemas.WithdrawalRequest
 * <p>출금 신청 (3.5.0)
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record WithdrawalRequest(
        // 출금 금액 (1,000원 이상, 사용 가능 잔액 이하)
        @NotNull @Min(1000) @Max(1000000) Integer amount
) {
}
