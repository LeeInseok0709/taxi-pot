package com.taxipot.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 명세 3.9.0 스키마 UserUpdateRequest (Request — @Valid로 검증).
 * 변경할 항목만 전달
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record UserUpdateRequest(
        /** '탈퇴회원_'으로 시작 불가 (RESERVED_NICKNAME); 선택 */
        @Size(max = 30) String nickname,
        /** 선택 */
        @Size(max = 13) @Pattern(regexp = "^01[016789]-?[0-9]{3,4}-?[0-9]{4}$") String phone,
        /** 선택 */
        Boolean agreeMarketing
) {}
