package com.taxipot.domain.pot.dto;

import com.taxipot.global.code.PreferredSeat;

/**
 * JoinRequest — API 명세서 3.9.0 components.schemas.JoinRequest
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record JoinRequest(
        // 선택
        PreferredSeat preferredSeat
) {
}
