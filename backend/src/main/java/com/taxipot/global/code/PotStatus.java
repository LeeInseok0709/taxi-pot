package com.taxipot.global.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * TAXIPOT.status 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * 값 이름 변경·삭제 금지. 추가는 '명세 변경 이력 기록 → 앱 반영 → 서버 반영' 순서 (변경은 기성만).
 */
@Getter
@RequiredArgsConstructor
public enum PotStatus {
    RECRUITING("모집중"),
    CLOSED("마감(매칭 확정)"),
    RUNNING("운행중 (실요금 청구·운영자 확인 대기 포함)"),
    COMPLETED("종료 (운영자 청구 승인·정산 완료)"),
    CANCELED("취소");

    private final String label;
}
