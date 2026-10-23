package com.taxipot.wallet.service;

import com.taxipot.global.auth.UnpaidDebtChecker;
import com.taxipot.global.code.DebtStatus;
import com.taxipot.wallet.entity.Settlement;
import com.taxipot.wallet.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 미수금 이용 제한 인터셉터가 사용 (global.auth.UnpaidDebtChecker 구현). 수정은 효우 */
@Component
@RequiredArgsConstructor
public class SettlementDebtChecker implements UnpaidDebtChecker {

    private static final List<DebtStatus> UNPAID_STATUSES = List.of(DebtStatus.UNPAID, DebtStatus.OVERDUE);

    private final SettlementRepository settlementRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Summary> findUnpaid(Long memberId) {
        List<Settlement> debts = settlementRepository.findByMemberIdAndDebtStatusIn(memberId, UNPAID_STATUSES);
        if (debts.isEmpty()) {
            return Optional.empty();
        }
        int total = debts.stream().mapToInt(Settlement::getDebtAmount).sum();
        LocalDateTime earliestDue = debts.stream()
                .filter(s -> s.getDebtStatus() == DebtStatus.UNPAID)
                .map(Settlement::getDebtDueAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        return Optional.of(new Summary(total, earliestDue));
    }
}
