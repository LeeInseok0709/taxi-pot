package com.taxipot.domain.wallet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * WalletAdjustRequest — API 명세서 3.9.0 components.schemas.WalletAdjustRequest
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record WalletAdjustRequest(
        // 더할 금액 (음수면 차감). 조정 후 balance 0 이상, balance + held_balance 1,000,000원 이하
        @NotNull @Min(-1000000) @Max(1000000) Integer amount
) {
}
