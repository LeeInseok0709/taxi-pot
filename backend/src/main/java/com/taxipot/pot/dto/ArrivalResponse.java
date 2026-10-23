package com.taxipot.pot.dto;

import java.time.OffsetDateTime;

/**
 * 명세 3.9.0 스키마 ArrivalResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record ArrivalResponse(
        Long potId,
        OffsetDateTime arrivedAt,
        /** 약속 장소(출발지)까지 계산된 거리 */
        Integer distanceMeters,
        /** 도착 확인한 참여자 수 (방장 포함) */
        Integer arrivedCount,
        /** 현재 참여 인원 */
        Integer totalMembers,
        /** 남은 참여자 전원 도착 여부 */
        Boolean allArrived
) {}
