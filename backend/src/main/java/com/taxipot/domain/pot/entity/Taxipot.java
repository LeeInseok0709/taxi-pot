package com.taxipot.domain.pot.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TAXIPOT. 좌표는 DECIMAL → BigDecimal (Double 로 매핑하면 validate 실패).
 * <p>상태 변경 메서드는 DB CHECK 제약을 지키도록 짝지어 바꾼다. 상태 검증(예외)은 서비스에서.
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

    @Column(name = "host_id", nullable = false)
    private Long hostId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "departure_name", nullable = false, length = 100)
    private String departureName;

    @Column(name = "departure_lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal departureLat;

    @Column(name = "departure_lng", nullable = false, precision = 11, scale = 7)
    private BigDecimal departureLng;

    @Column(name = "destination_name", nullable = false, length = 100)
    private String destinationName;

    @Column(name = "destination_lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal destinationLat;

    @Column(name = "destination_lng", nullable = false, precision = 11, scale = 7)
    private BigDecimal destinationLng;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    /** 집결 기한 = 참여 마감 = 출발 5분 전 */
    @Column(name = "arrival_deadline", nullable = false)
    private LocalDateTime arrivalDeadline;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "max_members", nullable = false)
    private Integer maxMembers;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "current_members", nullable = false)
    private Integer currentMembers;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "gender_option", nullable = false, length = 10)
    private GenderOption genderOption;

    @Column(name = "expected_fare", nullable = false)
    private Integer expectedFare;

    @Column(name = "actual_fare")
    private Integer actualFare;

    @Column(name = "receipt_img_path", length = 255)
    private String receiptImgPath;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "start_reminded_at")
    private LocalDateTime startRemindedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "fare_claim_status", nullable = false, length = 10)
    private FareClaimStatus fareClaimStatus;

    @Column(name = "fare_claimed_at")
    private LocalDateTime fareClaimedAt;

    @Column(name = "fare_reviewed_at")
    private LocalDateTime fareReviewedAt;

    @Column(name = "fare_reject_reason", length = 255)
    private String fareRejectReason;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private PotStatus status;

    @Column(name = "has_canceled", nullable = false)
    private Boolean hasCanceled;

    @Column(name = "open_chat_url", length = 500)
    private String openChatUrl;

    public static Taxipot create(Long hostId, String title,
                                 String departureName, BigDecimal departureLat, BigDecimal departureLng,
                                 String destinationName, BigDecimal destinationLat, BigDecimal destinationLng,
                                 LocalDateTime departureTime, LocalDateTime arrivalDeadline,
                                 int maxMembers, GenderOption genderOption, int expectedFare, String openChatUrl) {
        Taxipot p = new Taxipot();
        p.hostId = hostId;
        p.title = title;
        p.departureName = departureName;
        p.departureLat = departureLat;
        p.departureLng = departureLng;
        p.destinationName = destinationName;
        p.destinationLat = destinationLat;
        p.destinationLng = destinationLng;
        p.departureTime = departureTime;
        p.arrivalDeadline = arrivalDeadline;
        p.maxMembers = maxMembers;
        p.currentMembers = 1;
        p.genderOption = genderOption == null ? GenderOption.ALL : genderOption;
        p.expectedFare = expectedFare;
        p.fareClaimStatus = FareClaimStatus.NONE;
        p.status = PotStatus.RECRUITING;
        p.hasCanceled = false;
        p.openChatUrl = openChatUrl;
        return p;
    }

    public boolean isHost(Long userId) {
        return hostId.equals(userId);
    }

    public boolean isFull() {
        return currentMembers >= maxMembers;
    }

    /** 참여 마감 시각 (= 집결 기한) */
    public LocalDateTime getJoinDeadline() {
        return arrivalDeadline;
    }

    public void updateInfo(String title, String openChatUrl) {
        if (title != null) {
            this.title = title;
        }
        if (openChatUrl != null) {
            this.openChatUrl = openChatUrl;
        }
    }

    /** 인원 + 1 (참여·입장 요청 수락). 다 차면 CLOSED → 호출 측에서 isFull() 로 알림 판단 */
    public void addMember() {
        this.currentMembers++;
        if (isFull() && status == PotStatus.RECRUITING) {
            this.status = PotStatus.CLOSED;
        }
    }

    /** 인원 − 1 (참여 취소·내보내기·노쇼). has_canceled = 1, CLOSED 였다면 RECRUITING */
    public void removeMember() {
        this.currentMembers--;
        this.hasCanceled = true;
        if (status == PotStatus.CLOSED) {
            this.status = PotStatus.RECRUITING;
        }
    }

    /** 방 전체 취소 (has_canceled 는 바꾸지 않음) */
    public void cancel() {
        this.status = PotStatus.CANCELED;
    }

    public void start(LocalDateTime now) {
        this.status = PotStatus.RUNNING;
        this.startedAt = now;
    }

    public void markStartReminded(LocalDateTime now) {
        this.startRemindedAt = now;
    }

    /** 실요금 청구 (재청구 시 덮어씀, 반려 사유 초기화) */
    public void claimFare(int actualFare, String receiptImgPath, LocalDateTime now) {
        this.actualFare = actualFare;
        this.receiptImgPath = receiptImgPath;
        this.fareClaimStatus = FareClaimStatus.PENDING;
        this.fareClaimedAt = now;
        this.fareRejectReason = null;
    }

    /** 운영자 승인 → 정산 실행 후 COMPLETED (actualFare 는 운영자 승인 금액) */
    public void approveFareClaim(int approvedFare, LocalDateTime now) {
        this.actualFare = approvedFare;
        this.fareClaimStatus = FareClaimStatus.APPROVED;
        this.fareReviewedAt = now;
        this.status = PotStatus.COMPLETED;
    }

    public void rejectFareClaim(String reason, LocalDateTime now) {
        this.fareClaimStatus = FareClaimStatus.REJECTED;
        this.fareRejectReason = reason;
        this.fareReviewedAt = now;
    }

    public void markFareClaimOverdue() {
        this.fareClaimStatus = FareClaimStatus.OVERDUE;
    }

    /** 스케줄러 10번: 영수증 파일 삭제 후 */
    public void clearReceipt() {
        this.receiptImgPath = null;
    }
}
