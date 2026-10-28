package com.taxipot.global.web;

import com.taxipot.global.error.Errors;
import com.taxipot.global.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 명세 '미수금 이용 제한' (3.5.0, 3.8.0 예외 없음): 미수금이 있으면 허용 목록 외 모든 API 에서 409 UNPAID_DEBT_EXISTS.
 * <p>각 API 에서 따로 검사하지 않는다. 운영자 API(/api/admin/**)는 대상 아님.
 * <p>새 API 를 허용 목록에 넣으려면 명세부터 바꾼다.
 */
@Component
@RequiredArgsConstructor
public class DebtRestrictionInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** "METHOD path" ("*" = 모든 메서드) */
    private static final List<String[]> ALLOW_LIST = List.of(
            new String[]{"POST", "/api/auth/login"},
            new String[]{"POST", "/api/auth/refresh"},
            new String[]{"POST", "/api/auth/logout"},
            new String[]{"PUT", "/api/users/me/fcm-token"},
            new String[]{"GET", "/api/users/me"},
            new String[]{"GET", "/api/wallet"},
            new String[]{"GET", "/api/wallet/history"},
            new String[]{"GET", "/api/wallet/debts"},
            new String[]{"POST", "/api/wallet/debts/*/pay"},
            new String[]{"POST", "/api/payments"},
            new String[]{"POST", "/api/payments/confirm"},
            new String[]{"GET", "/api/payments"},
            new String[]{"*", "/api/notifications/**"},
            new String[]{"*", "/api/notifications"},
            new String[]{"GET", "/api/universities"},
            new String[]{"GET", "/api/banks"}
    );

    private final UnpaidDebtQuery unpaidDebtQuery;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/api/admin/") || isAllowed(request.getMethod(), path)) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            return true; // 비로그인 공개 API (가입 등)
        }
        unpaidDebtQuery.findUnpaid(user.id()).ifPresent(debt -> {
            throw Errors.unpaidDebtExists(debt.totalAmount(), debt.earliestDueAt());
        });
        return true;
    }

    static boolean isAllowed(String method, String path) {
        for (String[] rule : ALLOW_LIST) {
            if (("*".equals(rule[0]) || rule[0].equalsIgnoreCase(method)) && MATCHER.match(rule[1], path)) {
                return true;
            }
        }
        return false;
    }
}
