package com.taxipot.global.code;

/**
 * TAXIPOT_MEMBER.preferred_seat 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * <p>코드값 추가는 가능, 이름 변경·삭제 금지 (명세 '코드값(enum) 확장 정책')
 * <p>Entity 에서는 반드시 {@code @Enumerated(EnumType.STRING)} + {@code @JdbcTypeCode(SqlTypes.VARCHAR)} 로 매핑
 */
public enum PreferredSeat {
    /** 조수석 */
    FRONT,
    /** 뒷좌석 왼쪽 */
    BACK_LEFT,
    /** 뒷좌석 가운데 */
    BACK_MID,
    /** 뒷좌석 오른쪽 */
    BACK_RIGHT,
    /** 상관없음 */
    NONE
}
