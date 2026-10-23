package com.taxipot.global.auth;

import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.ErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 명세 '미수금 이용 제한': 미수금이 있으면 허용 목록 외 모든 API에서 409 UNPAID_DEBT_EXISTS.
 * 각 API에서 따로 검사하지 않는다 (여기서 일괄). /api/admin/**는 대상 아님 (WebConfig에서 제외).
 * 허용 목록은 명세 3.9.0 '공통 규칙'과 1:1 — 명세가 바뀌면 여기만 고친다.
 */
@Component
@RequiredArgsConstructor
public class UnpaidDebtInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** "METHOD path" ("*"는 모든 메서드) */
    private static final List<String[]> ALLOW = List.of(
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
            new String[]{"GET", "/api/universities"},
            new String[]{"GET", "/api/banks"}
    );

    private final CurrentUser currentUser;
    private final ObjectProvider<UnpaidDebtChecker> checkerProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isAllowed(request)) {
            return true;
        }
        UnpaidDebtChecker checker = checkerProvider.getIfAvailable();
        if (checker == null) {
            return true;
        }
        currentUser.findId(request).flatMap(checker::findUnpaid).ifPresent(debt -> {
            throw new BusinessException(ErrorCode.UNPAID_DEBT_EXISTS,
                    "미납 금액 %,d원을 먼저 납부해 주세요.".formatted(debt.debtAmount()),
                    ErrorDetail.of("debtAmount", debt.debtAmount(), "debtDueAt", debt.debtDueAt()));
        });
        return true;
    }

    private boolean isAllowed(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();
        for (String[] rule : ALLOW) {
            if (("*".equals(rule[0]) || rule[0].equalsIgnoreCase(method)) && MATCHER.match(rule[1], path)) {
                return true;
            }
        }
        return false;
    }
}
