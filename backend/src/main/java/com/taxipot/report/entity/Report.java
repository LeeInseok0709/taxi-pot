package com.taxipot.report.entity;

import com.taxipot.global.code.ReportStatus;
import com.taxipot.global.code.ReportType;
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
import java.time.LocalDateTime;

/**
 * REPORT (DB 설계서 13차). 컬럼·타입은 설계서와 1:1 — 필드 추가·삭제·타입 변경 금지.
 * - 다른 테이블 FK는 전부 Long id 필드 (연관관계 매핑 안 함)
 * - DB DEFAULT가 있는 컬럼은 Java 필드 초기값으로도 같은 값을 넣어 둠 (JPA는 NULL을 명시적으로 INSERT하므로)
 * - 상태 변경은 아래 '상태 변경 메서드' 영역에 의미 있는 이름으로 추가 (Setter 금지). 추가는 report 담당만.
 */
@Getter
@Entity
@Table(name = "REPORT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 신고자 회원번호 */
    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    /** 피신고자 회원번호 */
    @Column(name = "reported_id", nullable = false)
    private Long reportedId;

    /** 택시팟 번호 */
    @Column(name = "pot_id", nullable = false)
    private Long potId;

    /** 신고 유형 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, length = 10)
    private ReportType type;

    /** 신고 사유 */
    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    /** 처리 상태 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private ReportStatus status = ReportStatus.PENDING;

    /** 처리 일시 */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Builder
    private Report(Long reporterId, Long reportedId, Long potId, ReportType type, String reason) {
        this.reporterId = reporterId;
        this.reportedId = reportedId;
        this.potId = potId;
        this.type = type;
        this.reason = reason;
    }

    // ===== 상태 변경 메서드 (report 담당이 추가) =====
}
