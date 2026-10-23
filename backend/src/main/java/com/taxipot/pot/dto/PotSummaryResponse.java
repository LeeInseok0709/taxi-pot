package com.taxipot.pot.dto;

import com.taxipot.global.code.GenderOption;
import com.taxipot.global.code.PotStatus;
import java.time.OffsetDateTime;

/**
 * 명세 3.9.0 스키마 PotSummaryResponse (Response — required 필드는 항상 값, "null 가능"만 null).
 * 지도 마커·바텀 시트 목록 공용
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record PotSummaryResponse(
        Long id,
        String title,
        Long hostId,
        String hostNickname,
        Double hostRatingAvg,
        String departureName,
        /** 지도 마커용 */
        Double departureLat,
        /** 지도 마커용 */
        Double departureLng,
        String destinationName,
        OffsetDateTime departureTime,
        Integer maxMembers,
        Integer currentMembers,
        GenderOption genderOption,
        /** 방 제목 옆 예상 금액 표시용 */
        Integer expectedFare,
        /** 참여 시 필요한 1인 예치액 = floor(expectedFare × 1.2 ÷ maxMembers) + 1,000 (잔액 부족 판단 기준, 3.7.0) */
        Integer holdAmount,
        /** 일반 참여 마감 시각 (= 집결 기한, 출발 5분 전). 이후에는 입장 요청 (3.3.0) */
        OffsetDateTime joinDeadline,
        PotStatus status,
        /** 참여자 이탈로 빈자리가 생긴 적이 있는 방. RECRUITING이면서 true면 목록 상단 노출 + 색상 구분 (방 전체 취소는 status = CANCELED로 표현) */
        Boolean hasCanceled,
        OffsetDateTime createdAt,
        /** lat·lng로 조회한 경우만 값 있음; null 가능 */
        Integer distanceMeters
) {}
