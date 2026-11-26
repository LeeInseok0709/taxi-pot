package com.taxipot.domain.pot.dto;

import com.taxipot.global.code.PotStatus;

/**
 * JoinResponse — API 명세서 3.9.0 components.schemas.JoinResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record JoinResponse(
        Long potId,
        PotMemberResponse member,
        // 예치된 금액 (예상 분담금 + 수수료)
        Integer heldAmount,
        // 예치 후 사용 가능 잔액
        Integer balanceAfter,
        PotStatus potStatus
) {
}
