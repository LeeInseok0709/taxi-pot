package com.taxipot.global.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * TAXIPOT_MEMBER.status 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * 값 이름 변경·삭제 금지. 추가는 '명세 변경 이력 기록 → 앱 반영 → 서버 반영' 순서 (변경은 기성만).
 */
@Getter
@RequiredArgsConstructor
public enum PotMemberStatus {
    JOINED("참여중"),
    CANCELED("참여 취소·입장 요청 철회·자동 취소"),
    BOARDED("탑승 완료"),
    NO_SHOW("노쇼(집결 기한까지 미도착 퇴장)"),
    KICKED("퇴장(집결 기한 전 방장 내보내기, 노쇼 아님)"),
    REQUESTED("입장 요청(참여 마감 후, 방장 수락 대기)"),
    REJECTED("입장 요청 거절");

    private final String label;
}
