package com.taxipot.global.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * NOTIFICATION.type 코드값 (DB 설계서 13차 '코드 정의' 시트와 1:1).
 * 값 이름 변경·삭제 금지. 추가는 '명세 변경 이력 기록 → 앱 반영 → 서버 반영' 순서 (변경은 기성만).
 */
@Getter
@RequiredArgsConstructor
public enum NotificationType {
    POT("택시팟 참여·취소·마감·자동 해산·입장 요청 처리·출발 확인 알림"),
    PAYMENT("충전·정산·미수금·출금·실요금 청구 결과"),
    LATE_WARNING("지각 경고"),
    KICKED("퇴장·노쇼 처리"),
    RATING_REQUEST("평가 요청"),
    REPORT_RESULT("신고 처리 결과"),
    SYSTEM("공지·시스템");

    private final String label;
}
