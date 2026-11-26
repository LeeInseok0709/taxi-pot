package com.taxipot.domain.notification.push;

import com.google.api.core.ApiFutures;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FcmPushSenderTest {

    private static final PushMessage MESSAGE =
            PushMessage.notification(7L, "POT", 10L, "택시팟 마감", "모집 인원이 모두 찼어요");

    private final FirebaseMessaging messaging = mock(FirebaseMessaging.class);
    private final FcmPushSender sender = new FcmPushSender(messaging);

    @Test
    void 토큰이_있으면_FCM으로_한_번_보낸다() {
        when(messaging.sendAsync(any(Message.class))).thenReturn(ApiFutures.immediateFuture("msg-1"));

        sender.send("token-abc", MESSAGE);

        verify(messaging, times(1)).sendAsync(any(Message.class));
    }

    @Test
    void 토큰이_비면_보내지_않는다() {
        sender.send(" ", MESSAGE);
        sender.send(null, MESSAGE);

        verify(messaging, never()).sendAsync(any(Message.class));
    }

    @Test
    void 발송_실패는_예외를_던지지_않고_로그만_남긴다() {
        when(messaging.sendAsync(any(Message.class)))
                .thenReturn(ApiFutures.immediateFailedFuture(new IllegalStateException("network")));

        assertThatCode(() -> sender.send("token-abc", MESSAGE)).doesNotThrowAnyException();
    }

    @Test
    void 메시지를_만들_때_예외가_나지_않는다() {
        assertThatCode(() -> FcmPushSender.buildMessage("token-abc",
                PushMessage.notification(1L, "SYSTEM", null, "공지", "점검 안내")))
                .doesNotThrowAnyException();
    }
}
