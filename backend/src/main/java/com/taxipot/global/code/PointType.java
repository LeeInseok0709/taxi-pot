package com.taxipot.global.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * POINT_HISTORY.type 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * 값 이름 변경·삭제 금지. 추가는 '명세 변경 이력 기록 → 앱 반영 → 서버 반영' 순서 (변경은 기성만).
 */
@Getter
@RequiredArgsConstructor
public enum PointType {
    CHARGE("충전"),
    CHARGE_REFUND("충전 환불(전액)"),
    ADMIN_ADJUST("운영자 잔액 조정(시연용)"),
    FARE_HOLD("택시비·수수료 선결제 예치"),
    FARE_REFUND("예치 해제·환불"),
    FARE_ADJUST("실요금 초과분 추가 차감"),
    FARE_PAY("택시비 분담금 결제"),
    FEE("서비스 수수료 (정산 시점에 기록)"),
    HOST_PAYOUT("방장 정산금 입금(실요금 전액)"),
    DEBT_PAY("미수금 납부"),
    BALANCE_EXPIRE("탈퇴 시 잔액 소멸"),
    WITHDRAW("등록 계좌로 출금"),
    WITHDRAW_REFUND("송금 실패로 출금액 복구");

    private final String label;
}
