package com.taxipot.domain.wallet.service;

import com.taxipot.domain.wallet.dto.PointHistoryPageResponse;
import com.taxipot.domain.wallet.dto.WalletResponse;
import com.taxipot.global.code.PointType;
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
public class WalletService {

    /**
     * <b>GET /api/wallet</b> — 내 지갑 잔액 조회
     * <p>
     * 메인 화면 상시 표시용
     */
    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long userId) {
        // TODO(지갑·정산): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/wallet");
    }

    /**
     * <b>GET /api/wallet/history</b> — 포인트 이용 내역 조회
     * <p>
     * POINT_HISTORY 최신순 (충전·예치·환불·택시비·수수료·방장 정산금·미수금 납부·잔액 소멸).
     * 정산 1건은 같은 시각에 여러 행(FARE_REFUND 예치 해제 → FARE_PAY → FEE, 방장은 HOST_PAYOUT 포함)으로 기록되므로 id 내림차순으로 정렬한다
     */
    @Transactional(readOnly = true)
    public PointHistoryPageResponse getHistory(Long userId, PointType type, int page, int size) {
        // TODO(지갑·정산): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/wallet/history");
    }
}
