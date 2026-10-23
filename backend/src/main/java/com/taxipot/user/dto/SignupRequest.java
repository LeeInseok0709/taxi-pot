package com.taxipot.user.dto;

import com.taxipot.global.code.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 명세 3.9.0 스키마 SignupRequest (Request — @Valid로 검증).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record SignupRequest(
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank @Size(max = 50) String name,
        /** '탈퇴회원_'으로 시작 불가 (RESERVED_NICKNAME) */
        @NotBlank @Size(max = 30) String nickname,
        /** 하이픈 있어도 됨. 서버가 숫자만 남겨 암호화 저장 */
        @NotBlank @Size(max = 13) @Pattern(regexp = "^01[016789]-?[0-9]{3,4}-?[0-9]{4}$") String phone,
        @NotNull Gender gender,
        /** GET /api/universities 응답의 name. 이메일 도메인이 이 학교 emailDomain과 일치해야 함 */
        @NotBlank @Size(max = 50) String university,
        @NotBlank @Size(max = 20) String studentNo,
        /** 출금 은행 코드 (`GET /api/banks`, 3.5.0) */
        @NotBlank @Size(max = 10) String bankCode,
        /** 본인 명의 출금 계좌번호 (하이픈 허용, 서버가 숫자만 남겨 암호화 저장, 3.5.0) */
        @NotBlank @Size(max = 20) String accountNumber,
        /** 예금주 (name과 같아야 함, 3.5.0) */
        @NotBlank @Size(max = 50) String accountHolder,
        /** 서비스 이용·개인정보·위치정보 필수 약관 일괄 동의 (true만 허용) */
        @NotNull Boolean agreeRequired,
        /** 선택 */
        Boolean agreeMarketing
) {}
