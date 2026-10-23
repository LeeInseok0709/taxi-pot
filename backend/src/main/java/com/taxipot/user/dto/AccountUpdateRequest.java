package com.taxipot.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 명세 3.9.0 스키마 AccountUpdateRequest (Request — @Valid로 검증).
 * 출금 계좌 등록·변경 (3.5.0)
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record AccountUpdateRequest(
        /** 현재 비밀번호 */
        @NotBlank String password,
        @NotBlank @Size(max = 10) String bankCode,
        @NotBlank @Size(max = 20) String accountNumber,
        @NotBlank @Size(max = 50) String accountHolder
) {}
