package com.taxipot.wallet.service;

import com.taxipot.wallet.dto.WalletAdjustRequest;
import com.taxipot.wallet.dto.WalletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AdminWalletController 전용 API 서비스. 담당: 효우
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminWalletService {

    /**
     * PATCH /api/admin/users/{userId}/wallet — 지갑 잔액 조정 (시연 충전 수단, 3.7.0 확정) (담당 효우)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 시연 시 DB를 직접 수정하지 않고 운영자 화면에서 잔액을 조정하기 위한 API. 3.3.0부터 시연 충전은 이 API로만 한다 (앱 DUMMY 충전 폐지).
     * POINT_HISTORY에 ADMIN_ADJUST(±금액, payment_id = NULL)로 기록해 실제 PG 충전(CHARGE)과 구분. 락 순서: 대상 회원 USERS → WALLET
     * 조정 후 balance가 0 미만이면 409(INSUFFICIENT_BALANCE), 잔액 상한 초과면 409(WALLET_LIMIT_EXCEEDED)
     * - +금액 조정이면 직후 미수금 자동 납부(DEBT_PAY, 충전 승인과 같은 규칙, 3.5.0) — 시연에서 미수금 해제 시나리오에 사용
     */
    @Transactional
    public WalletResponse adjustWallet(Long adminId, Long userId, WalletAdjustRequest request) {
        throw new UnsupportedOperationException("TODO(효우): PATCH /api/admin/users/{userId}/wallet");
    }
}
