package com.taxipot.domain.pot.dto;

import com.taxipot.global.code.GenderOption;
import com.taxipot.global.code.PreferredSeat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * PotCreateRequest — API 명세서 3.9.0 components.schemas.PotCreateRequest
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record PotCreateRequest(
        @NotBlank @Size(max = 100) String title,
        // 카카오맵 핀 위치의 장소명
        @NotBlank @Size(max = 100) String departureName,
        @NotNull Double departureLat,
        @NotNull Double departureLng,
        @NotBlank @Size(max = 100) String destinationName,
        @NotNull Double destinationLat,
        @NotNull Double destinationLng,
        // 현재 + 15분 이후(DEPARTURE_TIME_TOO_SOON) ~ 현재 + 24시간 이내(DEPARTURE_TIME_TOO_FAR, 3.4.0)여야 함. …
        @NotNull OffsetDateTime departureTime,
        // 기본값 4
        @NotNull @Min(3) @Max(4) Integer maxMembers,
        // 선택
        GenderOption genderOption,
        @NotNull @Min(0) @Max(500000) Integer expectedFare,
        // 선택
        @Size(max = 500) String openChatUrl,
        // 선택
        PreferredSeat preferredSeat
) {
    /** 명세 기본값: 앱이 값을 보내지 않으면(null) 기본값으로 채운다 */
    public PotCreateRequest {
        if (maxMembers == null) {
            maxMembers = 4;
        }
    }
}
