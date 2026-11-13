package com.taxipot.domain.pot.service;

import com.taxipot.domain.notification.service.NotificationSender;
import com.taxipot.domain.pot.dto.ArrivalRequest;
import com.taxipot.domain.pot.dto.ArrivalResponse;
import com.taxipot.domain.pot.dto.JoinRequest;
import com.taxipot.domain.pot.dto.JoinResponse;
import com.taxipot.domain.pot.dto.PotMemberResponse;
import com.taxipot.domain.pot.dto.SeatChangeRequest;
import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.entity.TaxipotMember;
import com.taxipot.domain.pot.realtime.PotRealtimePublisher;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.domain.wallet.service.LockedMember;
import com.taxipot.domain.wallet.service.MemberWalletLocker;
import com.taxipot.global.code.NotificationType;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.Errors;
import com.taxipot.global.time.Kst;
import com.taxipot.global.time.TimeProvider;
import com.taxipot.global.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 담당: 택시팟
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class PotMemberService {

    // ---- 도착 확인·내보내기(담당: 인석)에서 쓰는 의존성. 참여·취소·좌석 담당도 같은 필드를 그대로 쓰면 된다.
    private final TaxipotRepository taxipotRepository;
    private final TaxipotMemberRepository taxipotMemberRepository;
    private final MemberWalletLocker memberWalletLocker;
    private final PotExitHandler potExitHandler;
    private final PotPolicy potPolicy;
    private final NotificationSender notificationSender;
    private final PotRealtimePublisher potRealtimePublisher;
    private final TimeProvider timeProvider;

    // ---- 참여·취소·좌석(담당: 서연)에서 추가로 쓰는 의존성
    private final UserRepository userRepository;
    private final PotEligibilityValidator eligibilityValidator;
    private final PotDeposit potDeposit;
    private final PotMapper potMapper;

    /** 도착 확인이 의미 있는 방 상태 (탑승 전) */
    private static final List<PotStatus> BEFORE_START = List.of(PotStatus.RECRUITING, PotStatus.CLOSED);

    /**
     * <b>POST /api/pots/{potId}/members</b> — 택시팟 참여
     * <p>
     * 하나의 트랜잭션으로 처리. 락 순서: USERS(본인) → WALLET(본인) → TAXIPOT(대상 방), 모두 `SELECT ... FOR UPDATE`
     * - USERS 락으로 같은 회원의 서로 다른 방 동시 참여를 막고, TAXIPOT 락으로 동시 신청 정원 초과를 막는다
     * - 검증: 공통 이용 조건, 방 상태 RECRUITING, now &lt; 참여 마감(arrival_deadline), current_members &lt; max_members, 성별 제한 일치, 진행 중인 다른 방 없음, 잔액 ≥ 예치액
     *   - 참여 마감이 지났으면 409 JOIN_DEADLINE_PASSED (detail.joinDeadline) → 앱은 입장 요청(`POST /api/pots/{potId}/join-requests`)으로 안내
     * - 처리: TAXIPOT_MEMBER 등록(GUEST), current_members + 1, SETTLEMENT 생성(HELD), WALLET 예치, POINT_HISTORY FARE_HOLD(−예치액) 1행
     * - 취소 이력이 있으면 INSERT 대신 기존 TAXIPOT_MEMBER·SETTLEMENT 행을 UPDATE — UNIQUE(pot_id, member_id)
     *   - TAXIPOT_MEMBER: status = JOINED, joined_at = now, arrived_at·canceled_at = NULL, preferred_seat 갱신
     *   - SETTLEMENT: status = HELD, held_amount = 새 예치액, fare_share·fee_amount·diff_amount·settled_at = NULL, uncovered_amount = 0, debt_status = NONE
     * - 내보내진(KICKED)·노쇼(NO_SHOW)·입장 거절(REJECTED) 처리된 방에는 재참여 불가
     * - 인원이 다 차면 status = CLOSED, 참여자 전원에게 POT 알림
     * <p>
     * <b>에러 코드</b>
     * - 403: NOT_VERIFIED / SUSPENDED_USER / GENDER_RESTRICTED / KICKED_FROM_POT(KICKED·NO_SHOW) / JOIN_REQUEST_REJECTED
     * - 404: NOT_FOUND
     * - 409: POT_FULL / INVALID_POT_STATUS / ALREADY_JOINED / ACTIVE_POT_EXISTS / INSUFFICIENT_BALANCE / JOIN_DEADLINE_PASSED / UNPAID_DEBT_EXISTS
     */
    @Transactional
    public JoinResponse join(Long userId, Long potId, JoinRequest request) {
        // 1. USERS 2. WALLET 3. TAXIPOT — 같은 회원의 다른 방 동시 참여, 같은 방 동시 신청 정원 초과를 막는다
        LockedMember me = memberWalletLocker.lock(userId);
        Taxipot pot = taxipotRepository.findByIdForUpdate(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        LocalDateTime now = timeProvider.now();

        // 이 방과의 이력 (UNIQUE(pot_id, member_id) → 행은 하나)
        TaxipotMember existing = taxipotMemberRepository.findByPotIdAndMemberId(potId, userId).orElse(null);
        if (existing != null) {
            rejectByHistory(existing.getStatus());
        }

        // 방 상태: CLOSED(정원 마감)는 아래 검증기에서 POT_FULL 로 알려 준다
        if (!BEFORE_START.contains(pot.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_POT_STATUS);
        }
        // 참여 마감(= 집결 기한) — 지났으면 앱이 입장 요청으로 안내
        if (!now.isBefore(pot.getJoinDeadline())) {
            throw Errors.joinDeadlinePassed(pot.getJoinDeadline());
        }

        // 공통 이용 조건: 인증 → 정지 → 미수금 → 성별 → 진행 중인 다른 방 → 정원 → 잔액
        int holdAmount = potPolicy.holdAmount(pot.getExpectedFare(), pot.getMaxMembers());
        eligibilityValidator.validate(me.user(), me.wallet(), pot, null, holdAmount);

        TaxipotMember member;
        if (existing == null) {
            member = taxipotMemberRepository.save(TaxipotMember.guest(potId, userId, request.preferredSeat(), now));
        } else {
            member = existing;   // CANCELED 이력 → 같은 행을 다시 JOINED
            member.rejoin(request.preferredSeat(), now);
        }
        potDeposit.hold(me.wallet(), potId, holdAmount);
        pot.addMember();

        if (pot.getStatus() == PotStatus.CLOSED) {
            notifyFull(pot);
        }
        potRealtimePublisher.potUpdated(potId);
        return new JoinResponse(potId, potMapper.toMember(member, me.user()), holdAmount,
                me.wallet().getBalance(), pot.getStatus());
    }

    /**
     * <b>DELETE /api/pots/{potId}/members/me</b> — 택시팟 참여 취소 (동승자 전용)
     * <p>
     * 방장은 취소 대신 방 취소(DELETE /api/pots/{potId}) 사용. Body 없음.
     * - 출발 10분 전부터 취소 불가 (서버 검증)
     * - TAXIPOT_MEMBER status = CANCELED, canceled_at = now
     * - current_members − 1, has_canceled = 1, CLOSED였다면 RECRUITING으로 변경
     * - 예치금 전액 환불 (SETTLEMENT → REFUNDED, POINT_HISTORY FARE_REFUND). 수수료는 기록·차감하지 않음
     * - 입장 요청 대기 중(REQUESTED)인 사람은 이 API가 아니라 `DELETE /api/pots/{potId}/join-requests/me`로 철회
     * - 방장에게 POT 알림
     * <p>
     * <b>에러 코드</b>
     * - 403: HOST_CANNOT_LEAVE / NOT_POT_MEMBER
     * - 409: CANCEL_TIME_EXPIRED
     */
    @Transactional
    public void leave(Long userId, Long potId) {
        LockedMember me = memberWalletLocker.lock(userId);
        Taxipot pot = taxipotRepository.findByIdForUpdate(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        if (pot.isHost(userId)) {
            throw new BusinessException(ErrorCode.HOST_CANNOT_LEAVE);
        }
        TaxipotMember member = taxipotMemberRepository.findByPotIdAndMemberId(potId, userId)
                .filter(TaxipotMember::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_POT_MEMBER));
        // 탑승 시작(BOARDED) 이후·방 종료 후에는 취소 대상이 아니다
        if (member.getStatus() != PotMemberStatus.JOINED || !BEFORE_START.contains(pot.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_POT_STATUS);
        }
        LocalDateTime now = timeProvider.now();
        LocalDateTime cancelDeadline = potPolicy.cancelDeadline(pot.getDepartureTime());
        if (!now.isBefore(cancelDeadline)) {
            throw Errors.cancelTimeExpired(cancelDeadline, potPolicy.cancelDeadlineMinutes());
        }

        member.cancel(now);
        pot.removeMember();                                  // current − 1, has_canceled = 1, CLOSED → RECRUITING
        potExitHandler.refundHold(me.wallet(), potId);       // 전액 환불(HELD 일 때만), 수수료 없음

        notificationSender.send(pot.getHostId(), potId, NotificationType.POT, "참여 취소",
                "%s님이 '%s' 방 참여를 취소했어요.".formatted(me.user().getNickname(), pot.getTitle()));
        potRealtimePublisher.potUpdated(potId);
    }

    /**
     * <b>PATCH /api/pots/{potId}/members/me/seat</b> — 희망 좌석 변경
     * <p>
     * RECRUITING·CLOSED 상태에서만 변경 가능
     * <p>
     * <b>에러 코드</b>
     * - 403: NOT_POT_MEMBER
     * - 409: INVALID_POT_STATUS
     */
    @Transactional
    public PotMemberResponse changeSeat(Long userId, Long potId, SeatChangeRequest request) {
        Taxipot pot = taxipotRepository.findById(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        TaxipotMember member = taxipotMemberRepository.findByPotIdAndMemberId(potId, userId)
                .filter(TaxipotMember::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_POT_MEMBER));
        if (!BEFORE_START.contains(pot.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_POT_STATUS);
        }

        member.changeSeat(request.preferredSeat());
        potRealtimePublisher.potUpdated(potId);
        User me = userRepository.findById(userId).orElse(null);
        return potMapper.toMember(member, me);
    }

    /**
     * <b>POST /api/pots/{potId}/members/me/arrival</b> — 도착 확인 (GPS 검증)
     * <p>
     * 현재 GPS 좌표와 출발지 좌표의 거리(하버사인 공식)가 도착 인정 반경(서버 설정값, 50m — 16일 회의) 이내일 때만 arrived_at = now 기록.
     * 앱은 반경 안에서만 버튼을 활성화하되 최종 판정은 서버가 한다.
     * - 집결 기한 이후에는 호출 불가 (자동 퇴장 대상)
     * - 도착 확인한 참여자는 방장이 내보낼 수 없음
     * - 전원 도착 시 참여자 전원에게 POT 알림, STOMP `/topic/pots/{potId}`로 `POT_UPDATED` 전송
     * - 시연 주의: 50m는 실내·고층 건물 옆에서 GPS 오차보다 작을 수 있음. 시연은 실외에서 하거나 Android 개발자 옵션의 '모의 위치 앱'으로 좌표를 고정하고,
     *   앱은 위치 요청에 `Priority.PRIORITY_HIGH_ACCURACY`를 사용
     * <p>
     * <b>에러 코드</b>
     * - 400: OUT_OF_ARRIVAL_RANGE (detail에 현재 거리·인정 반경)
     * - 403: NOT_POT_MEMBER
     * - 409: ARRIVAL_DEADLINE_PASSED / ALREADY_ARRIVED
     */
    @Transactional
    public ArrivalResponse confirmArrival(Long userId, Long potId, ArrivalRequest request) {
        LocalDateTime now = timeProvider.now();

        // ---------------------------------------------------------------- 1. 락 (USERS → WALLET → TAXIPOT)
        // 도착 확인은 지갑을 건드리지 않지만, 스케줄러 1(미도착 자동 퇴장)과 '같은 사람'을 두고 경쟁한다.
        //   예) 집결 기한 직전 59초에 버튼을 누름 ↔ 같은 순간 스케줄러가 이 사람을 노쇼 처리
        // 두 쪽이 같은 순서로 잠가야 서로 기다릴 뿐 데드락이 안 난다. 그래서 규칙(8장)대로 회원부터 잠근다.
        memberWalletLocker.lock(userId);
        Taxipot pot = taxipotRepository.findByIdForUpdate(potId)
                .orElseThrow(() -> Errors.notFound("택시팟"));

        // ---------------------------------------------------------------- 2. 검증 (락을 잡은 '뒤'에 해야 최신 상태로 판단)
        // 2-1. 이 방의 '남아 있는' 참여자인가. 취소·내보내기·노쇼·입장 요청 대기는 모두 참여자가 아님
        TaxipotMember me = taxipotMemberRepository.findByPotIdAndMemberId(potId, userId)
                .filter(m -> m.getStatus() == PotMemberStatus.JOINED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_POT_MEMBER));

        // 2-2. 탑승 전 방인가. (명세 에러 목록엔 없지만, 방이 취소·운행 중인데 도착을 기록하면 안 되므로 409 INVALID_POT_STATUS)
        if (!BEFORE_START.contains(pot.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_POT_STATUS);
        }

        // 2-3. 이미 도착 확인함 (입장 요청 수락자는 수락 시각으로 이미 arrived_at 이 들어 있다)
        if (me.hasArrived()) {
            throw new BusinessException(ErrorCode.ALREADY_ARRIVED);
        }

        // 2-4. 집결 기한이 지났으면 불가 (now >= arrival_deadline). 기한 이후는 스케줄러 1의 노쇼 처리 대상
        //      '기한 정각'도 지난 것으로 본다 → 스케줄러 1의 대상 조건(arrival_deadline <= now)과 경계를 맞춤
        if (!now.isBefore(pot.getArrivalDeadline())) {
            throw Errors.arrivalDeadlinePassed(pot.getArrivalDeadline());
        }

        // 2-5. GPS 거리 (하버사인). 앱도 반경 밖이면 버튼을 막지만, 최종 판정은 서버가 한다 (앱 값을 믿지 않음)
        int distance = GeoUtils.distanceMeters(pot.getDepartureLat(), pot.getDepartureLng(), request.lat(), request.lng());
        int radius = potPolicy.arrivalRadiusMeters();   // 50m (app.arrival.radius-meters)
        if (distance > radius) {
            throw Errors.outOfArrivalRange(distance, radius);   // detail: distanceMeters, allowedRadiusMeters
        }

        // ---------------------------------------------------------------- 3. 처리
        me.arrive(now);

        // 남은 참여자(JOINED) 기준 집계. 방금 바꾼 me 도 같은 영속성 컨텍스트라 arrived_at 이 반영된 상태로 조회된다
        List<TaxipotMember> joined = taxipotMemberRepository.findAllByPotIdAndStatusInOrderByIdAsc(
                potId, List.of(PotMemberStatus.JOINED));
        int arrivedCount = (int) joined.stream().filter(TaxipotMember::hasArrived).count();
        boolean allArrived = arrivedCount == joined.size();

        // 전원 도착 알림: 내가 '마지막 도착자'일 때 한 번만 나간다 (그 전 도착자들의 호출에선 allArrived = false).
        // 2명 미만이면 탑승 시작이 어차피 불가하므로 '모두 모였다'는 알림을 보내지 않는다.
        if (allArrived && joined.size() >= 2) {
            notificationSender.sendAll(joined.stream().map(TaxipotMember::getMemberId).toList(), potId,
                    NotificationType.POT, "모두 도착했어요",
                    String.format("'%s' 택시팟 참여자가 모두 모였어요. 방장이 탑승 시작을 누르면 출발해요.", pot.getTitle()));
        }

        // 방 화면 갱신 (도착 표시). 커밋 후에 방송된다
        potRealtimePublisher.potUpdated(potId);

        return new ArrivalResponse(
                potId,
                Kst.toOffset(now),
                distance,
                arrivedCount,
                pot.getCurrentMembers(),
                allArrived);
    }

    /**
     * <b>POST /api/pots/{potId}/members/{userId}/kick</b> — 미도착자 내보내기 (방장 전용)
     * <p>
     * RECRUITING·CLOSED 상태에서 도착 확인(arrived_at)이 없는 동승자만 내보낼 수 있음. Body 없음.
     * - 공통: canceled_at = now(퇴장 처리 시각, 16일 회의), current_members − 1, has_canceled = 1, CLOSED였다면 RECRUITING으로 변경,
     *   예치금 전액 환불 (SETTLEMENT → REFUNDED, fare_share NULL, POINT_HISTORY FARE_REFUND), 대상자에게 KICKED 알림, 같은 방 재참여 불가
     * - 집결 기한 전: TAXIPOT_MEMBER status = KICKED (노쇼 아님, 정지·집계 없음)
     * - 집결 기한 후: TAXIPOT_MEMBER status = NO_SHOW, no_show_count + 1, 15분 이용 정지 (now + 15분, 기존 정지가 더 길면 유지)
     * - 락 순서: 대상 회원 USERS → WALLET → TAXIPOT
     * - 집결 기한까지 미도착한 동승자는 이 API 없이도 스케줄러가 자동 퇴장 처리
     * <p>
     * <b>에러 코드</b>
     * - 403: NOT_POT_HOST
     * - 409: MEMBER_ALREADY_ARRIVED / INVALID_POT_STATUS / NOT_POT_MEMBER(대상이 참여 중이 아님·본인)
     */
    @Transactional
    public void kick(Long requesterId, Long potId, Long userId) {
        LocalDateTime now = timeProvider.now();

        // 0. 자기 자신은 내보낼 수 없다 (명세: NOT_POT_MEMBER — 대상이 본인).
        //    락을 잡기 전에 걸러서 쓸데없는 락을 피한다.
        //    ※ 명세는 이 경우를 409로 적었지만 ErrorCode.NOT_POT_MEMBER 는 403 으로 정의돼 있다 → 공통 담당과 확인 필요
        if (requesterId.equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_POT_MEMBER);
        }

        // 1. 락: 방장이 아니라 '대상 회원'의 USERS → WALLET → TAXIPOT (명세 동시성 제어).
        //    내보내기는 대상의 지갑(환불)과 정지 상태를 바꾸므로, 대상이 동시에 다른 일(참여 취소·충전)을 해도 순서가 꼬이지 않게.
        LockedMember target = memberWalletLocker.lock(userId);
        Taxipot pot = taxipotRepository.findByIdForUpdate(potId)
                .orElseThrow(() -> Errors.notFound("택시팟"));

        // 2. 검증 (명세 에러 순서: 403 → 409)
        if (!pot.isHost(requesterId)) {
            throw new BusinessException(ErrorCode.NOT_POT_HOST);
        }
        if (!BEFORE_START.contains(pot.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_POT_STATUS);
        }
        // 대상이 이 방에 '남아 있는' 동승자여야 한다. 이미 취소·내보내기·노쇼 처리됐거나 입장 요청 대기 중이면 대상 아님
        TaxipotMember member = taxipotMemberRepository.findByPotIdAndMemberId(potId, userId)
                .filter(m -> m.getStatus() == PotMemberStatus.JOINED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_POT_MEMBER));
        // 도착 확인한 사람은 보호 (입장 요청 수락자도 arrived_at 이 있으므로 여기서 걸린다)
        if (member.hasArrived()) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_ARRIVED);
        }

        // 3. 처리: 집결 기한 기준으로 KICKED / NO_SHOW 가 갈린다.
        //    기한 '정각'부터는 NO_SHOW (스케줄러 1의 arrival_deadline <= now 와 같은 경계).
        //    → 기한이 지났는데 스케줄러가 아직 안 돈 몇십 초 사이에 방장이 먼저 내보내도, 결과(노쇼·정지)는 스케줄러와 똑같다.
        boolean afterDeadline = !now.isBefore(pot.getArrivalDeadline());
        if (afterDeadline) {
            potExitHandler.noShowGuest(pot, member, target, now);
        } else {
            potExitHandler.kickGuest(pot, member, target, now);
        }

        potRealtimePublisher.potUpdated(potId);
    }

    // ===== private (참여·취소·좌석) =====

    /**
     * 이 방과의 기존 이력으로 참여를 막는 경우.
     * KICKED·NO_SHOW → 403 KICKED_FROM_POT, REJECTED → 403 JOIN_REQUEST_REJECTED, JOINED·BOARDED → 409 ALREADY_JOINED.
     * REQUESTED(입장 요청 대기)는 참여 마감 이후에만 생기므로 마감 검사에서 JOIN_DEADLINE_PASSED 로 걸린다. CANCELED 는 재참여 가능.
     */
    private static void rejectByHistory(PotMemberStatus status) {
        switch (status) {
            case KICKED, NO_SHOW -> throw new BusinessException(ErrorCode.KICKED_FROM_POT);
            case REJECTED -> throw new BusinessException(ErrorCode.JOIN_REQUEST_REJECTED);
            case JOINED, BOARDED -> throw new BusinessException(ErrorCode.ALREADY_JOINED);
            default -> {
                // CANCELED·REQUESTED: 다음 검사로
            }
        }
    }

    /** 정원이 다 차 CLOSED 가 되면 참여자 전원에게 POT 알림 */
    private void notifyFull(Taxipot pot) {
        List<Long> memberIds = taxipotMemberRepository.findAllByPotIdAndStatusInOrderByIdAsc(pot.getId(), PotMapper.ACTIVE)
                .stream().map(TaxipotMember::getMemberId).toList();
        notificationSender.sendAll(memberIds, pot.getId(), NotificationType.POT, "모집 완료",
                "'%s' 방 인원이 모두 모였어요. 출발지에서 도착 확인을 해 주세요.".formatted(pot.getTitle()));
    }
}
