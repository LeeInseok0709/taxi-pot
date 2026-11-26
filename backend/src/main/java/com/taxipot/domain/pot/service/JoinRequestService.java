package com.taxipot.domain.pot.service;

import com.taxipot.domain.notification.service.NotificationSender;
import com.taxipot.domain.pot.dto.JoinRequestCreateRequest;
import com.taxipot.domain.pot.dto.JoinRequestResponse;
import com.taxipot.domain.pot.dto.JoinResponse;
import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.entity.TaxipotMember;
import com.taxipot.domain.pot.realtime.PotRealtimePublisher;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.wallet.service.LockedMember;
import com.taxipot.domain.wallet.service.MemberWalletLocker;
import com.taxipot.global.code.NotificationType;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.Errors;
import com.taxipot.global.time.TimeProvider;
import com.taxipot.global.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 담당: 택시팟
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 * <p>입장 요청 행은 TAXIPOT_MEMBER(status = REQUESTED)에 두고, 요청 시각은 joined_at 에 기록한다 (예치는 수락 때).
 */
@Service
@RequiredArgsConstructor
public class JoinRequestService {

    private final TaxipotRepository taxipotRepository;
    private final TaxipotMemberRepository memberRepository;
    private final MemberWalletLocker memberWalletLocker;
    private final PotEligibilityValidator eligibilityValidator;
    private final PotPolicy potPolicy;
    private final PotDeposit potDeposit;
    private final PotExitHandler potExitHandler;
    private final PotMapper potMapper;
    private final NotificationSender notificationSender;
    private final PotRealtimePublisher potRealtimePublisher;
    private final TimeProvider timeProvider;

    /**
     * <b>POST /api/pots/{potId}/join-requests</b> — 입장 요청 (참여 마감 후, 3.3.0)
     * <p>
     * 참여 마감(집결 기한, 출발 5분 전) 이후 출발지 근처에서 급하게 합류하려는 사람이 방장에게 입장을 요청한다 (16일 회의).
     * 락 순서: USERS(본인) → WALLET(본인) → TAXIPOT(대상 방)
     * - 가능 조건: 방 상태 RECRUITING(빈자리 있음), now ≥ 참여 마감, 탑승 시작 전
     *   - 마감 전이면 409 JOIN_REQUEST_NOT_AVAILABLE (일반 참여 사용)
     * - 위치: 요청 좌표와 출발지 거리가 50m 이내 (아니면 400 OUT_OF_ARRIVAL_RANGE)
     * - 검증: 일반 참여와 같은 공통 이용 조건(인증·정지·미수금·성별·진행 중인 다른 방·잔액 ≥ 예치액)
     *   - KICKED·NO_SHOW면 403 KICKED_FROM_POT, REJECTED면 403 JOIN_REQUEST_REJECTED, 이미 REQUESTED면 409 ALREADY_REQUESTED, JOINED면 409 ALREADY_JOINED
     * - 처리: TAXIPOT_MEMBER status = REQUESTED, joined_at = now, preferred_seat (CANCELED 행이 있으면 UPDATE), 예치는 아직 하지 않음
     * - 방장에게 POT 알림 + STOMP `POT_UPDATED`
     * <p>
     * <b>에러 코드</b>
     * - 400: OUT_OF_ARRIVAL_RANGE
     * - 403: NOT_VERIFIED / SUSPENDED_USER / GENDER_RESTRICTED / KICKED_FROM_POT / JOIN_REQUEST_REJECTED
     * - 404: NOT_FOUND
     * - 409: JOIN_REQUEST_NOT_AVAILABLE / INVALID_POT_STATUS / POT_FULL / ALREADY_REQUESTED / ALREADY_JOINED / ACTIVE_POT_EXISTS / INSUFFICIENT_BALANCE / UNPAID_DEBT_EXISTS
     */
    @Transactional
    public JoinRequestResponse createJoinRequest(Long userId, Long potId, JoinRequestCreateRequest request) {
        // 1. USERS 2. WALLET 3. TAXIPOT
        LockedMember me = memberWalletLocker.lock(userId);
        Taxipot pot = taxipotRepository.findByIdForUpdate(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        LocalDateTime now = timeProvider.now();

        TaxipotMember existing = memberRepository.findByPotIdAndMemberId(potId, userId).orElse(null);
        if (existing != null) {
            switch (existing.getStatus()) {
                case KICKED, NO_SHOW -> throw new BusinessException(ErrorCode.KICKED_FROM_POT);
                case REJECTED -> throw new BusinessException(ErrorCode.JOIN_REQUEST_REJECTED);
                case REQUESTED -> throw new BusinessException(ErrorCode.ALREADY_REQUESTED);
                case JOINED, BOARDED -> throw new BusinessException(ErrorCode.ALREADY_JOINED);
                default -> {
                    // CANCELED: 같은 행으로 다시 요청 가능
                }
            }
        }

        // 가능 조건: RECRUITING(빈자리) + 참여 마감 이후 + 탑승 시작 전
        if (pot.getStatus() == PotStatus.CLOSED) {
            throw Errors.potFull(pot.getMaxMembers(), pot.getCurrentMembers());
        }
        if (pot.getStatus() != PotStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.INVALID_POT_STATUS);
        }
        if (now.isBefore(pot.getJoinDeadline())) {
            throw new BusinessException(ErrorCode.JOIN_REQUEST_NOT_AVAILABLE);
        }

        // 위치: 출발지 반경 안 (서버 설정값)
        int radius = potPolicy.arrivalRadiusMeters();
        int distance = GeoUtils.distanceMeters(pot.getDepartureLat(), pot.getDepartureLng(), request.lat(), request.lng());
        if (distance > radius) {
            throw Errors.outOfArrivalRange(distance, radius);
        }

        // 일반 참여와 같은 공통 이용 조건 (예치는 수락 때 하지만 잔액도 지금 확인)
        int holdAmount = potPolicy.holdAmount(pot.getExpectedFare(), pot.getMaxMembers());
        eligibilityValidator.validate(me.user(), me.wallet(), pot, null, holdAmount);

        TaxipotMember member;
        if (existing == null) {
            member = memberRepository.save(TaxipotMember.request(potId, userId, request.preferredSeat(), now));
        } else {
            member = existing;
            member.rerequest(request.preferredSeat(), now);
        }

        notificationSender.send(pot.getHostId(), potId, NotificationType.POT, "입장 요청",
                "%s님이 '%s' 방에 입장을 요청했어요.".formatted(me.user().getNickname(), pot.getTitle()));
        potRealtimePublisher.potUpdated(potId);
        return potMapper.toJoinRequest(member, me.user());
    }

