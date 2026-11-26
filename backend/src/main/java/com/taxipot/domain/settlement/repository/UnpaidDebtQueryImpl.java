package com.taxipot.domain.settlement.repository;

import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.global.code.DebtStatus;
import com.taxipot.global.web.UnpaidDebtQuery;
import com.taxipot.global.web.UnpaidDebtSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 미수금 이용 제한 인터셉터·이용 조건 검사용 */
@Component
@RequiredArgsConstructor
public class UnpaidDebtQueryImpl implements UnpaidDebtQuery {

    private static final List<DebtStatus> UNPAID = List.of(DebtStatus.UNPAID, DebtStatus.OVERDUE);

    private final SettlementRepository settlementRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<UnpaidDebtSummary> findUnpaid(Long memberId) {
        List<Settlement> debts = settlementRepository.findAllByMemberIdAndDebtStatusInOrderByDebtDueAtAscIdAsc(memberId, UNPAID);
        if (debts.isEmpty()) {
            return Optional.empty();
        }
        int total = debts.stream().mapToInt(Settlement::getDebtAmount).sum();
        LocalDateTime earliest = debts.stream()
                .filter(s -> s.getDebtStatus() == DebtStatus.UNPAID)
                .map(Settlement::getDebtDueAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        return Optional.of(new UnpaidDebtSummary(total, earliest));
    }
}
