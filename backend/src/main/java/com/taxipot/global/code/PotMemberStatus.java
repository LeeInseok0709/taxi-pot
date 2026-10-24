package com.taxipot.global.code;

/**
 * TAXIPOT_MEMBER.status 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * <p>코드값 추가는 가능, 이름 변경·삭제 금지 (명세 '코드값(enum) 확장 정책')
 * <p>Entity 에서는 반드시 {@code @Enumerated(EnumType.STRING)} + {@code @JdbcTypeCode(SqlTypes.VARCHAR)} 로 매핑
 */
public enum PotMemberStatus {
    /** 참여중 */
    JOINED,
    /** 참여 취소·입장 요청 철회·자동 취소 */
    CANCELED,
    /** 탑승 완료 */
    BOARDED,
    /** 노쇼(집결 기한까지 미도착 퇴장 — 자동 퇴장·기한 후 방장 내보내기) */
    NO_SHOW,
    /** 퇴장(집결 기한 전 방장 내보내기, 노쇼 아님) */
    KICKED,
    /** 입장 요청(참여 마감 후, 방장 수락 대기) (7차) */
    REQUESTED,
    /** 입장 요청 거절 (7차) */
    REJECTED
}
