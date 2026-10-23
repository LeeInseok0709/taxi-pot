package com.taxipot.pot.service;

import com.taxipot.pot.dto.ArrivalRequest;
import com.taxipot.pot.dto.ArrivalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ArrivalController 전용 API 서비스. 담당: 인석
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArrivalService {

    /**
     * POST /api/pots/{potId}/members/me/arrival — 도착 확인 (GPS 검증) (담당 인석)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 현재 GPS 좌표와 출발지 좌표의 거리(하버사인 공식)가 도착 인정 반경(서버 설정값, 50m — 16일 회의) 이내일 때만 arrived_at = now 기록.
     * 앱은 반경 안에서만 버튼을 활성화하되 최종 판정은 서버가 한다.
     * - 집결 기한 이후에는 호출 불가 (자동 퇴장 대상)
     * - 도착 확인한 참여자는 방장이 내보낼 수 없음
     * - 전원 도착 시 참여자 전원에게 POT 알림, STOMP `/topic/pots/{potId}`로 `POT_UPDATED` 전송
     * - 시연 주의: 50m는 실내·고층 건물 옆에서 GPS 오차보다 작을 수 있음. 시연은 실외에서 하거나 Android 개발자 옵션의 '모의 위치 앱'으로 좌표를 고정하고,
     *   앱은 위치 요청에 `Priority.PRIORITY_HIGH_ACCURACY`를 사용
     */
    @Transactional
    public ArrivalResponse confirmArrival(Long userId, Long potId, ArrivalRequest request) {
        throw new UnsupportedOperationException("TODO(인석): POST /api/pots/{potId}/members/me/arrival");
    }
}
