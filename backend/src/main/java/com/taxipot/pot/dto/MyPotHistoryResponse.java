package com.taxipot.pot.dto;

import com.taxipot.global.code.DebtStatus;
import com.taxipot.global.code.PotMemberRole;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.code.SettlementStatus;
import java.time.OffsetDateTime;

/**
 * 명세 3.9.0 스키마 MyPotHistoryResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 택시 이용 기록 (방 정보 + 내 참여·정산 결과)
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record MyPotHistoryResponse(
        Long potId,
        String title,
        String departureName,
        String destinationName,
        OffsetDateTime departureTime,
        PotStatus potStatus,
        /** null 가능 */
        Integer actualFare,
        PotMemberRole myRole,
        PotMemberStatus myStatus,
        Integer heldAmount,
        /** 정산 전·노쇼·환불 행은 null; null 가능 */
        Integer fareShare,
        /** 정산 시점 기록, 정산 전·환불 행은 null (3.3.0); null 가능 */
        Integer feeAmount,
        /** 양수: 환불 / 음수: 추가 차감 대상액; null 가능 */
        Integer diffAmount,
        /** 잔액 부족으로 못 걷은 금액 (기본 0, 미수금으로 전환) */
        Integer uncoveredAmount,
        DebtStatus debtStatus,
        /** 이 방 정산의 남은 미수 납부액 (3.3.0) */
        Integer debtAmount,
        SettlementStatus settlementStatus,
        /** 내가 이 방 동승자 평가를 마쳤는지 (3.2.0에서 rated → isRated로 이름 통일) */
        Boolean isRated
) {}
