package com.taxipot.domain.auth.dto;

/**
 * BankResponse — API 명세서 3.9.0 components.schemas.BankResponse
 * <p>출금 은행 (3.5.0)
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record BankResponse(
        String code,
        String name
) {
}
