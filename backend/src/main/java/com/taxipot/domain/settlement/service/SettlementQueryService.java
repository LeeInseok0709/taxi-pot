package com.taxipot.domain.settlement.service;

import com.taxipot.domain.settlement.dto.PotSettlementResponse;
import com.taxipot.global.error.NotImplementedYetException;
import com.taxipot.global.file.StoredFile;
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
public class SettlementQueryService {

    /**
     * <b>GET /api/pots/{potId}/settlement</b> — 택시팟 정산 내역 조회
     * <p>
     * 해당 방 참여자만 조회 가능. 참여자별 예치액·분담금·수수료·차액·미수금 + 방장 정산금·영수증 URL 반환. 운영자 승인 전에는 정산 값이 null (3.5.0)
     * <p>
     * <b>에러 코드</b>
     * - 403: NOT_POT_MEMBER
     */
    @Transactional(readOnly = true)
    public PotSettlementResponse getPotSettlement(Long userId, Long potId) {
        // TODO(지갑·정산): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/pots/{potId}/settlement");
    }

    /**
     * <b>GET /api/pots/{potId}/receipt</b> — 결제내역(영수증) 사진 조회 (3.3.0)
     * <p>
     * 해당 방 탑승자(BOARDED)만 조회 가능 (운영자는 `/api/admin/pots/{potId}/receipt`). 이미지 바이너리 반환, Bearer 토큰 필요
     * (공통 규칙 '이미지 조회 API' 참고). 영수증이 없거나 보관 기간(정산 승인 후 90일)이 지나 파기됐으면 404 (3.6.0)
     * <p>
     * <b>에러 코드</b>
     * - 403: NOT_POT_MEMBER
     * - 404: NOT_FOUND
     */
    @Transactional(readOnly = true)
    public StoredFile getReceipt(Long userId, Long potId) {
        // TODO(지갑·정산): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/pots/{potId}/receipt");
    }
}
