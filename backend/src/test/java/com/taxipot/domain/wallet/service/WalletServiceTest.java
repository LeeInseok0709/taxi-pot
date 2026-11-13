package com.taxipot.domain.wallet.service;

import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.domain.wallet.dto.PointHistoryPageResponse;
import com.taxipot.domain.wallet.dto.WalletResponse;
import com.taxipot.domain.wallet.entity.PointHistory;
import com.taxipot.domain.wallet.entity.Wallet;
import com.taxipot.domain.wallet.repository.PointHistoryRepository;
import com.taxipot.domain.wallet.repository.WalletRepository;
import com.taxipot.global.code.PointType;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 내 지갑 조회·포인트 내역 (GET /api/wallet, GET /api/wallet/history) 단위 테스트 — DB 없이 실행 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WalletServiceTest {

    private static final Long USER_ID = 7L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 31, 1, 30);

    @Mock WalletRepository walletRepository;
    @Mock SettlementRepository settlementRepository;
    @Mock PointHistoryRepository pointHistoryRepository;
    @Mock TaxipotRepository taxipotRepository;

    WalletService service;

    @BeforeEach
    void setUp() {
        service = new WalletService(walletRepository,
                new WalletResponseFactory(settlementRepository, walletRepository),
                pointHistoryRepository, taxipotRepository);
    }

    // ===================================================================== GET /api/wallet

    @Test
    @DisplayName("내 지갑: 잔액·예치금과 남은 미수금 합계를 함께 보여준다")
    void getWallet() {
        Wallet wallet = Wallet.open(USER_ID);
        wallet.deposit(20_000);
        wallet.hold(7_000);                     // 잔액 13,000 / 예치 7,000

        Settlement debt = Settlement.hold(100L, USER_ID, 7_000);
        debt.settle(10_000, 1_000, -4_000, 3_000, NOW, NOW.plusDays(3));   // 미수금 3,000

        when(walletRepository.findByMemberId(USER_ID)).thenReturn(Optional.of(wallet));
        when(settlementRepository.findAllByMemberIdAndDebtStatusInOrderByDebtDueAtAscIdAsc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(debt));

        WalletResponse res = service.getWallet(USER_ID);

        assertThat(res.memberId()).isEqualTo(USER_ID);
        assertThat(res.balance()).isEqualTo(13_000);
        assertThat(res.heldBalance()).isEqualTo(7_000);
        assertThat(res.debtAmount()).isEqualTo(3_000);
    }

    @Test
    @DisplayName("미수금이 없으면 debtAmount는 0")
    void getWallet_noDebt() {
        when(walletRepository.findByMemberId(USER_ID)).thenReturn(Optional.of(Wallet.open(USER_ID)));
        when(settlementRepository.findAllByMemberIdAndDebtStatusInOrderByDebtDueAtAscIdAsc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of());

        WalletResponse res = service.getWallet(USER_ID);

        assertThat(res.balance()).isZero();
        assertThat(res.debtAmount()).isZero();
    }

    @Test
    @DisplayName("지갑이 없으면 404 NOT_FOUND")
    void getWallet_notFound() {
        when(walletRepository.findByMemberId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getWallet(USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    // ===================================================================== GET /api/wallet/history

    private PointHistory history(long id, PointType type, int amount, int balanceAfter, Long potId) {
        PointHistory h = PointHistory.of(USER_ID, type, amount, balanceAfter, potId, null, null);
        ReflectionTestUtils.setField(h, "id", id);
        return h;
    }

    @Test
    @DisplayName("포인트 내역: id 내림차순으로 조회하고, 방 관련 줄에는 방 제목, 충전 줄에는 null")
    void getHistory() {
        Taxipot pot = mock(Taxipot.class);
        when(pot.getId()).thenReturn(100L);
        when(pot.getTitle()).thenReturn("두정역 → 본관");

        List<PointHistory> rows = List.of(
                history(3, PointType.FARE_HOLD, -7_000, 13_000, 100L),
                history(2, PointType.ADMIN_ADJUST, 20_000, 20_000, null));
        when(pointHistoryRepository.findAllByMemberId(eq(USER_ID), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(rows, inv.getArgument(1), 2));
        when(taxipotRepository.findAllById(anyIterable())).thenReturn(List.of(pot));

        PointHistoryPageResponse res = service.getHistory(USER_ID, null, 0, 20);

        // 정렬: id 내림차순으로 요청했는지
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(pointHistoryRepository).findAllByMemberId(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "id"));

        assertThat(res.content()).hasSize(2);
        assertThat(res.content().get(0).type()).isEqualTo(PointType.FARE_HOLD);
        assertThat(res.content().get(0).potTitle()).isEqualTo("두정역 → 본관");
        assertThat(res.content().get(1).potTitle()).isNull();
        assertThat(res.page().totalElements()).isEqualTo(2L);
    }

    @Test
    @DisplayName("포인트 내역: type을 고르면 그 유형만 조회, 방이 없는 줄뿐이면 방 조회 안 함")
    void getHistory_filterByType() {
        when(pointHistoryRepository.findAllByMemberIdAndType(eq(USER_ID), eq(PointType.ADMIN_ADJUST), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(history(2, PointType.ADMIN_ADJUST, 20_000, 20_000, null)),
                        inv.getArgument(2), 1));

        PointHistoryPageResponse res = service.getHistory(USER_ID, PointType.ADMIN_ADJUST, 0, 20);

        assertThat(res.content()).extracting(r -> r.type()).containsExactly(PointType.ADMIN_ADJUST);
        verify(pointHistoryRepository, never()).findAllByMemberId(any(), any());
        verify(taxipotRepository, never()).findAllById(anyIterable());
    }

    @Test
    @DisplayName("포인트 내역: size가 범위(1~100) 밖이면 400 INVALID_INPUT")
    void getHistory_invalidSize() {
        assertThatThrownBy(() -> service.getHistory(USER_ID, null, 0, 101))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
