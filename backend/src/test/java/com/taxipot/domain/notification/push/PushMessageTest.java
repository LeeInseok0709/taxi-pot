package com.taxipot.domain.notification.push;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 앱(지연님)과 맞춘 푸시 data 규격 — 키 이름·값 형식이 바뀌면 앱 화면 이동이 깨지므로 테스트로 고정 */
class PushMessageTest {

    @Test
    void 알림_푸시는_type_potId_notificationId를_문자열로_담는다() {
        PushMessage message = PushMessage.notification(7L, "PAYMENT", 10L, "정산 완료", "내역을 확인하세요");

        assertThat(message.data())
                .containsEntry("type", "PAYMENT")
                .containsEntry("potId", "10")
                .containsEntry("notificationId", "7")
                .hasSize(3);
    }

    @Test
    void 방과_무관한_알림은_potId를_넣지_않는다() {
        PushMessage message = PushMessage.notification(7L, "SYSTEM", null, "공지", "점검 안내");

        assertThat(message.data()).containsEntry("type", "SYSTEM").doesNotContainKey("potId").hasSize(2);
    }

    @Test
    void 채팅_푸시는_type_CHAT이고_notificationId가_없다() {
        PushMessage message = PushMessage.chat(10L, "정문 → 터미널", "새 메시지가 있어요");

        assertThat(message.data())
                .containsEntry("type", "CHAT")
                .containsEntry("potId", "10")
                .doesNotContainKey("notificationId");
    }
}
