package com.taxipot.domain.user.dto;

import com.taxipot.global.response.PageMeta;

import java.util.List;

/**
 * MyPotHistoryPageResponse — API 명세서 3.9.0 components.schemas.MyPotHistoryPageResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record MyPotHistoryPageResponse(
        List<MyPotHistoryResponse> content,
        PageMeta page
) {
}
