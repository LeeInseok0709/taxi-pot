package com.taxipot.domain.wallet.service;

import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.domain.wallet.dto.WalletResponse;
import com.taxipot.domain.wallet.entity.Wallet;
import com.taxipot.domain.wallet.repository.WalletRepository;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** 내 지갑 조회 (GET /api/wallet) 단위 테스트 — DB 없이 실행 */
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    private static final Long USER_ID = 7L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 31, 1, 30);

    @Mock WalletRepository walletRepository;
    @Mock SettlementRepository settlementRepository;

    WalletService service;

    @BeforeEach
    void setUp() {
        service = new WalletService(walletRepository, new WalletResponseFactory(settlementRepository, walletRepository));
    }

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
}
