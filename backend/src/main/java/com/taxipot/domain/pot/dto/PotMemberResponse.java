package com.taxipot.domain.pot.dto;

import com.taxipot.global.code.PotMemberRole;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PreferredSeat;

import java.time.OffsetDateTime;

/**
 * PotMemberResponse — API 명세서 3.9.0 components.schemas.PotMemberResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record PotMemberResponse(
        // 참여 고유번호 (TAXIPOT_MEMBER.id)
        Long id,
        Long memberId,
        String nickname,
        Double ratingAvg,
        PotMemberRole role,
        PreferredSeat preferredSeat,
        PotMemberStatus status,
        // nullable
        OffsetDateTime arrivedAt,
        OffsetDateTime joinedAt,
        // nullable / 참여 취소·입장 요청 철회 시각, KICKED·NO_SHOW 퇴장 처리 시각 (3.3.0)
        OffsetDateTime canceledAt
) {
}
