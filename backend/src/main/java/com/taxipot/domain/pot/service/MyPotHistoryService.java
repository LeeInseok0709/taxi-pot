package com.taxipot.domain.pot.service;

import com.taxipot.domain.user.dto.MyPotHistoryPageResponse;
import com.taxipot.global.code.PotMemberRole;
import com.taxipot.global.code.PotStatus;
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
public class MyPotHistoryService {

    /**
     * <b>GET /api/users/me/pots</b> — 내 택시 이용 기록 조회
     * <p>
     * 내가 방장이거나 참여한 택시팟 목록 (TAXIPOT_MEMBER 기준, 최신순).
     * 방 정보 + 내 참여 상태 + 내 정산 결과(SETTLEMENT)를 한 번에 반환하므로 '이용 기록'·'정산 내역' 화면에 함께 사용.
     * - 입장 요청만 하고 참여하지 못한 행(REQUESTED·REJECTED, 또는 SETTLEMENT가 없는 행)은 제외 (3.3.0)
     */
    @Transactional(readOnly = true)
    public MyPotHistoryPageResponse getMyPots(Long userId, PotMemberRole role, PotStatus status, int page, int size) {
        // TODO(택시팟): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/users/me/pots");
    }

    /**
     * <b>GET /api/admin/users/{userId}/pots</b> — 회원 이용 기록 조회
     * <p>
     * 앱의 '내 택시 이용 기록'과 같은 응답 형식
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN
     */
    @Transactional(readOnly = true)
    public MyPotHistoryPageResponse getUserPotsForAdmin(Long adminId, Long userId, int page, int size) {
        // TODO(택시팟): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/admin/users/{userId}/pots");
    }
}
