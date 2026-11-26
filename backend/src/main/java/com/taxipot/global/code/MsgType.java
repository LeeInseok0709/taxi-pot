package com.taxipot.global.code;

/**
 * CHAT_MESSAGE.msg_type 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * <p>코드값 추가는 가능, 이름 변경·삭제 금지 (명세 '코드값(enum) 확장 정책')
 * <p>Entity 에서는 반드시 {@code @Enumerated(EnumType.STRING)} + {@code @JdbcTypeCode(SqlTypes.VARCHAR)} 로 매핑
 */
public enum MsgType {
    /** 일반 텍스트 */
    TEXT,
    /** 원터치 퀵블록 */
    QUICK
}
