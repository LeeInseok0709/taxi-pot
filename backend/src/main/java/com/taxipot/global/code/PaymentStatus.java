package com.taxipot.global.code;

/**
 * PAYMENT.status 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * <p>코드값 추가는 가능, 이름 변경·삭제 금지 (명세 '코드값(enum) 확장 정책')
 * <p>Entity 에서는 반드시 {@code @Enumerated(EnumType.STRING)} + {@code @JdbcTypeCode(SqlTypes.VARCHAR)} 로 매핑
 */
public enum PaymentStatus {
    /** 결제 대기 */
    PENDING,
    /** 결제 완료 */
    PAID,
    /** 결제 실패 */
    FAILED,
    /** 환불 (11차 충전 환불 폐지 — 과거 행 호환용) */
    REFUNDED
}
