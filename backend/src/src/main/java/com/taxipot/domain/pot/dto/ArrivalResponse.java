package com.taxipot.domain.pot.dto;

import java.time.OffsetDateTime;

/**
 * ArrivalResponse — API 명세서 3.9.0 components.schemas.ArrivalResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record ArrivalResponse(
        Long potId,
        OffsetDateTime arrivedAt,
        // 약속 장소(출발지)까지 계산된 거리
        Integer distanceMeters,
        // 도착 확인한 참여자 수 (방장 포함)
        Integer arrivedCount,
        // 현재 참여 인원
        Integer totalMembers,
        // 남은 참여자 전원 도착 여부
        Boolean allArrived
) {
}
