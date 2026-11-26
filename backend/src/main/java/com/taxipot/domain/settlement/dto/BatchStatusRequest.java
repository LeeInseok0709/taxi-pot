package com.taxipot.domain.settlement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * BatchStatusRequest — API 명세서 3.9.0 components.schemas.BatchStatusRequest
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record BatchStatusRequest(
        @NotNull @Valid List<BatchStatusItem> items
) {
}
