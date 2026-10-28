package com.taxipot.global.code;

/**
 * TAXIPOT.fare_claim_status 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * <p>코드값 추가는 가능, 이름 변경·삭제 금지 (명세 '코드값(enum) 확장 정책')
 * <p>Entity 에서는 반드시 {@code @Enumerated(EnumType.STRING)} + {@code @JdbcTypeCode(SqlTypes.VARCHAR)} 로 매핑
 */
public enum FareClaimStatus {
    /** 청구 전 (9차) */
    NONE,
    /** 운영자 확인 대기 (9차) */
    PENDING,
    /** 승인 → 정산 완료 (9차) */
    APPROVED,
    /** 반려 — 방장 재청구 가능 (9차) */
    REJECTED,
    /** 탑승 시작 24시간 내 미청구 — 운영자 처리 대상 (9차) */
    OVERDUE
}
