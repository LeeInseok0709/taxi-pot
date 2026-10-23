package com.taxipot.notification.entity;

import com.taxipot.global.code.NotificationType;
import com.taxipot.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * NOTIFICATION (DB 설계서 13차). 컬럼·타입은 설계서와 1:1 — 필드 추가·삭제·타입 변경 금지.
 * - 다른 테이블 FK는 전부 Long id 필드 (연관관계 매핑 안 함)
 * - DB DEFAULT가 있는 컬럼은 Java 필드 초기값으로도 같은 값을 넣어 둠 (JPA는 NULL을 명시적으로 INSERT하므로)
 * - 상태 변경은 아래 '상태 변경 메서드' 영역에 의미 있는 이름으로 추가 (Setter 금지). 추가는 notification 담당만.
 */
@Getter
@Entity
@Table(name = "NOTIFICATION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 수신 회원번호 */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 택시팟 번호 */
    @Column(name = "pot_id")
    private Long potId;

    /** 알림 유형 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, length = 20)
    private NotificationType type;

    /** 알림 제목 */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /** 알림 내용 */
    @Column(name = "content", nullable = false, length = 300)
    private String content;

    /** 읽음 여부 */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Builder
    private Notification(Long memberId, Long potId, NotificationType type, String title, String content) {
        this.memberId = memberId;
        this.potId = potId;
        this.type = type;
        this.title = title;
        this.content = content;
    }

    // ===== 상태 변경 메서드 (notification 담당이 추가) =====
}
