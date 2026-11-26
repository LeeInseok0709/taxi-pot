package com.taxipot.domain.auth.dto;

/**
 * AvailabilityResponse — API 명세서 3.9.0 components.schemas.AvailabilityResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record AvailabilityResponse(
        Boolean available,
        // nullable / 값: DUPLICATE, RESERVED / 사용 불가 사유 (사용 가능하면 null). RESERVED = '탈퇴회원_'으로 시작 → '이후 사용할 수 없는 닉네임입니다' (3.3.0)
        Reason reason
) {

    public enum Reason {
        DUPLICATE, RESERVED
    }
}
