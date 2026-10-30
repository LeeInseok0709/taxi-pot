package com.taxipot.domain.pot.dto;

import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PreferredSeat;

import java.time.OffsetDateTime;

/**
 * JoinRequestResponse — API 명세서 3.9.0 components.schemas.JoinRequestResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record JoinRequestResponse(
        Long potId,
        // 수락·거절 API 경로의 userId
        Long memberId,
        String nickname,
        Double ratingAvg,
        PreferredSeat preferredSeat,
        PotMemberStatus status,
        OffsetDateTime requestedAt
) {
}
