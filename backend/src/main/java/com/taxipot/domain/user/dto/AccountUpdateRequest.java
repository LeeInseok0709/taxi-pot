package com.taxipot.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AccountUpdateRequest — API 명세서 3.9.0 components.schemas.AccountUpdateRequest
 * <p>출금 계좌 등록·변경 (3.5.0)
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record AccountUpdateRequest(
        // 현재 비밀번호
        @NotBlank String password,
        // `GET /api/banks` 목록 코드. 004·011·088·020·090·092 중 하나
        @NotBlank @Size(max = 10) String bankCode,
        @NotBlank @Size(max = 20) String accountNumber,
        @NotBlank @Size(max = 50) String accountHolder
) {
}
