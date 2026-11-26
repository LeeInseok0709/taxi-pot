package com.taxipot.domain.notification.controller;

import com.taxipot.domain.notification.dto.NotificationPageResponse;
import com.taxipot.domain.notification.dto.UnreadCountResponse;
import com.taxipot.domain.notification.service.NotificationService;
import com.taxipot.global.security.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 명세 tag: Notification — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:
 * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.
 */
@Tag(name = "Notification")
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "내 알림 목록 조회")
    @GetMapping("/api/notifications")
    public NotificationPageResponse getMyNotifications(@LoginUserId Long userId, @RequestParam(defaultValue = "false") boolean unreadOnly, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return notificationService.getMyNotifications(userId, unreadOnly, page, size);
    }

    @Operation(summary = "안 읽은 알림 개수 조회")
    @GetMapping("/api/notifications/unread-count")
    public UnreadCountResponse getUnreadCount(@LoginUserId Long userId) {
        return notificationService.getUnreadCount(userId);
    }

    @Operation(summary = "알림 읽음 처리")
    @PatchMapping("/api/notifications/{notificationId}/read")
    public ResponseEntity<Void> markRead(@LoginUserId Long userId, @PathVariable Long notificationId) {
        notificationService.markRead(userId, notificationId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "알림 전체 읽음 처리")
    @PatchMapping("/api/notifications/read-all")
    public ResponseEntity<Void> markAllRead(@LoginUserId Long userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }
}
