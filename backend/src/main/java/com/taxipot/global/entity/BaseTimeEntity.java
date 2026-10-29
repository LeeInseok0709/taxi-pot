package com.taxipot.global.entity;

import com.taxipot.global.time.Kst;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * created_at + updated_at 이 둘 다 있는 테이블 전용: USERS, TAXIPOT 두 개뿐.
 * <p>나머지 테이블은 시각 컬럼 이름이 달라서(joined_at, sent_at, requested_at, updated_at 만 있음 등)
 * 각 Entity 에서 직접 @PrePersist 로 채운다. 이 클래스를 다른 Entity 에 상속하면 validate 에서 서버가 안 뜬다.
 */
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = Kst.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Kst.now();
    }
}
