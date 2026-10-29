package com.taxipot.global.security;

import com.taxipot.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP 인증·구독 검사 (명세 '실시간 통신').
 * <ul>
 *   <li>CONNECT: 헤더 Authorization: Bearer {accessToken} 필수 (HTTP 와 같은 token_version 검사)</li>
 *   <li>SUBSCRIBE: /topic/pots/{potId} 만 허용, 참여자 여부·미수금 검사는 PotSubscriptionGuard</li>
 *   <li>SEND: 앱 → 서버 전송은 쓰지 않는다 (메시지 저장은 REST POST /api/pots/{potId}/messages)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern POT_TOPIC = Pattern.compile("^/topic/pots/(\\d+)$");

    private final TokenAuthenticator tokenAuthenticator;
    private final PotSubscriptionGuard potSubscriptionGuard;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        try {
            StompCommand command = accessor.getCommand();
            if (command == StompCommand.CONNECT) {
                String token = TokenAuthenticator.extractBearer(accessor.getFirstNativeHeader("Authorization"));
                if (token == null) {
                    throw new MessagingException("UNAUTHORIZED");
                }
                AuthUser user = tokenAuthenticator.authenticate(token);
                accessor.setUser(new UsernamePasswordAuthenticationToken(user, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()))));
            } else if (command == StompCommand.SUBSCRIBE) {
                AuthUser user = currentUser(accessor);
                Matcher m = POT_TOPIC.matcher(String.valueOf(accessor.getDestination()));
                if (!m.matches()) {
                    throw new MessagingException("FORBIDDEN");
                }
                potSubscriptionGuard.checkSubscribe(user.id(), Long.valueOf(m.group(1)));
            } else if (command == StompCommand.SEND) {
                throw new MessagingException("FORBIDDEN: 메시지 전송은 REST API 를 사용합니다");
            }
        } catch (BusinessException e) {
            // STOMP ERROR 프레임 message 헤더에 명세 코드가 들어간다
            throw new MessagingException(e.getErrorCode().name());
        }
        return message;
    }

    private static AuthUser currentUser(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken t && t.getPrincipal() instanceof AuthUser u) {
            return u;
        }
        throw new MessagingException("UNAUTHORIZED");
    }
}
