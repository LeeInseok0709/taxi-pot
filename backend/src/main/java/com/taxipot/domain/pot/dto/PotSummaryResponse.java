package com.taxipot.domain.pot.dto;

import com.taxipot.global.code.GenderOption;
import com.taxipot.global.code.PotStatus;

import java.time.OffsetDateTime;

/**
 * PotSummaryResponse — API 명세서 3.9.0 components.schemas.PotSummaryResponse
 * <p>지도 마커·바텀 시트 목록 공용
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record PotSummaryResponse(
        Long id,
        String title,
        Long hostId,
        String hostNickname,
        Double hostRatingAvg,
        String departureName,
        // 지도 마커용
        Double departureLat,
        // 지도 마커용
        Double departureLng,
        String destinationName,
        OffsetDateTime departureTime,
        Integer maxMembers,
        Integer currentMembers,
        GenderOption genderOption,
        // 방 제목 옆 예상 금액 표시용
        Integer expectedFare,
        // 참여 시 필요한 1인 예치액 = floor(expectedFare × 1.2 ÷ maxMembers) + 1,000 (잔액 부족 판단 기준, 3.7.0)
        Integer holdAmount,
        // 일반 참여 마감 시각 (= 집결 기한, 출발 5분 전). 이후에는 입장 요청 (3.3.0)
        OffsetDateTime joinDeadline,
        PotStatus status,
        // 참여자 이탈로 빈자리가 생긴 적이 있는 방. RECRUITING이면서 true면 목록 상단 노출 + 색상 구분 (방 전체 취소는 status = CANCELE…
        Boolean hasCanceled,
        OffsetDateTime createdAt,
        // nullable / lat·lng로 조회한 경우만 값 있음
        Integer distanceMeters
) {
}
