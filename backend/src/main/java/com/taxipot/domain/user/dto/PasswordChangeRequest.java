package com.taxipot.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PasswordChangeRequest — API 명세서 3.9.0 components.schemas.PasswordChangeRequest
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record PasswordChangeRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8) String newPassword
) {
}
