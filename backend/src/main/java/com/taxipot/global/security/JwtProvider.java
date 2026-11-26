package com.taxipot.global.security;

import com.taxipot.global.code.UserRole;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 발급·검증. 리프레시 토큰도 DB 에 저장하지 않는 서명 검증형 (명세 /api/auth/refresh).
 * <p>claim: sub = 회원 id, tv = USERS.token_version(발급 시점), typ = access|refresh, role
 */
@Component
public class JwtProvider {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final Clock clock;

    public JwtProvider(AppProperties props, Clock clock) {
        String secret = props.jwt() == null ? null : props.jwt().secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret 은 32바이트 이상이어야 합니다 (환경변수 TAXIPOT_JWT_SECRET)");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofMinutes(props.jwt().accessTokenMinutes());
        this.refreshTtl = Duration.ofDays(props.jwt().refreshTokenDays());
        this.clock = clock;
    }

    public String createAccessToken(Long userId, UserRole role, int tokenVersion) {
        return create(userId, role, tokenVersion, TYPE_ACCESS, accessTtl);
    }

    public String createRefreshToken(Long userId, UserRole role, int tokenVersion) {
        return create(userId, role, tokenVersion, TYPE_REFRESH, refreshTtl);
    }

    /** TokenResponse.expiresIn (초) */
    public int accessTokenExpiresInSeconds() {
        return (int) accessTtl.toSeconds();
    }

    /**
     * 서명·만료·타입 검증. 실패 시 401 UNAUTHORIZED.
     * (token_version 비교는 호출 측에서 DB 값과 한다 → 다르면 TOKEN_REVOKED)
     */
    public TokenClaims parse(String token, String expectedType) {
        try {
            Claims c = Jwts.parser().verifyWith(key).clock(() -> Date.from(clock.instant())).build()
                    .parseSignedClaims(token).getPayload();
            if (!expectedType.equals(c.get("typ", String.class))) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "토큰 종류가 올바르지 않습니다.");
            }
            Integer tv = c.get("tv", Integer.class);
            return new TokenClaims(Long.valueOf(c.getSubject()), tv == null ? -1 : tv);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "인증 토큰이 없거나 만료되었습니다.");
        }
    }

    private String create(Long userId, UserRole role, int tokenVersion, String type, Duration ttl) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("tv", tokenVersion)
                .claim("typ", type)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public record TokenClaims(Long userId, int tokenVersion) {
    }
}
