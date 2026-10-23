package com.taxipot.global.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * TAXIPOT.fare_claim_status 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * 값 이름 변경·삭제 금지. 추가는 '명세 변경 이력 기록 → 앱 반영 → 서버 반영' 순서 (변경은 기성만).
 */
@Getter
@RequiredArgsConstructor
public enum FareClaimStatus {
    NONE("청구 전"),
    PENDING("운영자 확인 대기"),
    APPROVED("승인 → 정산 완료"),
    REJECTED("반려"),
    OVERDUE("탑승 시작 24시간 내 미청구");

    private final String label;
}
