package com.taxipot.rating.dto;

import com.taxipot.global.code.PotMemberRole;

/**
 * 명세 3.9.0 스키마 RatingTargetResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record RatingTargetResponse(
        Long memberId,
        String nickname,
        PotMemberRole role,
        /** 내가 이미 평가했는지 여부 (자동 기본 평가가 들어갔어도 true) */
        Boolean isRated
) {}
