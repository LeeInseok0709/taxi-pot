package com.taxipot.domain.chat.service;

import com.taxipot.domain.notification.push.PushSender;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 채팅 새 메시지 푸시 단위 테스트 — DB 없이 실행 (트랜잭션 밖이라 바로 발송된다) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatPushNotifierTest {

    private static final Long POT_ID = 3L;
    private static final String POT_TITLE = "정문 → 터미널";

    @Mock UserRepository userRepository;
    @Mock PushSender pushSender;

    ChatPushNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new ChatPushNotifier(userRepository, pushSender);
    }

    @Test
    @DisplayName("푸시 토큰이 있는 사람에게만 '새 메시지가 있어요' 를 보낸다 (제목 = 방 제목)")
    void sendsToTokenHolders() {
        List<User> found = List.of(user("token-a"), user(null), user("  "));
        when(userRepository.findAllById(List.of(8L, 9L, 10L))).thenReturn(found);

        notifier.notifyNewMessage(List.of(8L, 9L, 10L), POT_ID, POT_TITLE);

        verify(pushSender).send("token-a", POT_TITLE, "새 메시지가 있어요", POT_ID);
        verify(pushSender, org.mockito.Mockito.times(1)).send(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("받을 사람이 없으면 회원을 조회하지도, 보내지도 않는다")
    void noReceivers() {
        notifier.notifyNewMessage(List.of(), POT_ID, POT_TITLE);

        verify(userRepository, never()).findAllById(any());
        verify(pushSender, never()).send(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("한 명에게 발송이 실패해도 예외를 던지지 않고 나머지에게 계속 보낸다")
    void failureDoesNotPropagate() {
        List<User> found = List.of(user("token-a"), user("token-b"));
        when(userRepository.findAllById(List.of(8L, 9L))).thenReturn(found);
        doThrow(new IllegalStateException("FCM 오류")).when(pushSender).send("token-a", POT_TITLE, "새 메시지가 있어요", POT_ID);

        assertThatCode(() -> notifier.notifyNewMessage(List.of(8L, 9L), POT_ID, POT_TITLE)).doesNotThrowAnyException();

        verify(pushSender).send("token-b", POT_TITLE, "새 메시지가 있어요", POT_ID);
    }

    private static User user(String fcmToken) {
        User u = mock(User.class);
        when(u.getFcmToken()).thenReturn(fcmToken);
        return u;
    }
}
