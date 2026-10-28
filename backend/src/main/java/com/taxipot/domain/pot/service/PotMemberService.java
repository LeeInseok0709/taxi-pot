package com.taxipot.domain.pot.service;

import com.taxipot.domain.pot.dto.ArrivalRequest;
import com.taxipot.domain.pot.dto.ArrivalResponse;
import com.taxipot.domain.pot.dto.JoinRequest;
import com.taxipot.domain.pot.dto.JoinResponse;
import com.taxipot.domain.pot.dto.PotMemberResponse;
import com.taxipot.domain.pot.dto.SeatChangeRequest;
import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담당: 택시팟
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class PotMemberService {

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
        // TODO(택시팟): 위 명세대로 구현
        throw new NotImplementedYetException("POST /api/pots/{potId}/members");
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
        // TODO(택시팟): 위 명세대로 구현
        throw new NotImplementedYetException("DELETE /api/pots/{potId}/members/me");
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
        // TODO(택시팟): 위 명세대로 구현
        throw new NotImplementedYetException("PATCH /api/pots/{potId}/members/me/seat");
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
        // TODO(택시팟): 위 명세대로 구현
        throw new NotImplementedYetException("POST /api/pots/{potId}/members/me/arrival");
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
        // TODO(택시팟): 위 명세대로 구현
        throw new NotImplementedYetException("POST /api/pots/{potId}/members/{userId}/kick");
    }
}
