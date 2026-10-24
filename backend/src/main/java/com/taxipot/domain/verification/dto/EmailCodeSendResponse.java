package com.taxipot.domain.verification.dto;

import java.time.OffsetDateTime;

/**
 * EmailCodeSendResponse — API 명세서 3.9.0 components.schemas.EmailCodeSendResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record EmailCodeSendResponse(
        Long verificationId,
        String email,
        OffsetDateTime expiresAt
) {
}
