package com.taxipot.wallet.dto;

import com.taxipot.global.code.PaymentStatus;
import com.taxipot.global.code.PgProvider;
import java.time.OffsetDateTime;

/**
 * 명세 3.9.0 스키마 PaymentResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record PaymentResponse(
        Long id,
        String orderId,
        PgProvider pgProvider,
        /** null 가능 */
        String pgTid,
        Integer amount,
        PaymentStatus status,
        /** null 가능 */
        OffsetDateTime paidAt,
        /** 승인·환불 직후 지갑 잔액 (주문 생성 시 null); null 가능 */
        Integer balanceAfter,
        OffsetDateTime createdAt
) {}
