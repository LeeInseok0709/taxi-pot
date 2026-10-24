package com.taxipot.global.code;

/**
 * POINT_HISTORY.type 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * <p>코드값 추가는 가능, 이름 변경·삭제 금지 (명세 '코드값(enum) 확장 정책')
 * <p>Entity 에서는 반드시 {@code @Enumerated(EnumType.STRING)} + {@code @JdbcTypeCode(SqlTypes.VARCHAR)} 로 매핑
 */
public enum PointType {
    /** 충전 */
    CHARGE,
    /** 충전 환불(전액) (11차 폐지 — 과거 행 호환용) */
    CHARGE_REFUND,
    /** 운영자 잔액 조정(시연용) (11차: 시연 충전 수단) */
    ADMIN_ADJUST,
    /** 택시비·수수료 선결제 예치 */
    FARE_HOLD,
    /** 예치 해제·환불 */
    FARE_REFUND,
    /** 실요금 초과분 추가 차감 (6차까지, 7차부터 기록 안 함) */
    FARE_ADJUST,
    /** 택시비 분담금 결제 (7차) */
    FARE_PAY,
    /** 서비스 수수료 (정산 시점에 기록) */
    FEE,
    /** 방장 정산금 입금(실요금 전액) (7차) */
    HOST_PAYOUT,
    /** 미수금 납부 (7차) */
    DEBT_PAY,
    /** 탈퇴 시 잔액 소멸 (7차) */
    BALANCE_EXPIRE,
    /** 등록 계좌로 출금 (9차) */
    WITHDRAW,
    /** 송금 실패로 출금액 복구 (9차) */
    WITHDRAW_REFUND
}
