package com.taxipot.domain.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxipot.global.code.NotificationType;

import java.time.OffsetDateTime;

/**
 * NotificationResponse — API 명세서 3.9.0 components.schemas.NotificationResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record NotificationResponse(
        Long id,
        // nullable / 알림 클릭 시 이동할 방
        Long potId,
        NotificationType type,
        String title,
        String content,
        @JsonProperty("isRead") Boolean isRead,
        OffsetDateTime createdAt
) {
}
