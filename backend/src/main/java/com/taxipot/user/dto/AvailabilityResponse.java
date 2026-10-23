package com.taxipot.user.dto;

/**
 * 명세 3.9.0 스키마 AvailabilityResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record AvailabilityResponse(
        Boolean available,
        /** 사용 불가 사유 (사용 가능하면 null). RESERVED = '탈퇴회원_'으로 시작 → '이후 사용할 수 없는 닉네임입니다' (3.3.0); null 가능 */
        Reason reason
) {

    public enum Reason { DUPLICATE, RESERVED }
}
