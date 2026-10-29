package com.taxipot.domain.notification.service;

import com.taxipot.domain.notification.dto.NotificationPageResponse;
import com.taxipot.domain.notification.dto.NotificationResponse;
import com.taxipot.domain.notification.dto.UnreadCountResponse;
import com.taxipot.domain.notification.entity.Notification;
import com.taxipot.domain.notification.repository.NotificationRepository;
import com.taxipot.global.error.Errors;
import com.taxipot.global.response.PageMeta;
import com.taxipot.global.response.Paging;
import com.taxipot.global.time.Kst;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담당: 공통·인증 (구현 완료). ★ 목록 API 를 만들 때 이 클래스를 참고 예시로 쓴다:
 * Paging.of 로 page·size 검사 → Repository Page 조회 → DTO 변환 → XxxPageResponse(content, PageMeta.from(page)).
 * 알림 '보내기'는 NotificationSender.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /** GET /api/notifications — 최신순 */
    @Transactional(readOnly = true)
    public NotificationPageResponse getMyNotifications(Long userId, boolean unreadOnly, int page, int size) {
        Pageable pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<Notification> result = unreadOnly
                ? notificationRepository.findAllByMemberIdAndIsReadFalse(userId, pageable)
                : notificationRepository.findAllByMemberId(userId, pageable);
        return new NotificationPageResponse(result.map(NotificationService::toResponse).getContent(), PageMeta.from(result));
    }

    /** GET /api/notifications/unread-count */
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long userId) {
        return new UnreadCountResponse((int) notificationRepository.countByMemberIdAndIsReadFalse(userId));
    }

    /** PATCH /api/notifications/{notificationId}/read — 남의 알림은 404 */
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        notificationRepository.findByIdAndMemberId(notificationId, userId)
                .orElseThrow(() -> Errors.notFound("알림"))
                .markRead();
    }

    /** PATCH /api/notifications/read-all */
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }

    public static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getPotId(), n.getType(), n.getTitle(), n.getContent(),
                n.getIsRead(), Kst.toOffset(n.getCreatedAt()));
    }
}
