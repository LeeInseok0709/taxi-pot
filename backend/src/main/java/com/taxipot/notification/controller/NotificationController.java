package com.taxipot.notification.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.global.common.PageRequests;
import com.taxipot.global.common.PageResponse;
import com.taxipot.notification.dto.NotificationResponse;
import com.taxipot.notification.dto.UnreadCountResponse;
import com.taxipot.notification.service.NotificationService;
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
 * 자동 생성 (명세 3.9.0). 담당: 기성
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 NotificationService에만 작성.
 */
@Tag(name = "Notification")
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** GET /api/notifications — 담당 기성 */
    @Operation(summary = "내 알림 목록 조회")
    @GetMapping(value = "/api/notifications")
    public ResponseEntity<PageResponse<NotificationResponse>> getNotificationPage(
            @LoginUserId Long userId,
            @RequestParam(defaultValue = "false") Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getNotificationPage(userId, unreadOnly, PageRequests.of(page, size)));
    }

    /** GET /api/notifications/unread-count — 담당 기성 */
    @Operation(summary = "안 읽은 알림 개수 조회")
    @GetMapping(value = "/api/notifications/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @LoginUserId Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    /** PATCH /api/notifications/{notificationId}/read — 담당 기성 */
    @Operation(summary = "알림 읽음 처리")
    @PatchMapping(value = "/api/notifications/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @LoginUserId Long userId,
            @PathVariable Long notificationId) {
        notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.noContent().build();
    }

    /** PATCH /api/notifications/read-all — 담당 기성 */
    @Operation(summary = "알림 전체 읽음 처리")
    @PatchMapping(value = "/api/notifications/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @LoginUserId Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}
