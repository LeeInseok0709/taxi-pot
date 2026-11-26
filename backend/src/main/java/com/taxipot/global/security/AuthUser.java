package com.taxipot.global.security;

import com.taxipot.global.code.UserRole;

/** 인증된 요청의 주체 (SecurityContext principal). 컨트롤러에서는 {@link LoginUserId} 로 id 만 받는다 */
public record AuthUser(Long id, UserRole role) {
}
