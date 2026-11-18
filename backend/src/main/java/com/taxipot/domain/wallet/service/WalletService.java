package com.taxipot.domain.wallet.service;

import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.wallet.dto.PointHistoryPageResponse;
import com.taxipot.domain.wallet.dto.PointHistoryResponse;
import com.taxipot.domain.wallet.dto.WalletResponse;
import com.taxipot.domain.wallet.entity.PointHistory;
import com.taxipot.domain.wallet.entity.Wallet;
import com.taxipot.domain.wallet.repository.PointHistoryRepository;
import com.taxipot.domain.wallet.repository.WalletRepository;
import com.taxipot.global.code.PointType;
import com.taxipot.global.error.Errors;
import com.taxipot.global.response.PageMeta;
import com.taxipot.global.response.Paging;
import com.taxipot.global.time.Kst;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 담당: 지갑·정산
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletResponseFactory walletResponseFactory;
    private final PointHistoryRepository pointHistoryRepository;
    private final TaxipotRepository taxipotRepository;

    /**
     * <b>GET /api/wallet</b> — 내 지갑 잔액 조회
     * <p>
     * 메인 화면 상시 표시용
     */
    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long userId) {
        // 내 지갑 (가입할 때 자동으로 만들어지므로 보통은 항상 있다)
        Wallet wallet = walletRepository.findByMemberId(userId)
                .orElseThrow(() -> Errors.notFound("지갑"));
        // 잔액·예치금 + 남은 미수금 합계(debtAmount)를 함께 내려준다 (운영자 잔액 조정과 같은 응답)
        return walletResponseFactory.of(wallet);
    }

    /**
     * <b>GET /api/wallet/history</b> — 포인트 이용 내역 조회
     * <p>
     * POINT_HISTORY 최신순 (충전·예치·환불·택시비·수수료·방장 정산금·미수금 납부·잔액 소멸).
     * 정산 1건은 같은 시각에 여러 행(FARE_REFUND 예치 해제 → FARE_PAY → FEE, 방장은 HOST_PAYOUT 포함)으로 기록되므로 id 내림차순으로 정렬한다
     */
    @Transactional(readOnly = true)
    public PointHistoryPageResponse getHistory(Long userId, PointType type, int page, int size) {
        // 1. 최신순 = id 내림차순 (정산 1건은 같은 시각에 여러 줄이라 시각이 아니라 id로 정렬)
        //    page·size 범위 검사(size 1~100)는 Paging 이 한다 → 벗어나면 400 INVALID_INPUT
        Pageable pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        // 2. 유형(type)을 고르면 그 유형만, 안 고르면 전체
        Page<PointHistory> result = (type == null)
                ? pointHistoryRepository.findAllByMemberId(userId, pageable)
                : pointHistoryRepository.findAllByMemberIdAndType(userId, type, pageable);

        // 3. 방과 관련된 줄은 방 제목도 보여준다 (한 번에 조회)
        Map<Long, String> potTitles = findPotTitles(result.getContent());

        List<PointHistoryResponse> content = result.getContent().stream()
                .map(h -> new PointHistoryResponse(
                        h.getId(),
                        h.getPotId(),
                        h.getPotId() == null ? null : potTitles.get(h.getPotId()),
                        h.getPaymentId(),
                        h.getType(),
                        h.getAmount(),
                        h.getBalanceAfter(),
                        Kst.toOffset(h.getCreatedAt())))
                .toList();

        return new PointHistoryPageResponse(content, PageMeta.from(result));
    }

    /** 원장 줄들에 나온 방 번호 → 방 제목 (방이 없는 줄만 있으면 조회하지 않음) */
    private Map<Long, String> findPotTitles(List<PointHistory> histories) {
        List<Long> potIds = histories.stream().map(PointHistory::getPotId).filter(Objects::nonNull).distinct().toList();
        if (potIds.isEmpty()) {
            return Map.of();
        }
        return taxipotRepository.findAllById(potIds).stream()
                .collect(Collectors.toMap(Taxipot::getId, Taxipot::getTitle));
    }
}
