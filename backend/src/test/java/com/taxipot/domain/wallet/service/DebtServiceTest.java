package com.taxipot.domain.wallet.service;

import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.domain.wallet.dto.DebtResponse;
import com.taxipot.global.code.DebtStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 내 미수금 목록 (GET /api/wallet/debts) 단위 테스트 — DB 없이 실행 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DebtServiceTest {

    private static final Long USER_ID = 7L;
    private static final LocalDateTime SETTLED_AT = LocalDateTime.of(2026, 10, 28, 22, 0);

    @Mock SettlementRepository settlementRepository;
    @Mock TaxipotRepository taxipotRepository;

    DebtService service;

    @BeforeEach
    void setUp() {
        service = new DebtService(settlementRepository, taxipotRepository);
    }

    private Settlement debt(Long potId, int uncovered, LocalDateTime dueAt) {
        Settlement s = Settlement.hold(potId, USER_ID, 7_000);
        s.settle(10_000, 1_000, -4_000, uncovered, SETTLED_AT, dueAt);
        return s;
    }

    private Taxipot pot(Long id, String title) {
        Taxipot pot = mock(Taxipot.class);
        when(pot.getId()).thenReturn(id);
        when(pot.getTitle()).thenReturn(title);
        return pot;
    }

    @Test
    @DisplayName("미수금 목록: 납부 기한 순서 그대로, 방 제목·남은 금액·기한(+09:00)을 보여준다")
    void getMyDebts() {
        Settlement overdue = debt(100L, 3_000, SETTLED_AT.plusDays(3));
        overdue.markDebtOverdue();
        overdue.payDebt(1_000, SETTLED_AT.plusDays(4));              // 일부만 납부 → 남은 2,000
        Settlement unpaid = debt(200L, 5_000, SETTLED_AT.plusDays(5));

        // 가짜 방은 when(...) 밖에서 먼저 만든다 (stubbing 도중에 다른 stubbing을 하면 Mockito 에러)
        Taxipot pot100 = pot(100L, "두정역 → 본관");
        Taxipot pot200 = pot(200L, "터미널 → 기숙사");

        when(settlementRepository.findAllByMemberIdAndDebtStatusInOrderByDebtDueAtAscIdAsc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of(overdue, unpaid));
        when(taxipotRepository.findAllById(anyIterable())).thenReturn(List.of(pot100, pot200));

        List<DebtResponse> res = service.getMyDebts(USER_ID);

        assertThat(res).hasSize(2);
        DebtResponse first = res.get(0);
        assertThat(first.potId()).isEqualTo(100L);
        assertThat(first.potTitle()).isEqualTo("두정역 → 본관");
        assertThat(first.debtStatus()).isEqualTo(DebtStatus.OVERDUE);
        assertThat(first.uncoveredAmount()).isEqualTo(3_000);   // 정산 당시 못 걷은 금액
        assertThat(first.debtAmount()).isEqualTo(2_000);        // 지금 남은 금액
        assertThat(first.debtDueAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        assertThat(first.settledAt().toLocalDateTime()).isEqualTo(SETTLED_AT);

        assertThat(res.get(1).potTitle()).isEqualTo("터미널 → 기숙사");
        assertThat(res.get(1).debtStatus()).isEqualTo(DebtStatus.UNPAID);
    }

    @Test
    @DisplayName("미수금이 없으면 빈 목록 (방 조회도 안 함)")
    void getMyDebts_empty() {
        when(settlementRepository.findAllByMemberIdAndDebtStatusInOrderByDebtDueAtAscIdAsc(eq(USER_ID), anyCollection()))
                .thenReturn(List.of());

        assertThat(service.getMyDebts(USER_ID)).isEmpty();
        verify(taxipotRepository, never()).findAllById(anyIterable());
    }
}
