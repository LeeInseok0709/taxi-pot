package com.taxipot.pot.service;

import com.taxipot.global.code.PotMemberRole;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.common.PageResponse;
import com.taxipot.pot.dto.MyPotHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MyPotController 전용 API 서비스. 담당: 서연
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPotService {

    /**
     * GET /api/users/me/pots — 내 택시 이용 기록 조회 (담당 서연)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 내가 방장이거나 참여한 택시팟 목록 (TAXIPOT_MEMBER 기준, 최신순).
     * 방 정보 + 내 참여 상태 + 내 정산 결과(SETTLEMENT)를 한 번에 반환하므로 '이용 기록'·'정산 내역' 화면에 함께 사용.
     * - 입장 요청만 하고 참여하지 못한 행(REQUESTED·REJECTED, 또는 SETTLEMENT가 없는 행)은 제외 (3.3.0)
     */
    public PageResponse<MyPotHistoryResponse> getMyPotHistoryPage(Long userId, PotMemberRole role, PotStatus status, Pageable pageable) {
        throw new UnsupportedOperationException("TODO(서연): GET /api/users/me/pots");
    }
}
