package com.taxipot.global.security;

import com.taxipot.global.error.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authorization: Bearer 토큰 인증. SecurityConfig 에서 직접 생성(@Component 아님 → 서블릿 필터 이중 등록 방지).
 * <p>토큰이 잘못돼도 여기서 응답하지 않고 사유만 request 속성에 남긴다. 인증이 필요한 API 면
 * RestAuthenticationEntryPoint 가 그 사유(UNAUTHORIZED / TOKEN_REVOKED)로 401 을 보낸다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String DEBUG_HEADER = "X-Debug-User-Id";
    public static final String AUTH_ERROR_ATTRIBUTE = "taxipot.authError";

    private final TokenAuthenticator tokenAuthenticator;
    private final boolean debugHeaderEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = TokenAuthenticator.extractBearer(request.getHeader(HttpHeaders.AUTHORIZATION));
        try {
            if (token != null) {
                setAuthentication(tokenAuthenticator.authenticate(token));
            } else if (debugHeaderEnabled && request.getHeader(DEBUG_HEADER) != null) {
                setAuthentication(tokenAuthenticator.authenticateDebugUser(Long.valueOf(request.getHeader(DEBUG_HEADER))));
            }
        } catch (BusinessException e) {
            SecurityContextHolder.clearContext();
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, e.getErrorCode());
        } catch (NumberFormatException e) {
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(request, response);
    }

    private void setAuthentication(AuthUser user) {
        var auth = new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
