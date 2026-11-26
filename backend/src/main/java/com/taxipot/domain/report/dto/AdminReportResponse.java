package com.taxipot.domain.report.dto;

import com.taxipot.global.code.ReportStatus;
import com.taxipot.global.code.ReportType;

import java.time.OffsetDateTime;

/**
 * AdminReportResponse — API 명세서 3.9.0 components.schemas.AdminReportResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record AdminReportResponse(
        Long id,
        Long reporterId,
        String reporterNickname,
        Long reportedId,
        String reportedNickname,
        // 피신고자의 현재 신고 누적 횟수
        Integer reportedReportCount,
        Long potId,
        String potTitle,
        ReportType type,
        String reason,
        ReportStatus status,
        // nullable
        OffsetDateTime processedAt,
        OffsetDateTime createdAt
) {
}
