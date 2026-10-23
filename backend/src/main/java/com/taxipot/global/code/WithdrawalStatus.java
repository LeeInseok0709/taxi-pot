package com.taxipot.global.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * WITHDRAWAL.status 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * 값 이름 변경·삭제 금지. 추가는 '명세 변경 이력 기록 → 앱 반영 → 서버 반영' 순서 (변경은 기성만).
 */
@Getter
@RequiredArgsConstructor
public enum WithdrawalStatus {
    REQUESTED("송금 처리 중"),
    COMPLETED("송금 완료"),
    FAILED("송금 실패");

    private final String label;
}
