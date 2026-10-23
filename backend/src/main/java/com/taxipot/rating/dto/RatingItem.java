package com.taxipot.rating.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 명세 3.9.0 스키마 RatingItem (Request — @Valid로 검증).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record RatingItem(
        @NotNull Long revieweeId,
        /** 시간 약속 별점 */
        @NotNull @Min(1) @Max(5) Integer noshowScore,
        /** 언행·태도 별점 */
        @NotNull @Min(1) @Max(5) Integer mannerScore,
        /** 연락 별점 */
        @NotNull @Min(1) @Max(5) Integer communicationScore,
        /** 정산 별점 */
        @NotNull @Min(1) @Max(5) Integer paymentScore,
        /** 4항목 평균 3점 미만(별로였어요)일 때 필수; 선택 */
        @Size(max = 255) String badReason
) {}
