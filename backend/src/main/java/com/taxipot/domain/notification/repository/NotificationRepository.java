package com.taxipot.domain.notification.repository;

import com.taxipot.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByMemberId(Long memberId, Pageable pageable);

    Page<Notification> findAllByMemberIdAndIsReadFalse(Long memberId, Pageable pageable);

    long countByMemberIdAndIsReadFalse(Long memberId);

    Optional<Notification> findByIdAndMemberId(Long id, Long memberId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.memberId = :memberId and n.isRead = false")
    int markAllRead(@Param("memberId") Long memberId);
}
