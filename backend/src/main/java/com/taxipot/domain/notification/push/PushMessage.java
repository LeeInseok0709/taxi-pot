package com.taxipot.domain.notification.push;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 푸시 한 건의 내용. 제목·내용은 알림창에 보이고, data 는 앱이 푸시를 눌렀을 때 화면을 고르는 데 쓴다.
 * <p><b>data 규격 (값은 모두 문자열, 값이 없는 키는 아예 넣지 않음)</b></p>
 * <ul>
 *   <li>{@code type}: NOTIFICATION.type 값(POT·PAYMENT·LATE_WARNING·KICKED·RATING_REQUEST·REPORT_RESULT·SYSTEM)
 *       또는 채팅 새 메시지면 {@value #TYPE_CHAT} — 항상 있음</li>
 *   <li>{@code potId}: 방과 관련된 알림일 때만</li>
 *   <li>{@code notificationId}: 알림 목록(NOTIFICATION)에 저장된 알림일 때만. 채팅 푸시에는 없음</li>
 * </ul>
 * 앱은 알림 화면과 같은 규칙(type·potId)으로 이동하고, notificationId 로 읽음 처리(PATCH /api/notifications/{id}/read)를 할 수 있다.
 */
public record PushMessage(String title, String content, String type, Long potId, Long notificationId) {

    public static final String TYPE_CHAT = "CHAT";

    public static final String DATA_TYPE = "type";
    public static final String DATA_POT_ID = "potId";
    public static final String DATA_NOTIFICATION_ID = "notificationId";

    /** 알림 목록에 저장된 알림의 푸시 */
    public static PushMessage notification(Long notificationId, String type, Long potId, String title, String content) {
        return new PushMessage(title, content, type, potId, notificationId);
    }

    /** 채팅 새 메시지 푸시 (알림 목록에 저장하지 않음) */
    public static PushMessage chat(Long potId, String potTitle, String content) {
        return new PushMessage(potTitle, content, TYPE_CHAT, potId, null);
    }

    public Map<String, String> data() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(DATA_TYPE, type);
        if (potId != null) {
            data.put(DATA_POT_ID, String.valueOf(potId));
        }
        if (notificationId != null) {
            data.put(DATA_NOTIFICATION_ID, String.valueOf(notificationId));
        }
        return data;
    }
}
