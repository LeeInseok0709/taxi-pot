package com.taxipot.global.security;

import com.taxipot.global.code.UserRole;
import com.taxipot.global.code.UserStatus;

/** 인증 필터가 매 요청 확인하는 회원 상태 (SELECT id, role, status, token_version FROM USERS WHERE id = ?) */
public record AuthUserStatus(Long id, UserRole role, UserStatus status, Integer tokenVersion) {
}
