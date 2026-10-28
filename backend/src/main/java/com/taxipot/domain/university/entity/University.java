package com.taxipot.domain.university.entity;

import com.taxipot.global.time.Kst;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** UNIVERSITY: 가입 가능 학교 고정 목록 (db/data.sql 로 입력) */
@Getter
@Entity
@Table(name = "UNIVERSITY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class University {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "email_domain", nullable = false, length = 100)
    private String emailDomain;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Kst.now();
        if (isActive == null) {
            isActive = true;
        }
    }
}
