package com.taxipot.global.code;

/**
 * SETTLEMENT.debt_status 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * <p>코드값 추가는 가능, 이름 변경·삭제 금지 (명세 '코드값(enum) 확장 정책')
 * <p>Entity 에서는 반드시 {@code @Enumerated(EnumType.STRING)} + {@code @JdbcTypeCode(SqlTypes.VARCHAR)} 로 매핑
 */
public enum DebtStatus {
    /** 미수 없음 (7차) */
    NONE,
    /** 미수금 미납(기한 내) (7차) */
    UNPAID,
    /** 기한 경과 (금액 변동 없음, 10배는 약관 문구만 — 8차) */
    OVERDUE,
    /** 미수금 납부 완료 (7차) */
    PAID
}
