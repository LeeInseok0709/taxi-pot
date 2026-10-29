package com.taxipot.global.code;

/**
 * WITHDRAWAL.status 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * <p>코드값 추가는 가능, 이름 변경·삭제 금지 (명세 '코드값(enum) 확장 정책')
 * <p>Entity 에서는 반드시 {@code @Enumerated(EnumType.STRING)} + {@code @JdbcTypeCode(SqlTypes.VARCHAR)} 로 매핑
 */
public enum WithdrawalStatus {
    /** 송금 처리 중 (9차) */
    REQUESTED,
    /** 송금 완료 (9차) */
    COMPLETED,
    /** 송금 실패 — 잔액 복구 (9차) */
    FAILED
}
