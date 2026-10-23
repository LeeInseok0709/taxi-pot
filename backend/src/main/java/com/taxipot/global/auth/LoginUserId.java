package com.taxipot.global.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러에서 로그인한 회원 id를 받는 유일한 방법. (자동 생성된 컨트롤러에 이미 다 붙어 있음)
 * - JWT 완성 전·후 모두 코드가 같다
 * - 서비스 메서드는 SecurityContext를 직접 읽지 말고 userId를 파라미터로 받는다
 *   (스케줄러에서 같은 로직을 호출하면 SecurityContext가 비어 있어서 터진다)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUserId {
}