    /**
     * <b>GET /api/pots/{potId}/join-requests</b> — 입장 요청 대기 목록 조회 (방장 전용, 3.3.0)
     * <p>
     * status = REQUESTED인 요청자 목록 (요청 시각 순)
     * <p>
     * <b>에러 코드</b>
     * - 403: NOT_POT_HOST
     */
    @Transactional(readOnly = true)
    public List<JoinRequestResponse> getJoinRequests(Long userId, Long potId) {
        Taxipot pot = taxipotRepository.findById(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        if (!pot.isHost(userId)) {
            throw new BusinessException(ErrorCode.NOT_POT_HOST);
        }
        List<TaxipotMember> requests = memberRepository.findAllByPotIdAndStatus(potId, PotMemberStatus.REQUESTED)
                .stream()
                .sorted(Comparator.comparing(TaxipotMember::getJoinedAt).thenComparing(TaxipotMember::getId))
                .toList();
        Map<Long, User> users = potMapper.usersById(requests.stream().map(TaxipotMember::getMemberId).toList());
        return requests.stream().map(m -> potMapper.toJoinRequest(m, users.get(m.getMemberId()))).toList();
    }

    /**
     * <b>DELETE /api/pots/{potId}/join-requests/me</b> — 입장 요청 철회 (3.3.0)
     * <p>
     * 내 REQUESTED 요청을 CANCELED로 바꾸고 canceled_at = now. Body 없음. 예치 전이라 환불 없음. 방장에게 STOMP `POT_UPDATED`
     * <p>
     * <b>에러 코드</b>
     * - 409: JOIN_REQUEST_NOT_FOUND (대기 중인 요청 없음)
     */
    @Transactional
    public void withdrawJoinRequest(Long userId, Long potId) {
        // 수락과 동시에 들어와도 한쪽만 처리되도록 수락과 같은 순서로 잠근다 (요청자 USERS → WALLET → TAXIPOT)
        memberWalletLocker.lock(userId);
        taxipotRepository.findByIdForUpdate(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        TaxipotMember member = memberRepository.findByPotIdAndMemberId(potId, userId)
                .filter(m -> m.getStatus() == PotMemberStatus.REQUESTED)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOIN_REQUEST_NOT_FOUND));

        member.cancel(timeProvider.now());   // 예치 전이라 환불 없음
        potRealtimePublisher.potUpdated(potId);
    }

