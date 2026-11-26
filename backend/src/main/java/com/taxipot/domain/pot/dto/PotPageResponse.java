package com.taxipot.domain.pot.dto;

import com.taxipot.global.response.PageMeta;

import java.util.List;

/**
 * PotPageResponse — API 명세서 3.9.0 components.schemas.PotPageResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record PotPageResponse(
        List<PotSummaryResponse> content,
        PageMeta page
) {
}
