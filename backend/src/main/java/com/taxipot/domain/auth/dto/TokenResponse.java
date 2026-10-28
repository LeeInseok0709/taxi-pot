package com.taxipot.domain.auth.dto;

/**
 * TokenResponse — API 명세서 3.9.0 components.schemas.TokenResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        // 액세스 토큰 만료까지 남은 초
        Integer expiresIn
) {
}
