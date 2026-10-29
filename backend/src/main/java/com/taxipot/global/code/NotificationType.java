package com.taxipot.global.code;

/**
 * NOTIFICATION.type 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * <p>코드값 추가는 가능, 이름 변경·삭제 금지 (명세 '코드값(enum) 확장 정책')
 * <p>Entity 에서는 반드시 {@code @Enumerated(EnumType.STRING)} + {@code @JdbcTypeCode(SqlTypes.VARCHAR)} 로 매핑
 */
public enum NotificationType {
    /** 택시팟 참여·취소·마감·자동 해산·입장 요청 처리·출발 확인 알림 */
    POT,
    /** 충전·정산·미수금·출금·실요금 청구 결과 */
    PAYMENT,
    /** 지각 경고 */
    LATE_WARNING,
    /** 퇴장·노쇼 처리 */
    KICKED,
    /** 평가 요청 */
    RATING_REQUEST,
    /** 신고 처리 결과 */
    REPORT_RESULT,
    /** 공지·시스템 */
    SYSTEM
}
