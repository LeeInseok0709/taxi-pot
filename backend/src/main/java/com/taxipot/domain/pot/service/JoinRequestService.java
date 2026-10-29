package com.taxipot.domain.pot.service;

import com.taxipot.domain.pot.dto.JoinRequestCreateRequest;
import com.taxipot.domain.pot.dto.JoinRequestResponse;
import com.taxipot.domain.pot.dto.JoinResponse;
import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 담당: 택시팟
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class JoinRequestService {

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
        // TODO(택시팟): 위 명세대로 구현
        throw new NotImplementedYetException("POST /api/pots/{potId}/join-requests");
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
        // TODO(택시팟): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/pots/{potId}/join-requests");
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
        // TODO(택시팟): 위 명세대로 구현
        throw new NotImplementedYetException("DELETE /api/pots/{potId}/join-requests/me");
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
        // TODO(택시팟): 위 명세대로 구현
        throw new NotImplementedYetException("POST /api/pots/{potId}/join-requests/{userId}/accept");
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
        // TODO(택시팟): 위 명세대로 구현
        throw new NotImplementedYetException("POST /api/pots/{potId}/join-requests/{userId}/reject");
    }
}
