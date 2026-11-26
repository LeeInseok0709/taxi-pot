package com.taxipot.global.security;

import java.util.Optional;

/**
 * global → domain 의존을 끊기 위한 인터페이스. 구현: domain.user.repository.AuthUserStatusQueryImpl
 * <p>규칙: global 패키지는 domain 패키지를 import 하지 않는다.
 */
public interface AuthUserStatusQuery {

    Optional<AuthUserStatus> findById(Long userId);
}
