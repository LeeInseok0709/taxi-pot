package com.taxipot.pot.service;

import com.taxipot.pot.dto.JoinRequest;
import com.taxipot.pot.dto.JoinResponse;
import com.taxipot.pot.dto.PotMemberResponse;
import com.taxipot.pot.dto.SeatChangeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PotMemberController 전용 API 서비스. 담당: 서연
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PotMemberService {

    /**
     * POST /api/pots/{potId}/members — 택시팟 참여 (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 하나의 트랜잭션으로 처리. 락 순서: USERS(본인) → WALLET(본인) → TAXIPOT(대상 방), 모두 `SELECT ... FOR UPDATE`
     * - USERS 락으로 같은 회원의 서로 다른 방 동시 참여를 막고, TAXIPOT 락으로 동시 신청 정원 초과를 막는다
     * - 검증: 공통 이용 조건, 방 상태 RECRUITING, now < 참여 마감(arrival_deadline), current_members < max_members, 성별 제한 일치, 진행 중인 다른 방 없음, 잔액 ≥ 예치액
     *   - 참여 마감이 지났으면 409 JOIN_DEADLINE_PASSED (detail.joinDeadline) → 앱은 입장 요청(`POST /api/pots/{potId}/join-requests`)으로 안내
     * - 처리: TAXIPOT_MEMBER 등록(GUEST), current_members + 1, SETTLEMENT 생성(HELD), WALLET 예치, POINT_HISTORY FARE_HOLD(−예치액) 1행
     * - 취소 이력이 있으면 INSERT 대신 기존 TAXIPOT_MEMBER·SETTLEMENT 행을 UPDATE — UNIQUE(pot_id, member_id)
     *   - TAXIPOT_MEMBER: status = JOINED, joined_at = now, arrived_at·canceled_at = NULL, preferred_seat 갱신
     *   - SETTLEMENT: status = HELD, held_amount = 새 예치액, fare_share·fee_amount·diff_amount·settled_at = NULL, uncovered_amount = 0, debt_status = NONE
     * - 내보내진(KICKED)·노쇼(NO_SHOW)·입장 거절(REJECTED) 처리된 방에는 재참여 불가
     * - 인원이 다 차면 status = CLOSED, 참여자 전원에게 POT 알림
     */
    @Transactional
    public JoinResponse joinPot(Long userId, Long potId, JoinRequest request) {
        throw new UnsupportedOperationException("TODO(서연): POST /api/pots/{potId}/members");
    }

    /**
     * DELETE /api/pots/{potId}/members/me — 택시팟 참여 취소 (동승자 전용) (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 방장은 취소 대신 방 취소(DELETE /api/pots/{potId}) 사용. Body 없음.
     * - 출발 10분 전부터 취소 불가 (서버 검증)
     * - TAXIPOT_MEMBER status = CANCELED, canceled_at = now
     * - current_members − 1, has_canceled = 1, CLOSED였다면 RECRUITING으로 변경
     * - 예치금 전액 환불 (SETTLEMENT → REFUNDED, POINT_HISTORY FARE_REFUND). 수수료는 기록·차감하지 않음
     * - 입장 요청 대기 중(REQUESTED)인 사람은 이 API가 아니라 `DELETE /api/pots/{potId}/join-requests/me`로 철회
     * - 방장에게 POT 알림
     */
    @Transactional
    public void leavePot(Long userId, Long potId) {
        throw new UnsupportedOperationException("TODO(서연): DELETE /api/pots/{potId}/members/me");
    }

    /**
     * PATCH /api/pots/{potId}/members/me/seat — 희망 좌석 변경 (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * RECRUITING·CLOSED 상태에서만 변경 가능
     */
    @Transactional
    public PotMemberResponse changeSeat(Long userId, Long potId, SeatChangeRequest request) {
        throw new UnsupportedOperationException("TODO(서연): PATCH /api/pots/{potId}/members/me/seat");
    }

    /**
     * POST /api/pots/{potId}/members/{userId}/kick — 미도착자 내보내기 (방장 전용) (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * RECRUITING·CLOSED 상태에서 도착 확인(arrived_at)이 없는 동승자만 내보낼 수 있음. Body 없음.
     * - 공통: canceled_at = now(퇴장 처리 시각, 16일 회의), current_members − 1, has_canceled = 1, CLOSED였다면 RECRUITING으로 변경,
     *   예치금 전액 환불 (SETTLEMENT → REFUNDED, fare_share NULL, POINT_HISTORY FARE_REFUND), 대상자에게 KICKED 알림, 같은 방 재참여 불가
     * - 집결 기한 전: TAXIPOT_MEMBER status = KICKED (노쇼 아님, 정지·집계 없음)
     * - 집결 기한 후: TAXIPOT_MEMBER status = NO_SHOW, no_show_count + 1, 15분 이용 정지 (now + 15분, 기존 정지가 더 길면 유지)
     * - 락 순서: 대상 회원 USERS → WALLET → TAXIPOT
     * - 집결 기한까지 미도착한 동승자는 이 API 없이도 스케줄러가 자동 퇴장 처리
     */
    @Transactional
    public void kickMember(Long userId, Long potId, Long targetUserId) {
        throw new UnsupportedOperationException("TODO(서연): POST /api/pots/{potId}/members/{userId}/kick");
    }
}
