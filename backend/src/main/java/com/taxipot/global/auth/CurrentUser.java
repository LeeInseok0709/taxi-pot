package com.taxipot.global.auth;

import com.taxipot.global.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** 웹 계층(리졸버·인터셉터) 전용. 서비스·스케줄러에서는 사용 금지. */
@Component
@RequiredArgsConstructor
public class CurrentUser {

    public static final String DEBUG_HEADER = "X-Debug-User-Id";

    private final AppProperties appProperties;

    public Optional<Long> findId(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long id) {
            return Optional.of(id);
        }
        if (appProperties.auth() != null && appProperties.auth().debugHeaderEnabled()) {
            String header = request.getHeader(DEBUG_HEADER);
            if (header != null && !header.isBlank()) {
                try {
                    return Optional.of(Long.parseLong(header.trim()));
                } catch (NumberFormatException ignored) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }
}
