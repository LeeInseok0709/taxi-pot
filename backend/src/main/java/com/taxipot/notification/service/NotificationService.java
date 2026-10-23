package com.taxipot.notification.service;

import com.taxipot.global.common.PageResponse;
import com.taxipot.notification.dto.NotificationResponse;
import com.taxipot.notification.dto.UnreadCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * NotificationController 전용 API 서비스. 담당: 기성
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    /**
     * GET /api/notifications — 내 알림 목록 조회 (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     *
     */
    public PageResponse<NotificationResponse> getNotificationPage(Long userId, Boolean unreadOnly, Pageable pageable) {
        throw new UnsupportedOperationException("TODO(기성): GET /api/notifications");
    }

    /**
     * GET /api/notifications/unread-count — 안 읽은 알림 개수 조회 (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 메인 화면 알림 배지용
     */
    public UnreadCountResponse getUnreadCount(Long userId) {
        throw new UnsupportedOperationException("TODO(기성): GET /api/notifications/unread-count");
    }

    /**
     * PATCH /api/notifications/{notificationId}/read — 알림 읽음 처리 (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     *
     */
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        throw new UnsupportedOperationException("TODO(기성): PATCH /api/notifications/{notificationId}/read");
    }

    /**
     * PATCH /api/notifications/read-all — 알림 전체 읽음 처리 (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     *
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        throw new UnsupportedOperationException("TODO(기성): PATCH /api/notifications/read-all");
    }
}
