package com.taxipot.global.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 미수금 이용 제한 허용 목록 (명세 3.5.0·3.8.0) */
class DebtRestrictionInterceptorTest {

    @Test
    void allowList() {
        assertTrue(DebtRestrictionInterceptor.isAllowed("GET", "/api/users/me"));
        assertTrue(DebtRestrictionInterceptor.isAllowed("POST", "/api/wallet/debts/12/pay"));
        assertTrue(DebtRestrictionInterceptor.isAllowed("PATCH", "/api/notifications/3/read"));
        assertTrue(DebtRestrictionInterceptor.isAllowed("GET", "/api/notifications"));
        assertFalse(DebtRestrictionInterceptor.isAllowed("PATCH", "/api/users/me"));
        assertFalse(DebtRestrictionInterceptor.isAllowed("POST", "/api/pots"));
        assertFalse(DebtRestrictionInterceptor.isAllowed("POST", "/api/pots/1/members/me/arrival"));
        assertFalse(DebtRestrictionInterceptor.isAllowed("GET", "/api/pots/1/messages"));
    }
}
