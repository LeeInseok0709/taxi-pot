package com.taxipot.pot.dto;

import com.taxipot.global.code.PotStatus;

/**
 * 명세 3.9.0 스키마 JoinResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record JoinResponse(
        Long potId,
        PotMemberResponse member,
        /** 예치된 금액 (예상 분담금 + 수수료) */
        Integer heldAmount,
        /** 예치 후 사용 가능 잔액 */
        Integer balanceAfter,
        PotStatus potStatus
) {}
