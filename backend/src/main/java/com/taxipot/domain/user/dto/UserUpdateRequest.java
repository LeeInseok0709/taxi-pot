package com.taxipot.domain.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * UserUpdateRequest — API 명세서 3.9.0 components.schemas.UserUpdateRequest
 * <p>변경할 항목만 전달
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record UserUpdateRequest(
        // 선택 / '탈퇴회원_'으로 시작 불가 (RESERVED_NICKNAME)
        @Size(max = 30) String nickname,
        // 선택
        @Size(max = 13) @Pattern(regexp = "^01[016789]-?[0-9]{3,4}-?[0-9]{4}$") String phone,
        // 선택
        Boolean agreeMarketing
) {
}
