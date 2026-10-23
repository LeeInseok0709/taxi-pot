package com.taxipot.user.entity;

import com.taxipot.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * UNIVERSITY (DB 설계서 13차). 컬럼·타입은 설계서와 1:1 — 필드 추가·삭제·타입 변경 금지.
 * - 다른 테이블 FK는 전부 Long id 필드 (연관관계 매핑 안 함)
 * - DB DEFAULT가 있는 컬럼은 Java 필드 초기값으로도 같은 값을 넣어 둠 (JPA는 NULL을 명시적으로 INSERT하므로)
 * - 상태 변경은 아래 '상태 변경 메서드' 영역에 의미 있는 이름으로 추가 (Setter 금지). 추가는 user 담당만.
 */
@Getter
@Entity
@Table(name = "UNIVERSITY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class University extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 대학교명 */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /** 학교 이메일 도메인(@ 뒤, 소문자) */
    @Column(name = "email_domain", nullable = false, length = 100)
    private String emailDomain;

    /** 가입 허용 여부 */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    private University(String name, String emailDomain) {
        this.name = name;
        this.emailDomain = emailDomain;
    }

    // ===== 상태 변경 메서드 (user 담당이 추가) =====
}
