package com.taxipot.domain.user.dto;

/**
 * AccountResponse — API 명세서 3.9.0 components.schemas.AccountResponse
 * <p>내 출금 계좌 (3.5.0)
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record AccountResponse(
        String bankCode,
        String bankName,
        // 앞 3자리·뒤 4자리만 보이게 마스킹
        String maskedAccountNumber,
        String accountHolder
) {
}
