package com.taxipot.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxipot.global.code.DebtStatus;
import com.taxipot.global.code.PotMemberRole;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.code.SettlementStatus;

import java.time.OffsetDateTime;

/**
 * MyPotHistoryResponse — API 명세서 3.9.0 components.schemas.MyPotHistoryResponse
 * <p>택시 이용 기록 (방 정보 + 내 참여·정산 결과)
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record MyPotHistoryResponse(
        Long potId,
        String title,
        String departureName,
        String destinationName,
        OffsetDateTime departureTime,
        PotStatus potStatus,
        // nullable
        Integer actualFare,
        PotMemberRole myRole,
        PotMemberStatus myStatus,
        Integer heldAmount,
        // nullable / 정산 전·노쇼·환불 행은 null
        Integer fareShare,
        // nullable / 정산 시점 기록, 정산 전·환불 행은 null (3.3.0)
        Integer feeAmount,
        // nullable / 양수: 환불 / 음수: 추가 차감 대상액
        Integer diffAmount,
        // 잔액 부족으로 못 걷은 금액 (기본 0, 미수금으로 전환)
        Integer uncoveredAmount,
        DebtStatus debtStatus,
        // 이 방 정산의 남은 미수 납부액 (3.3.0)
        Integer debtAmount,
        SettlementStatus settlementStatus,
        // 내가 이 방 동승자 평가를 마쳤는지 (3.2.0에서 rated → isRated로 이름 통일)
        @JsonProperty("isRated") Boolean isRated
) {
}
