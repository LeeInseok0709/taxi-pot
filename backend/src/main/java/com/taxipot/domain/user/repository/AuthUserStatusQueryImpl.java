package com.taxipot.domain.user.repository;

import com.taxipot.global.security.AuthUserStatus;
import com.taxipot.global.security.AuthUserStatusQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthUserStatusQueryImpl implements AuthUserStatusQuery {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthUserStatus> findById(Long userId) {
        return userRepository.findAuthStatus(userId);
    }
}
