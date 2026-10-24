package com.taxipot.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * LoginRequest — API 명세서 3.9.0 components.schemas.LoginRequest
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        // 선택 / 로그인 기기의 FCM 토큰 (선택)
        String fcmToken
) {
}
