package com.taxipot.domain.pot.service;

import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 담당: 택시팟 — 스케줄러 작업 1·2·3·4·11 (명세 '서버 배치(스케줄러) 작업').
 * <p>방 단위 트랜잭션 + TAXIPOT 행 락 후 상태를 다시 확인 (API 와 동시에 실행돼도 이중 환불 없음).
 * 여러 회원은 MemberWalletLocker.lockAll (member_id 오름차순) → TAXIPOT 순으로 잠근다.
 * 팁: '대상 방 id 목록 조회'(트랜잭션 밖) → 방마다 별도 @Transactional 메서드 호출 (한 방 실패가 전체를 막지 않게).
 */
@Service
@RequiredArgsConstructor
public class PotJobService {

    /**
     * 1. 미도착 자동 퇴장 (1분): RECRUITING·CLOSED 이고 arrival_deadline &lt;= now 인 방.
     * arrived_at 이 NULL 인 JOINED 동승자 → NO_SHOW, canceled_at = now, no_show_count + 1, 예치금 전액 환불(SETTLEMENT REFUNDED),
     * 15분 정지(User.suspend: 더 긴 쪽 유지), current_members − 1, has_canceled = 1, CLOSED 였다면 RECRUITING, KICKED 알림.
     * 방장이 미도착이면 자동 해산(CANCELED) + 전원 환불, 방장도 같은 노쇼 처리, 남은 REQUESTED → CANCELED, 참여자·요청자에게 POT 알림.
     */
    public void autoNoShow() {
        throw new NotImplementedYetException("스케줄러 1 미도착 자동 퇴장");
    }

    /** 2. 방장 단독 방 자동 해산 (1분): departure_time &lt;= now, RECRUITING, current_members = 1 → CANCELED, 방장 전액 환불(정지 없음), REQUESTED → CANCELED, POT 알림 */
    public void dissolveSoloPots() {
        throw new NotImplementedYetException("스케줄러 2 방장 단독 방 자동 해산");
    }

    /** 3. 미출발 방 자동 취소 (1분): departure_time + 30분 &lt;= now 이고 RECRUITING·CLOSED → CANCELED, 전원 환불(정지 없음), REQUESTED → CANCELED, POT 알림 */
    public void cancelNotStartedPots() {
        throw new NotImplementedYetException("스케줄러 3 미출발 방 자동 취소");
    }

    /** 4. 지각 경고 (1분): 집결 기한 N분(app.pot.late-warning-minutes) 전까지 미도착인 참여자에게 LATE_WARNING 알림 (중복 발송 주의) */
    public void lateWarning() {
        throw new NotImplementedYetException("스케줄러 4 지각 경고");
    }

    /**
     * 11. 출발 확인 알림 (30초): RECRUITING·CLOSED, start_reminded_at IS NULL, 남은 JOINED 2명 이상 전원 arrived_at NOT NULL,
     * MAX(arrived_at) + 3분 &lt;= now → 방장에게 POT 알림, start_reminded_at = now (1회만)
     */
    public void startReminder() {
        throw new NotImplementedYetException("스케줄러 11 출발 확인 알림");
    }
}
