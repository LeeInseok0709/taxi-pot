package com.taxipot.domain.user.dto;

import com.taxipot.global.code.UserStatus;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

/**
 * UserStatusUpdateRequest — API 명세서 3.9.0 components.schemas.UserStatusUpdateRequest
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record UserStatusUpdateRequest(
        // 허용 값: ACTIVE, SUSPENDED (그 외는 서비스에서 INVALID_INPUT)
        @NotNull UserStatus status,
        // 선택 / null 허용 / SUSPENDED일 때 해제 예정 시각, 미지정 시 영구 정지. 기존 정지가 더 길면 기존 값 유지 (3.3.0)
        OffsetDateTime suspendedUntil
) {
}
