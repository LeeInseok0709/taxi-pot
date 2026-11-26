package com.taxipot.global.code;

/**
 * STUDENT_VERIFICATION.review_status 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * <p>코드값 추가는 가능, 이름 변경·삭제 금지 (명세 '코드값(enum) 확장 정책')
 * <p>Entity 에서는 반드시 {@code @Enumerated(EnumType.STRING)} + {@code @JdbcTypeCode(SqlTypes.VARCHAR)} 로 매핑
 */
public enum ReviewStatus {
    /** 캡처 미제출 (7차) */
    NOT_SUBMITTED,
    /** 심사 대기 */
    PENDING,
    /** 승인 */
    APPROVED,
    /** 반려 */
    REJECTED
}