    /**
     * <b>POST /api/pots/{potId}/join-requests/{userId}/accept</b> — 입장 요청 수락 (방장 전용, 3.3.0)
     * <p>
     * 하나의 트랜잭션. 락 순서: 요청자 USERS → 요청자 WALLET → TAXIPOT. Body 없음.
     * - 재검증(요청 이후 바뀌었을 수 있음): 요청 status = REQUESTED, 방 RECRUITING·빈자리, 요청자 정지·미수금 없음, 진행 중인 다른 방 없음, 잔액 ≥ 예치액
     *   - 요청자 잔액 부족 등으로 수락할 수 없으면 해당 에러 반환, 요청은 REQUESTED로 남는다 (방장은 거절 가능)
     * - 처리: status = JOINED, arrived_at = now(요청 시 반경 50m 확인 → 도착 확인으로 간주, 수락 즉시 노쇼 처리 방지), current_members + 1,
     *   SETTLEMENT 생성(HELD, 있으면 UPDATE), WALLET 예치, POINT_HISTORY FARE_HOLD(−예치액)
     * - 인원이 다 차면 status = CLOSED, 남은 REQUESTED는 CANCELED + POT 알림(자리 마감)
     * - 요청자에게 POT 알림(수락), 참여자에게 STOMP `POT_UPDATED`
     * <p>
     * <b>에러 코드</b>
     * - 403: NOT_POT_HOST / SUSPENDED_USER(요청자 정지)
     * - 409: JOIN_REQUEST_NOT_FOUND / INVALID_POT_STATUS / POT_FULL / ACTIVE_POT_EXISTS / INSUFFICIENT_BALANCE(요청자 잔액) / UNPAID_DEBT_EXISTS
     */
    @Transactional
    public JoinResponse acceptJoinRequest(Long requesterId, Long potId, Long userId) {
        // 권한은 락 없이 먼저 (방장은 바뀌지 않는다) — 방장이 아니면 남의 지갑을 잠그지 않는다
        Taxipot snapshot = taxipotRepository.findById(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        if (!snapshot.isHost(requesterId)) {
            throw new BusinessException(ErrorCode.NOT_POT_HOST);
        }

        // 요청자 USERS → 요청자 WALLET → TAXIPOT
        LockedMember target = memberWalletLocker.lock(userId);
        Taxipot pot = taxipotRepository.findByIdForUpdate(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        TaxipotMember member = memberRepository.findByPotIdAndMemberId(potId, userId)
                .filter(m -> m.getStatus() == PotMemberStatus.REQUESTED)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOIN_REQUEST_NOT_FOUND));
        if (pot.getStatus() == PotStatus.CLOSED) {
            throw Errors.potFull(pot.getMaxMembers(), pot.getCurrentMembers());
        }
        if (pot.getStatus() != PotStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.INVALID_POT_STATUS);
        }

        // 재검증 (요청 이후 바뀌었을 수 있음). 실패하면 예외 → 롤백되어 요청은 REQUESTED 로 남는다
        int holdAmount = potPolicy.holdAmount(pot.getExpectedFare(), pot.getMaxMembers());
        eligibilityValidator.validate(target.user(), target.wallet(), pot, null, holdAmount);

        LocalDateTime now = timeProvider.now();
        member.acceptRequest(now);                       // JOINED + arrived_at = now (반경 확인을 마친 요청이라 도착으로 간주)
        potDeposit.hold(target.wallet(), potId, holdAmount);
        pot.addMember();

        notificationSender.send(userId, potId, NotificationType.POT, "입장 요청 수락",
                "'%s' 방 입장이 수락되었어요. 예치금 %,d원이 예치되었습니다.".formatted(pot.getTitle(), holdAmount));

        if (pot.getStatus() == PotStatus.CLOSED) {
            // 자리가 다 찼으면 남은 대기자는 자동 취소
            List<Long> others = potExitHandler.cancelPendingRequests(potId, now);
            notificationSender.sendAll(others, potId, NotificationType.POT, "입장 요청 마감",
                    "'%s' 방 자리가 모두 차서 입장 요청이 취소되었어요.".formatted(pot.getTitle()));
        }
        potRealtimePublisher.potUpdated(potId);
        return new JoinResponse(potId, potMapper.toMember(member, target.user()), holdAmount,
                target.wallet().getBalance(), pot.getStatus());
    }

    /**
     * <b>POST /api/pots/{potId}/join-requests/{userId}/reject</b> — 입장 요청 거절 (방장 전용, 3.3.0)
     * <p>
     * status = REJECTED, canceled_at = now. Body 없음. 같은 방에 다시 요청·참여 불가. 요청자에게 POT 알림.
     * has_canceled는 바꾸지 않는다 (참여자 이탈이 아님)
     * <p>
     * <b>에러 코드</b>
     * - 403: NOT_POT_HOST
     * - 409: JOIN_REQUEST_NOT_FOUND
     */
    @Transactional
    public void rejectJoinRequest(Long requesterId, Long potId, Long userId) {
        Taxipot snapshot = taxipotRepository.findById(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        if (!snapshot.isHost(requesterId)) {
            throw new BusinessException(ErrorCode.NOT_POT_HOST);
        }
        // 수락·철회와 같은 순서로 잠가 동시에 처리되지 않게 한다
        memberWalletLocker.lock(userId);
        Taxipot pot = taxipotRepository.findByIdForUpdate(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        TaxipotMember member = memberRepository.findByPotIdAndMemberId(potId, userId)
                .filter(m -> m.getStatus() == PotMemberStatus.REQUESTED)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOIN_REQUEST_NOT_FOUND));

        member.rejectRequest(timeProvider.now());   // has_canceled 는 그대로 (참여자 이탈이 아님)
        notificationSender.send(userId, potId, NotificationType.POT, "입장 요청 거절",
                "'%s' 방 방장이 입장 요청을 거절했어요.".formatted(pot.getTitle()));
        potRealtimePublisher.potUpdated(potId);
    }
}
