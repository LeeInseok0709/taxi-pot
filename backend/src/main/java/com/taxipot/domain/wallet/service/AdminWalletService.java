package com.taxipot.domain.wallet.service;

import com.taxipot.domain.wallet.dto.WalletAdjustRequest;
import com.taxipot.domain.wallet.dto.WalletResponse;
import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 담당: 지갑·정산
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class AdminWalletService {

    /**
     * <b>PATCH /api/admin/users/{userId}/wallet</b> — 지갑 잔액 조정 (시연 충전 수단, 3.7.0 확정)
     * <p>
     * 시연 시 DB를 직접 수정하지 않고 운영자 화면에서 잔액을 조정하기 위한 API. 3.3.0부터 시연 충전은 이 API로만 한다 (앱 DUMMY 충전 폐지).
     * POINT_HISTORY에 ADMIN_ADJUST(±금액, payment_id = NULL)로 기록해 실제 PG 충전(CHARGE)과 구분. 락 순서: 대상 회원 USERS → WALLET
     * 조정 후 balance가 0 미만이면 409(INSUFFICIENT_BALANCE), 잔액 상한 초과면 409(WALLET_LIMIT_EXCEEDED)
     * - +금액 조정이면 직후 미수금 자동 납부(DEBT_PAY, 충전 승인과 같은 규칙, 3.5.0) — 시연에서 미수금 해제 시나리오에 사용
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN
     * - 409: INSUFFICIENT_BALANCE / WALLET_LIMIT_EXCEEDED
     */
    @Transactional
    public WalletResponse adjustWallet(Long adminId, Long userId, WalletAdjustRequest request) {
        // TODO(지갑·정산): 위 명세대로 구현
        throw new NotImplementedYetException("PATCH /api/admin/users/{userId}/wallet");
    }
}
