package com.taxipot.domain.report.dto;

import com.taxipot.global.code.ReportStatus;
import com.taxipot.global.code.ReportType;

import java.time.OffsetDateTime;

/**
 * ReportResponse — API 명세서 3.9.0 components.schemas.ReportResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record ReportResponse(
        Long id,
        Long reportedId,
        String reportedNickname,
        Long potId,
        ReportType type,
        String reason,
        ReportStatus status,
        // nullable
        OffsetDateTime processedAt,
        OffsetDateTime createdAt
) {
}
