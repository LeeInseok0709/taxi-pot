package com.taxipot.rating.dto;

import java.time.OffsetDateTime;

/**
 * 명세 3.9.0 스키마 RatingResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record RatingResponse(
        Long id,
        Long potId,
        Long reviewerId,
        Long revieweeId,
        Integer noshowScore,
        Integer mannerScore,
        Integer communicationScore,
        Integer paymentScore,
        /** DB 생성 컬럼 값 */
        Double avgScore,
        /** null 가능 */
        String badReason,
        /** true = 정산 24시간 후 서버가 넣은 기본 평가(4항목 ★3), false = 회원 제출 (3.3.0) */
        Boolean isDefault,
        OffsetDateTime createdAt
) {}
