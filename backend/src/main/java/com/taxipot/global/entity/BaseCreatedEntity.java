package com.taxipot.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * created_at만 있는 테이블 전용:
 * UNIVERSITY, STUDENT_VERIFICATION, PAYMENT, POINT_HISTORY, SETTLEMENT, RATING, REPORT, NOTIFICATION
 *
 * 상속하지 않는 테이블 (시각 컬럼 이름이 다름 → 각 Entity에 직접 선언):
 * - WALLET: updated_at만 → @LastModifiedDate
 * - TAXIPOT_MEMBER: joined_at, WITHDRAWAL: requested_at, CHAT_MESSAGE: sent_at → @CreatedDate
 *   (필드에 @CreatedDate를 쓰려면 그 Entity에 @EntityListeners(AuditingEntityListener.class) 추가)
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseCreatedEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
