package com.taxipot.domain.auth.dto;

/**
 * UniversityResponse — API 명세서 3.9.0 components.schemas.UniversityResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record UniversityResponse(
        Long id,
        // SignupRequest.university에 그대로 보낼 학교명
        String name,
        // 학교 이메일의 '@' 뒤 도메인
        String emailDomain
) {
}
