package com.taxipot.global.security;

import com.taxipot.global.code.UserStatus;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 액세스 토큰 → AuthUser (HTTP 필터와 STOMP CONNECT 공용).
 * 명세: 매 요청 USERS.token_version·status 를 확인해 다르거나 탈퇴면 401 TOKEN_REVOKED.
 */
@Component
@RequiredArgsConstructor
public class TokenAuthenticator {

    private final JwtProvider jwtProvider;
    private final AuthUserStatusQuery authUserStatusQuery;

    public AuthUser authenticate(String accessToken) {
        JwtProvider.TokenClaims claims = jwtProvider.parse(accessToken, JwtProvider.TYPE_ACCESS);
        AuthUserStatus status = authUserStatusQuery.findById(claims.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_REVOKED));
        if (status.status() == UserStatus.WITHDRAWN || status.tokenVersion() != claims.tokenVersion()) {
            throw new BusinessException(ErrorCode.TOKEN_REVOKED);
        }
        return new AuthUser(status.id(), status.role());
    }

    /** X-Debug-User-Id (local 프로필 전용): 토큰 없이 회원 지정. 탈퇴 회원은 불가 */
    public AuthUser authenticateDebugUser(Long userId) {
        AuthUserStatus status = authUserStatusQuery.findById(userId)
                .filter(s -> s.status() != UserStatus.WITHDRAWN)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "X-Debug-User-Id 회원이 없습니다."));
        return new AuthUser(status.id(), status.role());
    }

    public static String extractBearer(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}
