package com.taxipot.global.web;

import java.util.Optional;

/** 구현: domain.settlement (SETTLEMENT.debt_status IN (UNPAID, OVERDUE)). 미수금이 없으면 empty */
public interface UnpaidDebtQuery {

    Optional<UnpaidDebtSummary> findUnpaid(Long memberId);
}
