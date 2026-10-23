package com.taxipot.pot.dto;

import com.taxipot.global.code.GenderOption;
import com.taxipot.global.code.PreferredSeat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * 명세 3.9.0 스키마 PotCreateRequest (Request — @Valid로 검증).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record PotCreateRequest(
        @NotBlank @Size(max = 100) String title,
        /** 카카오맵 핀 위치의 장소명 */
        @NotBlank @Size(max = 100) String departureName,
        @NotNull Double departureLat,
        @NotNull Double departureLng,
        @NotBlank @Size(max = 100) String destinationName,
        @NotNull Double destinationLat,
        @NotNull Double destinationLng,
        /** 현재 + 15분 이후(DEPARTURE_TIME_TOO_SOON) ~ 현재 + 24시간 이내(DEPARTURE_TIME_TOO_FAR, 3.4.0)여야 함. 집결 기한·참여 마감은 서버가 출발 5분 전으로 자동 설정 */
        @NotNull OffsetDateTime departureTime,
        @NotNull @Min(3) @Max(4) Integer maxMembers,
        /** 선택 */
        GenderOption genderOption,
        @NotNull @Min(0) @Max(500000) Integer expectedFare,
        /** 선택 */
        @Size(max = 500) String openChatUrl,
        /** 선택 */
        PreferredSeat preferredSeat
) {}
