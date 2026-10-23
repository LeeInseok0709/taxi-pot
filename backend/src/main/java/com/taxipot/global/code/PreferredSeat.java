package com.taxipot.global.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * TAXIPOT_MEMBER.preferred_seat 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * 값 이름 변경·삭제 금지. 추가는 '명세 변경 이력 기록 → 앱 반영 → 서버 반영' 순서 (변경은 기성만).
 */
@Getter
@RequiredArgsConstructor
public enum PreferredSeat {
    FRONT("조수석"),
    BACK_LEFT("뒷좌석 왼쪽"),
    BACK_MID("뒷좌석 가운데"),
    BACK_RIGHT("뒷좌석 오른쪽"),
    NONE("상관없음");

    private final String label;
}
