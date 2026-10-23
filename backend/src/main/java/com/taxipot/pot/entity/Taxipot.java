package com.taxipot.pot.entity;

import com.taxipot.global.code.FareClaimStatus;
import com.taxipot.global.code.GenderOption;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.entity.BaseTimeEntity;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TAXIPOT (DB 설계서 13차). 컬럼·타입은 설계서와 1:1 — 필드 추가·삭제·타입 변경 금지.
 * - 다른 테이블 FK는 전부 Long id 필드 (연관관계 매핑 안 함)
 * - DB DEFAULT가 있는 컬럼은 Java 필드 초기값으로도 같은 값을 넣어 둠 (JPA는 NULL을 명시적으로 INSERT하므로)
 * - 상태 변경은 아래 '상태 변경 메서드' 영역에 의미 있는 이름으로 추가 (Setter 금지). 추가는 pot 담당만.
 */
@Getter
@Entity
@Table(name = "TAXIPOT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Taxipot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 방장 회원번호 */
    @Column(name = "host_id", nullable = false)
    private Long hostId;

    /** 방 제목 */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /** 출발지 명칭 */
    @Column(name = "departure_name", nullable = false, length = 100)
    private String departureName;

    /** 출발지 위도 */
    @Column(name = "departure_lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal departureLat;

    /** 출발지 경도 */
    @Column(name = "departure_lng", nullable = false, precision = 11, scale = 7)
    private BigDecimal departureLng;

    /** 도착지 명칭 */
    @Column(name = "destination_name", nullable = false, length = 100)
    private String destinationName;

    /** 도착지 위도 */
    @Column(name = "destination_lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal destinationLat;

    /** 도착지 경도 */
    @Column(name = "destination_lng", nullable = false, precision = 11, scale = 7)
    private BigDecimal destinationLng;

    /** 출발 예정 시각 */
    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    /** 집결 기한(= 참여 마감) */
    @Column(name = "arrival_deadline", nullable = false)
    private LocalDateTime arrivalDeadline;

    /** 모집 인원 */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "max_members", nullable = false)
    private Integer maxMembers = 4;

    /** 현재 참여 인원 */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "current_members", nullable = false)
    private Integer currentMembers = 1;

    /** 성별 제한 옵션 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "gender_option", nullable = false, length = 10)
    private GenderOption genderOption = GenderOption.ALL;

    /** 예상 총 택시 요금 */
    @Column(name = "expected_fare", nullable = false)
    private Integer expectedFare;

    /** 실제 택시 요금 */
    @Column(name = "actual_fare")
    private Integer actualFare;

    /** 결제내역(영수증) 사진 파일 경로(서버 로컬) */
    @Column(name = "receipt_img_path", length = 255)
    private String receiptImgPath;

    /** 탑승 시작 일시 */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** 출발 확인 알림 일시 */
    @Column(name = "start_reminded_at")
    private LocalDateTime startRemindedAt;

    /** 실요금 청구 상태 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "fare_claim_status", nullable = false, length = 10)
    private FareClaimStatus fareClaimStatus = FareClaimStatus.NONE;

    /** 실요금 청구 일시 */
    @Column(name = "fare_claimed_at")
    private LocalDateTime fareClaimedAt;

    /** 청구 확인 일시 */
    @Column(name = "fare_reviewed_at")
    private LocalDateTime fareReviewedAt;

    /** 청구 반려 사유 */
    @Column(name = "fare_reject_reason", length = 255)
    private String fareRejectReason;

    /** 방 상태 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private PotStatus status = PotStatus.RECRUITING;

    /** 취소 발생 여부 */
    @Column(name = "has_canceled", nullable = false)
    private Boolean hasCanceled = false;

    /** 오픈채팅 링크 */
    @Column(name = "open_chat_url", length = 500)
    private String openChatUrl;

    @Builder
    private Taxipot(Long hostId, String title, String departureName, BigDecimal departureLat, BigDecimal departureLng, String destinationName, BigDecimal destinationLat, BigDecimal destinationLng, LocalDateTime departureTime, LocalDateTime arrivalDeadline, Integer maxMembers, GenderOption genderOption, Integer expectedFare, String openChatUrl) {
        this.hostId = hostId;
        this.title = title;
        this.departureName = departureName;
        this.departureLat = departureLat;
        this.departureLng = departureLng;
        this.destinationName = destinationName;
        this.destinationLat = destinationLat;
        this.destinationLng = destinationLng;
        this.departureTime = departureTime;
        this.arrivalDeadline = arrivalDeadline;
        if (maxMembers != null) {
            this.maxMembers = maxMembers;
        }
        if (genderOption != null) {
            this.genderOption = genderOption;
        }
        this.expectedFare = expectedFare;
        this.openChatUrl = openChatUrl;
    }

    // ===== 상태 변경 메서드 (pot 담당이 추가) =====
}
