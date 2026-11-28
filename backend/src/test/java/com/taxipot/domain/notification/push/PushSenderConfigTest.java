package com.taxipot.domain.notification.push;

import com.taxipot.global.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PushSenderConfigTest {

    private final PushSenderConfig config = new PushSenderConfig();

    @Test
    void 꺼져_있으면_로그_발송기() {
        PushSender sender = config.pushSender(props(new AppProperties.Push(false, null)));

        assertThat(sender).isInstanceOf(LogPushSender.class);
    }

    @Test
    void 켜졌는데_키_경로가_비면_서버_시작_실패() {
        assertThatThrownBy(() -> config.pushSender(props(new AppProperties.Push(true, " "))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("credentials-path");
    }

    @Test
    void 켜졌는데_키_파일이_없으면_서버_시작_실패(@TempDir Path dir) {
        String missing = dir.resolve("없는-키.json").toString();

        assertThatThrownBy(() -> config.pushSender(props(new AppProperties.Push(true, missing))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("키 파일이 없습니다");
    }

    private static AppProperties props(AppProperties.Push push) {
        AppProperties props = mock(AppProperties.class);
        when(props.push()).thenReturn(push);
        return props;
    }
}
