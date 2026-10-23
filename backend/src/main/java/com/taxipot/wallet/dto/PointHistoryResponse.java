package com.taxipot.wallet.dto;

import com.taxipot.global.code.PointType;
import java.time.OffsetDateTime;

/**
 * 명세 3.9.0 스키마 PointHistoryResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record PointHistoryResponse(
        Long id,
        /** null 가능 */
        Long potId,
        /** null 가능 */
        String potTitle,
        /** null 가능 */
        Long paymentId,
        PointType type,
        /** +충전·환불 / −차감 */
        Integer amount,
        Integer balanceAfter,
        OffsetDateTime createdAt
) {}
