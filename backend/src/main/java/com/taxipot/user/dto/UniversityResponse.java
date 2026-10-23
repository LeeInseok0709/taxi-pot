package com.taxipot.user.dto;

/**
 * 명세 3.9.0 스키마 UniversityResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record UniversityResponse(
        Long id,
        /** SignupRequest.university에 그대로 보낼 학교명 */
        String name,
        /** 학교 이메일의 '@' 뒤 도메인 */
        String emailDomain
) {}
