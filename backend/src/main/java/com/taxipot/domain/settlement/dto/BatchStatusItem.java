package com.taxipot.domain.settlement.dto;

import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PotStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * BatchStatusItem — API 명세서 3.9.0 components.schemas.BatchStatusItem
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record BatchStatusItem(
        @NotNull Long potId,
        @NotNull PotStatus potStatus,
        // 선택 / null 허용 / potStatus = COMPLETED면 필수 (정산까지 함께 처리, 영수증 없이 가능)
        Integer actualFare,
        // 선택
        @Valid List<Member> members
) {

    public record Member(
            // 선택
            Long memberId,
            // 선택
            PotMemberStatus status
    ) {
    }
}
