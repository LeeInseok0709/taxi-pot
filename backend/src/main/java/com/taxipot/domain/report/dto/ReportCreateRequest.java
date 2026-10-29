package com.taxipot.domain.report.dto;

import com.taxipot.global.code.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * ReportCreateRequest — API 명세서 3.9.0 components.schemas.ReportCreateRequest
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record ReportCreateRequest(
        @NotNull Long reportedId,
        @NotNull Long potId,
        @NotNull ReportType type,
        @NotBlank @Size(max = 255) String reason
) {
}
