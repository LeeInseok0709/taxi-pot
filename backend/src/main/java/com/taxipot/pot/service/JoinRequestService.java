package com.taxipot.pot.service;

import com.taxipot.pot.dto.JoinRequestCreateRequest;
import com.taxipot.pot.dto.JoinRequestResponse;
import com.taxipot.pot.dto.JoinResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * JoinRequestController 전용 API 서비스. 담당: 서연
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JoinRequestService {

    /**
     * POST /api/pots/{potId}/join-requests — 입장 요청 (참여 마감 후, 3.3.0) (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 참여 마감(집결 기한, 출발 5분 전) 이후 출발지 근처에서 급하게 합류하려는 사람이 방장에게 입장을 요청한다 (16일 회의).
     * 락 순서: USERS(본인) → WALLET(본인) → TAXIPOT(대상 방)
     * - 가능 조건: 방 상태 RECRUITING(빈자리 있음), now ≥ 참여 마감, 탑승 시작 전
     *   - 마감 전이면 409 JOIN_REQUEST_NOT_AVAILABLE (일반 참여 사용)
     * - 위치: 요청 좌표와 출발지 거리가 50m 이내 (아니면 400 OUT_OF_ARRIVAL_RANGE)
     * - 검증: 일반 참여와 같은 공통 이용 조건(인증·정지·미수금·성별·진행 중인 다른 방·잔액 ≥ 예치액)
     *   - KICKED·NO_SHOW면 403 KICKED_FROM_POT, REJECTED면 403 JOIN_REQUEST_REJECTED, 이미 REQUESTED면 409 ALREADY_REQUESTED, JOINED면 409 ALREADY_JOINED
     * - 처리: TAXIPOT_MEMBER status = REQUESTED, joined_at = now, preferred_seat (CANCELED 행이 있으면 UPDATE), 예치는 아직 하지 않음
     * - 방장에게 POT 알림 + STOMP `POT_UPDATED`
     */
    @Transactional
    public JoinRequestResponse createJoinRequest(Long userId, Long potId, JoinRequestCreateRequest request) {
        throw new UnsupportedOperationException("TODO(서연): POST /api/pots/{potId}/join-requests");
    }

    /**
     * GET /api/pots/{potId}/join-requests — 입장 요청 대기 목록 조회 (방장 전용, 3.3.0) (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * status = REQUESTED인 요청자 목록 (요청 시각 순)
     */
    public List<JoinRequestResponse> getJoinRequestList(Long userId, Long potId) {
        throw new UnsupportedOperationException("TODO(서연): GET /api/pots/{potId}/join-requests");
    }

    /**
     * DELETE /api/pots/{potId}/join-requests/me — 입장 요청 철회 (3.3.0) (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 내 REQUESTED 요청을 CANCELED로 바꾸고 canceled_at = now. Body 없음. 예치 전이라 환불 없음. 방장에게 STOMP `POT_UPDATED`
     */
    @Transactional
    public void cancelJoinRequest(Long userId, Long potId) {
        throw new UnsupportedOperationException("TODO(서연): DELETE /api/pots/{potId}/join-requests/me");
    }

    /**
     * POST /api/pots/{potId}/join-requests/{userId}/accept — 입장 요청 수락 (방장 전용, 3.3.0) (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 하나의 트랜잭션. 락 순서: 요청자 USERS → 요청자 WALLET → TAXIPOT. Body 없음.
     * - 재검증(요청 이후 바뀌었을 수 있음): 요청 status = REQUESTED, 방 RECRUITING·빈자리, 요청자 정지·미수금 없음, 진행 중인 다른 방 없음, 잔액 ≥ 예치액
     *   - 요청자 잔액 부족 등으로 수락할 수 없으면 해당 에러 반환, 요청은 REQUESTED로 남는다 (방장은 거절 가능)
     * - 처리: status = JOINED, arrived_at = now(요청 시 반경 50m 확인 → 도착 확인으로 간주, 수락 즉시 노쇼 처리 방지), current_members + 1,
     *   SETTLEMENT 생성(HELD, 있으면 UPDATE), WALLET 예치, POINT_HISTORY FARE_HOLD(−예치액)
     * - 인원이 다 차면 status = CLOSED, 남은 REQUESTED는 CANCELED + POT 알림(자리 마감)
     * - 요청자에게 POT 알림(수락), 참여자에게 STOMP `POT_UPDATED`
     */
    @Transactional
    public JoinResponse acceptJoinRequest(Long userId, Long potId, Long targetUserId) {
        throw new UnsupportedOperationException("TODO(서연): POST /api/pots/{potId}/join-requests/{userId}/accept");
    }

    /**
     * POST /api/pots/{potId}/join-requests/{userId}/reject — 입장 요청 거절 (방장 전용, 3.3.0) (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * status = REJECTED, canceled_at = now. Body 없음. 같은 방에 다시 요청·참여 불가. 요청자에게 POT 알림.
     * has_canceled는 바꾸지 않는다 (참여자 이탈이 아님)
     */
    @Transactional
    public void rejectJoinRequest(Long userId, Long potId, Long targetUserId) {
        throw new UnsupportedOperationException("TODO(서연): POST /api/pots/{potId}/join-requests/{userId}/reject");
    }
}
