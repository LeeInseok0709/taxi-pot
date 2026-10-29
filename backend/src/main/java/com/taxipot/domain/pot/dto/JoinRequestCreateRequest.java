package com.taxipot.domain.pot.dto;

import com.taxipot.global.code.PreferredSeat;
import jakarta.validation.constraints.NotNull;

/**
 * JoinRequestCreateRequest — API 명세서 3.9.0 components.schemas.JoinRequestCreateRequest
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record JoinRequestCreateRequest(
        // 현재 GPS 위도 (출발지 반경 50m 안이어야 함)
        @NotNull Double lat,
        @NotNull Double lng,
        // 선택
        PreferredSeat preferredSeat
) {
}
