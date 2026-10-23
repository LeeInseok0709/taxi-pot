package com.taxipot.settlement.dto;

import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PotStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 명세 3.9.0 스키마 BatchStatusItem (Request — @Valid로 검증).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record BatchStatusItem(
        @NotNull Long potId,
        @NotNull PotStatus potStatus,
        /** potStatus = COMPLETED면 필수 (정산까지 함께 처리, 영수증 없이 가능); null 가능 */
        Integer actualFare,
        /** 선택 */
        List<@Valid Member> members
) {

    public record Member(
            /** 선택 */
            Long memberId,
            /** 선택 */
            PotMemberStatus status
    ) {
    }
}
