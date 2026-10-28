package com.taxipot.global.code;

/**
 * TAXIPOT.status 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * <p>코드값 추가는 가능, 이름 변경·삭제 금지 (명세 '코드값(enum) 확장 정책')
 * <p>Entity 에서는 반드시 {@code @Enumerated(EnumType.STRING)} + {@code @JdbcTypeCode(SqlTypes.VARCHAR)} 로 매핑
 */
public enum PotStatus {
    /** 모집중 */
    RECRUITING,
    /** 마감(매칭 확정) */
    CLOSED,
    /** 운행중 (실요금 청구·운영자 확인 대기 포함) */
    RUNNING,
    /** 종료 (운영자 청구 승인·정산 완료) */
    COMPLETED,
    /** 취소 */
    CANCELED
}
