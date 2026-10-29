package com.taxipot.global.code;

/**
 * TAXIPOT.gender_option 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * <p>코드값 추가는 가능, 이름 변경·삭제 금지 (명세 '코드값(enum) 확장 정책')
 * <p>Entity 에서는 반드시 {@code @Enumerated(EnumType.STRING)} + {@code @JdbcTypeCode(SqlTypes.VARCHAR)} 로 매핑
 */
public enum GenderOption {
    /** 제한 없음 */
    ALL,
    /** 남성만 */
    MALE,
    /** 여성만 */
    FEMALE
}
