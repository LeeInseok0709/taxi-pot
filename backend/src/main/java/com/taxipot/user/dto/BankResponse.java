package com.taxipot.user.dto;

/**
 * 명세 3.9.0 스키마 BankResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 출금 은행 (3.5.0)
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record BankResponse(
        String code,
        String name
) {}
