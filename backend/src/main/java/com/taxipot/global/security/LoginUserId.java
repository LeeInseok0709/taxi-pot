package com.taxipot.global.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 로그인 회원 id 주입. 로그인 회원은 이 방법 하나로만 받는다.
 * <pre>
 * &#64;GetMapping("/api/users/me")
 * public MyProfileResponse me(@LoginUserId Long userId) { ... }
 * </pre>
 * JWT 가 없거나 무효면 401. local 프로필에서는 Swagger 의 X-Debug-User-Id 칸으로도 지정 가능.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUserId {
}
