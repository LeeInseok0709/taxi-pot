package com.taxipot.global.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * SETTLEMENT.debt_status 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * 값 이름 변경·삭제 금지. 추가는 '명세 변경 이력 기록 → 앱 반영 → 서버 반영' 순서 (변경은 기성만).
 */
@Getter
@RequiredArgsConstructor
public enum DebtStatus {
    NONE("미수 없음"),
    UNPAID("미수금 미납(기한 내)"),
    OVERDUE("기한 경과 (금액 변동 없음, 10배는 약관 문구만)"),
    PAID("미수금 납부 완료");

    private final String label;
}
