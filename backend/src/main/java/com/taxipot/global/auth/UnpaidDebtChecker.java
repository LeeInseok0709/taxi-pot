package com.taxipot.global.auth;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 미수금 이용 제한(명세 3.5.0/3.8.0)에 쓰는 조회 계약. 구현은 wallet 담당(효우)이
 * wallet 패키지에 @Component로 만든다. 구현이 없으면 인터셉터는 검사를 건너뛴다.
 *
 * SETTLEMENT.debt_status IN (UNPAID, OVERDUE)인 행이 있으면
 * Summary(남은 미수금 합계, 가장 이른 UNPAID의 debt_due_at — OVERDUE만 있으면 null) 반환, 없으면 empty.
 */
public interface UnpaidDebtChecker {

    Optional<Summary> findUnpaid(Long memberId);

    record Summary(int debtAmount, LocalDateTime debtDueAt) {}
}
