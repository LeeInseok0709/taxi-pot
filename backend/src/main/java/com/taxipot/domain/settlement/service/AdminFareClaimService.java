package com.taxipot.domain.settlement.service;

import com.taxipot.domain.notification.service.NotificationSender;
import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.entity.TaxipotMember;
import com.taxipot.domain.pot.realtime.PotRealtimePublisher;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.settlement.dto.FareClaimPageResponse;
import com.taxipot.domain.settlement.dto.FareClaimResponse;
import com.taxipot.domain.settlement.dto.FareClaimReviewRequest;
import com.taxipot.domain.settlement.repository.FareClaimRepository;
import com.taxipot.domain.wallet.service.LockedMember;
import com.taxipot.domain.wallet.service.MemberWalletLocker;
import com.taxipot.global.code.FareClaimStatus;
import com.taxipot.global.code.NotificationType;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.Errors;
import com.taxipot.global.file.FileStorage;
import com.taxipot.global.file.StoredFile;
import com.taxipot.global.response.PageMeta;
import com.taxipot.global.response.Paging;
import com.taxipot.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 담당: 지갑·정산
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class AdminFareClaimService {

    /** 운영자 화면에서 영수증을 여는 경로 */
    static final String ADMIN_RECEIPT_PREFIX = "/api/admin/pots/";

    private static final List<PotMemberStatus> BOARDED = List.of(PotMemberStatus.BOARDED);

    private final FareClaimRepository fareClaimRepository;
    private final TaxipotRepository taxipotRepository;
    private final FareClaimResponseFactory fareClaimResponseFactory;
    private final FileStorage fileStorage;
    private final TaxipotMemberRepository taxipotMemberRepository;
    private final MemberWalletLocker memberWalletLocker;
    private final SettlementExecutor settlementExecutor;
    private final NotificationSender notificationSender;
    private final PotRealtimePublisher potRealtimePublisher;
    private final TimeProvider timeProvider;

    /**
     * <b>GET /api/admin/fare-claims</b> — 실요금 청구 목록 (운영자, 3.5.0)
     * <p>
     * fareClaimStatus 미지정 시 PENDING(확인 대기)을 청구 시각 순으로 반환. OVERDUE(24시간 내 미청구)·REJECTED도 지정해서 조회.
     * 각 항목의 receiptUrl(`/api/admin/pots/{potId}/receipt`)로 사진을 열어 금액을 대조하고, fareRatio(실요금 ÷ 예상 요금)가 크면 주의해서 확인
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN
     */
    @Transactional(readOnly = true)
    public FareClaimPageResponse getFareClaims(Long adminId, FareClaimStatus fareClaimStatus, int page, int size) {
        // 상태를 안 고르면 PENDING(확인 대기)
        FareClaimStatus status = fareClaimStatus == null ? FareClaimStatus.PENDING : fareClaimStatus;

        // 청구 시각 순 (오래 기다린 것부터). OVERDUE 는 청구 시각이 없으니 탑승 시작 시각, 마지막으로 방 번호
        Sort sort = Sort.by(Sort.Order.asc("fareClaimedAt"), Sort.Order.asc("startedAt"), Sort.Order.asc("id"));
        Page<Taxipot> result = fareClaimRepository.findAllByFareClaimStatus(status, Paging.of(page, size, sort));

        return new FareClaimPageResponse(
                result.getContent().stream()
                        .map(pot -> fareClaimResponseFactory.of(pot, ADMIN_RECEIPT_PREFIX))
                        .toList(),
                PageMeta.from(result));
    }

    /**
     * <b>PATCH /api/admin/pots/{potId}/fare-claim</b> — 실요금 청구 승인·반려 (운영자, 3.5.0)
     * <p>
     * fare_claim_status = PENDING인 청구만 처리 (아니면 409 FARE_CLAIM_NOT_PENDING). 하나의 트랜잭션.
     * - REJECTED: rejectReason 필수(아니면 400 REJECT_REASON_REQUIRED) → fare_claim_status = REJECTED, fare_reject_reason, fare_reviewed_at = now,
     *   방장에게 PAYMENT 알림(반려 사유·재청구 안내). 돈 이동 없음
     * - APPROVED: 아래 순서로 정산 실행 (기존 즉시 정산 API의 로직을 그대로 옮김)
     * 락 순서: 탑승자 전원 USERS·WALLET(member_id 오름차순) → TAXIPOT
     * 1. TAXIPOT fare_claim_status = APPROVED, fare_reviewed_at = now (actual_fare·receipt_img_path는 청구 때 저장된 값)
     * 2. n = 탑승 인원(BOARDED), fare_share = floor(actual_fare ÷ n), 끝수 = actual_fare − n × fare_share (택시팟 부담)
     * 3. 방장 정산금: 방장 WALLET.balance + actual_fare, POINT_HISTORY HOST_PAYOUT(+actual_fare) — 지갑 상한 검사 안 함
     * 4. 탑승자별(방장 포함, member_id 오름차순):
     *    - 예치 해제: held_balance − held_amount, balance + held_amount, POINT_HISTORY FARE_REFUND(+held_amount)
     *    - 청구액 = fare_share + 1,000. 분담금부터 차감 FARE_PAY(−min(balance, fare_share)), 그다음 수수료 FEE(−min(남은 balance, 1,000)) — 0원 행은 기록 안 함
     *    - uncovered_amount = 청구액 − 실제 차감액 (잔액이 충분하면 0)
     *    - SETTLEMENT: fare_share, fee_amount = 1,000, diff_amount = held_amount − 청구액, uncovered_amount, status = SETTLED, settled_at = now(= 승인 시각)
     *    - uncovered_amount > 0이면 debt_status = UNPAID, debt_amount = uncovered_amount, debt_due_at = now + 3일, PAYMENT 알림('미수금 N원, 3일 안에 납부')
     *    - 예: held 5,500, 해제 전 balance 0, fare_share 8,333 → FARE_REFUND +5,500, FARE_PAY −5,500, FEE 기록 없음, uncovered = 3,833(분담금 2,833 + 수수료 1,000)
     * 5. TAXIPOT status = COMPLETED
     * 6. 탑승자 clean_streak + 1 (report_count ≥ 1일 때만), 3 도달 시 report_count − 1 후 0으로 초기화 (정지 상태는 바꾸지 않음)
     * 7. 탑승자 전원에게 PAYMENT(정산 완료)·RATING_REQUEST(평가 요청) 알림
     * - 방장 지갑 변화 예 (실요금 25,000, 3명, 방장 예치 5,500): +25,000 +5,500 −8,333 −1,000 → 정산 전보다 +21,167 (예치 5,500 포함, 순수 +15,667 = 동승자 분담금 16,666 + 끝수 1 − 수수료 1,000)
     * <p>
     * <b>에러 코드</b>
     * - 400: REJECT_REASON_REQUIRED
     * - 403: FORBIDDEN
     * - 409: FARE_CLAIM_NOT_PENDING / INVALID_POT_STATUS
     */
    @Transactional
    public FareClaimResponse reviewFareClaim(Long adminId, Long potId, FareClaimReviewRequest request) {
        FareClaimStatus decision = request.decision();
        if (decision != FareClaimStatus.APPROVED && decision != FareClaimStatus.REJECTED) {
            throw Errors.invalidInput("decision 은 APPROVED 또는 REJECTED 만 가능합니다.");
        }
        if (decision == FareClaimStatus.REJECTED) {
            return reject(potId, request.rejectReason());
        }
        return approve(potId);
    }

    // =====================================================================
    // 반려 — 돈 이동 없음, 방 행만 잠근다
    // =====================================================================

    private FareClaimResponse reject(Long potId, String rejectReason) {
        if (rejectReason == null || rejectReason.isBlank()) {
            throw new BusinessException(ErrorCode.REJECT_REASON_REQUIRED);
        }
        Taxipot pot = lockPot(potId);
        requirePending(pot);

        String reason = rejectReason.trim();
        pot.rejectFareClaim(reason, timeProvider.now());

        notificationSender.send(pot.getHostId(), potId, NotificationType.PAYMENT, "정산 청구 반려",
                "반려 사유: %s. 결제내역을 확인해 다시 청구해 주세요.".formatted(reason));
        potRealtimePublisher.potUpdated(potId);
        return fareClaimResponseFactory.of(pot, ADMIN_RECEIPT_PREFIX);
    }

    // =====================================================================
    // 승인 → 정산 실행 (명세 1~7단계, 한 트랜잭션)
    // =====================================================================

    private FareClaimResponse approve(Long potId) {
        // 락 순서: 탑승자 전원 USERS·WALLET(member_id 오름차순) → TAXIPOT
        List<Long> boardedIds = boardedMemberIds(potId);
        Map<Long, LockedMember> locked = memberWalletLocker.lockAll(boardedIds);
        Taxipot pot = lockPot(potId);

        requirePending(pot);
        if (pot.getStatus() != PotStatus.RUNNING || boardedIds.isEmpty()
                || !boardedIds.equals(boardedMemberIds(potId))) {       // 잠그는 사이 탑승자가 바뀌지 않았는지
            throw new BusinessException(ErrorCode.INVALID_POT_STATUS);
        }

        // 3~7단계(방장 정산금, 탑승자별 정산·미수금, 방 종료, 알림)는 장애 복구 입력과 같은 로직이라 SettlementExecutor 가 한다
        settlementExecutor.execute(pot, pot.getActualFare(), locked, timeProvider.now());

        potRealtimePublisher.potUpdated(potId);

        return fareClaimResponseFactory.of(pot, ADMIN_RECEIPT_PREFIX);
    }

    // =====================================================================
    // 내부 도우미
    // =====================================================================

    private Taxipot lockPot(Long potId) {
        return taxipotRepository.findByIdForUpdate(potId).orElseThrow(() -> Errors.notFound("택시팟"));
    }

    private static void requirePending(Taxipot pot) {
        if (pot.getFareClaimStatus() != FareClaimStatus.PENDING) {
            throw new BusinessException(ErrorCode.FARE_CLAIM_NOT_PENDING);
        }
    }

    /** 탑승 완료(BOARDED) 회원 번호, 오름차순 */
    private List<Long> boardedMemberIds(Long potId) {
        return taxipotMemberRepository.findAllByPotIdAndStatusInOrderByIdAsc(potId, BOARDED).stream()
                .map(TaxipotMember::getMemberId)
                .sorted()
                .toList();
    }

    /**
     * <b>GET /api/admin/pots/{potId}/receipt</b> — 결제내역(영수증) 사진 조회 (운영자, 3.3.0)
     * <p>
     * 실요금 청구 확인·방장 정산금 지급 근거용 (3.5.0). 영수증이 없으면(장애 복구 일괄 입력 등)·90일 보관 후 파기됐으면 404 (3.6.0)
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN
     * - 404: NOT_FOUND
     */
    @Transactional(readOnly = true)
    public StoredFile getReceipt(Long adminId, Long potId) {
        Taxipot pot = taxipotRepository.findById(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        // 영수증이 없거나(장애 복구 입력 등) 90일 지나 파기됐으면 FileStorage 가 404
        return fileStorage.load(pot.getReceiptImgPath());
    }
}
