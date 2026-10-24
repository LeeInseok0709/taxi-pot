package com.taxipot.domain.auth.dto;

import com.taxipot.domain.user.dto.MyProfileResponse;

/**
 * LoginResponse — API 명세서 3.9.0 components.schemas.LoginResponse
 * <p>TokenResponse + 내 정보 (가입·로그인 공용)
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        // 액세스 토큰 만료까지 남은 초
        Integer expiresIn,
        MyProfileResponse user
) {
}
