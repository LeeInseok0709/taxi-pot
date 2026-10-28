package com.taxipot.global.web;

import java.time.LocalDateTime;

/** 미수금(UNPAID·OVERDUE) 합계와 가장 이른 납부 기한(UNPAID 기준, 연체만 있으면 null) */
public record UnpaidDebtSummary(int totalAmount, LocalDateTime earliestDueAt) {
}
